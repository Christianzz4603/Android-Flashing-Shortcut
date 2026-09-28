package com.example.ui

import android.app.Application
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.backend.BackendRegistry
import com.example.backend.BackendType
import com.example.backend.CommandResult
import com.example.backend.CommandSpec
import com.example.backend.TargetDeviceState
import com.example.backend.TargetScope
import com.example.data.db.AfsDatabase
import com.example.data.model.CommandHistoryEntity
import com.example.data.model.ShortcutEntity
import com.example.data.repository.AfsRepository
import com.example.model.AppInfoItem
import com.example.model.FileItem
import com.example.model.HostDeviceInfo
import com.example.model.ProcessItem
import com.example.model.TargetDeviceInfo
import com.example.utils.DeviceScanner
import com.example.utils.LogcatReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class MainTab(val label: String, val icon: String) {
    HOME("Home", "\uD83C\uDFE0"),
    THIS_PHONE("ADB Tools", "\uD83D\uDCF1"),
    ANOTHER_PHONE("Fastboot Tools", "\uD83D\uDCF2"),
    APP_MANAGEMENT("App Management", "\uD83E\uDDE9"),
    SETTINGS("Settings", "\u2699"),
    ABOUT("About", "\u2139"),
    UPDATES("Updates", "\uD83D\uDD04")
}

data class DestructiveAction(
    val title: String,
    val target: String,
    val partition: String,
    val fileOrDetails: String,
    val operation: String,
    val onConfirm: () -> Unit
)

data class FlashProgress(val label: String, val sent: Long, val total: Long)

class AfsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AfsDatabase.getDatabase(application)
    val backendRegistry = BackendRegistry(application)
    val repository = AfsRepository(db.shortcutDao(), db.historyDao(), backendRegistry)

    val shortcuts: StateFlow<List<ShortcutEntity>> = repository.allShortcuts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val commandHistory: StateFlow<List<CommandHistoryEntity>> = repository.recentHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current navigation tab
    private val _currentTab = MutableStateFlow(MainTab.HOME)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    // Host telemetry
    private val _hostDeviceInfo = MutableStateFlow<HostDeviceInfo?>(null)
    val hostDeviceInfo: StateFlow<HostDeviceInfo?> = _hostDeviceInfo.asStateFlow()

    // Target telemetry & connection
    val targetDeviceState: StateFlow<TargetDeviceState> = backendRegistry.targetAdb.deviceState

    private val _targetDeviceInfo = MutableStateFlow(TargetDeviceInfo())
    val targetDeviceInfo: StateFlow<TargetDeviceInfo> = _targetDeviceInfo.asStateFlow()

    private val _targetPackages = MutableStateFlow<List<String>>(emptyList())
    val targetPackages: StateFlow<List<String>> = _targetPackages.asStateFlow()

    private val _flashProgress = MutableStateFlow<FlashProgress?>(null)
    val flashProgress: StateFlow<FlashProgress?> = _flashProgress.asStateFlow()

    // Apps
    private val _installedApps = MutableStateFlow<List<AppInfoItem>>(emptyList())
    val installedApps: StateFlow<List<AppInfoItem>> = _installedApps.asStateFlow()
    private val _appSearchQuery = MutableStateFlow("")
    val appSearchQuery: StateFlow<String> = _appSearchQuery.asStateFlow()
    private val _showSystemApps = MutableStateFlow(false)
    val showSystemApps: StateFlow<Boolean> = _showSystemApps.asStateFlow()

    // Files
    private val _currentFilePath = MutableStateFlow(Environment.getExternalStorageDirectory().path)
    val currentFilePath: StateFlow<String> = _currentFilePath.asStateFlow()
    private val _fileItems = MutableStateFlow<List<FileItem>>(emptyList())
    val fileItems: StateFlow<List<FileItem>> = _fileItems.asStateFlow()

    // Processes
    private val _processItems = MutableStateFlow<List<ProcessItem>>(emptyList())
    val processItems: StateFlow<List<ProcessItem>> = _processItems.asStateFlow()

    // Logs
    val logcatReader = LogcatReader()
    val logs = logcatReader.logs

    // Active command result modal
    private val _lastCommandResult = MutableStateFlow<CommandResult?>(null)
    val lastCommandResult: StateFlow<CommandResult?> = _lastCommandResult.asStateFlow()

    private val _showResultDialog = MutableStateFlow(false)
    val showResultDialog: StateFlow<Boolean> = _showResultDialog.asStateFlow()

    // Destructive confirmation dialog
    private val _destructiveDialog = MutableStateFlow<DestructiveAction?>(null)
    val destructiveDialog: StateFlow<DestructiveAction?> = _destructiveDialog.asStateFlow()

    // Status badges
    private val _isAdbAvailable = MutableStateFlow(false)
    val isAdbAvailable: StateFlow<Boolean> = _isAdbAvailable.asStateFlow()

    private val _isShizukuAvailable = MutableStateFlow(false)
    val isShizukuAvailable: StateFlow<Boolean> = _isShizukuAvailable.asStateFlow()

    private val _isRootAvailable = MutableStateFlow(false)
    val isRootAvailable: StateFlow<Boolean> = _isRootAvailable.asStateFlow()

    // Settings
    val confirmDestructive = MutableStateFlow(true)
    val defaultHostBackend = MutableStateFlow(BackendType.LOCAL_SHELL)
    val defaultTargetPort = MutableStateFlow(5555)

    // Remote screen screenshot preview
    private val _targetScreenshotTimestamp = MutableStateFlow<Long?>(null)
    val targetScreenshotTimestamp: StateFlow<Long?> = _targetScreenshotTimestamp.asStateFlow()

    init {
        refreshHostInfo()
        refreshCapabilities()
        loadApps()
        loadFiles(_currentFilePath.value)
        loadProcesses()
        refreshLogs()
        seedDefaultShortcutsIfEmpty()
    }

    fun setTab(tab: MainTab) {
        _currentTab.value = tab
    }

    fun refreshCapabilities() {
        backendRegistry.shizuku.requestPermissionIfNeeded()
        _isAdbAvailable.value = backendRegistry.adb.isAvailable()
        _isShizukuAvailable.value = backendRegistry.shizuku.isAvailable()
        _isRootAvailable.value = backendRegistry.root.isAvailable()
    }

    fun refreshHostInfo() {
        viewModelScope.launch {
            _hostDeviceInfo.value = withContext(Dispatchers.IO) { DeviceScanner.getHostDeviceInfo(getApplication()) }
        }
    }

    fun loadApps() {
        viewModelScope.launch {
            _installedApps.value = DeviceScanner.getInstalledApps(getApplication())
        }
    }

    fun setAppSearchQuery(query: String) {
        _appSearchQuery.value = query
    }

    fun toggleSystemApps() {
        _showSystemApps.value = !_showSystemApps.value
    }

    fun loadFiles(path: String) {
        viewModelScope.launch {
            _currentFilePath.value = path
            _fileItems.value = DeviceScanner.listFiles(path)
        }
    }

    fun navigateUpFile() {
        val parent = File(_currentFilePath.value).parentFile
        if (parent != null && parent.exists() && parent.canRead()) {
            loadFiles(parent.absolutePath)
        }
    }

    /** Runs a host command on the most privileged backend that works: Shizuku, then root, then plain shell. */
    private suspend fun executeOnBestHostBackend(cmd: String): CommandResult {
        if (backendRegistry.shizuku.isAvailable()) {
            val r = backendRegistry.shizuku.execute(cmd)
            if (r.isSuccess || r.stdout.isNotBlank()) return r
        }
        if (backendRegistry.root.isAvailable()) {
            val r = backendRegistry.root.execute(cmd)
            if (r.isSuccess || r.stdout.isNotBlank()) return r
        }
        return backendRegistry.localShell.execute(cmd)
    }

    fun loadProcesses() {
        viewModelScope.launch {
            val res = executeOnBestHostBackend("ps -A -o PID,USER,RSS,ARGS")
            _processItems.value = DeviceScanner.parseProcessList(res.stdout, android.os.Process.myPid())
        }
    }

    fun refreshLogs() {
        viewModelScope.launch {
            logcatReader.readHostLogcat(runner = { cmd -> executeOnBestHostBackend(cmd) })
        }
    }

    fun clearLogs() {
        logcatReader.clear()
    }

    fun runCommand(
        command: String,
        target: TargetScope = TargetScope.HOST,
        backend: BackendType = BackendType.LOCAL_SHELL,
        isDestructive: Boolean = false,
        destructiveInfo: DestructiveAction? = null
    ) {
        if (isDestructive && confirmDestructive.value && destructiveInfo != null) {
            _destructiveDialog.value = destructiveInfo
            return
        }

        viewModelScope.launch {
            val spec = CommandSpec(
                command = command,
                target = target,
                backend = backend,
                isDestructive = isDestructive
            )
            val result = repository.executeCommand(spec)
            _lastCommandResult.value = result
            if (isDestructive || !result.isSuccess) {
                _showResultDialog.value = true
            }
        }
    }

    /** Shows the confirmation dialog for an action that is not a plain command (e.g. flashing a file). */
    fun requestDestructiveConfirmation(info: DestructiveAction) {
        if (confirmDestructive.value) {
            _destructiveDialog.value = info
        } else {
            info.onConfirm()
        }
    }

    fun dismissDestructiveDialog() {
        _destructiveDialog.value = null
    }

    fun showResultDialog() {
        _showResultDialog.value = true
    }

    fun dismissResultModal() {
        _showResultDialog.value = false
    }

    // ------------------------------------------------------------ Target connection

    /** Step 1 (once per device): pair using the 6-digit code from "Wireless debugging > Pair device with pairing code". */
    fun pairWirelessTarget(host: String, port: Int, code: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val (success, message) = backendRegistry.targetAdb.pairWireless(host, port, code)
            onResult(success, message)
        }
    }

    /** Step 2: like Shizuku's "Start" - auto-discovers an already-paired target on the current Wi-Fi network. */
    fun startWirelessTarget(host: String? = null, port: Int? = null, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val (success, message) = backendRegistry.targetAdb.startWireless(host, port)
            if (success) refreshTargetInfo()
            onResult(success, message)
        }
    }

    fun scanUsbOtgTargets(onResult: (String) -> Unit = {}) {
        viewModelScope.launch {
            val devices = backendRegistry.targetAdb.scanUsbDevices()
            if (devices.isEmpty()) {
                onResult("No ADB/fastboot USB device found. Connect the target with an OTG cable (bootloader mode for Fastboot tools).")
                return@launch
            }
            val (ok, msg) = backendRegistry.targetAdb.connectUsb(devices.first())
            if (ok) refreshTargetInfo()
            onResult(msg)
        }
    }

    /** Real toggle: reboots a fastboot device to system, or an ADB device to the bootloader. */
    fun toggleTargetFastbootMode() {
        val state = backendRegistry.targetAdb.deviceState.value
        if (state.isFastbootMode) {
            runCommand("reboot", TargetScope.TARGET, BackendType.TARGET_FASTBOOT)
        } else {
            runCommand("reboot bootloader", TargetScope.TARGET, BackendType.TARGET_ADB)
        }
    }

    fun disconnectTarget() {
        backendRegistry.targetAdb.disconnect()
        _targetDeviceInfo.value = TargetDeviceInfo()
        _targetPackages.value = emptyList()
    }

    private fun parseFastbootVars(stdout: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (line in stdout.lines()) {
            val l = line.removePrefix("(bootloader)").trim()
            val i = l.indexOf(':')
            if (i > 0) map[l.substring(0, i).trim()] = l.substring(i + 1).trim()
        }
        return map
    }

    /** Reads real telemetry from the connected target (ADB properties or fastboot variables). */
    fun refreshTargetInfo(onResult: (String) -> Unit = {}) {
        viewModelScope.launch {
            val adb = backendRegistry.targetAdb
            val state = adb.deviceState.value
            when {
                adb.isAvailable() -> {
                    val p = adb.fetchDeviceProps()
                    if (p.isEmpty()) {
                        onResult("Could not read properties from the target.")
                        return@launch
                    }
                    val ramKb = p["mem"].orEmpty().filter { it.isDigit() }.toLongOrNull()
                    val dfParts = p["df"].orEmpty().trim().split(Regex("\\s+"))
                    val totalKb = dfParts.getOrNull(1)?.toLongOrNull()
                    val freeKb = dfParts.getOrNull(3)?.toLongOrNull()
                    val locked = p["locked"].orEmpty()
                    _targetDeviceInfo.value = TargetDeviceInfo(
                        model = p["model"].orEmpty().ifBlank { "Unknown" },
                        manufacturer = p["manufacturer"].orEmpty().ifBlank { "Unknown" },
                        androidVersion = p["release"].orEmpty().ifBlank { "Unknown" },
                        sdkInt = p["sdk"]?.toIntOrNull() ?: 0,
                        buildId = p["build"].orEmpty().ifBlank { "Unknown" },
                        securityPatch = p["patch"].orEmpty().ifBlank { "Unknown" },
                        kernelVersion = p["kernel"].orEmpty().ifBlank { "Unknown" },
                        cpuAbi = p["abi"].orEmpty().ifBlank { "Unknown" },
                        ramInfo = if (ramKb != null) String.format("%.1f GB", ramKb / 1024.0 / 1024.0) else "Unknown",
                        storageInfo = if (totalKb != null && freeKb != null) {
                            String.format("%.1f GB free of %.1f GB", freeKb / 1024.0 / 1024.0, totalKb / 1024.0 / 1024.0)
                        } else "Unknown",
                        battery = p["battery"].orEmpty().let { if (it.isBlank()) "Unknown" else "$it%" },
                        unlockedState = when (locked) {
                            "0" -> "unlocked"
                            "1" -> "locked"
                            else -> "Unknown"
                        },
                        currentSlot = p["slot"].orEmpty().removePrefix("_").ifBlank { "N/A" },
                        hasInitBoot = (p["initboot"]?.toIntOrNull() ?: 0) > 0,
                        isAbDevice = p["ab"] == "true",
                        fastbootVariant = "Normal"
                    )
                    onResult("Target info updated")
                }
                state.isConnected && state.isFastbootMode -> {
                    val res = backendRegistry.targetFastboot.execute("getvar all")
                    val v = parseFastbootVars(res.stdout + "\n" + res.stderr)
                    if (!res.isSuccess && v.isEmpty()) {
                        onResult("Fastboot getvar failed: ${res.stderr.ifBlank { res.stdout }}")
                        return@launch
                    }
                    _targetDeviceInfo.value = TargetDeviceInfo(
                        model = v["product"] ?: state.model,
                        unlockedState = when (v["unlocked"]) {
                            "yes" -> "unlocked"
                            "no" -> "locked"
                            else -> "Unknown"
                        },
                        currentSlot = v["current-slot"] ?: "N/A",
                        hasInitBoot = v.containsKey("has-slot:init_boot"),
                        isAbDevice = (v["slot-count"]?.toIntOrNull() ?: 0) > 1,
                        fastbootVariant = "Fastboot"
                    )
                    onResult("Fastboot variables read")
                }
                else -> onResult("No target connected.")
            }
        }
    }

    // ------------------------------------------------------------ Target apps and files

    fun loadTargetPackages(includeSystem: Boolean, onResult: (String) -> Unit = {}) {
        viewModelScope.launch {
            val res = backendRegistry.targetAdb.execute(if (includeSystem) "pm list packages" else "pm list packages -3")
            if (res.isSuccess) {
                val names = mutableListOf<String>()
                for (line in res.stdout.lines()) {
                    val t = line.trim()
                    if (t.startsWith("package:")) names.add(t.removePrefix("package:"))
                }
                _targetPackages.value = names.sorted()
                onResult("Loaded ${names.size} packages from the target")
            } else {
                onResult(res.stderr.ifBlank { res.stdout }.ifBlank { "Failed to list packages" })
            }
        }
    }

    private fun displayName(uri: Uri): String {
        getApplication<Application>().contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (i >= 0) return c.getString(i).replace('/', '_')
                }
            }
        return (uri.lastPathSegment ?: "file").replace('/', '_')
    }

    fun pushToTarget(uri: Uri, remoteDir: String, onResult: (String) -> Unit = {}) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val name = displayName(uri)
            val remote = remoteDir.trimEnd('/') + "/" + name
            val result = withContext(Dispatchers.IO) {
                val input = app.contentResolver.openInputStream(uri)
                if (input == null) null else input.use { backendRegistry.targetAdb.pushFile(it, remote) { } }
            }
            if (result == null) {
                onResult("Could not open the selected file")
                return@launch
            }
            _lastCommandResult.value = result.copy(command = "push $name -> $remote")
            onResult(if (result.isSuccess) result.stdout else result.stderr)
        }
    }

    fun pullFromTarget(remotePath: String, destination: Uri, onResult: (String) -> Unit = {}) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val result = withContext(Dispatchers.IO) {
                val out = app.contentResolver.openOutputStream(destination)
                if (out == null) null else out.use { backendRegistry.targetAdb.pullFile(remotePath, it) }
            }
            if (result == null) {
                onResult("Could not open the destination file")
                return@launch
            }
            _lastCommandResult.value = result.copy(command = "pull $remotePath")
            onResult(if (result.isSuccess) result.stdout else result.stderr)
        }
    }

    // ------------------------------------------------------------ Fastboot flashing

    fun flashFastboot(partition: String, uri: Uri, onResult: (String) -> Unit = {}) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val size = withContext(Dispatchers.IO) {
                app.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            }
            if (size <= 0L) {
                onResult("Could not determine the image size")
                return@launch
            }
            _flashProgress.value = FlashProgress("Flashing $partition", 0, size)
            val result = withContext(Dispatchers.IO) {
                val input = app.contentResolver.openInputStream(uri)
                if (input == null) null else input.use {
                    backendRegistry.targetFastboot.flashImage(partition, it, size) { sent ->
                        _flashProgress.value = FlashProgress("Flashing $partition", sent, size)
                    }
                }
            }
            _flashProgress.value = null
            if (result == null) {
                onResult("Could not open the selected image")
                return@launch
            }
            _lastCommandResult.value = result.copy(command = "fastboot flash $partition")
            _showResultDialog.value = true
            onResult(if (result.isSuccess) "Flash of $partition finished" else "Flash failed")
        }
    }

    fun captureTargetScreenshot() {
        viewModelScope.launch {
            runCommand(
                command = "screencap -p /sdcard/screenshot_afs.png",
                target = TargetScope.TARGET,
                backend = BackendType.TARGET_ADB
            )
            _targetScreenshotTimestamp.value = System.currentTimeMillis()
        }
    }

    fun saveShortcut(title: String, command: String, target: String, backend: String, category: String, isDestructive: Boolean) {
        viewModelScope.launch {
            repository.saveShortcut(
                ShortcutEntity(
                    title = title,
                    command = command,
                    targetType = target,
                    backend = backend,
                    category = category,
                    isDestructive = isDestructive
                )
            )
        }
    }

    fun deleteShortcut(shortcut: ShortcutEntity) {
        viewModelScope.launch {
            repository.deleteShortcut(shortcut)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    private fun seedDefaultShortcutsIfEmpty() {
        viewModelScope.launch {
            val defaults = listOf(
                ShortcutEntity(title = "Reboot to System", command = "reboot", targetType = "HOST", backend = "LOCAL_SHELL", category = "Reboot"),
                ShortcutEntity(title = "Reboot to Recovery", command = "reboot recovery", targetType = "HOST", backend = "ROOT", category = "Reboot", isDestructive = true),
                ShortcutEntity(title = "Reboot to Bootloader", command = "reboot bootloader", targetType = "HOST", backend = "ROOT", category = "Reboot", isDestructive = true),
                ShortcutEntity(title = "Inspect Battery Stats", command = "dumpsys battery", targetType = "HOST", backend = "LOCAL_SHELL", category = "Diagnostics"),
                ShortcutEntity(title = "List System Packages", command = "pm list packages -s", targetType = "HOST", backend = "LOCAL_SHELL", category = "Apps"),
                ShortcutEntity(title = "Target Fastboot Check", command = "getvar all", targetType = "TARGET", backend = "TARGET_FASTBOOT", category = "Fastboot"),
                ShortcutEntity(title = "Target Slot Query", command = "getvar current-slot", targetType = "TARGET", backend = "TARGET_FASTBOOT", category = "Fastboot"),
                ShortcutEntity(title = "Target Logcat", command = "logcat -d -t 200", targetType = "TARGET", backend = "TARGET_ADB", category = "Diagnostics")
            )
            for (sc in defaults) {
                repository.saveShortcut(sc)
            }
        }
    }
}
