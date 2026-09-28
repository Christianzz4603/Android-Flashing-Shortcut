package com.example.backend

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.lang.reflect.Method

/**
 * Real Shizuku integration. Commands run in the privileged Shizuku process (shell/root uid)
 * through Shizuku's remote process API, not in this app's own sandbox.
 */
class ShizukuBackend(private val context: Context) : CommandBackend {
    override val backendType: BackendType = BackendType.SHIZUKU

    companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        const val PERMISSION_REQUEST_CODE = 4021
    }

    fun isInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun binderAlive(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    private fun permissionGranted(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }

    /** Asks Shizuku for permission when the service is running but this app has not been approved yet. */
    fun requestPermissionIfNeeded() {
        try {
            if (binderAlive() && !Shizuku.isPreV11() && !permissionGranted() &&
                !Shizuku.shouldShowRequestPermissionRationale()
            ) {
                Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
            }
        } catch (_: Throwable) {
        }
    }

    override fun isAvailable(): Boolean = binderAlive() && permissionGranted()

    override fun getAvailabilityDetails(): String = when {
        !isInstalled() -> "Shizuku is not installed. Install Shizuku to run ADB-level commands without root."
        !binderAlive() -> "Shizuku is installed but its service is not running. Open Shizuku and start it."
        !permissionGranted() -> "Shizuku is running but this app has not been granted permission yet."
        else -> "Shizuku service running and permission granted."
    }

    private val newProcessMethod: Method? by lazy {
        try {
            Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply { isAccessible = true }
        } catch (_: Throwable) {
            null
        }
    }

    override suspend fun execute(command: String, timeoutMs: Long): CommandResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        fun fail(code: Int, msg: String) = CommandResult(
            exitCode = code,
            stdout = "",
            stderr = msg,
            durationMs = System.currentTimeMillis() - start,
            backend = BackendType.SHIZUKU,
            targetScope = TargetScope.HOST
        )

        if (!isInstalled()) return@withContext fail(1, getAvailabilityDetails())
        if (!binderAlive()) return@withContext fail(1, getAvailabilityDetails())
        if (!permissionGranted()) {
            requestPermissionIfNeeded()
            return@withContext fail(1, "Shizuku permission not granted yet. Approve the prompt, then retry.")
        }
        val method = newProcessMethod
            ?: return@withContext fail(1, "This Shizuku API build does not expose remote process execution.")

        try {
            coroutineScope {
                val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
                var timedOut = false
                val watchdog = launch {
                    delay(timeoutMs)
                    timedOut = true
                    process.destroy()
                }
                val out = async(Dispatchers.IO) { process.inputStream.bufferedReader().readText() }
                val err = async(Dispatchers.IO) { process.errorStream.bufferedReader().readText() }
                val stdout = out.await()
                val stderr = err.await()
                val exit = process.waitFor()
                watchdog.cancel()
                if (timedOut) {
                    fail(-1, "Command timed out after ${timeoutMs}ms")
                } else {
                    CommandResult(
                        exitCode = exit,
                        stdout = stdout.trimEnd(),
                        stderr = stderr.trimEnd(),
                        durationMs = System.currentTimeMillis() - start,
                        backend = BackendType.SHIZUKU,
                        targetScope = TargetScope.HOST
                    )
                }
            }
        } catch (e: Exception) {
            fail(1, "Shizuku execution failed: ${e.cause?.localizedMessage ?: e.localizedMessage}")
        }
    }
}
