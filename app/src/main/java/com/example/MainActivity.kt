package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.AfsViewModel
import com.example.ui.MainTab
import com.example.ui.components.ConsoleResultDialog
import com.example.ui.components.DestructiveConfirmationDialog
import com.example.ui.components.HeaderStatusBar
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AnotherPhoneScreen
import com.example.ui.screens.HomeMenuScreen
import com.example.ui.screens.HostPrimaryCategory
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ThisPhoneScreen
import com.example.ui.screens.UpdatesScreen
import com.example.ui.theme.AfsSurface
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

    BackHandler(enabled = currentTab != MainTab.HOME) {
        viewModel.setTab(MainTab.HOME)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AfsSurface,
        topBar = {
            if (currentTab != MainTab.HOME) {
                HeaderStatusBar(
                    isAdbAvailable = isAdbAvailable,
                    isShizukuAvailable = isShizukuAvailable,
                    isRootAvailable = isRootAvailable,
                    targetConnected = targetState.isConnected,
                    targetTransport = targetState.transport.label,
                    onRefresh = {
                        viewModel.refreshHostInfo()
                        viewModel.refreshCapabilities()
                    },
                    onBack = { viewModel.setTab(MainTab.HOME) }
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
                MainTab.HOME -> HomeMenuScreen(onNavigate = { tab -> viewModel.setTab(tab) })
                MainTab.THIS_PHONE -> ThisPhoneScreen(viewModel)
                MainTab.ANOTHER_PHONE -> AnotherPhoneScreen(viewModel)
                MainTab.APP_MANAGEMENT -> ThisPhoneScreen(viewModel, initialCategory = HostPrimaryCategory.APPS)
                MainTab.SETTINGS -> SettingsScreen(viewModel)
                MainTab.ABOUT -> AboutScreen()
                MainTab.UPDATES -> UpdatesScreen()
            }

            if (showResultDialog && lastResult != null) {
                ConsoleResultDialog(
                    result = lastResult!!,
                    onDismiss = { viewModel.dismissResultModal() }
                )
            }

            destructiveDialog?.let { action ->
                DestructiveConfirmationDialog(
                    action = action,
                    onDismiss = { viewModel.dismissDestructiveDialog() }
                )
            }
        }
    }
}
