package com.example.backend

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

class AdbBackend : CommandBackend {
    override val backendType: BackendType = BackendType.ADB

    override fun isAvailable(): Boolean {
        // Check if local adb binary exists or if port 5555 is listening on loopback
        if (File("/system/bin/adb").exists() || File("/data/local/tmp/adb").exists()) {
            return true
        }
        return isLocalAdbdPortOpen()
    }

    private fun isLocalAdbdPortOpen(): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("127.0.0.1", 5555), 300)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun getAvailabilityDetails(): String {
        return if (isAvailable()) {
            "Local ADB / Wireless ADB loopback (127.0.0.1:5555) detected"
        } else {
            "Local ADB not active. Enable Wireless Debugging on this phone or use Local Shell / Shizuku."
        }
    }

    override suspend fun execute(command: String, timeoutMs: Long): CommandResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            // If local adb command works in shell, run it
            val process = ProcessBuilder("sh", "-c", "adb $command 2>&1 || (sh -c \"$command\")")
                .redirectErrorStream(false)
                .start()

            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            CommandResult(
                exitCode = exitCode,
                stdout = stdout.trimEnd(),
                stderr = stderr.trimEnd(),
                durationMs = System.currentTimeMillis() - startTime,
                backend = BackendType.ADB,
                targetScope = TargetScope.HOST
            )
        } catch (e: Exception) {
            CommandResult(
                exitCode = 1,
                stdout = "",
                stderr = "ADB execution error: ${e.localizedMessage}",
                durationMs = System.currentTimeMillis() - startTime,
                backend = BackendType.ADB,
                targetScope = TargetScope.HOST
            )
        }
    }
}
