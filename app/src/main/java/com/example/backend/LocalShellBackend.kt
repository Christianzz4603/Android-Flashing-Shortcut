package com.example.backend

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.InputStreamReader

class LocalShellBackend : CommandBackend {
    override val backendType: BackendType = BackendType.LOCAL_SHELL

    override fun isAvailable(): Boolean = true

    override fun getAvailabilityDetails(): String = "Linux userspace shell (sh) available"

    override suspend fun execute(command: String, timeoutMs: Long): CommandResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var process: Process? = null
        try {
            val result = withTimeoutOrNull(timeoutMs) {
                process = ProcessBuilder("sh", "-c", command)
                    .redirectErrorStream(false)
                    .start()

                val stdoutBuilder = StringBuilder()
                val stderrBuilder = StringBuilder()

                val stdoutReader = BufferedReader(InputStreamReader(process?.inputStream))
                val stderrReader = BufferedReader(InputStreamReader(process?.errorStream))

                var stdoutLine: String?
                while (stdoutReader.readLine().also { stdoutLine = it } != null) {
                    if (stdoutBuilder.length < 50000) {
                        stdoutBuilder.append(stdoutLine).append("\n")
                    }
                }

                var stderrLine: String?
                while (stderrReader.readLine().also { stderrLine = it } != null) {
                    if (stderrBuilder.length < 50000) {
                        stderrBuilder.append(stderrLine).append("\n")
                    }
                }

                val exitCode = process?.waitFor() ?: -1
                val duration = System.currentTimeMillis() - startTime

                CommandResult(
                    exitCode = exitCode,
                    stdout = stdoutBuilder.toString().trimEnd(),
                    stderr = stderrBuilder.toString().trimEnd(),
                    durationMs = duration,
                    backend = BackendType.LOCAL_SHELL,
                    targetScope = TargetScope.HOST
                )
            }

            result ?: run {
                process?.destroy()
                CommandResult(
                    exitCode = -1,
                    stdout = "",
                    stderr = "Command timed out after ${timeoutMs}ms",
                    durationMs = System.currentTimeMillis() - startTime,
                    backend = BackendType.LOCAL_SHELL,
                    targetScope = TargetScope.HOST
                )
            }
        } catch (e: Exception) {
            CommandResult(
                exitCode = 1,
                stdout = "",
                stderr = "Execution error: ${e.localizedMessage}",
                durationMs = System.currentTimeMillis() - startTime,
                backend = BackendType.LOCAL_SHELL,
                targetScope = TargetScope.HOST
            )
        }
    }
}
