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

/** Live telemetry read from a connected target phone. Every field is "Unknown" until read from the device. */
data class TargetDeviceInfo(
    val model: String = "Unknown",
    val manufacturer: String = "Unknown",
    val androidVersion: String = "Unknown",
    val sdkInt: Int = 0,
    val buildId: String = "Unknown",
    val securityPatch: String = "Unknown",
    val kernelVersion: String = "Unknown",
    val cpuAbi: String = "Unknown",
    val ramInfo: String = "Unknown",
    val storageInfo: String = "Unknown",
    val battery: String = "Unknown",
    val unlockedState: String = "Unknown",
    val currentSlot: String = "Unknown",
    val hasInitBoot: Boolean = false,
    val isAbDevice: Boolean = false,
    val fastbootVariant: String = "Normal"
)
