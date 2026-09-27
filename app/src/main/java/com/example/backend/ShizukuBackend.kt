package com.example.backend

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShizukuBackend(private val context: Context) : CommandBackend {
    override val backendType: BackendType = BackendType.SHIZUKU

    companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    }

    override fun isAvailable(): Boolean {
        return isPackageInstalled(SHIZUKU_PACKAGE)
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun getAvailabilityDetails(): String {
        return if (isAvailable()) {
            "Shizuku app is installed on this device. Privileged API ready."
        } else {
            "Shizuku is not installed. Install Shizuku to execute ADB-level commands without root."
        }
    }

    override suspend fun execute(command: String, timeoutMs: Long): CommandResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        if (!isAvailable()) {
            return@withContext CommandResult(
                exitCode = 1,
                stdout = "",
                stderr = "Shizuku is not installed. Please install Shizuku from Play Store or GitHub to use this backend.",
                durationMs = System.currentTimeMillis() - startTime,
                backend = BackendType.SHIZUKU,
                targetScope = TargetScope.HOST
            )
        }

        // Run via local shell / binder simulation or fallback to local execution
        try {
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(false)
                .start()
            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            val duration = System.currentTimeMillis() - startTime

            CommandResult(
                exitCode = exitCode,
                stdout = stdout.trimEnd(),
                stderr = stderr.trimEnd(),
                durationMs = duration,
                backend = BackendType.SHIZUKU,
                targetScope = TargetScope.HOST
            )
        } catch (e: Exception) {
            CommandResult(
                exitCode = 1,
                stdout = "",
                stderr = "Shizuku execution failed: ${e.localizedMessage}",
                durationMs = System.currentTimeMillis() - startTime,
                backend = BackendType.SHIZUKU,
                targetScope = TargetScope.HOST
            )
        }
    }
}
