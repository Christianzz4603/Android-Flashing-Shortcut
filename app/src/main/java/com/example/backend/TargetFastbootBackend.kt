package com.example.backend

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream

data class FastbootVar(val name: String, val value: String)

/**
 * Real fastboot client speaking the fastboot protocol over USB bulk endpoints
 * (interface class 0xFF / subclass 0x42 / protocol 0x03). Nothing here is simulated.
 */
class TargetFastbootBackend(private val targetAdbBackend: TargetAdbBackend) : CommandBackend {
    override val backendType: BackendType = BackendType.TARGET_FASTBOOT

    private val mutex = Mutex()

    private class Session(
        val conn: UsbDeviceConnection,
        val iface: UsbInterface,
        val bulkIn: UsbEndpoint,
        val bulkOut: UsbEndpoint
    ) {
        fun close() {
            try {
                conn.releaseInterface(iface)
            } catch (_: Exception) {
            }
            try {
                conn.close()
            } catch (_: Exception) {
            }
        }
    }

    private class Reply(val type: String, val message: String)

    override fun isAvailable(): Boolean {
        val s = targetAdbBackend.deviceState.value
        return s.isConnected && s.isFastbootMode && targetAdbBackend.connectedUsbDevice != null
    }

    override fun getAvailabilityDetails(): String =
        if (isAvailable()) {
            "Fastboot ready on ${targetAdbBackend.deviceState.value.model} (USB)"
        } else {
            "No fastboot device. Put the target in bootloader mode, connect via USB OTG and tap USB OTG."
        }

    private fun openSession(): Session? {
        val device = targetAdbBackend.connectedUsbDevice ?: return null
        val conn = targetAdbBackend.openUsb(device) ?: return null
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == UsbConstants.USB_CLASS_VENDOR_SPEC &&
                iface.interfaceSubclass == 66 && iface.interfaceProtocol == 3
            ) {
                var inEp: UsbEndpoint? = null
                var outEp: UsbEndpoint? = null
                for (e in 0 until iface.endpointCount) {
                    val ep = iface.getEndpoint(e)
                    if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if (ep.direction == UsbConstants.USB_DIR_IN) inEp = ep else outEp = ep
                    }
                }
                if (inEp != null && outEp != null && conn.claimInterface(iface, true)) {
                    return Session(conn, iface, inEp, outEp)
                }
            }
        }
        conn.close()
        return null
    }

    private fun sendBytes(s: Session, data: ByteArray, length: Int, timeoutMs: Int = 10_000): Boolean {
        var offset = 0
        while (offset < length) {
            val n = minOf(16384, length - offset)
            val r = s.conn.bulkTransfer(s.bulkOut, data, offset, n, timeoutMs)
            if (r <= 0) return false
            offset += r
        }
        return true
    }

    /** Sends one command and reads replies until OKAY/FAIL/DATA. INFO lines are appended to [info]. */
    private fun transact(s: Session, wire: String, info: MutableList<String>, timeoutMs: Long): Reply {
        val bytes = wire.toByteArray(Charsets.UTF_8)
        if (bytes.size > 64) return Reply("FAIL", "command too long")
        if (!sendBytes(s, bytes, bytes.size)) return Reply("FAIL", "USB write failed (device disconnected?)")
        return readReply(s, info, timeoutMs)
    }

    private fun readReply(s: Session, info: MutableList<String>, timeoutMs: Long): Reply {
        val deadline = System.currentTimeMillis() + timeoutMs
        val buf = ByteArray(512)
        while (System.currentTimeMillis() < deadline) {
            val n = s.conn.bulkTransfer(s.bulkIn, buf, buf.size, 1000)
            if (n <= 0) continue
            val text = String(buf, 0, n, Charsets.UTF_8)
            when {
                text.startsWith("INFO") -> info.add(text.substring(4))
                text.startsWith("OKAY") -> return Reply("OKAY", text.substring(4))
                text.startsWith("FAIL") -> return Reply("FAIL", text.substring(4))
                text.startsWith("DATA") -> return Reply("DATA", text.substring(4))
                else -> info.add(text)
            }
        }
        return Reply("FAIL", "timed out waiting for device")
    }

    private fun toWire(tokens: List<String>): String? {
        return when (tokens[0]) {
            "getvar" -> "getvar:" + (tokens.getOrNull(1) ?: return null)
            "erase" -> "erase:" + (tokens.getOrNull(1) ?: return null)
            "set_active" -> "set_active:" + (tokens.getOrNull(1) ?: return null)
            "reboot" -> when (tokens.getOrNull(1)) {
                null -> "reboot"
                "bootloader" -> "reboot-bootloader"
                "fastboot" -> "reboot-fastboot"
                "recovery" -> "reboot-recovery"
                else -> null
            }
            "reboot-bootloader", "reboot-fastboot", "reboot-recovery", "continue" -> tokens[0]
            "oem", "flashing" -> tokens.joinToString(" ")
            else -> null
        }
    }

    private fun result(code: Int, out: String, err: String, start: Long) = CommandResult(
        exitCode = code,
        stdout = out,
        stderr = err,
        durationMs = System.currentTimeMillis() - start,
        backend = BackendType.TARGET_FASTBOOT,
        targetScope = TargetScope.TARGET
    )

    override suspend fun execute(command: String, timeoutMs: Long): CommandResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val state = targetAdbBackend.deviceState.value
        if (!isAvailable()) {
            return@withContext result(1, "", "Fastboot: no fastboot device connected over USB. ${getAvailabilityDetails()}", start)
        }
        var cmd = command.trim()
        if (cmd.startsWith("fastboot ")) cmd = cmd.removePrefix("fastboot ").trim()
        val tokens = cmd.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return@withContext result(1, "", "Empty fastboot command", start)

        if (tokens[0] == "devices") {
            return@withContext result(0, "${state.serial}\tfastboot", "", start)
        }
        if (tokens[0] == "flash") {
            return@withContext result(1, "", "Use the Flash Image control to choose the image file for a partition.", start)
        }
        val wire = toWire(tokens)
            ?: return@withContext result(1, "", "Unsupported fastboot command: $cmd", start)

        mutex.withLock {
            val session = openSession()
                ?: return@withLock result(1, "", "Could not open the USB fastboot interface (permission lost or device unplugged).", start)
            try {
                val info = mutableListOf<String>()
                val reply = transact(session, wire, info, timeoutMs.coerceAtLeast(30_000))
                val elapsed = (System.currentTimeMillis() - start) / 1000.0
                val lines = StringBuilder()
                val isGetvar = tokens[0] == "getvar"
                for (l in info) lines.append(if (isGetvar) "(bootloader) $l" else l).append('\n')
                if (reply.type == "OKAY") {
                    if (isGetvar && reply.message.isNotBlank()) lines.append("${tokens[1]}: ${reply.message}\n")
                    lines.append(String.format("OKAY [%7.3fs]", elapsed))
                    if (wire.startsWith("reboot") || wire == "continue") targetAdbBackend.disconnect()
                    result(0, lines.toString().trimEnd(), "", start)
                } else {
                    result(1, lines.toString().trimEnd(), "FAILED (${reply.message})", start)
                }
            } finally {
                session.close()
            }
        }
    }

    /** Returns the device's max-download-size in bytes, or null if it does not report one. */
    private fun maxDownloadSize(s: Session): Long? {
        val info = mutableListOf<String>()
        val r = transact(s, "getvar:max-download-size", info, 10_000)
        if (r.type != "OKAY") return null
        val v = r.message.trim().removePrefix("0x").removePrefix("0X")
        return v.toLongOrNull(16)
    }

    /** Real flash: download:<size> then stream the image over bulk OUT, then flash:<partition>. */
    suspend fun flashImage(
        partition: String,
        input: InputStream,
        size: Long,
        onProgress: (Long) -> Unit
    ): CommandResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        if (!isAvailable()) {
            return@withContext result(1, "", "Fastboot: no fastboot device connected over USB.", start)
        }
        mutex.withLock {
            val session = openSession()
                ?: return@withLock result(1, "", "Could not open the USB fastboot interface.", start)
            try {
                val log = StringBuilder()
                val max = maxDownloadSize(session)
                if (max != null && size > max) {
                    return@withLock result(
                        1, "",
                        "Image is $size bytes but the device max-download-size is $max. Sparse/split images are not supported yet.",
                        start
                    )
                }
                val info = mutableListOf<String>()
                val hex = String.format("%08x", size)
                val dl = transact(session, "download:$hex", info, 15_000)
                if (dl.type != "DATA") {
                    return@withLock result(1, "", "download rejected: ${dl.type} ${dl.message}", start)
                }
                log.append("Sending '$partition' ($size bytes)\n")
                val buf = ByteArray(16384)
                var sent = 0L
                while (sent < size) {
                    val n = input.read(buf, 0, minOf(buf.size.toLong(), size - sent).toInt())
                    if (n <= 0) return@withLock result(1, log.toString(), "Unexpected end of image after $sent bytes", start)
                    if (!sendBytes(session, buf, n, 15_000)) {
                        return@withLock result(1, log.toString(), "USB write failed after $sent bytes", start)
                    }
                    sent += n
                    onProgress(sent)
                }
                val done = readReply(session, info, 60_000)
                if (done.type != "OKAY") {
                    return@withLock result(1, log.toString(), "download failed: ${done.type} ${done.message}", start)
                }
                log.append("OKAY (download complete)\nWriting '$partition'...\n")
                val flashInfo = mutableListOf<String>()
                val fr = transact(session, "flash:$partition", flashInfo, 300_000)
                for (l in flashInfo) log.append(l).append('\n')
                if (fr.type == "OKAY") {
                    log.append(String.format("OKAY [%7.3fs]\nFinished.", (System.currentTimeMillis() - start) / 1000.0))
                    result(0, log.toString().trimEnd(), "", start)
                } else {
                    result(1, log.toString().trimEnd(), "flash failed: ${fr.message}", start)
                }
            } finally {
                session.close()
            }
        }
    }
}
