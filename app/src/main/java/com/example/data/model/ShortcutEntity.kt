package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shortcuts")
data class ShortcutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val command: String,
    val targetType: String, // "HOST" or "TARGET"
    val backend: String,    // "LOCAL_SHELL", "ADB", "SHIZUKU", "ROOT", "TARGET_ADB", "TARGET_FASTBOOT"
    val category: String,   // "General", "Reboot", "Apps", "Fastboot", "Flash", "Diagnostics"
    val isDestructive: Boolean = false,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
