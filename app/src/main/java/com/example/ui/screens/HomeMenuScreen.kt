package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainTab
import com.example.ui.theme.AfsOutline
import com.example.ui.theme.AfsSurface
import com.example.ui.theme.AfsSurfaceContainer
import com.example.ui.theme.AfsSurfaceContainerHigh
import com.example.ui.theme.AfsTextPrimary
import com.example.ui.theme.AfsTextSecondary

private data class HomeMenuEntry(
    val label: String,
    val icon: ImageVector,
    val iconColor: Color,
    val tab: MainTab
)

private val homeMenuEntries = listOf(
    HomeMenuEntry("ADB Tools", Icons.Default.Android, Color(0xFF1DE9B6), MainTab.THIS_PHONE),
    HomeMenuEntry("Fastboot Tools", Icons.Default.Build, Color(0xFF29B6F6), MainTab.ANOTHER_PHONE),
    HomeMenuEntry("App Management", Icons.Default.Apps, Color(0xFF2979FF), MainTab.APP_MANAGEMENT),
    HomeMenuEntry("About", Icons.Default.Info, Color(0xFFFFA726), MainTab.ABOUT),
    HomeMenuEntry("Settings", Icons.Default.Settings, Color(0xFFEC407A), MainTab.SETTINGS),
    HomeMenuEntry("Updates", Icons.Default.History, Color(0xFFEC407A), MainTab.UPDATES)
)

@Composable
fun HomeMenuScreen(onNavigate: (MainTab) -> Unit) {
    var query by remember { mutableStateOf("") }

    val filteredEntries = if (query.isBlank()) {
        homeMenuEntries
    } else {
        homeMenuEntries.filter { it.label.contains(query, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AfsSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, top = 12.dp, bottom = 4.dp)
        ) {
            Text(
                text = "Android Flashing Shortcuts",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AfsTextPrimary
            )
            Text(
                text = "Pick a tool to get started",
                fontSize = 12.sp,
                color = AfsTextSecondary,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search ADB and fastboot commands...", fontSize = 13.sp, color = AfsTextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AfsTextSecondary) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = AfsSurfaceContainer,
                unfocusedContainerColor = AfsSurfaceContainer,
                focusedIndicatorColor = AfsOutline,
                unfocusedIndicatorColor = AfsOutline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredEntries, key = { it.label }) { entry ->
                HomeMenuRow(entry = entry, onClick = { onNavigate(entry.tab) })
            }
        }
    }
}

@Composable
private fun HomeMenuRow(entry: HomeMenuEntry, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AfsSurfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, AfsOutline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(entry.iconColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = entry.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = entry.label,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = AfsTextPrimary
            )
        }
    }
}
