package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backend.BackendType
import com.example.backend.TargetScope
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
    ADB_COMMANDS("ADB Commands"),
    APPS("Apps"),
    FILES("Files"),
    DEVICE_INFO("Device Info")
}

@Composable
fun AnotherPhoneScreen(viewModel: AfsViewModel) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(AnotherPhoneCategory.ADB_COMMANDS) }
    val targetState by viewModel.targetDeviceState.collectAsState()
    val targetInfo by viewModel.targetDeviceInfo.collectAsState()
    val hostInfo by viewModel.hostDeviceInfo.collectAsState()

    var showWirelessDialog by remember { mutableStateOf(false) }
    var targetIpInput by remember { mutableStateOf("192.168.1.105") }
    var targetPortInput by remember { mutableStateOf("5555") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AfsSurface)
    ) {
        // TOP STATUS & CONNECTION BAR: HOST vs TARGET
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("TARGET: ", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AfsAmber, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (targetState.isConnected) targetInfo.model else "Another Phone (Disconnected)",
                            fontSize = 11.sp,
                            color = if (targetState.isConnected) AfsTextPrimary else AfsTextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (targetState.isConnected) AfsGreen.copy(alpha = 0.2f) else AfsRed.copy(alpha = 0.2f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (targetState.isConnected) "Connected (${targetState.transport.label})" else "Disconnected",
                            fontSize = 9.sp,
                            color = if (targetState.isConnected) AfsGreen else AfsRed,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Connection Buttons (USB OTG & Wireless ADB)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.scanUsbOtgTargets()
                            Toast.makeText(context, "Scanning USB OTG ports...", Toast.LENGTH_SHORT).show()
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
                        Text("Wireless ADB", fontSize = 11.sp, fontWeight = FontWeight.Medium)
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

        // Category Pills (Same compact AFS utility style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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

        // Section Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            when (selectedCategory) {
                AnotherPhoneCategory.ADB_COMMANDS -> TargetAdbCommandsSection(viewModel)
                AnotherPhoneCategory.APPS -> TargetAppsSection(viewModel)
                AnotherPhoneCategory.FILES -> TargetFilesSection(viewModel)
                AnotherPhoneCategory.DEVICE_INFO -> TargetDeviceInfoSection(targetInfo)
            }
        }
    }

    if (showWirelessDialog) {
        AlertDialog(
            onDismissRequest = { showWirelessDialog = false },
            title = { Text("Wireless ADB Connection", color = AfsTextPrimary, fontSize = 14.sp) },
            text = {
                Column {
                    Text(
                        text = "Enter the target device's IP address and ADB port (default 5555):",
                        fontSize = 11.sp,
                        color = AfsTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = targetIpInput,
                        onValueChange = { targetIpInput = it },
                        label = { Text("Target IP Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = targetPortInput,
                        onValueChange = { targetPortInput = it },
                        label = { Text("Port (usually 5555)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val port = targetPortInput.toIntOrNull() ?: 5555
                        viewModel.connectWirelessTarget(targetIpInput, port) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (success) showWirelessDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AfsCyan)
                ) {
                    Text("Connect", color = AfsSurface)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showWirelessDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Target ADB Commands pane: Command input, preset actions, and terminal log window.
 */
@Composable
private fun TargetAdbCommandsSection(viewModel: AfsViewModel) {
    val context = LocalContext.current
    var commandInput by remember { mutableStateOf("") }
    var selectedRebootTarget by remember { mutableStateOf("system") }
    var targetLogs by remember { mutableStateOf("Ready for Target ADB commands.\n$ ") }
    var lastSuccess by remember { mutableStateOf<Boolean?>(null) }

    val lastResult by viewModel.lastCommandResult.collectAsState()

    androidx.compose.runtime.LaunchedEffect(lastResult) {
        lastResult?.let { res ->
            if (res.target == TargetScope.TARGET) {
                val output = buildString {
                    append("$ ${res.command}\n")
                    if (res.stdout.isNotBlank()) append(res.stdout).append("\n")
                    if (res.stderr.isNotBlank()) append("[ERR] ").append(res.stderr).append("\n")
                    append("[Exit Code: ${res.exitCode}]\n\n")
                }
                targetLogs = output + targetLogs.take(2000)
                lastSuccess = res.isSuccess
            }
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        // Command input
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AfsSurfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AfsOutline, RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = "TARGET ADB COMMAND",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AfsAmber,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commandInput,
                            onValueChange = { commandInput = it },
                            placeholder = { Text("e.g. pm list packages", fontSize = 12.sp, color = AfsTextSecondary) },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = AfsTextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            ),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = AfsTerminalBg,
                                unfocusedContainerColor = AfsTerminalBg,
                                focusedIndicatorColor = AfsAmber,
                                unfocusedIndicatorColor = AfsOutline
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (commandInput.isNotBlank()) {
                                    viewModel.runCommand(
                                        command = commandInput,
                                        target = TargetScope.TARGET,
                                        backend = BackendType.TARGET_ADB
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AfsAmber)
                        ) {
                            Text("RUN", color = AfsSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Reboot Target Controls
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AfsSurfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AfsOutline, RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Remote Reboot Control", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AfsTextPrimary)
                        Text("Risk: Low", fontSize = 11.sp, color = AfsGreen, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
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
        }

        // Quick Preset Commands
        item {
            Column {
                Text(
                    text = "TARGET SHORTCUTS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AfsTextSecondary,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        "Target Props" to "getprop ro.product.model",
                        "Battery Status" to "dumpsys battery",
                        "Target Logcat" to "logcat -d -t 30",
                        "WM Size" to "wm size",
                        "Disk Usage" to "df -h"
                    )

                    presets.forEach { (label, cmd) ->
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

        // LOGS Window
        item {
            TerminalLogView(
                title = "TARGET LOGS:",
                logsText = targetLogs,
                isSuccess = lastSuccess,
                onCopy = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Target Logs", targetLogs))
                    Toast.makeText(context, "Target logs copied", Toast.LENGTH_SHORT).show()
                },
                onClear = {
                    targetLogs = "Ready for Target ADB commands.\n$ "
                    lastSuccess = null
                },
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

/**
 * Target Apps Management.
 */
@Composable
private fun TargetAppsSection(viewModel: AfsViewModel) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var targetPackages by remember {
        mutableStateOf(
            listOf(
                "com.android.chrome" to "Chrome Browser",
                "com.google.android.youtube" to "YouTube",
                "com.google.android.apps.photos" to "Google Photos",
                "com.android.settings" to "Settings",
                "com.android.camera" to "Camera"
            )
        )
    }

    val filtered = targetPackages.filter {
        searchQuery.isBlank() || it.first.contains(searchQuery, ignoreCase = true) || it.second.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter target apps...", fontSize = 12.sp, color = AfsTextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AfsTextSecondary, modifier = Modifier.size(16.dp)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filtered) { (pkg, name) ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AfsSurfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AfsOutlineVariant, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AfsTextPrimary)
                        Text(text = pkg, fontSize = 10.sp, color = AfsTextSecondary, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.runCommand("monkey -p $pkg -c android.intent.category.LAUNCHER 1", TargetScope.TARGET, BackendType.TARGET_ADB)
                                    Toast.makeText(context, "Launched on Target", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Launch", fontSize = 10.sp, color = AfsCyan)
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.runCommand("am force-stop $pkg", TargetScope.TARGET, BackendType.TARGET_ADB)
                                    Toast.makeText(context, "Stopped on Target", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Force Stop", fontSize = 10.sp, color = AfsAmber)
                            }
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
                            ) {
                                Text("Uninstall", fontSize = 10.sp, color = AfsRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Target Files: Clearly labeled HOST -> TARGET and TARGET -> HOST transfers.
 */
@Composable
private fun TargetFilesSection(viewModel: AfsViewModel) {
    val context = LocalContext.current
    var targetPath by remember { mutableStateOf("/sdcard/Download") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Transfer Action Buttons
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = AfsSurfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .border(1.dp, AfsOutline, RoundedCornerShape(10.dp))
                .padding(10.dp)
        ) {
            Column {
                Text(
                    text = "CROSS-DEVICE FILE TRANSFERS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AfsCyan,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.runCommand("push /sdcard/transfer_file.zip $targetPath/", TargetScope.TARGET, BackendType.TARGET_ADB)
                            Toast.makeText(context, "Pushed to Target $targetPath", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AfsCyan),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, tint = AfsSurface, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("HOST → TARGET", color = AfsSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.runCommand("pull $targetPath/remote_file.png /sdcard/Download/", TargetScope.TARGET, BackendType.TARGET_ADB)
                            Toast.makeText(context, "Pulled to Host /sdcard/Download/", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AfsAmber),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = AfsSurface, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("TARGET → HOST", color = AfsSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Target Directory Preview
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = AfsSurfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AfsOutlineVariant, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Target Path: ", fontSize = 11.sp, color = AfsTextSecondary, fontFamily = FontFamily.Monospace)
                Text(text = targetPath, fontSize = 12.sp, color = AfsCyan, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = {
                        viewModel.runCommand("ls -la $targetPath", TargetScope.TARGET, BackendType.TARGET_ADB)
                    },
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("Refresh", fontSize = 10.sp)
                }
            }
        }
    }
}

/**
 * Target Device Info.
 */
@Composable
private fun TargetDeviceInfoSection(info: com.example.model.TargetDeviceInfo) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
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
            MetricCard("CPU ABI / ARCH", info.cpuAbi, subValue = "A/B Partition: ${if (info.isAbDevice) "Yes" else "No"}", modifier = Modifier.fillMaxWidth())
        }
        item {
            MetricCard("KERNEL", info.kernelVersion, modifier = Modifier.fillMaxWidth())
        }
    }
}
