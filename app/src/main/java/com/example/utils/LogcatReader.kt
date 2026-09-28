package com.example.utils

import com.example.backend.CommandResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class LogEntry(
    val id: Long = System.nanoTime(),
    val raw: String,
    val level: String = "I",
    val tag: String = "System",
    val message: String = ""
)

class LogcatReader {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private var isPaused = false

    /**
     * Reads real logcat output. When [runner] is supplied (Shizuku / root / shell) the command runs through it,
     * so a privileged backend returns system-wide logs. Without one, this app's own logcat is read.
     * If nothing can be read the list stays empty; no placeholder lines are ever generated.
     */
    suspend fun readHostLogcat(
        maxLines: Int = 150,
        runner: (suspend (String) -> CommandResult)? = null
    ) = withContext(Dispatchers.IO) {
        if (isPaused) return@withContext
        val cmd = "logcat -d -v time -t $maxLines"
        try {
            val text = if (runner != null) {
                runner(cmd).stdout
            } else {
                val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "time", "-t", maxLines.toString()))
                val out = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
                process.waitFor()
                out
            }
            _logs.value = text.lines().filter { it.isNotBlank() }.map { parseLogLine(it) }
        } catch (_: Exception) {
            _logs.value = emptyList()
        }
    }

    private fun parseLogLine(line: String): LogEntry {
        val level = when {
            line.contains(" E/") -> "E"
            line.contains(" W/") -> "W"
            line.contains(" D/") -> "D"
            line.contains(" V/") -> "V"
            else -> "I"
        }
        val tag = when {
            line.contains("/") && line.contains("(") -> line.substringAfter("/").substringBefore("(").trim()
            else -> "System"
        }
        val msg = line.substringAfter("): ", line)
        return LogEntry(raw = line, level = level, tag = tag, message = msg)
    }

    fun setPaused(paused: Boolean) {
        isPaused = paused
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
