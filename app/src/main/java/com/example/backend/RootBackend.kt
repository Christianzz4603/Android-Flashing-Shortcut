package com.example.backend

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class RootBackend : CommandBackend {
    override val backendType: BackendType = BackendType.ROOT

    private val suPaths = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/data/local/su",
        "/su/bin/su",
        "/magisk/.core/bin/su"
    )

    override fun isAvailable(): Boolean {
        // Check standard binary paths
        for (path in suPaths) {
            try {
                if (File(path).exists()) return true
            } catch (_: Exception) {}
        }
        // Test execution check
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            process.waitFor()
            !line.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }

    override fun getAvailabilityDetails(): String {
        for (path in suPaths) {
            if (File(path).exists()) return "Root binary detected at $path"
        }
        return "No su binary detected. Device is unrooted or Magisk/KernelSU/APatch access is not granted."
    }

    override suspend fun execute(command: String, timeoutMs: Long): CommandResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        if (!isAvailable()) {
            return@withContext CommandResult(
                exitCode = 127,
                stdout = "",
                stderr = "su: not found or root privileges unavailable on this device.",
                durationMs = System.currentTimeMillis() - startTime,
                backend = BackendType.ROOT,
                targetScope = TargetScope.HOST
            )
        }

        var process: Process? = null
        try {
            val result = withTimeoutOrNull(timeoutMs) {
                process = ProcessBuilder("su", "-c", command)
                    .redirectErrorStream(false)
                    .start()

                val stdoutBuilder = StringBuilder()
                val stderrBuilder = StringBuilder()

                val stdoutReader = BufferedReader(InputStreamReader(process?.inputStream))
                val stderrReader = BufferedReader(InputStreamReader(process?.errorStream))

                var line: String?
                while (stdoutReader.readLine().also { line = it } != null) {
                    if (stdoutBuilder.length < 50000) stdoutBuilder.append(line).append("\n")
                }
                while (stderrReader.readLine().also { line = it } != null) {
                    if (stderrBuilder.length < 50000) stderrBuilder.append(line).append("\n")
                }

                val exitCode = process?.waitFor() ?: -1
                val duration = System.currentTimeMillis() - startTime

                CommandResult(
                    exitCode = exitCode,
                    stdout = stdoutBuilder.toString().trimEnd(),
                    stderr = stderrBuilder.toString().trimEnd(),
                    durationMs = duration,
                    backend = BackendType.ROOT,
                    targetScope = TargetScope.HOST
                )
            }

            result ?: run {
                process?.destroy()
                CommandResult(
                    exitCode = -1,
                    stdout = "",
                    stderr = "Root command timed out after ${timeoutMs}ms",
                    durationMs = System.currentTimeMillis() - startTime,
                    backend = BackendType.ROOT,
                    targetScope = TargetScope.HOST
                )
            }
        } catch (e: Exception) {
            CommandResult(
                exitCode = 1,
                stdout = "",
                stderr = "Root execution error: ${e.localizedMessage}",
                durationMs = System.currentTimeMillis() - startTime,
                backend = BackendType.ROOT,
                targetScope = TargetScope.HOST
            )
        }
    }
}
