package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val LocalThemePreset = compositionLocalOf { ThemePreset.SHARK_BLUE }
val LocalFontColorPreset = compositionLocalOf { FontColorPreset.DEEP_CHARCOAL }
val LocalFontScale = compositionLocalOf { 1.0f }

@Composable
fun MyApplicationTheme(
    themePreset: ThemePreset = ThemePreset.SHARK_BLUE,
    fontColorPreset: FontColorPreset = FontColorPreset.DEEP_CHARCOAL,
    fontSizeScale: Float = 1.0f,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val isDark = themePreset == ThemePreset.DARK_SLATE || (darkTheme && themePreset != ThemePreset.SHARK_BLUE)

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = themePreset.primary,
            onPrimary = Color.White,
            primaryContainer = themePreset.primaryLight,
            onPrimaryContainer = Color.White,
            secondary = PastelMintSavings,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFF1B4332),
            onSecondaryContainer = PastelMintLight,
            tertiary = PastelPeachVar,
            onTertiary = Color.White,
            background = themePreset.background,
            onBackground = Color(0xFFF1F5F9),
            surface = themePreset.surface,
            onSurface = Color(0xFFF8FAFC),
            surfaceVariant = Color(0xFF334155),
            onSurfaceVariant = Color(0xFFCBD5E1),
            outline = themePreset.border
        )
    } else {
        lightColorScheme(
            primary = themePreset.primary,
            onPrimary = Color.White,
            primaryContainer = themePreset.primaryLight,
            onPrimaryContainer = themePreset.primaryDark,
            secondary = PastelMintSavings,
            onSecondary = Color.White,
            secondaryContainer = PastelMintLight,
            onSecondaryContainer = fontColorPreset.primaryColor,
            tertiary = PastelPeachVar,
            onTertiary = Color.White,
            tertiaryContainer = PastelPeachLight,
            onTertiaryContainer = fontColorPreset.primaryColor,
            background = themePreset.background,
            onBackground = fontColorPreset.primaryColor,
            surface = Color.White,
            onSurface = fontColorPreset.primaryColor,
            surfaceVariant = themePreset.surface,
            onSurfaceVariant = fontColorPreset.secondaryColor,
            outline = themePreset.border
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            }
        }
    }

    // Dynamic Typography scaled by fontSizeScale
    val scale = fontSizeScale.coerceIn(0.80f, 1.40f)
    val customTypography = Typography(
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = (16 * scale).sp,
            lineHeight = (24 * scale).sp,
            letterSpacing = 0.5.sp,
            color = fontColorPreset.primaryColor
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = (14 * scale).sp,
            lineHeight = (20 * scale).sp,
            color = fontColorPreset.secondaryColor
        ),
        bodySmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = (12 * scale).sp,
            lineHeight = (16 * scale).sp,
            color = fontColorPreset.mutedColor
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = (20 * scale).sp,
            lineHeight = (26 * scale).sp,
            color = fontColorPreset.primaryColor
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = (16 * scale).sp,
            lineHeight = (22 * scale).sp,
            color = fontColorPreset.primaryColor
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = (11 * scale).sp,
            lineHeight = (14 * scale).sp,
            color = fontColorPreset.mutedColor
        )
    )

    CompositionLocalProvider(
        LocalThemePreset provides themePreset,
        LocalFontColorPreset provides fontColorPreset,
        LocalFontScale provides scale
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = customTypography,
            content = content
        )
    }
}
