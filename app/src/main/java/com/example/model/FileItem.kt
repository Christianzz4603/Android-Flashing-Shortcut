package com.example.model

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val permissions: String = "rw-r--r--"
) {
    val formattedSize: String
        get() {
            if (isDirectory) return "<DIR>"
            return when {
                sizeBytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", sizeBytes / (1024.0 * 1024 * 1024))
                sizeBytes >= 1024 * 1024 -> String.format("%.2f MB", sizeBytes / (1024.0 * 1024))
                sizeBytes >= 1024 -> String.format("%.1f KB", sizeBytes / 1024.0)
                else -> "$sizeBytes B"
            }
        }
}
