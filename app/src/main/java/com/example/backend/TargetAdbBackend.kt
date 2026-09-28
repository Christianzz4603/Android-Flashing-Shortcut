package com.example.backend

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Base64
import androidx.core.content.ContextCompat
import com.tananaev.adblib.AdbBase64
import com.tananaev.adblib.AdbConnection
import com.tananaev.adblib.AdbCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.coroutines.resume

enum class TargetTransport(val label: String) {
    NONE("Disconnected"),
    USB_OTG("USB OTG"),
    WIRELESS_ADB("Wireless ADB")
}

enum class AdbAuthState(val label: String, val isReady: Boolean) {
    DISCONNECTED("Disconnected", false),
    UNAUTHORIZED("Unauthorized (Check prompt on target phone)", false),
    OFFLINE("Device Offline", false),
    AUTHORIZED("Authorized & Connected", true)
}

data class TargetDeviceState(
    val isConnected: Boolean = false,
    val transport: TargetTransport = TargetTransport.NONE,
    val model: String = "No Target Connected",
    val serial: String = "N/A",
    val ipAddress: String = "",
    val port: Int = 5555,
    val authState: AdbAuthState = AdbAuthState.DISCONNECTED,
    val androidVersion: String = "Unknown",
    val sdkVersion: String = "Unknown",
    val buildId: String = "Unknown",
    val batteryLevel: String = "Unknown",
    val isFastbootMode: Boolean = false,
    val fastbootVariant: String = "Normal" // Normal, Fastboot
)

enum class UsbMode { ADB, FASTBOOT, OTHER }

private data class ShellOutput(val output: String, val exitCode: Int, val error: String = "")

/**
 * Real target-phone backend.
 *  - Wireless: speaks the actual ADB wire protocol (CNXN/AUTH/OPEN) via adblib over TCP.
 *  - USB: detects the device, requests Android USB permission, and hands fastboot-mode devices to TargetFastbootBackend.
 */
class TargetAdbBackend(private val context: Context) : CommandBackend {
    override val backendType: BackendType = BackendType.TARGET_ADB

    private val _deviceState = MutableStateFlow(TargetDeviceState())
    val deviceState: StateFlow<TargetDeviceState> = _deviceState.asStateFlow()

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val sessionMutex = Mutex()

    @Volatile private var adbConnection: AdbConnection? = null
    @Volatile private var adbSocket: Socket? = null

    @Volatile
    var connectedUsbDevice: UsbDevice? = null
        private set

    private val base64 = object : AdbBase64 {
        override fun encodeToString(data: ByteArray): String = Base64.encodeToString(data, Base64.NO_WRAP)
    }

    override fun isAvailable(): Boolean {
        val s = _deviceState.value
        return adbConnection != null && s.isConnected && s.authState.isReady
    }

    override fun getAvailabilityDetails(): String {
        val state = _deviceState.value
        return when {
            isAvailable() -> "Target connected via ${state.transport.label}: ${state.model} [${state.authState.label}]"
            state.isConnected && state.isFastbootMode -> "Target in fastboot mode over USB (use Fastboot tools)."
            else -> "No target ADB session. Use Wireless ADB (adb tcpip 5555) or connect a fastboot-mode phone over USB OTG."
        }
    }

    // ---------------------------------------------------------------- USB

    suspend fun scanUsbDevices(): List<UsbDevice> = withContext(Dispatchers.IO) {
        usbManager.deviceList.values.filter { classifyUsbDevice(it) != UsbMode.OTHER }
    }

    fun classifyUsbDevice(device: UsbDevice): UsbMode {
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == UsbConstants.USB_CLASS_VENDOR_SPEC && iface.interfaceSubclass == 66) {
                when (iface.interfaceProtocol) {
                    3 -> return UsbMode.FASTBOOT
                    1 -> return UsbMode.ADB
                }
            }
        }
        return UsbMode.OTHER
    }

    fun openUsb(device: UsbDevice): UsbDeviceConnection? = try {
        usbManager.openDevice(device)
    } catch (_: Exception) {
        null
    }

    /** Shows Android's USB permission dialog (if needed) and waits for the answer. */
    suspend fun ensureUsbPermission(device: UsbDevice): Boolean {
        if (usbManager.hasPermission(device)) return true
        val action = "${context.packageName}.USB_PERMISSION"
        return withTimeoutOrNull(60_000) {
            suspendCancellableCoroutine<Boolean> { cont ->
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(c: Context, intent: Intent) {
                        if (intent.action == action) {
                            try {
                                context.unregisterReceiver(this)
                            } catch (_: Exception) {
                            }
                            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                            if (cont.isActive) cont.resume(granted)
                        }
                    }
                }
                ContextCompat.registerReceiver(
                    context, receiver, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED
                )
                val flags = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
                val pi = PendingIntent.getBroadcast(
                    context, 0, Intent(action).setPackage(context.packageName), flags
                )
                cont.invokeOnCancellation {
                    try {
                        context.unregisterReceiver(receiver)
                    } catch (_: Exception) {
                    }
                }
                usbManager.requestPermission(device, pi)
            }
        } ?: false
    }

    suspend fun connectUsb(device: UsbDevice): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!ensureUsbPermission(device)) {
            return@withContext Pair(false, "USB permission was denied for the target device.")
        }
        val name = try {
            device.productName ?: "Android device"
        } catch (_: Exception) {
            "Android device"
        }
        val serial = try {
            device.serialNumber ?: device.deviceName
        } catch (_: Exception) {
            device.deviceName
        }
        when (classifyUsbDevice(device)) {
            UsbMode.FASTBOOT -> {
                closeAdbSession()
                connectedUsbDevice = device
                _deviceState.value = TargetDeviceState(
                    isConnected = true,
                    transport = TargetTransport.USB_OTG,
                    model = name,
                    serial = serial,
                    authState = AdbAuthState.AUTHORIZED,
                    isFastbootMode = true,
                    fastbootVariant = "Fastboot"
                )
                Pair(true, "Fastboot device connected over USB: $name")
            }
            UsbMode.ADB -> Pair(
                false,
                "$name is in normal Android (ADB) mode. USB ADB is not supported yet: use Wireless ADB (adb tcpip 5555) " +
                    "or reboot the phone to bootloader (fastboot) mode and rescan."
            )
            UsbMode.OTHER -> Pair(false, "That USB device does not expose an ADB or fastboot interface.")
        }
    }

    // ---------------------------------------------------------------- Wireless ADB

    private fun loadOrCreateCrypto(): AdbCrypto {
        val priv = File(context.filesDir, "adbkey")
        val pub = File(context.filesDir, "adbkey.pub")
        if (priv.exists() && pub.exists()) {
            try {
                return AdbCrypto.loadAdbKeyPair(base64, priv, pub)
            } catch (_: Exception) {
            }
        }
        val crypto = AdbCrypto.generateAdbKeyPair(base64)
        try {
            crypto.saveAdbKeyPair(priv, pub)
        } catch (_: Exception) {
        }
        return crypto
    }

    private fun closeAdbSession() {
        try {
            adbConnection?.close()
        } catch (_: Exception) {
        }
        try {
            adbSocket?.close()
        } catch (_: Exception) {
        }
        adbConnection = null
        adbSocket = null
    }

    suspend fun connectWirelessAdb(ip: String, port: Int = 5555): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val host = ip.trim()
        if (host.isBlank()) return@withContext Pair(false, "IP address cannot be empty")
        closeAdbSession()
        connectedUsbDevice = null
        val socket = Socket()
        try {
            socket.connect(InetSocketAddress(host, port), 4000)
            socket.tcpNoDelay = true
            val crypto = loadOrCreateCrypto()
            val conn = AdbConnection.create(socket, crypto)
            _deviceState.value = TargetDeviceState(
                isConnected = false,
                transport = TargetTransport.WIRELESS_ADB,
                model = "Waiting for authorization on target...",
                serial = "$host:$port",
                ipAddress = host,
                port = port,
                authState = AdbAuthState.UNAUTHORIZED
            )
            coroutineScope {
                val watchdog = launch {
                    delay(45_000)
                    try {
                        socket.close()
                    } catch (_: Exception) {
                    }
                }
                try {
                    conn.connect()
                } finally {
                    watchdog.cancel()
                }
            }
            adbConnection = conn
            adbSocket = socket
            _deviceState.value = _deviceState.value.copy(
                isConnected = true,
                model = "Target ($host)",
                authState = AdbAuthState.AUTHORIZED
            )
            Pair(true, "Connected to $host:$port")
        } catch (e: Exception) {
            try {
                socket.close()
            } catch (_: Exception) {
            }
            adbConnection = null
            adbSocket = null
            _deviceState.value = TargetDeviceState()
            Pair(
                false,
                "Connection to $host:$port failed: ${e.localizedMessage ?: e.javaClass.simpleName}. " +
                    "The target must run plain ADB over TCP (adb tcpip 5555) and you must tap Allow on it. " +
                    "Android 11+ 'Wireless debugging' (TLS pairing) is not supported."
            )
        }
    }

    fun disconnect() {
        closeAdbSession()
        connectedUsbDevice = null
        _deviceState.value = TargetDeviceState()
    }

    // ---------------------------------------------------------------- Shell execution

    private fun normalize(command: String): String {
        var c = command.trim()
        if (c.startsWith("adb ")) c = c.removePrefix("adb ").trim()
        if (c.startsWith("shell ")) c = c.removePrefix("shell ").trim()
        return c
    }

    private fun q(s: String): String = "'" + s.replace("'", "'\\''") + "'"

    private suspend fun runShell(conn: AdbConnection, cmd: String, timeoutMs: Long): ShellOutput =
        withContext(Dispatchers.IO) {
            val marker = "__AFS_EXIT__"
            val sb = StringBuilder()
            val stream = try {
                conn.open("shell:$cmd; echo $marker\$?")
            } catch (e: Exception) {
                return@withContext ShellOutput("", 1, "Failed to open shell stream: ${e.localizedMessage}")
            }
            var timedOut = false
            try {
                val finished = withTimeoutOrNull(timeoutMs) {
                    runInterruptible {
                        try {
                            while (true) {
                                val chunk = stream.read()
                                sb.append(String(chunk, Charsets.UTF_8))
                                if (sb.length > 2_000_000) break
                            }
                        } catch (_: IOException) {
                        } catch (_: InterruptedException) {
                        }
                    }
                    true
                }
                if (finished == null) timedOut = true
            } finally {
                try {
                    stream.close()
                } catch (_: Exception) {
                }
            }
            val raw = sb.toString().replace("\r\n", "\n").replace("\r", "\n")
            val idx = raw.lastIndexOf(marker)
            var out = raw
            var exit = 0
            if (idx >= 0) {
                out = raw.substring(0, idx)
                exit = raw.substring(idx + marker.length).trim().takeWhile { it.isDigit() }.toIntOrNull() ?: 0
            } else if (timedOut) {
                exit = -1
            }
            ShellOutput(out, exit, if (timedOut) "Command timed out after ${timeoutMs}ms" else "")
        }

    override suspend fun execute(command: String, timeoutMs: Long): CommandResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        fun result(code: Int, out: String, err: String) = CommandResult(
            exitCode = code,
            stdout = out,
            stderr = err,
            durationMs = System.currentTimeMillis() - start,
            backend = BackendType.TARGET_ADB,
            targetScope = TargetScope.TARGET
        )

        val state = _deviceState.value
        if (!state.isConnected) {
            return@withContext result(1, "", "Target ADB: no target connected. Use Wireless ADB first.")
        }
        val conn = adbConnection ?: return@withContext result(
            1, "",
            "Target ADB: no active ADB session (a USB fastboot device only supports the Fastboot tools)."
        )
        val cmd = normalize(command)
        if (cmd.isBlank()) return@withContext result(1, "", "Empty command")

        val out = sessionMutex.withLock { runShell(conn, cmd, timeoutMs) }
        if (cmd.startsWith("reboot") && out.exitCode == 0) {
            disconnect()
        }
        result(out.exitCode, out.output.trimEnd(), out.error)
    }

    /** Reads real device properties from the target with a single shell round trip. */
    suspend fun fetchDeviceProps(): Map<String, String> = withContext(Dispatchers.IO) {
        val conn = adbConnection ?: return@withContext emptyMap()
        val script = listOf(
            "echo model=\$(getprop ro.product.model)",
            "echo manufacturer=\$(getprop ro.product.manufacturer)",
            "echo release=\$(getprop ro.build.version.release)",
            "echo sdk=\$(getprop ro.build.version.sdk)",
            "echo build=\$(getprop ro.build.display.id)",
            "echo patch=\$(getprop ro.build.version.security_patch)",
            "echo abi=\$(getprop ro.product.cpu.abi)",
            "echo kernel=\$(uname -r)",
            "echo slot=\$(getprop ro.boot.slot_suffix)",
            "echo ab=\$(getprop ro.build.ab_update)",
            "echo initboot=\$(ls /dev/block/by-name 2>/dev/null | grep -c init_boot)",
            "echo battery=\$(dumpsys battery | grep -m1 'level:' | sed 's/.*: *//')",
            "echo mem=\$(grep MemTotal /proc/meminfo)",
            "echo df=\$(df -k /data | tail -n 1)",
            "echo locked=\$(getprop ro.boot.flash.locked)"
        ).joinToString("; ")
        val res = sessionMutex.withLock { runShell(conn, script, 20_000) }
        val map = mutableMapOf<String, String>()
        for (line in res.output.lines()) {
            val i = line.indexOf('=')
            if (i > 0) map[line.substring(0, i).trim()] = line.substring(i + 1).trim()
        }
        if (map.isNotEmpty()) {
            _deviceState.value = _deviceState.value.copy(
                model = map["model"].orEmpty().ifBlank { _deviceState.value.model },
                androidVersion = "Android " + map["release"].orEmpty(),
                sdkVersion = "API " + map["sdk"].orEmpty(),
                buildId = map["build"].orEmpty(),
                batteryLevel = map["battery"].orEmpty().let { if (it.isBlank()) "Unknown" else "$it%" }
            )
        }
        map
    }

    // ---------------------------------------------------------------- File transfer (real, over the ADB session)

    /** Copies a local stream to the target using base64 chunks over shell (works without the sync service). */
    suspend fun pushFile(
        input: InputStream,
        remotePath: String,
        onProgress: (Long) -> Unit
    ): CommandResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        fun result(code: Int, out: String, err: String) = CommandResult(
            exitCode = code, stdout = out, stderr = err,
            durationMs = System.currentTimeMillis() - start,
            backend = BackendType.TARGET_ADB, targetScope = TargetScope.TARGET
        )
        val conn = adbConnection ?: return@withContext result(1, "", "No active ADB session")
        sessionMutex.withLock {
            val dir = remotePath.substringBeforeLast('/', "")
            val prep = runShell(
                conn,
                (if (dir.isNotEmpty()) "mkdir -p ${q(dir)} && " else "") + ": > ${q(remotePath)}",
                15_000
            )
            if (prep.exitCode != 0) {
                return@withLock result(prep.exitCode, "", "Cannot create ${remotePath}: ${prep.output.trim()}")
            }
            val buf = ByteArray(2400)
            var sent = 0L
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                val b64 = Base64.encodeToString(buf, 0, n, Base64.NO_WRAP)
                val r = runShell(conn, "echo $b64 | base64 -d >> ${q(remotePath)}", 20_000)
                if (r.exitCode != 0) {
                    return@withLock result(r.exitCode, "", "Write failed after $sent bytes: ${r.output.trim()}")
                }
                sent += n
                onProgress(sent)
            }
            val check = runShell(conn, "wc -c < ${q(remotePath)}", 15_000)
            val remoteSize = check.output.trim().toLongOrNull()
            if (remoteSize != sent) {
                result(1, "", "Size mismatch: sent $sent bytes, target has ${remoteSize ?: "unknown"}")
            } else {
                result(0, "Pushed $sent bytes to $remotePath", "")
            }
        }
    }

    /** Streams a file from the target into [out] using the raw exec service and verifies its size. */
    suspend fun pullFile(remotePath: String, out: OutputStream): CommandResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        fun result(code: Int, o: String, err: String) = CommandResult(
            exitCode = code, stdout = o, stderr = err,
            durationMs = System.currentTimeMillis() - start,
            backend = BackendType.TARGET_ADB, targetScope = TargetScope.TARGET
        )
        val conn = adbConnection ?: return@withContext result(1, "", "No active ADB session")
        sessionMutex.withLock {
            val sizeRes = runShell(conn, "[ -f ${q(remotePath)} ] && wc -c < ${q(remotePath)}", 15_000)
            val expected = sizeRes.output.trim().toLongOrNull()
                ?: return@withLock result(1, "", "Remote file not found: $remotePath")
            val stream = try {
                conn.open("exec:cat ${q(remotePath)}")
            } catch (e: Exception) {
                return@withLock result(1, "", "Could not open exec stream: ${e.localizedMessage}")
            }
            var received = 0L
            try {
                while (true) {
                    val chunk = stream.read()
                    out.write(chunk)
                    received += chunk.size
                }
            } catch (_: IOException) {
            } finally {
                try {
                    stream.close()
                } catch (_: Exception) {
                }
            }
            out.flush()
            if (received != expected) {
                result(1, "", "Size mismatch: expected $expected bytes, received $received")
            } else {
                result(0, "Pulled $received bytes from $remotePath", "")
            }
        }
    }

    fun updateTargetState(updater: (TargetDeviceState) -> TargetDeviceState) {
        _deviceState.value = updater(_deviceState.value)
    }
}
