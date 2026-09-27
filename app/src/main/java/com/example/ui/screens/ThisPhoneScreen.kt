package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
import com.example.model.AppInfoItem
import com.example.model.FileItem
import com.example.model.HostDeviceInfo
import com.example.model.ProcessItem
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
import java.io.File

enum class HostPrimaryCategory(val label: String) {
    ADB_TOOLS("ADB Tools"),
    APPS("Apps"),
    FILES("Files"),
    DEVICE_INFO("Device Info"),
    PROCESSES("Processes")
}

@Composable
fun ThisPhoneScreen(viewModel: AfsViewModel, initialCategory: HostPrimaryCategory = HostPrimaryCategory.ADB_TOOLS) {
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    val hostInfo by viewModel.hostDeviceInfo.collectAsState()
    val isRoot by viewModel.isRootAvailable.collectAsState()
    val isAdb by viewModel.isAdbAvailable.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AfsSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AfsCategoryCard(
                title = "ADB Tools",
                icon = Icons.Default.Android,
                iconBgColor = Color(0xFF00ACC1),
                isSelected = selectedCategory == HostPrimaryCategory.ADB_TOOLS,
                onClick = { selectedCategory = HostPrimaryCategory.ADB_TOOLS },
                modifier = Modifier.testTag("cat_adb_tools")
            )
            AfsCategoryCard(
                title = "Apps",
                icon = Icons.Default.Apps,
                iconBgColor = Color(0xFF1E88E5),
                isSelected = selectedCategory == HostPrimaryCategory.APPS,
                onClick = { selectedCategory = HostPrimaryCategory.APPS },
                modifier = Modifier.testTag("cat_apps")
            )
            AfsCategoryCard(
                title = "Files",
                icon = Icons.Default.Folder,
                iconBgColor = Color(0xFFFB8C00),
                isSelected = selectedCategory == HostPrimaryCategory.FILES,
                onClick = { selectedCategory = HostPrimaryCategory.FILES },
                modifier = Modifier.testTag("cat_files")
            )
            AfsCategoryCard(
                title = "Device Info",
                icon = Icons.Default.Info,
                iconBgColor = Color(0xFF00897B),
                isSelected = selectedCategory == HostPrimaryCategory.DEVICE_INFO,
                onClick = { selectedCategory = HostPrimaryCategory.DEVICE_INFO },
                modifier = Modifier.testTag("cat_device_info")
            )
            AfsCategoryCard(
                title = "Processes",
                icon = Icons.Default.Memory,
                iconBgColor = Color(0xFF8E24AA),
                isSelected = selectedCategory == HostPrimaryCategory.PROCESSES,
                onClick = { selectedCategory = HostPrimaryCategory.PROCESSES },
                modifier = Modifier.testTag("cat_processes")
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            when (selectedCategory) {
                HostPrimaryCategory.ADB_TOOLS -> HostAdbToolsSection(viewModel, isRoot)
                HostPrimaryCategory.APPS -> HostAppManagementSection(viewModel)
                HostPrimaryCategory.FILES -> HostFileToolsSection(viewModel)
                HostPrimaryCategory.DEVICE_INFO -> HostDeviceInfoSection(hostInfo)
                HostPrimaryCategory.PROCESSES -> HostProcessesSection(viewModel)
            }
        }
    }
}

@Composable
private fun HostAdbToolsSection(viewModel: AfsViewModel, isRoot: Boolean) {
    val context = LocalContext.current
    var commandInput by remember { mutableStateOf("") }
    var selectedRebootTarget by remember { mutableStateOf("system") }
    var terminalLogs by remember { mutableStateOf("AFS Ready. Local & ADB execution available.\n$ ") }
    var lastExitSuccess by remember { mutableStateOf<Boolean?>(null) }

    val lastResult by viewModel.lastCommandResult.collectAsState()

    androidx.compose.runtime.LaunchedEffect(lastResult) {
        lastResult?.let { res ->
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date(res.timestamp))
            val outputText = buildString {
                append("[$timestamp] $ ${res.command}\n")
                if (res.stdout.isNotBlank()) append(res.stdout).append("\n")
                if (res.stderr.isNotBlank()) append("[STDERR] ").append(res.stderr).append("\n")
                append("[Finished in ${res.durationMs}ms with Exit Code: ${res.exitCode}]\n\n")
            }
            terminalLogs = outputText + terminalLogs.take(2000)
            lastExitSuccess = res.isSuccess
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COMMAND INPUT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AfsCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isRoot) "Backend: Root (su)" else "Backend: Shell (sh)",
                            fontSize = 10.sp,
                            color = AfsTextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commandInput,
                            onValueChange = { commandInput = it },
                            placeholder = { Text("e.g. getprop ro.build.version.release", fontSize = 12.sp, color = AfsTextSecondary) },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = AfsTextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            ),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = AfsTerminalBg,
                                unfocusedContainerColor = AfsTerminalBg,
                                focusedIndicatorColor = AfsCyan,
                                unfocusedIndicatorColor = AfsOutline
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("command_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (commandInput.isNotBlank()) {
                                    val backend = if (isRoot) BackendType.ROOT else BackendType.LOCAL_SHELL
                                    viewModel.runCommand(
                                        command = commandInput,
                                        target = TargetScope.HOST,
                                        backend = backend
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AfsCyan),
                            modifier = Modifier.testTag("run_command_btn")
                        ) {
                            Text("RUN", color = AfsSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Command: adb reboot $selectedRebootTarget",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            color = AfsTextSecondary
                        )
                        Text(
                            text = "Risk: Low",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AfsGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Where to reboot?",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AfsTextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("system", "recovery", "bootloader", "fastbootd").forEach { target ->
                            UtilityChip(
                                label = target.replaceFirstChar { it.uppercase() },
                                isSelected = selectedRebootTarget == target,
                                onClick = { selectedRebootTarget = target },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val cmd = if (selectedRebootTarget == "system") "reboot" else "reboot $selectedRebootTarget"
                                viewModel.runCommand(
                                    command = cmd,
                                    target = TargetScope.HOST,
                                    backend = if (isRoot) BackendType.ROOT else BackendType.LOCAL_SHELL,
                                    isDestructive = true,
                                    destructiveInfo = DestructiveAction(
                                        title = "Reboot Device",
                                        target = "This Phone",
                                        partition = "N/A",
                                        fileOrDetails = "Target mode: $selectedRebootTarget",
                                        operation = "Immediate system reboot"
                                    ) {
                                        viewModel.runCommand(
                                            command = cmd,
                                            target = TargetScope.HOST,
                                            backend = if (isRoot) BackendType.ROOT else BackendType.LOCAL_SHELL
                                        )
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AfsCyan),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("execute_reboot_btn")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = AfsSurface, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Execute", color = AfsSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.runCommand(
                                    command = "logcat -d -t 50",
                                    target = TargetScope.HOST,
                                    backend = if (isRoot) BackendType.ROOT else BackendType.LOCAL_SHELL
                                )
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AfsCyan),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AfsCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Get logs", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Column {
                Text(
                    text = "QUICK SHORTCUTS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AfsTextSecondary,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        "Screenshot" to "screencap -p /sdcard/afs_shot.png && echo 'Saved to /sdcard/afs_shot.png'",
                        "Device Props" to "getprop | grep -E 'model|version|brand'",
                        "Battery Info" to "dumpsys battery",
                        "Installed Apps" to "pm list packages -3",
                        "Uptime" to "uptime",
                        "Storage Free" to "df -h /data /sdcard"
                    )

                    presets.forEach { (label, cmd) ->
                        UtilityChip(
                            label = label,
                            isSelected = false,
                            onClick = {
                                commandInput = cmd
                                viewModel.runCommand(
                                    command = cmd,
                                    target = TargetScope.HOST,
                                    backend = if (isRoot) BackendType.ROOT else BackendType.LOCAL_SHELL
                                )
                            }
                        )
                    }
                }
            }
        }

        item {
            TerminalLogView(
                title = "LOGS:",
                logsText = terminalLogs,
                isSuccess = lastExitSuccess,
                onCopy = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("AFS Logs", terminalLogs))
                    Toast.makeText(context, "Logs copied", Toast.LENGTH_SHORT).show()
                },
                onClear = {
                    terminalLogs = "Logs cleared.\n$ "
                    lastExitSuccess = null
                },
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun HostAppManagementSection(viewModel: AfsViewModel) {
    val context = LocalContext.current
    val apps by viewModel.installedApps.collectAsState()
    val searchQuery by viewModel.appSearchQuery.collectAsState()
    val showSystemApps by viewModel.showSystemApps.collectAsState()
    val isRoot by viewModel.isRootAvailable.collectAsState()

    val filteredApps = remember(apps, searchQuery, showSystemApps) {
        apps.filter { app ->
            (showSystemApps || !app.isSystemApp) &&
                (searchQuery.isBlank() || app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setAppSearchQuery(it) },
                placeholder = { Text("Search installed packages...", fontSize = 12.sp, color = AfsTextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AfsTextSecondary, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setAppSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = AfsTextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AfsSurfaceContainer,
                    unfocusedContainerColor = AfsSurfaceContainer,
                    focusedIndicatorColor = AfsCyan,
                    unfocusedIndicatorColor = AfsOutline
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("app_search_field")
            )

            Spacer(modifier = Modifier.width(6.dp))

            UtilityChip(
                label = if (showSystemApps) "System" else "User",
                isSelected = showSystemApps,
                onClick = { viewModel.toggleSystemApps() },
                modifier = Modifier.height(48.dp)
            )
        }

        Text(
            text = "Total: ${filteredApps.size} applications",
            fontSize = 10.sp,
            color = AfsTextSecondary,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredApps, key = { it.packageName }) { app ->
                AppRowCard(app = app, isRoot = isRoot, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun AppRowCard(app: AppInfoItem, isRoot: Boolean, viewModel: AfsViewModel) {
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = AfsSurfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AfsOutlineVariant, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AfsTextPrimary
                    )
                    Text(
                        text = app.packageName,
                        fontSize = 10.sp,
                        color = AfsTextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (app.isSystemApp) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AfsAmber.copy(alpha = 0.2f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("SYS", fontSize = 9.sp, color = AfsAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                        } else {
                            Toast.makeText(context, "No launchable activity", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Text("Launch", fontSize = 10.sp, color = AfsCyan)
                }

                OutlinedButton(
                    onClick = {
                        val cmd = "am force-stop ${app.packageName}"
                        viewModel.runCommand(cmd, TargetScope.HOST, if (isRoot) BackendType.ROOT else BackendType.LOCAL_SHELL)
                        Toast.makeText(context, "Force-stopped ${app.appName}", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Text("Stop", fontSize = 10.sp, color = AfsAmber)
                }

                OutlinedButton(
                    onClick = {
                        viewModel.runCommand(
                            command = "pm clear ${app.packageName}",
                            target = TargetScope.HOST,
                            backend = if (isRoot) BackendType.ROOT else BackendType.LOCAL_SHELL,
                            isDestructive = true,
                            destructiveInfo = DestructiveAction(
                                title = "Clear App Data",
                                target = "This Phone",
                                partition = "Data",
                                fileOrDetails = app.packageName,
                                operation = "Erase all app data and cache"
                            ) {
                                viewModel.runCommand("pm clear ${app.packageName}", TargetScope.HOST, if (isRoot) BackendType.ROOT else BackendType.LOCAL_SHELL)
                            }
                        )
                    },
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Text("Clear", fontSize = 10.sp, color = AfsRed)
                }

                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                            data = Uri.parse("package:${app.packageName}")
                            putExtra(Intent.EXTRA_RETURN_RESULT, true)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Text("Uninstall", fontSize = 10.sp, color = AfsRed)
                }
            }
        }
    }
}

@Composable
private fun HostFileToolsSection(viewModel: AfsViewModel) {
    val context = LocalContext.current
    val currentPath by viewModel.currentFilePath.collectAsState()
    val files by viewModel.fileItems.collectAsState()

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = AfsSurfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AfsOutline, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateUpFile() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AfsCyan, modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = currentPath,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = AfsTextPrimary,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { showNewFolderDialog = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", tint = AfsCyan, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(files) { file ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AfsSurfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            if (file.isDirectory) {
                                viewModel.loadFiles(file.path)
                            } else {
                                Toast.makeText(context, "${file.name} (${file.formattedSize})", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = if (file.isDirectory) AfsAmber else AfsCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = file.name, fontSize = 12.sp, color = AfsTextPrimary)
                            Text(
                                text = if (file.isDirectory) "Directory" else file.formattedSize,
                                fontSize = 10.sp,
                                color = AfsTextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Create Folder", color = AfsTextPrimary, fontSize = 14.sp) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = { Text("Folder name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            val newDir = File(currentPath, newFolderName)
                            newDir.mkdirs()
                            viewModel.loadFiles(currentPath)
                            showNewFolderDialog = false
                            newFolderName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AfsCyan)
                ) {
                    Text("Create", color = AfsSurface)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNewFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HostDeviceInfoSection(info: HostDeviceInfo?) {
    if (info == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading device telemetry...", color = AfsTextSecondary, fontFamily = FontFamily.Monospace)
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("MODEL", info.model, subValue = info.manufacturer, modifier = Modifier.weight(1f))
                MetricCard("ANDROID", info.androidVersion, subValue = "SDK ${info.sdkInt}", modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("SECURITY PATCH", info.securityPatch, modifier = Modifier.weight(1f))
                MetricCard("BATTERY", "${info.batteryLevel}%", subValue = info.batteryStatus, modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("RAM", "${info.availableRam} free", subValue = "Total: ${info.totalRam}", modifier = Modifier.weight(1f))
                MetricCard("STORAGE", "${info.freeStorage} free", subValue = "Total: ${info.totalStorage}", modifier = Modifier.weight(1f))
            }
        }
        item {
            MetricCard("KERNEL", info.kernelVersion, modifier = Modifier.fillMaxWidth())
        }
        item {
            MetricCard("BUILD ID", info.buildId, subValue = info.supportedAbis.joinToString(", "), modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun HostProcessesSection(viewModel: AfsViewModel) {
    val processes by viewModel.processItems.collectAsState()
    val isRoot by viewModel.isRootAvailable.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RUNNING PROCESSES (${processes.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AfsCyan,
                fontFamily = FontFamily.Monospace
            )
            IconButton(onClick = { viewModel.loadProcesses() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AfsCyan, modifier = Modifier.size(18.dp))
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(processes) { proc ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AfsSurfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = proc.name, fontSize = 12.sp, color = AfsTextPrimary, fontWeight = FontWeight.Medium)
                            Text(
                                text = "PID: ${proc.pid}  |  User: ${proc.user}  |  RAM: ${proc.formattedMemory}",
                                fontSize = 10.sp,
                                color = AfsTextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val cmd = "kill -9 ${proc.pid}"
                                viewModel.runCommand(cmd, TargetScope.HOST, if (isRoot) BackendType.ROOT else BackendType.LOCAL_SHELL)
                                viewModel.loadProcesses()
                            },
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Kill", fontSize = 10.sp, color = AfsRed)
                        }
                    }
                }
            }
        }
    }
}
