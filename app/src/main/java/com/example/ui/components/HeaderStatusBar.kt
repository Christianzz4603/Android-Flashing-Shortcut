package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AfsAmber
import com.example.ui.theme.AfsCyan
import com.example.ui.theme.AfsGreen
import com.example.ui.theme.AfsOutline
import com.example.ui.theme.AfsRed
import com.example.ui.theme.AfsSurface
import com.example.ui.theme.AfsSurfaceContainer
import com.example.ui.theme.AfsTextPrimary
import com.example.ui.theme.AfsTextSecondary

@Composable
fun HeaderStatusBar(
    isAdbAvailable: Boolean,
    isShizukuAvailable: Boolean,
    isRootAvailable: Boolean,
    targetConnected: Boolean,
    targetTransport: String,
    onRefresh: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = AfsSurface,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("header_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back to menu",
                                tint = AfsCyan
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AfsCyan.copy(alpha = 0.15f))
                            .border(1.dp, AfsCyan.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "AFS Icon",
                            tint = AfsCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Android Flashing Shortcuts",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = AfsTextPrimary,
                            letterSpacing = 0.2.sp
                        )
                        Text(
                            text = "AFS v1.0.0 \u2022 Desktop-Utility Mode",
                            fontSize = 11.sp,
                            color = AfsTextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("refresh_status_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Status",
                        tint = AfsCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.padding(top = 8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AfsSurfaceContainer)
                    .border(1.dp, AfsOutline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(
                    label = "ADB",
                    isActive = isAdbAvailable,
                    activeText = "Ready",
                    inactiveText = "Off"
                )
                StatusPill(
                    label = "Shizuku",
                    isActive = isShizukuAvailable,
                    activeText = "Active",
                    inactiveText = "None"
                )
                StatusPill(
                    label = "Root",
                    isActive = isRootAvailable,
                    activeText = "Granted",
                    inactiveText = "No Root"
                )
                StatusPill(
                    label = "Target",
                    isActive = targetConnected,
                    activeText = targetTransport,
                    inactiveText = "Disconnected",
                    isTarget = true
                )
            }
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    isActive: Boolean,
    activeText: String,
    inactiveText: String,
    isTarget: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        val dotColor = when {
            isActive && isTarget -> AfsCyan
            isActive -> AfsGreen
            isTarget -> AfsAmber
            else -> AfsRed
        }

        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "$label: ${if (isActive) activeText else inactiveText}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            color = if (isActive) AfsTextPrimary else AfsTextSecondary
        )
    }
}
