package com.example.ui.screens

import android.content.Context
import android.net.VpnService
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.viewmodels.FirewallViewModel
import com.example.ui.theme.TextWhite
import com.example.ui.theme.TextGray

@Composable
fun HomeScreen(
    viewModel: FirewallViewModel,
    onRequestVpnPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isRunning by viewModel.isFirewallRunning.collectAsState()
    val isW4bInstalled by viewModel.isW4bInstalled.collectAsState()
    val isW4bBlocked by viewModel.isW4bBlocked.collectAsState()

    // Real-time check if VPN permission is actually missing
    var isPermissionMissing by remember {
        mutableStateOf(VpnService.prepare(context) != null)
    }

    // Refresh permission check when screen gains focus or isRunning changes
    LaunchedEffect(isRunning) {
        isPermissionMissing = VpnService.prepare(context) != null
        viewModel.checkWhatsAppBusinessStatus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Elegant Dark Brand Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Firewall",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "WhatsApp Business Edition",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            // Subtle status badge with a live neon green / orange led dot
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRunning) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                )
            }
        }

        // VPN Authorization Status Warning Banner
        if (isPermissionMissing) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRequestVpnPermission() }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "VPN Permission Missing",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "VPN Authorization Required",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Tap here to allow VPN pairing & setup the secure blackhole.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // --- Active Protection Rounded Card Containing the Sphere Trigger ---
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val scaleAnim by animateFloatAsState(
                    targetValue = if (isRunning) 1.05f else 1.0f,
                    animationSpec = repeatallInfiniteSpec(),
                    label = "pulse"
                )

                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulsing Ring Glow Effect behind trigger
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = if (isRunning) {
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                            Color.Transparent
                                        )
                                    } else {
                                        listOf(
                                            Color.Gray.copy(alpha = 0.05f),
                                            Color.Transparent
                                        )
                                    }
                                )
                            )
                    )

                    // Compact Interactive Action Sphere
                    Surface(
                        onClick = {
                            if (isPermissionMissing) {
                                onRequestVpnPermission()
                            } else {
                                viewModel.toggleFirewall()
                            }
                        },
                        shape = CircleShape,
                        color = if (isRunning) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        },
                        border = BorderStroke(
                            width = 2.dp,
                            color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .size(130.dp * scaleAnim)
                            .testTag("firewall_toggle_button")
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_shield_lock),
                                contentDescription = "Activate Local Shield Button",
                                tint = if (isRunning) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Text status labels
                Text(
                    text = if (isRunning) "Protection Active" else "Firewall Stopped",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isRunning) "Outbound WhatsApp Business traffic is locked in localhost" else "App traffic bypasses the VPN tunnel",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp).padding(horizontal = 24.dp)
                )
            }
        }

        // --- Status details widgets grid ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Target App grid element
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "TARGET APP",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF25D366)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "W4B indicator logo",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "WA Business",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isW4bInstalled) "Installed & Linked" else "System Missing",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (isW4bInstalled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                }
            }

            // Firewall Mode network info card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "NETWORK MODE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NetworkWifi,
                            contentDescription = "Vpn Connection Mode Icon",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Local VPN",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tunnel Ready",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        // Bottom Technical context warning
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Operation Info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Technical Operation Information",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "This application handles all IP frames fully locally inside volatility buffers. Your phone acts as the singular firewall filter, making it completely private.",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 15.sp),
                    color = TextGray
                )
            }
        }
    }
}

private fun repeatallInfiniteSpec(): AnimationSpec<Float> {
    return infiniteRepeatable(
        animation = tween(durationMillis = 1500, easing = LinearOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    )
}

