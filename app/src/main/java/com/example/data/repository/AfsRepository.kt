package com.example.data.repository

import com.example.backend.BackendRegistry
import com.example.backend.CommandResult
import com.example.backend.CommandSpec
import com.example.data.db.HistoryDao
import com.example.data.db.ShortcutDao
import com.example.data.model.CommandHistoryEntity
import com.example.data.model.ShortcutEntity
import kotlinx.coroutines.flow.Flow

class AfsRepository(
    private val shortcutDao: ShortcutDao,
    private val historyDao: HistoryDao,
    val backendRegistry: BackendRegistry
) {
    val allShortcuts: Flow<List<ShortcutEntity>> = shortcutDao.getAllShortcuts()
    val recentHistory: Flow<List<CommandHistoryEntity>> = historyDao.getRecentHistory()

    fun getShortcutsByTarget(target: String): Flow<List<ShortcutEntity>> {
        return shortcutDao.getShortcutsByTarget(target)
    }

    suspend fun saveShortcut(shortcut: ShortcutEntity): Long {
        return shortcutDao.insertShortcut(shortcut)
    }

    suspend fun deleteShortcut(shortcut: ShortcutEntity) {
        shortcutDao.deleteShortcut(shortcut)
    }

    suspend fun deleteShortcutById(id: Long) {
        shortcutDao.deleteById(id)
    }

    suspend fun executeCommand(spec: CommandSpec): CommandResult {
        val result = backendRegistry.execute(spec)
        historyDao.insertHistory(
            CommandHistoryEntity(
                command = spec.command,
                targetType = spec.target.name,
                backend = spec.backend.name,
                exitCode = result.exitCode,
                stdout = result.stdout,
                stderr = result.stderr,
                durationMs = result.durationMs
            )
        )
        return result
    }

    suspend fun clearHistory() {
        historyDao.clearHistory()
    }
}
