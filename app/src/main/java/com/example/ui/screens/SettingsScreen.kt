package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodels.FirewallViewModel
import com.example.ui.theme.TextGray

@Composable
fun SettingsScreen(
    viewModel: FirewallViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val startOnBoot by viewModel.startOnBoot.collectAsState()
    val blockIpv6 by viewModel.blockIpv6.collectAsState()
    val blockUdp by viewModel.blockUdp.collectAsState()
    val persistentNotification by viewModel.persistentNotification.collectAsState()
    val loggingEnabled by viewModel.loggingEnabled.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Firewall Configuration",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        Text(
            text = "Fine-tune local network blocking policies",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // General Category Headers
        CategoryHeader(title = "Network Policies")

        SettingsToggleItem(
            title = "Block IPv6 Host Traffic",
            subtitle = "Routes both IPv4 and IPv6 traffic of the blocked app to the local blackhole. Recommended for LTE/5G.",
            checked = blockIpv6,
            icon = Icons.Default.Language,
            onCheckedChange = { viewModel.setBlockIpv6(it) },
            tag = "setting_block_ipv6"
        )

        SettingsToggleItem(
            title = "Block UDP Connections",
            subtitle = "Restricts high-speed UDP-based transport streams (like QUIC/HTTP3 WebRTC VoIP) for firmer blocking.",
            checked = blockUdp,
            icon = Icons.Default.CompareArrows,
            onCheckedChange = { viewModel.setBlockUdp(it) },
            tag = "setting_block_udp"
        )

        Spacer(modifier = Modifier.height(16.dp))
        CategoryHeader(title = "System Integration")

        SettingsToggleItem(
            title = "Start Firewall on Boot",
            subtitle = "Automatically re-engages local VPN protection whenever the S24 is rebooted.",
            checked = startOnBoot,
            icon = Icons.Default.PowerSettingsNew,
            onCheckedChange = { viewModel.setStartOnBoot(it) },
            tag = "setting_start_on_boot"
        )

        SettingsToggleItem(
            title = "Enable Persistent Notification",
            subtitle = "Draws standard Android system foreground indicators. Prevents the OS from garbage collecting the service.",
            checked = persistentNotification,
            icon = Icons.Default.NotificationsActive,
            onCheckedChange = { viewModel.setPersistentNotification(it) },
            tag = "setting_persistent_notification"
        )

        SettingsToggleItem(
            title = "Log Dropped Sockets",
            subtitle = "Enables local connection logging. Tracks IP connection target histories anonymously.",
            checked = loggingEnabled,
            icon = Icons.Default.HistoryToggleOff,
            onCheckedChange = { viewModel.setLoggingEnabled(it) },
            tag = "setting_logging_enabled"
        )

        Spacer(modifier = Modifier.height(16.dp))
        CategoryHeader(title = "Maintenance Actions")

        // Action Item: Recheck apps
        SettingsActionItem(
            title = "Refresh Apps Directory Scan",
            description = "Scans all installed packages on this device and updates the selectable lists.",
            icon = Icons.Default.Refresh,
            actionLabel = "Recheck Scan",
            onClick = {
                viewModel.loadInstalledApps()
                Toast.makeText(context, "Scanning completed. App list updated.", Toast.LENGTH_SHORT).show()
            },
            tag = "action_refresh_apps"
        )

        // Action Item: Export logs
        SettingsActionItem(
            title = "Export Blocks Log History",
            description = "Compiles block transcripts into formatted text files and copies them to sharing pipelines.",
            icon = Icons.Default.Share,
            actionLabel = "Export TXT",
            onClick = {
                val exportText = viewModel.getExportedLogsText()
                
                // Copy to clipboard
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("WAB Firewall Block Logs", exportText)
                clipboard.setPrimaryClip(clip)
                
                // Native system share intent
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, exportText)
                    type = "text/plain"
                }
                val chooser = Intent.createChooser(shareIntent, "Save or post WAB Firewall Logs:")
                context.startActivity(chooser)
                Toast.makeText(context, "Log files copied to clipboard!", Toast.LENGTH_SHORT).show()
            },
            tag = "action_export_logs"
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun CategoryHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 10.dp)
    )
}

@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    icon: ImageVector,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (checked) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    lineHeight = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
fun SettingsActionItem(
    title: String,
    description: String,
    icon: ImageVector,
    actionLabel: String,
    onClick: () -> Unit,
    tag: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    lineHeight = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text = actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
