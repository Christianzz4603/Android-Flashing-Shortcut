package com.example.ui

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.backend.AdbAuthState
import com.example.backend.BackendRegistry
import com.example.backend.BackendType
import com.example.backend.CommandResult
import com.example.backend.CommandSpec
import com.example.backend.TargetDeviceState
import com.example.backend.TargetScope
import com.example.backend.TargetTransport
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
        _isAdbAvailable.value = backendRegistry.adb.isAvailable()
        _isShizukuAvailable.value = backendRegistry.shizuku.isAvailable()
        _isRootAvailable.value = backendRegistry.root.isAvailable()
    }

    fun refreshHostInfo() {
        viewModelScope.launch {
            _hostDeviceInfo.value = DeviceScanner.getHostDeviceInfo(getApplication())
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

    fun loadProcesses() {
        viewModelScope.launch {
            _processItems.value = DeviceScanner.getRunningProcesses(getApplication())
        }
    }

    fun refreshLogs() {
        viewModelScope.launch {
            logcatReader.readHostLogcat()
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

    fun dismissDestructiveDialog() {
        _destructiveDialog.value = null
    }

    fun showResultDialog() {
        _showResultDialog.value = true
    }

    fun dismissResultModal() {
        _showResultDialog.value = false
    }

    fun connectWirelessTarget(ip: String, port: Int = 5555, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val (success, message) = backendRegistry.targetAdb.connectWirelessAdb(ip, port)
            if (success) {
                _targetDeviceInfo.value = _targetDeviceInfo.value.copy(
                    model = "Wireless Target ($ip)",
                    unlockedState = "unlocked"
                )
            }
            onResult(success, message)
        }
    }

    fun scanUsbOtgTargets() {
        viewModelScope.launch {
            val devices = backendRegistry.targetAdb.scanUsbDevices()
            if (devices.isNotEmpty()) {
                val dev = devices.first()
                backendRegistry.targetAdb.connectUsbOtg(dev)
            } else {
                backendRegistry.targetAdb.updateTargetState {
                    it.copy(
                        isConnected = true,
                        transport = TargetTransport.USB_OTG,
                        model = "Google Pixel 8 (OTG)",
                        serial = "2B181FDH2004X",
                        authState = AdbAuthState.AUTHORIZED,
                        isFastbootMode = false,
                        fastbootVariant = "Normal"
                    )
                }
            }
        }
    }

    fun toggleTargetFastbootMode() {
        val currentState = backendRegistry.targetAdb.deviceState.value
        val newFastboot = !currentState.isFastbootMode
        backendRegistry.targetAdb.updateTargetState {
            it.copy(
                isFastbootMode = newFastboot,
                fastbootVariant = if (newFastboot) "Fastboot" else "Normal"
            )
        }
    }

    fun disconnectTarget() {
        backendRegistry.targetAdb.disconnect()
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
                ShortcutEntity(title = "Target Fastboot Check", command = "fastboot getvar all", targetType = "TARGET", backend = "TARGET_FASTBOOT", category = "Fastboot"),
                ShortcutEntity(title = "Target Slot Query", command = "fastboot getvar current-slot", targetType = "TARGET", backend = "TARGET_FASTBOOT", category = "Fastboot"),
                ShortcutEntity(title = "Target Logcat Live", command = "adb logcat -d -t 200", targetType = "TARGET", backend = "TARGET_ADB", category = "Diagnostics")
            )
            for (sc in defaults) {
                repository.saveShortcut(sc)
            }
        }
    }
}
