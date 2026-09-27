package com.example.model

data class AppInfoItem(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val isEnabled: Boolean,
    val apkPath: String,
    val targetSdk: Int,
    val uid: Int,
    val dataDir: String
)
