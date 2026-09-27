package com.example.model

data class ProcessItem(
    val pid: Int,
    val name: String,
    val user: String = "u0_a0",
    val memoryKb: Long = 0,
    val isKillable: Boolean = true
) {
    val formattedMemory: String
        get() = if (memoryKb > 1024) "${memoryKb / 1024} MB" else "$memoryKb KB"
}
