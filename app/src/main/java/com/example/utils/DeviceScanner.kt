package com.example.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.view.Display
import com.example.model.AppInfoItem
import com.example.model.FileItem
import com.example.model.HostDeviceInfo
import com.example.model.ProcessItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

object DeviceScanner {

    fun getHostDeviceInfo(context: Context): HostDeviceInfo {
        // Battery Info
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempC = tempRaw / 10.0f
        val healthStr = when (batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }

        // Memory Info
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val totalRamGb = memInfo.totalMem / (1024.0 * 1024 * 1024)
        val availRamGb = memInfo.availMem / (1024.0 * 1024 * 1024)

        // Storage Info
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalStorageGb = (stat.blockCountLong * stat.blockSizeLong) / (1024.0 * 1024 * 1024)
        val freeStorageGb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024.0 * 1024 * 1024)

        // Display Info
        val displayMetrics = context.resources.displayMetrics
        val resolution = "${displayMetrics.widthPixels} x ${displayMetrics.heightPixels}"
        val densityDpi = displayMetrics.densityDpi
        val refresh = try {
            (context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
                .getDisplay(Display.DEFAULT_DISPLAY)?.refreshRate ?: 0f
        } catch (_: Exception) {
            0f
        }

        // Kernel
        val kernel = readKernelVersion()

        // System properties (Treble & A/B)
        val treble = getSystemProperty("ro.treble.enabled") == "true"
        val abUpdate = getSystemProperty("ro.build.ab_update") == "true"
        val slot = getSystemProperty("ro.boot.slot_suffix").ifBlank {
            if (abUpdate) "Unknown" else "N/A (A-only)"
        }

        return HostDeviceInfo(
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND,
            device = Build.DEVICE,
            product = Build.PRODUCT,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            buildId = Build.DISPLAY,
            securityPatch = Build.VERSION.SECURITY_PATCH ?: "N/A",
            kernelVersion = kernel,
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            totalRam = String.format("%.2f GB", totalRamGb),
            availableRam = String.format("%.2f GB", availRamGb),
            totalStorage = String.format("%.1f GB", totalStorageGb),
            freeStorage = String.format("%.1f GB", freeStorageGb),
            displayResolution = resolution,
            displayDensityDpi = densityDpi,
            refreshRate = refresh,
            batteryLevel = batteryPct,
            batteryStatus = if (isCharging) "Charging" else "Discharging",
            batteryHealth = healthStr,
            batteryTemperature = tempC,
            isTrebleEnabled = treble,
            isAbUpdateSupported = abUpdate,
            activeSlot = slot
        )
    }

    private fun readKernelVersion(): String {
        return try {
            val reader = BufferedReader(FileReader("/proc/version"))
            val line = reader.readLine()
            reader.close()
            line ?: System.getProperty("os.version") ?: "Linux"
        } catch (_: Exception) {
            System.getProperty("os.version") ?: "Linux"
        }
    }

    private fun getSystemProperty(prop: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("getprop", prop))
            val reader = BufferedReader(java.io.InputStreamReader(process.inputStream))
            val value = reader.readLine() ?: ""
            process.waitFor()
            value.trim()
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun getInstalledApps(context: Context): List<AppInfoItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
        val appList = mutableListOf<AppInfoItem>()

        for (pkg in packages) {
            val appInfo = pkg.applicationInfo ?: continue
            val appName = pm.getApplicationLabel(appInfo).toString()
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkg.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkg.versionCode.toLong()
            }

            appList.add(
                AppInfoItem(
                    packageName = pkg.packageName,
                    appName = appName,
                    versionName = pkg.versionName ?: "1.0",
                    versionCode = vCode,
                    isSystemApp = isSystem,
                    isEnabled = appInfo.enabled,
                    apkPath = appInfo.sourceDir ?: "",
                    targetSdk = appInfo.targetSdkVersion,
                    uid = appInfo.uid,
                    dataDir = appInfo.dataDir ?: ""
                )
            )
        }
        appList.sortedBy { it.appName.lowercase() }
    }

    /**
     * Parses real `ps -A -o PID,USER,RSS,ARGS` output (run through Shizuku / root / shell).
     * Memory is the real resident set size in KB. Nothing is invented if the command returns nothing.
     */
    fun parseProcessList(output: String, selfPid: Int): List<ProcessItem> {
        val list = mutableListOf<ProcessItem>()
        for (line in output.lines().drop(1)) {
            val t = line.trim()
            if (t.isEmpty()) continue
            val parts = t.split(Regex("\\s+"), limit = 4)
            if (parts.size < 4) continue
            val pid = parts[0].toIntOrNull() ?: continue
            val rss = parts[2].toLongOrNull() ?: continue
            val name = parts[3].substringBefore(' ')
            list.add(
                ProcessItem(
                    pid = pid,
                    name = name,
                    user = parts[1],
                    memoryKb = rss,
                    isKillable = pid != selfPid && pid > 1
                )
            )
        }
        return list.sortedByDescending { it.memoryKb }
    }

    suspend fun listFiles(dirPath: String): List<FileItem> = withContext(Dispatchers.IO) {
        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()

        val files = dir.listFiles() ?: return@withContext emptyList()
        files.map { file ->
            FileItem(
                name = file.name,
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                sizeBytes = if (file.isDirectory) 0L else file.length(),
                lastModified = file.lastModified(),
                permissions = "${if (file.canRead()) "r" else "-"}${if (file.canWrite()) "w" else "-"}${if (file.canExecute()) "x" else "-"}"
            )
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }
}
