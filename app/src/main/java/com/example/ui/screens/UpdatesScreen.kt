package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

private const val RELEASES_API = "https://api.github.com/repos/Christianzz4603/Android-Flashing-Shorts/releases/latest"
private const val RELEASES_PAGE = "https://github.com/Christianzz4603/Android-Flashing-Shorts/releases"

private sealed interface UpdateState {
    data object Checking : UpdateState
    data class UpToDate(val latest: String) : UpdateState
    data class Available(val tag: String, val pageUrl: String, val apkUrl: String?, val notes: String) : UpdateState
    data object NoRelease : UpdateState
    data class Failed(val message: String) : UpdateState
}

private fun parseVersion(v: String): List<Int> =
    v.trim().removePrefix("v").removePrefix("V").split('.', '-', '+')
        .mapNotNull { part -> part.takeWhile { it.isDigit() }.toIntOrNull() }

private fun isNewer(latest: String, current: String): Boolean {
    val a = parseVersion(latest)
    val b = parseVersion(current)
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}

/** Queries the real GitHub Releases API and compares the latest tag with the installed version. */
private suspend fun fetchUpdateState(currentVersion: String): UpdateState = withContext(Dispatchers.IO) {
    try {
        val request = Request.Builder()
            .url(RELEASES_API)
            .header("Accept", "application/vnd.github+json")
            .build()
        OkHttpClient().newCall(request).execute().use { resp ->
            when {
                resp.code == 404 -> UpdateState.NoRelease
                !resp.isSuccessful -> UpdateState.Failed("GitHub returned HTTP ${resp.code}")
                else -> {
                    val json = JSONObject(resp.body?.string().orEmpty())
                    val tag = json.optString("tag_name")
                    val page = json.optString("html_url", RELEASES_PAGE)
                    val notes = json.optString("body")
                    var apk: String? = null
                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val a = assets.getJSONObject(i)
                            if (a.optString("name").endsWith(".apk", ignoreCase = true)) {
                                apk = a.optString("browser_download_url")
                                break
                            }
                        }
                    }
                    if (tag.isBlank()) {
                        UpdateState.NoRelease
                    } else if (isNewer(tag, currentVersion)) {
                        UpdateState.Available(tag, page, apk, notes)
                    } else {
                        UpdateState.UpToDate(tag)
                    }
                }
            }
        }
    } catch (e: Exception) {
        UpdateState.Failed(e.localizedMessage ?: "Network error")
    }
}

@Composable
fun UpdatesScreen() {
    val context = LocalContext.current

    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
    } catch (e: Exception) {
        "1.0"
    }

    var state by remember { mutableStateOf<UpdateState>(UpdateState.Checking) }
    var checkNonce by remember { mutableIntStateOf(0) }

    LaunchedEffect(checkNonce) {
        state = UpdateState.Checking
        state = fetchUpdateState(versionName)
    }

    fun open(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
        }
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
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
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
                    when (val s = state) {
                        UpdateState.Checking -> {
                            CircularProgressIndicator(color = AfsCyan, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Checking GitHub for updates...", fontSize = 13.sp, color = AfsTextSecondary)
                        }
                        is UpdateState.UpToDate -> {
                            Text("You're up to date", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AfsGreen)
                            Text("Latest release: ${s.latest}", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = AfsTextSecondary)
                        }
                        is UpdateState.Available -> {
                            Text("Update available: ${s.tag}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AfsAmber)
                        }
                        UpdateState.NoRelease -> {
                            Text("No releases published yet", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AfsTextPrimary)
                        }
                        is UpdateState.Failed -> {
                            Text("Couldn't check for updates", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AfsRed)
                            Text(s.message, fontSize = 12.sp, color = AfsTextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Installed version: $versionName",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = AfsTextSecondary
                    )
                }
            }
        }

        val current = state
        if (current is UpdateState.Available) {
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
                        Text("RELEASE NOTES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AfsCyan, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = current.notes.ifBlank { "No notes provided for this release." }.take(1200),
                            fontSize = 12.sp,
                            color = AfsTextSecondary,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (current.apkUrl != null) {
                            Button(
                                onClick = { open(current.apkUrl) },
                                colors = ButtonDefaults.buttonColors(containerColor = AfsCyan),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Download APK", color = AfsSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        OutlinedButton(onClick = { open(current.pageUrl) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = AfsCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View release page", color = AfsCyan, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        item {
            Row2(onCheck = { checkNonce++ }, onReleases = { open(RELEASES_PAGE) })
        }
    }
}

@Composable
private fun Row2(onCheck: () -> Unit, onReleases: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onCheck,
            colors = ButtonDefaults.buttonColors(containerColor = AfsGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = AfsSurface, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Check for Updates", color = AfsSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onReleases, modifier = Modifier.fillMaxWidth()) {
            Text("All releases on GitHub", color = AfsCyan, fontSize = 13.sp)
        }
    }
}
