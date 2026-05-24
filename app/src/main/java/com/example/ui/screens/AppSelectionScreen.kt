package com.example.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.models.AppInfo
import com.example.repository.FirewallSettingsStore
import com.example.viewmodels.FirewallViewModel
import com.example.ui.theme.TextWhite
import com.example.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(
    viewModel: FirewallViewModel,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val isAppsLoading by viewModel.isAppsLoading.collectAsState()

    // Load list when screen is composed
    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Block Apps Selector",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        
        Text(
            text = "Select which applications to route to the dead-end VPN tunnel. WhatsApp Business is blocked by default.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Search Text Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search apps or package...", color = Color.Gray, fontSize = 14.sp) },
            prefix = { Spacer(modifier = Modifier.width(4.dp)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search icon",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = Color.Gray
                        )
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("app_search_input")
        )

        // Loading and Empty States
        if (isAppsLoading && installedApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (installedApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = "No apps found",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No applications match your search",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Try searching with a different term.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            // Apps list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(installedApps, key = { it.packageName }) { app ->
                    val isTargetApp = app.packageName == FirewallSettingsStore.WHATSAPP_BUSINESS_PACKAGE
                    
                    AppListItem(
                        app = app,
                        isTargetApp = isTargetApp,
                        onAppIconLoad = { viewModel.getAppIcon(app.packageName) },
                        onBlockedToggle = { viewModel.toggleAppBlockedStatus(app.packageName) }
                    )
                }
            }
        }
    }
}

@Composable
fun AppListItem(
    app: AppInfo,
    isTargetApp: Boolean,
    onAppIconLoad: () -> Drawable?,
    onBlockedToggle: () -> Unit
) {
    // Dynamically retrieve application icon caching locally
    val appIcon = remember(app.packageName) { onAppIconLoad() }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isTargetApp) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (isTargetApp) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_item_${app.packageName}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon Space
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon.toBitmap().asImageBitmap(),
                        contentDescription = "${app.appName} Icon",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = "Default app icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // App details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isTargetApp) MaterialTheme.colorScheme.primary else TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isTargetApp) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "DEFAULT",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Blocking Toggle Check / Switch
            Switch(
                checked = app.isBlocked,
                onCheckedChange = { onBlockedToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.testTag("block_toggle_${app.packageName}")
            )
        }
    }
}
