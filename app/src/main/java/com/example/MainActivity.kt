package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppSettingsAlt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.example.services.FirewallVpnService
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodels.FirewallViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FirewallViewModel by viewModels()

    // Activity launcher for Android standard VPN activation dialog confirmation
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // User approved local VPN tunneling activation! Start the service.
            viewModel.toggleFirewall()
        } else {
            Toast.makeText(
                this,
                "VPN authorization is strictly required to establish the local firewall block.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Activity launcher for Android 13+ POST_NOTIFICATIONS permission
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { progress ->
        // Proceed gracefully regardless of approval
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge to Edge fully immersive visual transparency standard
        enableEdgeToEdge()

        // Audit & request standard post notifications on modern OS targets
        checkAndRequestNotificationPermissions()

        setContent {
            MyApplicationTheme {
                MainContainerScreen(
                    viewModel = viewModel,
                    onRequestVpnPermission = { initiateVpnActivationFlow() }
                )
            }
        }
    }

    private fun checkAndRequestNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * Standard VPN permission authorization flow.
     * VpnService.prepare checks if permission is already present. 
     * If not, it returns an intent which must be activated via a system dialog.
     */
    private fun initiateVpnActivationFlow() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            // Already authorized! Proceed to start/stop toggle securely.
            viewModel.toggleFirewall()
        }
    }
}

/**
 * Clean Single-View consolidated Scaffold containing Tab Navigation and modern M3 Styling.
 */
@Composable
fun MainContainerScreen(
    viewModel: FirewallViewModel,
    onRequestVpnPermission: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    val navigationItems = listOf(
        NavigationTabItem("Home", Icons.Default.Home, Icons.Outlined.Home),
        NavigationTabItem("Apps", Icons.Default.List, Icons.Outlined.List),
        NavigationTabItem("Logs", Icons.Default.ReceiptLong, Icons.Outlined.ReceiptLong),
        NavigationTabItem("Settings", Icons.Default.Settings, Icons.Outlined.Settings),
        NavigationTabItem("Privacy", Icons.Default.PrivacyTip, Icons.Outlined.PrivacyTip)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                tonalElevation = NavigationBarDefaults.Elevation,
                modifier = Modifier.fillMaxWidth()
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                    modifier = Modifier.testTag("app_navigation_bar")
                ) {
                    navigationItems.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            label = { Text(tab.title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == index) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.title.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        val innerModifier = Modifier.padding(paddingValues)
        when (selectedTab) {
            0 -> HomeScreen(viewModel, onRequestVpnPermission, innerModifier)
            1 -> AppSelectionScreen(viewModel, innerModifier)
            2 -> LogsScreen(viewModel, innerModifier)
            3 -> SettingsScreen(viewModel, innerModifier)
            4 -> PrivacyScreen(viewModel, innerModifier)
        }
    }
}

data class NavigationTabItem(
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)
