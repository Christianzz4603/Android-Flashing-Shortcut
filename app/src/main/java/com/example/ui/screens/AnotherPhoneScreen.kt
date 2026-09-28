package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backend.BackendType
import com.example.backend.TargetScope
import com.example.model.TargetDeviceInfo
import com.example.ui.AfsViewModel
import com.example.ui.DestructiveAction
import com.example.ui.components.AfsCategoryCard
import com.example.ui.components.MetricCard
import com.example.ui.components.TerminalLogView
import com.example.ui.components.UtilityChip
import com.example.ui.theme.AfsAmber
import com.example.ui.theme.AfsCyan
import com.example.ui.theme.AfsGreen
import com.example.ui.theme.AfsOutline
import com.example.ui.theme.AfsOutlineVariant
import com.example.ui.theme.AfsRed
import com.example.ui.theme.AfsSurface
import com.example.ui.theme.AfsSurfaceContainer
import com.example.ui.theme.AfsTerminalBg
import com.example.ui.theme.AfsTextPrimary
import com.example.ui.theme.AfsTextSecondary

enum class AnotherPhoneCategory(val label: String) {
    FASTBOOT("Fastboot"),
    ADB_COMMANDS("ADB Commands"),
    APPS("Apps"),
    FILES("Files"),
    DEVICE_INFO("Device Info")
}

private val monoStyle = TextStyle(color = AfsTextPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)

@Composable
private fun fieldColors(accent: Color) = TextFieldDefaults.colors(
    focusedContainerColor = AfsTerminalBg,
    unfocusedContainerColor = AfsTerminalBg,
    focusedIndicatorColor = accent,
    unfocusedIndicatorColor = AfsOutline
)

private fun splitHostPort(input: String, defaultPort: Int): Pair<String, Int>? {
    val parts = input.trim().split(":", limit = 2)
    val host = parts.getOrNull(0)?.trim().orEmpty()
    if (host.isBlank()) return null
    val port = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: defaultPort
    return host to port
}

@Composable
fun AnotherPhoneScreen(
    viewModel: AfsViewModel,
    initialCategory: AnotherPhoneCategory = AnotherPhoneCategory.FASTBOOT
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    val targetState by viewModel.targetDeviceState.collectAsState()
    val targetInfo by viewModel.targetDeviceInfo.collectAsState()
    val hostInfo by viewModel.hostDeviceInfo.collectAsState()

    var showWirelessDialog by remember { mutableStateOf(false) }
    var pairHostPort by remember { mutableStateOf("") }
    var pairCode by remember { mutableStateOf("") }
    var manualHostPort by remember { mutableStateOf("") }
    var pairing by remember { mutableStateOf(false) }
    var starting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AfsSurface)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = AfsSurfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .border(1.dp, if (targetState.isConnected) AfsCyan.copy(alpha = 0.5f) else AfsOutline, RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("HOST: ", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AfsCyan, fontWeight = FontWeight.Bold)
                        Text(hostInfo?.model ?: "This Phone", fontSize = 11.sp, color = AfsTextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                    Text("Role: Controller", fontSize = 10.sp, color = AfsTextSecondary, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text("TARGET: ", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AfsAmber, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (targetState.isConnected) targetState.model else "Another Phone (Disconnected)",
                            fontSize = 11.sp,
                            color = if (targetState.isConnected) AfsTextPrimary else AfsTextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    targetState.isConnected -> AfsGreen.copy(alpha = 0.2f)
                                    targetState.isReconnecting -> AfsAmber.copy(alpha = 0.2f)
                                    else -> AfsRed.copy(alpha = 0.2f)
                                }
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = when {
                                targetState.isConnected -> "Connected (${targetState.transport.label})"
                                targetState.isReconnecting -> "Reconnecting (Wi-Fi changed)..."
                                else -> "Disconnected"
                            },
                            fontSize = 9.sp,
                            color = when {
                                targetState.isConnected -> AfsGreen
                                targetState.isReconnecting -> AfsAmber
                                else -> AfsRed
                            },
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            Toast.makeText(context, "Scanning USB ports...", Toast.LENGTH_SHORT).show()
                            viewModel.scanUsbOtgTargets { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AfsCyan),
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Cable, contentDescription = null, tint = AfsSurface, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("USB OTG", color = AfsSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showWirelessDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AfsCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AfsCyan),
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = AfsCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Wireless debugging", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }

                    if (targetState.isConnected) {
                        OutlinedButton(
                            onClick = { viewModel.disconnectTarget() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AfsRed),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AfsRed),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text("Disconnect", fontSize = 11.sp, color = AfsRed)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AfsCategoryCard(
                title = "Fastboot",
                icon = Icons.Default.Build,
                iconBgColor = Color(0xFF29B6F6),
                isSelected = selectedCategory == AnotherPhoneCategory.FASTBOOT,
                onClick = { selectedCategory = AnotherPhoneCategory.FASTBOOT }
            )
            AfsCategoryCard(
                title = "ADB Commands",
                icon = Icons.Default.Android,
                iconBgColor = Color(0xFF00ACC1),
                isSelected = selectedCategory == AnotherPhoneCategory.ADB_COMMANDS,
                onClick = { selectedCategory = AnotherPhoneCategory.ADB_COMMANDS }
            )
            AfsCategoryCard(
                title = "Apps",
                icon = Icons.Default.Apps,
                iconBgColor = Color(0xFF1E88E5),
                isSelected = selectedCategory == AnotherPhoneCategory.APPS,
                onClick = { selectedCategory = AnotherPhoneCategory.APPS }
            )
            AfsCategoryCard(
                title = "Files",
                icon = Icons.Default.Folder,
                iconBgColor = Color(0xFFFB8C00),
                isSelected = selectedCategory == AnotherPhoneCategory.FILES,
                onClick = { selectedCategory = AnotherPhoneCategory.FILES }
            )
            AfsCategoryCard(
                title = "Device Info",
                icon = Icons.Default.Info,
                iconBgColor = Color(0xFF00897B),
                isSelected = selectedCategory == AnotherPhoneCategory.DEVICE_INFO,
                onClick = { selectedCategory = AnotherPhoneCategory.DEVICE_INFO }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            when (selectedCategory) {
                AnotherPhoneCategory.FASTBOOT -> TargetFastbootSection(viewModel)
                AnotherPhoneCategory.ADB_COMMANDS -> TargetAdbCommandsSection(viewModel)
                AnotherPhoneCategory.APPS -> TargetAppsSection(viewModel)
                AnotherPhoneCategory.FILES -> TargetFilesSection(viewModel)
                AnotherPhoneCategory.DEVICE_INFO -> TargetDeviceInfoSection(viewModel, targetInfo)
            }
        }
    }

    if (showWirelessDialog) {
        AlertDialog(
            onDismissRequest = { if (!pairing && !starting) showWirelessDialog = false },
            title = { Text("Wireless debugging", color = AfsTextPrimary, fontSize = 14.sp) },
            text = {
                Column {
                    Text(
                        "Works like Shizuku: pair once with the code from the target's Wireless debugging screen, " +
                            "then just tap Start every time after that - even after switching Wi-Fi networks.",
                        fontSize = 11.sp,
                        color = AfsTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("1. PAIR NEW DEVICE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AfsCyan, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "On the target: Settings > Developer options > Wireless debugging > Pair device with pairing code.",
                        fontSize = 10.sp, color = AfsTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = pairHostPort,
                        onValueChange = { pairHostPort = it },
                        label = { Text("Pairing IP:port") },
                        placeholder = { Text("e.g. 192.168.1.42:37451") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = pairCode,
                        onValueChange = { pairCode = it },
                        label = { Text("6-digit pairing code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        enabled = !pairing && !starting && pairCode.isNotBlank() && splitHostPort(pairHostPort, 0) != null,
                        onClick = {
                            val hp = splitHostPort(pairHostPort, 0) ?: return@Button
                            pairing = true
                            viewModel.pairWirelessTarget(hp.first, hp.second, pairCode.trim()) { _, msg ->
                                pairing = false
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AfsCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (pairing) "Pairing..." else "Pair", color = AfsSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("2. START", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AfsGreen, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Already paired? Make sure Wireless debugging is toggled on, then just tap Start - " +
                            "it finds the target automatically on this Wi-Fi network.",
                        fontSize = 10.sp, color = AfsTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        enabled = !starting && !pairing,
                        onClick = {
                            starting = true
                            viewModel.startWirelessTarget { ok, msg ->
                                starting = false
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                if (ok) showWirelessDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AfsGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (starting) "Starting..." else "Start", color = AfsSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("ADVANCED: MANUAL adb tcpip", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AfsTextSecondary, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "For older Android or a rooted target already listening on plain TCP (adb tcpip 5555), skip pairing.",
                        fontSize = 10.sp, color = AfsTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = manualHostPort,
                        onValueChange = { manualHostPort = it },
                        label = { Text("IP:port (default 5555)") },
                        placeholder = { Text("e.g. 192.168.1.42:5555") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        enabled = !starting && !pairing && splitHostPort(manualHostPort, 5555) != null,
                        onClick = {
                            val hp = splitHostPort(manualHostPort, 5555) ?: return@OutlinedButton
                            starting = true
                            viewModel.startWirelessTarget(hp.first, hp.second) { ok, msg ->
                                starting = false
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                if (ok) showWirelessDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Connect manually", color = AfsCyan, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                OutlinedButton(enabled = !pairing && !starting, onClick = { showWirelessDialog = false }) {
                    Text("Close")
                }
            },
            dismissButton = {}
        )
    }
}

@Composable
private fun SectionCard(title: String, accent: Color = AfsAmber, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = AfsSurfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AfsOutline, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accent, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}

/** Output console fed by real results of target commands. */
@Composable
private fun TargetConsole(viewModel: AfsViewModel, title: String) {
    val context = LocalContext.current
    val initial = "Ready. Output from the target appears here.\n$ "
    var logs by remember { mutableStateOf(initial) }
    var success by remember { mutableStateOf<Boolean?>(null) }
    val lastResult by viewModel.lastCommandResult.collectAsState()

    LaunchedEffect(lastResult) {
        lastResult?.let { res ->
            if (res.target == TargetScope.TARGET) {
                val output = buildString {
                    append("$ ${res.command}\n")
                    if (res.stdout.isNotBlank()) append(res.stdout).append("\n")
                    if (res.stderr.isNotBlank()) append("[ERR] ").append(res.stderr).append("\n")
                    append("[Exit Code: ${res.exitCode}]\n\n")
                }
                logs = output + logs.take(6000)
                success = res.isSuccess
            }
        }
    }

    TerminalLogView(
        title = title,
        logsText = logs,
        isSuccess = success,
        onCopy = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Target Logs", logs))
            Toast.makeText(context, "Target logs copied", Toast.LENGTH_SHORT).show()
        },
        onClear = {
            logs = initial
            success = null
        },
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

/** Real fastboot over USB: getvar, reboot, slot switch, erase, and flashing an image file. */
@Composable
private fun TargetFastbootSection(viewModel: AfsViewModel) {
    val context = LocalContext.current
    val targetState by viewModel.targetDeviceState.collectAsState()
    val flashProgress by viewModel.flashProgress.collectAsState()
    var customCmd by remember { mutableStateOf("") }
    var erasePartition by remember { mutableStateOf("") }
    var flashPartition by remember { mutableStateOf("") }
    var rebootMode by remember { mutableStateOf("system") }
    val ready = targetState.isConnected && targetState.isFastbootMode

    fun fb(cmd: String) {
        viewModel.runCommand(cmd, TargetScope.TARGET, BackendType.TARGET_FASTBOOT)
    }

    fun fbConfirm(cmd: String, title: String, partition: String, details: String, op: String) {
        viewModel.runCommand(
            command = cmd,
            target = TargetScope.TARGET,
            backend = BackendType.TARGET_FASTBOOT,
            isDestructive = true,
            destructiveInfo = DestructiveAction(title, "Another Phone", partition, details, op) {
                viewModel.runCommand(cmd, TargetScope.TARGET, BackendType.TARGET_FASTBOOT)
            }
        )
    }

    val flashLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val part = flashPartition.trim()
        if (uri != null && part.isNotEmpty()) {
            viewModel.requestDestructiveConfirmation(
                DestructiveAction(
                    "Flash partition", "Another Phone", part,
                    uri.lastPathSegment ?: "image file",
                    "fastboot flash $part (overwrites the partition)"
                ) {
                    viewModel.flashFastboot(part, uri) { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                }
            )
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        item {
            SectionCard("FASTBOOT STATUS", if (ready) AfsGreen else AfsAmber) {
                if (ready) {
                    Text(
                        "Fastboot device connected: ${targetState.model} (serial ${targetState.serial})",
                        fontSize = 12.sp, color = AfsTextPrimary
                    )
                } else {
                    Text(
                        "No fastboot device. Boot the target into bootloader mode, connect it with a USB OTG cable, then tap USB OTG above.",
                        fontSize = 12.sp, color = AfsTextSecondary
                    )
                    if (targetState.isConnected && !targetState.isFastbootMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                viewModel.runCommand(
                                    command = "reboot bootloader",
                                    target = TargetScope.TARGET,
                                    backend = BackendType.TARGET_ADB,
                                    isDestructive = true,
                                    destructiveInfo = DestructiveAction(
                                        "Reboot target to bootloader", "Another Phone", "N/A", "adb reboot bootloader", "Remote reboot over ADB"
                                    ) {
                                        viewModel.runCommand("reboot bootloader", TargetScope.TARGET, BackendType.TARGET_ADB)
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AfsAmber)
                        ) {
                            Text("Reboot target to bootloader (ADB)", color = AfsSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            SectionCard("READ DEVICE VARIABLES", AfsCyan) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "All" to "getvar all",
                        "Product" to "getvar product",
                        "Current slot" to "getvar current-slot",
                        "Unlocked" to "getvar unlocked",
                        "Max download" to "getvar max-download-size",
                        "Slot count" to "getvar slot-count",
                        "Version" to "getvar version-bootloader"
                    ).forEach { (label, cmd) ->
                        UtilityChip(label = label, isSelected = false, onClick = { if (ready) fb(cmd) })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = customCmd,
                        onValueChange = { customCmd = it },
                        placeholder = { Text("e.g. getvar hw-revision", fontSize = 12.sp, color = AfsTextSecondary) },
                        textStyle = monoStyle,
                        singleLine = true,
                        colors = fieldColors(AfsCyan),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        enabled = ready && customCmd.isNotBlank(),
                        onClick = {
                            val c = customCmd.trim().removePrefix("fastboot ").trim()
                            val risky = c.startsWith("erase") || c.startsWith("oem") || c.startsWith("flashing") || c.startsWith("set_active")
                            if (risky) fbConfirm(c, "Run fastboot command", "N/A", "fastboot $c", "Sent directly to the bootloader") else fb(c)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AfsCyan)
                    ) {
                        Text("RUN", color = AfsSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            SectionCard("REBOOT") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("system", "bootloader", "fastbootd", "recovery").forEach { mode ->
                        UtilityChip(
                            label = mode.replaceFirstChar { it.uppercase() },
                            isSelected = rebootMode == mode,
                            onClick = { rebootMode = mode },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    enabled = ready,
                    onClick = {
                        val cmd = when (rebootMode) {
                            "system" -> "reboot"
                            "fastbootd" -> "reboot fastboot"
                            else -> "reboot $rebootMode"
                        }
                        fbConfirm(cmd, "Reboot target", "N/A", "fastboot $cmd", "Reboot the connected device")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AfsAmber),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = AfsSurface, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reboot target", color = AfsSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        item {
            SectionCard("ACTIVE SLOT (A/B DEVICES)") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("a", "b").forEach { slot ->
                        OutlinedButton(
                            enabled = ready,
                            onClick = {
                                fbConfirm("set_active $slot", "Switch active slot", "slot $slot", "fastboot set_active $slot", "Boot from slot $slot next time")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Set slot ${slot.uppercase()}", fontSize = 12.sp, color = AfsCyan)
                        }
                    }
                }
            }
        }

        item {
            SectionCard("ERASE PARTITION", AfsRed) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = erasePartition,
                        onValueChange = { erasePartition = it },
                        placeholder = { Text("partition, e.g. cache", fontSize = 12.sp, color = AfsTextSecondary) },
                        textStyle = monoStyle,
                        singleLine = true,
                        colors = fieldColors(AfsRed),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        enabled = ready && erasePartition.isNotBlank(),
                        onClick = {
                            val p = erasePartition.trim()
                            fbConfirm("erase $p", "Erase partition", p, "fastboot erase $p", "Permanently wipes the partition")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AfsRed)
                    ) {
                        Text("ERASE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            SectionCard("FLASH IMAGE", AfsRed) {
                OutlinedTextField(
                    value = flashPartition,
                    onValueChange = { flashPartition = it },
                    placeholder = { Text("partition, e.g. boot, init_boot, recovery", fontSize = 12.sp, color = AfsTextSecondary) },
                    textStyle = monoStyle,
                    singleLine = true,
                    colors = fieldColors(AfsRed),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    enabled = ready && flashPartition.isNotBlank() && flashProgress == null,
                    onClick = { flashLauncher.launch(arrayOf("*/*")) },
                    colors = ButtonDefaults.buttonColors(containerColor = AfsRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Choose image and flash", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                val p = flashProgress
                if (p != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val frac = if (p.total > 0) p.sent.toFloat() / p.total.toFloat() else 0f
                    Text(
                        "${p.label}: ${p.sent / 1024} / ${p.total / 1024} KB",
                        fontSize = 11.sp, color = AfsTextSecondary, fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(progress = { frac }, color = AfsCyan, modifier = Modifier.fillMaxWidth())
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Raw images only. Images larger than the device's max-download-size (sparse images) are rejected.",
                    fontSize = 10.sp, color = AfsTextSecondary
                )
            }
        }

        item { TargetConsole(viewModel, "FASTBOOT LOGS:") }
    }
}

/** Real shell commands on the target over the ADB session. */
@Composable
private fun TargetAdbCommandsSection(viewModel: AfsViewModel) {
    var commandInput by remember { mutableStateOf("") }
    var selectedRebootTarget by remember { mutableStateOf("system") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        item {
            SectionCard("TARGET ADB COMMAND") {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        placeholder = { Text("e.g. pm list packages", fontSize = 12.sp, color = AfsTextSecondary) },
                        textStyle = monoStyle,
                        singleLine = true,
                        colors = fieldColors(AfsAmber),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (commandInput.isNotBlank()) {
                                viewModel.runCommand(commandInput, TargetScope.TARGET, BackendType.TARGET_ADB)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AfsAmber)
                    ) {
                        Text("RUN", color = AfsSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            SectionCard("REMOTE REBOOT") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("system", "recovery", "bootloader").forEach { target ->
                        UtilityChip(
                            label = target.replaceFirstChar { it.uppercase() },
                            isSelected = selectedRebootTarget == target,
                            onClick = { selectedRebootTarget = target },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val cmd = if (selectedRebootTarget == "system") "reboot" else "reboot $selectedRebootTarget"
                        viewModel.runCommand(
                            command = cmd,
                            target = TargetScope.TARGET,
                            backend = BackendType.TARGET_ADB,
                            isDestructive = true,
                            destructiveInfo = DestructiveAction(
                                title = "Reboot Target Device",
                                target = "Another Phone",
                                partition = "N/A",
                                fileOrDetails = "Mode: $selectedRebootTarget",
                                operation = "Remote reboot over ADB"
                            ) {
                                viewModel.runCommand(cmd, TargetScope.TARGET, BackendType.TARGET_ADB)
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AfsAmber),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = AfsSurface, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reboot Target Phone", color = AfsSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        item {
            Column {
                Text("TARGET SHORTCUTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AfsTextSecondary, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Model" to "getprop ro.product.model",
                        "Battery" to "dumpsys battery",
                        "Logcat" to "logcat -d -t 30",
                        "Screen size" to "wm size",
                        "Disk usage" to "df -h"
                    ).forEach { (label, cmd) ->
                        UtilityChip(
                            label = label,
                            isSelected = false,
                            onClick = {
                                commandInput = cmd
                                viewModel.runCommand(cmd, TargetScope.TARGET, BackendType.TARGET_ADB)
                            }
                        )
                    }
                }
            }
        }

        item { TargetConsole(viewModel, "TARGET LOGS:") }
    }
}

/** Installed packages read live from the target with pm list packages. */
@Composable
private fun TargetAppsSection(viewModel: AfsViewModel) {
    val context = LocalContext.current
    val packages by viewModel.targetPackages.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filtered = packages.filter { searchQuery.isBlank() || it.contains(searchQuery, ignoreCase = true) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = { viewModel.loadTargetPackages(false) { toast(it) } },
                colors = ButtonDefaults.buttonColors(containerColor = AfsCyan),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = AfsSurface, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("User apps", color = AfsSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { viewModel.loadTargetPackages(true) { toast(it) } },
                modifier = Modifier.weight(1f)
            ) {
                Text("All apps", color = AfsCyan, fontSize = 11.sp)
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter target packages...", fontSize = 12.sp, color = AfsTextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AfsTextSecondary, modifier = Modifier.size(16.dp)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        )

        if (packages.isEmpty()) {
            Text(
                "Connect a target over Wireless debugging, then tap User apps to read its installed packages.",
                fontSize = 12.sp, color = AfsTextSecondary, modifier = Modifier.padding(8.dp)
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filtered, key = { it }) { pkg ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AfsSurfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AfsOutlineVariant, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text(text = pkg, fontSize = 12.sp, color = AfsTextPrimary, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.runCommand("monkey -p $pkg -c android.intent.category.LAUNCHER 1", TargetScope.TARGET, BackendType.TARGET_ADB)
                                },
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) { Text("Launch", fontSize = 10.sp, color = AfsCyan) }
                            OutlinedButton(
                                onClick = { viewModel.runCommand("am force-stop $pkg", TargetScope.TARGET, BackendType.TARGET_ADB) },
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) { Text("Force Stop", fontSize = 10.sp, color = AfsAmber) }
                            OutlinedButton(
                                onClick = {
                                    viewModel.runCommand(
                                        command = "pm uninstall $pkg",
                                        target = TargetScope.TARGET,
                                        backend = BackendType.TARGET_ADB,
                                        isDestructive = true,
                                        destructiveInfo = DestructiveAction(
                                            title = "Uninstall Target App",
                                            target = "Another Phone",
                                            partition = "Data",
                                            fileOrDetails = pkg,
                                            operation = "Remote uninstall over ADB"
                                        ) {
                                            viewModel.runCommand("pm uninstall $pkg", TargetScope.TARGET, BackendType.TARGET_ADB)
                                        }
                                    )
                                },
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) { Text("Uninstall", fontSize = 10.sp, color = AfsRed) }
                        }
                    }
                }
            }
        }
    }
}

/** Real file transfer: push a file you pick to the target, pull a target file into a location you pick. */
@Composable
private fun TargetFilesSection(viewModel: AfsViewModel) {
    val context = LocalContext.current
    var targetDir by remember { mutableStateOf("/sdcard/Download") }
    var pullPath by remember { mutableStateOf("") }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    val pushLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            toast("Pushing to $targetDir ...")
            viewModel.pushToTarget(uri, targetDir) { toast(it) }
        }
    }
    val pullLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri: Uri? ->
        if (uri != null) {
            toast("Pulling $pullPath ...")
            viewModel.pullFromTarget(pullPath.trim(), uri) { toast(it) }
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        item {
            SectionCard("HOST -> TARGET (PUSH)", AfsCyan) {
                OutlinedTextField(
                    value = targetDir,
                    onValueChange = { targetDir = it },
                    label = { Text("Destination folder on target") },
                    textStyle = monoStyle,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { pushLauncher.launch(arrayOf("*/*")) },
                    colors = ButtonDefaults.buttonColors(containerColor = AfsCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, tint = AfsSurface, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Choose file and push", color = AfsSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Transfers use base64 over the ADB shell, so large files are slow. The size is verified after the copy.",
                    fontSize = 10.sp, color = AfsTextSecondary
                )
            }
        }

        item {
            SectionCard("TARGET -> HOST (PULL)", AfsAmber) {
                OutlinedTextField(
                    value = pullPath,
                    onValueChange = { pullPath = it },
                    label = { Text("Full path of file on target") },
                    placeholder = { Text("/sdcard/Download/file.zip") },
                    textStyle = monoStyle,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    enabled = pullPath.isNotBlank(),
                    onClick = { pullLauncher.launch(pullPath.trim().substringAfterLast('/').ifBlank { "pulled_file" }) },
                    colors = ButtonDefaults.buttonColors(containerColor = AfsAmber),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = AfsSurface, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pull and choose where to save", color = AfsSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            SectionCard("BROWSE TARGET FOLDER") {
                Button(
                    onClick = { viewModel.runCommand("ls -la ${targetDir}", TargetScope.TARGET, BackendType.TARGET_ADB) },
                    colors = ButtonDefaults.buttonColors(containerColor = AfsCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("List $targetDir", color = AfsSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item { TargetConsole(viewModel, "TRANSFER LOGS:") }
    }
}

/** Live target telemetry read from the device (ADB properties or fastboot variables). */
@Composable
private fun TargetDeviceInfoSection(viewModel: AfsViewModel, info: TargetDeviceInfo) {
    val context = LocalContext.current
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Button(
                onClick = { viewModel.refreshTargetInfo { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() } },
                colors = ButtonDefaults.buttonColors(containerColor = AfsCyan),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = AfsSurface, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Read info from target", color = AfsSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("TARGET MODEL", info.model, subValue = info.manufacturer, modifier = Modifier.weight(1f))
                MetricCard("ANDROID", info.androidVersion, subValue = "SDK ${info.sdkInt}", modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("BATTERY", info.battery, modifier = Modifier.weight(1f))
                MetricCard("BOOTLOADER", info.unlockedState, modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("RAM", info.ramInfo, modifier = Modifier.weight(1f))
                MetricCard("STORAGE", info.storageInfo, modifier = Modifier.weight(1f))
            }
        }
        item {
            MetricCard(
                "CPU ABI / SLOT",
                info.cpuAbi,
                subValue = "A/B: ${if (info.isAbDevice) "Yes (slot ${info.currentSlot})" else "No"}",
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            MetricCard("BUILD", info.buildId, subValue = "Patch: ${info.securityPatch}", modifier = Modifier.fillMaxWidth())
        }
        item {
            MetricCard("KERNEL", info.kernelVersion, modifier = Modifier.fillMaxWidth())
        }
    }
}
