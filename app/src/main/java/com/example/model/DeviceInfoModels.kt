package com.example.model

data class HostDeviceInfo(
    val model: String,
    val manufacturer: String,
    val brand: String,
    val device: String,
    val product: String,
    val androidVersion: String,
    val sdkInt: Int,
    val buildId: String,
    val securityPatch: String,
    val kernelVersion: String,
    val supportedAbis: List<String>,
    val totalRam: String,
    val availableRam: String,
    val totalStorage: String,
    val freeStorage: String,
    val displayResolution: String,
    val displayDensityDpi: Int,
    val refreshRate: Float,
    val batteryLevel: Int,
    val batteryStatus: String,
    val batteryHealth: String,
    val batteryTemperature: Float,
    val isTrebleEnabled: Boolean,
    val isAbUpdateSupported: Boolean,
    val activeSlot: String
)

data class TargetDeviceInfo(
    val model: String = "Google Pixel 8 Pro",
    val manufacturer: String = "Google",
    val androidVersion: String = "14.0",
    val sdkInt: Int = 34,
    val buildId: String = "UQ1A.240205.004",
    val securityPatch: String = "2024-03-05",
    val kernelVersion: String = "5.15.131-android14-9-g8e91",
    val cpuAbi: String = "arm64-v8a",
    val ramInfo: String = "12 GB LPDDR5X",
    val storageInfo: String = "256 GB UFS 3.1",
    val battery: String = "88% (Charging)",
    val unlockedState: String = "unlocked",
    val currentSlot: String = "a",
    val hasInitBoot: Boolean = true,
    val isAbDevice: Boolean = true,
    val fastbootVariant: String = "Normal"
)
