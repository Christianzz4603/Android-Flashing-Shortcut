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
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Base64
import androidx.core.content.ContextCompat
import io.github.muntashirakon.adb.AdbStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
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
    val fastbootVariant: String = "Normal", // Normal, Fastboot
    val isPaired: Boolean = false,
    val isReconnecting: Boolean = false
)

enum class UsbMode { ADB, FASTBOOT, OTHER }

private data class ShellOutput(val output: String, val exitCode: Int, val error: String = "")

/**
 * Real target-phone backend.
 *  - Wireless: real ADB wireless-debugging client (pair once with the 6-digit code, then "Start"
 *    auto-discovers the target over mDNS and connects) built on libadb-android + Conscrypt, the
 *    same approach Shizuku's own wireless-debugging starter uses. A network callback keeps
 *    retrying the connection whenever this phone's Wi-Fi network changes, instead of staying
 *    stuck pointed at a now-unreachable address.
 *  - USB: detects the device, requests Android USB permission, and hands fastboot-mode devices
 *    to TargetFastbootBackend. Plain USB ADB (normal Android mode) is not supported yet.
 */
class TargetAdbBackend(private val context: Context) : CommandBackend {
    override val backendType: BackendType = BackendType.TARGET_ADB

    private val _deviceState = MutableStateFlow(TargetDeviceState())
    val deviceState: StateFlow<TargetDeviceState> = _deviceState.asStateFlow()

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val sessionMutex = Mutex()
    private val backendScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val adbManager: AdbKeyManager by lazy { AdbKeyManager.getInstance(context) }

    @Volatile private var wirelessAutoReconnect = false
    @Volatile private var networkCallback: ConnectivityManager.NetworkCallback? = null

    @Volatile
    var connectedUsbDevice: UsbDevice? = null
        private set

    override fun isAvailable(): Boolean {
        val s = _deviceState.value
        return s.isConnected && s.authState.isReady && !s.isFastbootMode
    }

    override fun getAvailabilityDetails(): String {
        val state = _deviceState.value
        return when {
            isAvailable() -> "Target connected via ${state.transport.label}: ${state.model} [${state.authState.label}]"
            state.isReconnecting -> "Wi-Fi changed - reconnecting to the target automatically..."
            state.isConnected && state.isFastbootMode -> "Target in fastboot mode over USB (use Fastboot tools)."
            else -> "No target ADB session. Pair once over Wireless debugging, then tap Start, or connect a fastboot-mode phone over USB OTG."
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
                stopWirelessKeepAlive()
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
                "$name is in normal Android (ADB) mode. USB ADB is not supported yet: pair it once over " +
                    "Wireless debugging instead, or reboot it to bootloader (fastboot) mode and rescan."
            )
            UsbMode.OTHER -> Pair(false, "That USB device does not expose an ADB or fastboot interface.")
        }
    }

    // ---------------------------------------------------------------- Wireless ADB (pair + start, Shizuku-style)

    /** Step 1: pair once using the 6-digit code from the target's "Pair device with pairing code" screen. */
    suspend fun pairWireless(host: String, port: Int, pairingCode: String): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            val h = host.trim()
            val code = pairingCode.trim()
            if (h.isBlank() || code.isBlank()) return@withContext Pair(false, "Enter the pairing IP:port and the 6-digit code.")
            try {
                val ok = adbManager.pair(h, port, code)
                if (ok) {
                    Pair(true, "Paired with $h. Now tap Start to connect.")
                } else {
                    Pair(false, "Pairing rejected. Double check the code and the pairing port (it changes each time you open that screen).")
                }
            } catch (e: Exception) {
                Pair(false, "Pairing failed: ${e.localizedMessage ?: e.javaClass.simpleName}")
            }
        }

    /**
     * Step 2: connect. With no host given this auto-discovers the target over mDNS on the current
     * Wi-Fi network (Shizuku's "Start" behavior) - the target must already be paired and its
     * "Wireless debugging" toggle on. With a host/port given, it dials that address directly
     * (useful for plain `adb tcpip 5555` on older Android or devices where mDNS discovery fails).
     */
    suspend fun startWireless(host: String? = null, port: Int? = null): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            stopWirelessKeepAlive()
            connectedUsbDevice = null
            _deviceState.value = TargetDeviceState(
                isConnected = false,
                transport = TargetTransport.WIRELESS_ADB,
                model = "Connecting...",
                authState = AdbAuthState.UNAUTHORIZED
            )
            val outcome = attemptConnect(host, port)
            if (outcome.first) {
                wirelessAutoReconnect = true
                startWirelessKeepAlive(host, port)
            } else {
                _deviceState.value = TargetDeviceState()
            }
            outcome
        }

    private fun verifyLiveShell(): Boolean = try {
        val stream = adbManager.openStream("shell:echo afs_ok")
        val text = readAllText(stream, 8_000)
        try {
            stream.close()
        } catch (_: Exception) {
        }
        text.contains("afs_ok")
    } catch (_: Exception) {
        false
    }

    private fun attemptConnect(host: String?, port: Int?): Pair<Boolean, String> {
        return try {
            if (!host.isNullOrBlank()) {
                @Suppress("UNUSED_EXPRESSION")
                adbManager.connect(host.trim(), port ?: 5555)
            } else {
                @Suppress("UNUSED_EXPRESSION")
                adbManager.autoConnect(context, 15_000L)
            }
            if (verifyLiveShell()) {
                _deviceState.value = _deviceState.value.copy(
                    isConnected = true,
                    transport = TargetTransport.WIRELESS_ADB,
                    model = if (!host.isNullOrBlank()) "Target ($host)" else "Target (auto-discovered)",
                    ipAddress = host.orEmpty(),
                    authState = AdbAuthState.AUTHORIZED,
                    isPaired = true,
                    isReconnecting = false
                )
                Pair(true, "Connected" + if (!host.isNullOrBlank()) " to $host" else " (auto-discovered on this Wi-Fi network)")
            } else {
                Pair(
                    false,
                    "Could not confirm a live shell. Make sure Wireless debugging is on and this app is " +
                        "already paired (tap \"Pair new device\" first), and that both phones are on the same Wi-Fi network."
                )
            }
        } catch (e: Exception) {
            Pair(false, "Connect failed: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }

    /** Watches for Wi-Fi network changes and silently redials, like Shizuku keeping its session alive across networks. */
    private fun startWirelessKeepAlive(host: String?, port: Int?) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (!wirelessAutoReconnect) return
                backendScope.launch {
                    if (_deviceState.value.isConnected && verifyLiveShell()) return@launch
                    _deviceState.value = _deviceState.value.copy(isConnected = false, isReconnecting = true)
                    delay(2_500) // let DHCP / mDNS settle on the new network
                    repeat(4) { attempt ->
                        if (!wirelessAutoReconnect) return@launch
                        val (ok, _) = withContext(Dispatchers.IO) { attemptConnect(host, port) }
                        if (ok) return@launch
                        delay(3_000L * (attempt + 1))
                    }
                    if (wirelessAutoReconnect) {
                        _deviceState.value = _deviceState.value.copy(isReconnecting = false)
                    }
                }
            }

            override fun onLost(network: Network) {
                if (wirelessAutoReconnect) {
                    _deviceState.value = _deviceState.value.copy(isConnected = false, isReconnecting = true)
                }
            }
        }
        networkCallback = callback
        try {
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            connectivityManager.registerNetworkCallback(request, callback)
        } catch (_: Exception) {
        }
    }

    private fun stopWirelessKeepAlive() {
        wirelessAutoReconnect = false
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (_: Exception) {
            }
        }
        networkCallback = null
    }

    fun disconnect() {
        stopWirelessKeepAlive()
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

    private fun readAllText(stream: AdbStream, timeoutMillis: Long): String {
        val input = stream.openInputStream()
        val buf = ByteArray(8192)
        val out = java.io.ByteArrayOutputStream()
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            val n = try {
                input.read(buf)
            } catch (_: IOException) {
                -1
            }
            if (n < 0) break
            if (n > 0) out.write(buf, 0, n)
        }
        return out.toString("UTF-8")
    }

    private suspend fun runShell(cmd: String, timeoutMs: Long): ShellOutput = withContext(Dispatchers.IO) {
        val marker = "__AFS_EXIT__"
        val stream = try {
            adbManager.openStream("shell:$cmd; echo $marker\$?")
        } catch (e: Exception) {
            return@withContext ShellOutput("", 1, "Failed to open shell stream: ${e.localizedMessage}")
        }
        var timedOut = false
        var raw = ""
        try {
            val finished = withTimeoutOrNull(timeoutMs) {
                raw = readAllText(stream, timeoutMs)
                true
            }
            if (finished == null) timedOut = true
        } finally {
            try {
                stream.close()
            } catch (_: Exception) {
            }
        }
        val normalized = raw.replace("\r\n", "\n").replace("\r", "\n")
        val idx = normalized.lastIndexOf(marker)
        var out = normalized
        var exit = 0
        if (idx >= 0) {
            out = normalized.substring(0, idx)
            exit = normalized.substring(idx + marker.length).trim().takeWhile { it.isDigit() }.toIntOrNull() ?: 0
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
        if (!state.isConnected || state.isFastbootMode) {
            return@withContext result(1, "", "Target ADB: no target connected. Pair once, then tap Start.")
        }
        val cmd = normalize(command)
        if (cmd.isBlank()) return@withContext result(1, "", "Empty command")

        val out = sessionMutex.withLock { runShell(cmd, timeoutMs) }
        if (cmd.startsWith("reboot") && out.exitCode == 0) {
            disconnect()
        }
        result(out.exitCode, out.output.trimEnd(), out.error)
    }

    /** Reads real device properties from the target with a single shell round trip. */
    suspend fun fetchDeviceProps(): Map<String, String> = withContext(Dispatchers.IO) {
        if (!_deviceState.value.isConnected) return@withContext emptyMap()
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
        val res = sessionMutex.withLock { runShell(script, 20_000) }
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
        if (!_deviceState.value.isConnected) return@withContext result(1, "", "No active ADB session")
        sessionMutex.withLock {
            val dir = remotePath.substringBeforeLast('/', "")
            val prep = runShell(
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
                val r = runShell("echo $b64 | base64 -d >> ${q(remotePath)}", 20_000)
                if (r.exitCode != 0) {
                    return@withLock result(r.exitCode, "", "Write failed after $sent bytes: ${r.output.trim()}")
                }
                sent += n
                onProgress(sent)
            }
            val check = runShell("wc -c < ${q(remotePath)}", 15_000)
            val remoteSize = check.output.trim().toLongOrNull()
            if (remoteSize != sent) {
                result(1, "", "Size mismatch: sent $sent bytes, target has ${remoteSize ?: "unknown"}")
            } else {
                result(0, "Pushed $sent bytes to $remotePath", "")
            }
        }
    }

    suspend fun pullFile(remotePath: String, out: OutputStream): CommandResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        fun result(code: Int, o: String, err: String) = CommandResult(
            exitCode = code, stdout = o, stderr = err,
            durationMs = System.currentTimeMillis() - start,
            backend = BackendType.TARGET_ADB, targetScope = TargetScope.TARGET
        )
        if (!_deviceState.value.isConnected) return@withContext result(1, "", "No active ADB session")
        sessionMutex.withLock {
            val sizeRes = runShell("[ -f ${q(remotePath)} ] && wc -c < ${q(remotePath)}", 15_000)
            val expected = sizeRes.output.trim().toLongOrNull()
                ?: return@withLock result(1, "", "Remote file not found: $remotePath")
            val stream = try {
                adbManager.openStream("exec:cat ${q(remotePath)}")
            } catch (e: Exception) {
                return@withLock result(1, "", "Could not open exec stream: ${e.localizedMessage}")
            }
            var received = 0L
            try {
                val input = stream.openInputStream()
                val buf = ByteArray(8192)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    if (n > 0) {
                        out.write(buf, 0, n)
                        received += n
                    }
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
