package com.example.backend

import android.content.Context

class BackendRegistry(context: Context) {
    val localShell = LocalShellBackend()
    val root = RootBackend()
    val shizuku = ShizukuBackend(context)
    val adb = AdbBackend()
    val targetAdb = TargetAdbBackend(context)
    val targetFastboot = TargetFastbootBackend(targetAdb)

    fun getBackend(type: BackendType): CommandBackend {
        return when (type) {
            BackendType.LOCAL_SHELL -> localShell
            BackendType.ROOT -> root
            BackendType.SHIZUKU -> shizuku
            BackendType.ADB -> adb
            BackendType.TARGET_ADB -> targetAdb
            BackendType.TARGET_FASTBOOT -> targetFastboot
        }
    }

    suspend fun execute(spec: CommandSpec): CommandResult {
        val backend = getBackend(spec.backend)
        val result = backend.execute(spec.command)
        return result.copy(command = spec.command, targetScope = spec.target)
    }
}
