package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import com.example.ui.AfsViewModel
import com.example.ui.components.UtilityChip
import com.example.ui.theme.AfsCyan
import com.example.ui.theme.AfsGreen
import com.example.ui.theme.AfsOutline
import com.example.ui.theme.AfsOutlineVariant
import com.example.ui.theme.AfsRed
import com.example.ui.theme.AfsSurface
import com.example.ui.theme.AfsSurfaceContainer
import com.example.ui.theme.AfsTextPrimary
import com.example.ui.theme.AfsTextSecondary

@Composable
fun SettingsScreen(viewModel: AfsViewModel) {
    val context = LocalContext.current

    val confirmDestructive by viewModel.confirmDestructive.collectAsState()
    val defaultBackend by viewModel.defaultHostBackend.collectAsState()
    val defaultPort by viewModel.defaultTargetPort.collectAsState()
    val history by viewModel.commandHistory.collectAsState()

    var portInput by remember(defaultPort) { mutableStateOf(defaultPort.toString()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AfsSurface)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color(0xFFE91E63)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "SETTINGS",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AfsTextPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "AFS Utility Configuration",
                        fontSize = 11.sp,
                        color = AfsTextSecondary
                    )
                }
            }
        }

        // 1. Backend Preference
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AfsSurfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AfsOutline, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "DEFAULT COMMAND BACKEND",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AfsCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Preferred engine for local device execution",
                        fontSize = 11.sp,
                        color = AfsTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            BackendType.LOCAL_SHELL to "Shell",
                            BackendType.ROOT to "Root",
                            BackendType.SHIZUKU to "Shizuku",
                            BackendType.ADB to "ADB"
                        ).forEach { (bType, label) ->
                            UtilityChip(
                                label = label,
                                isSelected = defaultBackend == bType,
                                onClick = { viewModel.defaultHostBackend.value = bType },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 2. Safety & Confirmations
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AfsSurfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AfsOutline, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = AfsGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Confirm Destructive Actions",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AfsTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Prompts before rebooting, clearing data, or uninstalling packages",
                            fontSize = 11.sp,
                            color = AfsTextSecondary
                        )
                    }

                    Switch(
                        checked = confirmDestructive,
                        onCheckedChange = { viewModel.confirmDestructive.value = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AfsCyan,
                            checkedTrackColor = AfsCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = AfsTextSecondary,
                            uncheckedTrackColor = AfsSurface
                        )
                    )
                }
            }
        }

        // 3. Wireless ADB Default Port
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AfsSurfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AfsOutline, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Wireless ADB Default Port",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AfsTextPrimary
                        )
                        Text(
                            text = "Standard port for target phone wireless connection",
                            fontSize = 11.sp,
                            color = AfsTextSecondary
                        )
                    }

                    OutlinedTextField(
                        value = portInput,
                        onValueChange = {
                            portInput = it
                            it.toIntOrNull()?.let { p -> viewModel.defaultTargetPort.value = p }
                        },
                        singleLine = true,
                        modifier = Modifier.width(90.dp).height(48.dp)
                    )
                }
            }
        }

        // 4. Command History Management
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AfsSurfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AfsOutline, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Command History",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AfsTextPrimary
                        )
                        Text(
                            text = "${history.size} recorded commands",
                            fontSize = 11.sp,
                            color = AfsTextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.clearAllHistory()
                            Toast.makeText(context, "Command history cleared", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AfsRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AfsRed.copy(alpha = 0.6f)),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = AfsRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear", fontSize = 11.sp, color = AfsRed)
                    }
                }
            }
        }

        // 5. Log Buffer
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AfsSurfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AfsOutline, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "System Logcat Buffer",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AfsTextPrimary
                        )
                        Text(
                            text = "Flush application memory log cache",
                            fontSize = 11.sp,
                            color = AfsTextSecondary
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.clearLogs()
                            Toast.makeText(context, "Logcat buffer cleared", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Text("Flush", fontSize = 11.sp, color = AfsCyan)
                    }
                }
            }
        }

        // 6. Check for Updates
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AfsSurfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AfsOutline, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Updates & Diagnostics",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AfsTextPrimary
                        )
                        Text(
                            text = "AFS v1.0.0 (Build 2026.09)",
                            fontSize = 11.sp,
                            color = AfsTextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.refreshCapabilities()
                            viewModel.refreshHostInfo()
                            Toast.makeText(context, "AFS is running latest build", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AfsCyan),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = AfsSurface, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Check", color = AfsSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
