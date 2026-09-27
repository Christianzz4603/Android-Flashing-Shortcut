package com.example.backend

enum class TargetScope(val label: String) {
    HOST("This Phone (Host)"),
    TARGET("Another Phone (Target)")
}

enum class BackendType(val label: String, val badge: String) {
    LOCAL_SHELL("Local Shell", "SHELL"),
    ADB("Local ADB", "ADB"),
    SHIZUKU("Shizuku", "SHIZUKU"),
    ROOT("Root", "SU"),
    TARGET_ADB("Target ADB", "T-ADB"),
    TARGET_FASTBOOT("Target Fastboot", "FASTBOOT")
}

data class CommandSpec(
    val command: String,
    val target: TargetScope = TargetScope.HOST,
    val backend: BackendType = BackendType.LOCAL_SHELL,
    val requiredPermissions: List<String> = emptyList(),
    val isDestructive: Boolean = false,
    val description: String = ""
)

data class CommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long,
    val backend: BackendType,
    val targetScope: TargetScope,
    val timestamp: Long = System.currentTimeMillis(),
    val command: String = ""
) {
    val isSuccess: Boolean get() = exitCode == 0
    val target: TargetScope get() = targetScope
}

interface CommandBackend {
    val backendType: BackendType
    fun isAvailable(): Boolean
    fun getAvailabilityDetails(): String
    suspend fun execute(command: String, timeoutMs: Long = 20000): CommandResult
}
