package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
fun UpdatesScreen() {
    val context = LocalContext.current

    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
    } catch (e: Exception) {
        "1.0"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AfsSurface)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AfsSurfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AfsOutline, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEC407A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "You're up to date",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = AfsTextPrimary
                    )
                    Text(
                        text = "Installed version: $versionName",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = AfsTextSecondary
                    )
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
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "RELEASE CHANNEL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AfsCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "This build is distributed directly from GitHub rather than an app store, so new versions are published as GitHub Releases.",
                        fontSize = 12.sp,
                        color = AfsTextSecondary,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/Christianzz4603/Android-Flashing-Shorts/releases")
                            )
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AfsCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = AfsSurface, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Releases on GitHub", color = AfsSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "You're on the latest version ($versionName)", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Check for Updates", color = AfsGreen, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
