package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = ElegantPrimary,
    primaryContainer = ElegantPrimaryContainer,
    onPrimary = ElegantOnPrimary,
    secondary = SecondaryGray,
    tertiary = ElegantSuccess,
    background = ElegantDarkBackground,
    surface = ElegantSurface,
    onBackground = TextWhite,
    onSurface = TextWhite,
    surfaceVariant = ElegantSurface,
    outline = ElegantOutline,
    error = ElegantError
  )

private val LightColorScheme = DarkColorScheme // Standard security deep theme preferred for both modes

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force Dark Security Slate theme for modern high-tech firewall look
  dynamicColor: Boolean = false, // Keep consistent branding colors
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
