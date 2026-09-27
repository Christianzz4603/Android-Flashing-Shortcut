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
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AfsCyan
import com.example.ui.theme.AfsGreen
import com.example.ui.theme.AfsOutline
import com.example.ui.theme.AfsSurface
import com.example.ui.theme.AfsSurfaceContainer
import com.example.ui.theme.AfsTextPrimary
import com.example.ui.theme.AfsTextSecondary

@Composable
fun AboutScreen() {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AfsSurface)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // App Identity Card
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AfsSurfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AfsCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Color(0xFFFB8C00)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Android Flashing Shortcuts",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = AfsTextPrimary
                    )
                    Text(
                        text = "AFS v1.0.0 (AFS-STABLE)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = AfsCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Lightweight dark compact Android GUI utility inspired by AFS and Bugjaeger.",
                        fontSize = 12.sp,
                        color = AfsTextSecondary,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }

        // Core Principles Card
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AfsSurfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AfsOutline, RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "UTILITY DESIGN PRINCIPLES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AfsCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val principles = listOf(
                        "• Lightweight GUI: No terminal bloat or IDE complexity",
                        "• Transparent Execution: Real exit codes, duration & logs",
                        "• Privacy First: Zero ads, cloud sync, accounts, or telemetry",
                        "• Cross-Device Support: Host this phone or connect target via USB/Wireless ADB",
                        "• Safety First: Explicit confirmation for destructive operations"
                    )

                    principles.forEach { principle ->
                        Text(
                            text = principle,
                            fontSize = 12.sp,
                            color = AfsTextSecondary,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // System Specs / Diagnostics
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AfsSurfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AfsOutline, RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "BUILD ENVIRONMENT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AfsGreen,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Target SDK: 36 (Android 16)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AfsTextPrimary)
                    Text(text = "Min SDK: 24 (Android 7.0+)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AfsTextSecondary)
                    Text(text = "Architecture: Jetpack Compose + Material 3", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AfsTextSecondary)
                    Text(text = "Database: Local Room Cache", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AfsTextSecondary)
                }
            }
        }
    }
}
