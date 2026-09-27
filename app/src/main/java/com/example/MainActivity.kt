package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AfsViewModel
import com.example.ui.MainTab
import com.example.ui.components.ConsoleResultDialog
import com.example.ui.components.DestructiveConfirmationDialog
import com.example.ui.components.HeaderStatusBar
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AnotherPhoneScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ThisPhoneScreen
import com.example.ui.theme.AfsCyan
import com.example.ui.theme.AfsOutline
import com.example.ui.theme.AfsSurface
import com.example.ui.theme.AfsSurfaceContainer
import com.example.ui.theme.AfsTextPrimary
import com.example.ui.theme.AfsTextSecondary
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AfsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AfsMainApp(viewModel)
            }
        }
    }
}

@Composable
fun AfsMainApp(viewModel: AfsViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val isAdbAvailable by viewModel.isAdbAvailable.collectAsState()
    val isShizukuAvailable by viewModel.isShizukuAvailable.collectAsState()
    val isRootAvailable by viewModel.isRootAvailable.collectAsState()
    val targetState by viewModel.targetDeviceState.collectAsState()
    val showResultDialog by viewModel.showResultDialog.collectAsState()
    val lastResult by viewModel.lastCommandResult.collectAsState()
    val destructiveDialog by viewModel.destructiveDialog.collectAsState()

    // Handle back button to return to THIS_PHONE if on another tab
    BackHandler(enabled = currentTab != MainTab.THIS_PHONE) {
        viewModel.setTab(MainTab.THIS_PHONE)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AfsSurface,
        topBar = {
            HeaderStatusBar(
                isAdbAvailable = isAdbAvailable,
                isShizukuAvailable = isShizukuAvailable,
                isRootAvailable = isRootAvailable,
                targetConnected = targetState.isConnected,
                targetTransport = targetState.transport.label,
                onRefresh = {
                    viewModel.refreshHostInfo()
                    viewModel.refreshCapabilities()
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = AfsSurfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AfsOutline.copy(alpha = 0.5f))
                    .navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = currentTab == MainTab.THIS_PHONE,
                    onClick = { viewModel.setTab(MainTab.THIS_PHONE) },
                    icon = { Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = "This Phone") },
                    label = {
                        Text(
                            text = "THIS PHONE",
                            fontSize = 10.sp,
                            fontWeight = if (currentTab == MainTab.THIS_PHONE) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AfsCyan,
                        selectedTextColor = AfsCyan,
                        unselectedIconColor = AfsTextSecondary,
                        unselectedTextColor = AfsTextSecondary,
                        indicatorColor = AfsCyan.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_this_phone")
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.ANOTHER_PHONE,
                    onClick = { viewModel.setTab(MainTab.ANOTHER_PHONE) },
                    icon = { Icon(imageVector = Icons.Default.Cable, contentDescription = "Another Phone") },
                    label = {
                        Text(
                            text = "ANOTHER PHONE",
                            fontSize = 10.sp,
                            fontWeight = if (currentTab == MainTab.ANOTHER_PHONE) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AfsCyan,
                        selectedTextColor = AfsCyan,
                        unselectedIconColor = AfsTextSecondary,
                        unselectedTextColor = AfsTextSecondary,
                        indicatorColor = AfsCyan.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_another_phone")
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.SETTINGS,
                    onClick = { viewModel.setTab(MainTab.SETTINGS) },
                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                    label = {
                        Text(
                            text = "SETTINGS",
                            fontSize = 10.sp,
                            fontWeight = if (currentTab == MainTab.SETTINGS) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AfsCyan,
                        selectedTextColor = AfsCyan,
                        unselectedIconColor = AfsTextSecondary,
                        unselectedTextColor = AfsTextSecondary,
                        indicatorColor = AfsCyan.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_settings")
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.ABOUT,
                    onClick = { viewModel.setTab(MainTab.ABOUT) },
                    icon = { Icon(imageVector = Icons.Default.Info, contentDescription = "About") },
                    label = {
                        Text(
                            text = "ABOUT",
                            fontSize = 10.sp,
                            fontWeight = if (currentTab == MainTab.ABOUT) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AfsCyan,
                        selectedTextColor = AfsCyan,
                        unselectedIconColor = AfsTextSecondary,
                        unselectedTextColor = AfsTextSecondary,
                        indicatorColor = AfsCyan.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_about")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AfsSurface)
        ) {
            when (currentTab) {
                MainTab.THIS_PHONE -> ThisPhoneScreen(viewModel)
                MainTab.ANOTHER_PHONE -> AnotherPhoneScreen(viewModel)
                MainTab.SETTINGS -> SettingsScreen(viewModel)
                MainTab.ABOUT -> AboutScreen()
            }

            // Command Output Console Dialog
            if (showResultDialog && lastResult != null) {
                ConsoleResultDialog(
                    result = lastResult!!,
                    onDismiss = { viewModel.dismissResultModal() }
                )
            }

            // Explicit Destructive Confirmation Alert
            destructiveDialog?.let { action ->
                DestructiveConfirmationDialog(
                    action = action,
                    onDismiss = { viewModel.dismissDestructiveDialog() }
                )
            }
        }
    }
}
