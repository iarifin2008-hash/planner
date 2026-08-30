package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// 1. PASTEL THEME PRESETS & GRADIENTS
// ==========================================

enum class ThemePreset(
    val id: String,
    val displayName: String,
    val primary: Color,
    val primaryLight: Color,
    val primaryDark: Color,
    val surface: Color,
    val background: Color,
    val border: Color,
    val gradientColors: List<Color>
) {
    SHARK_BLUE(
        id = "SHARK_BLUE",
        displayName = "Pastel Shark Blue",
        primary = Color(0xFF6599B8),
        primaryLight = Color(0xFFD6EAF8),
        primaryDark = Color(0xFF2B536E),
        surface = Color(0xFFEBF5FB),
        background = Color(0xFFF4F9FC),
        border = Color(0xFFBCE0FD),
        gradientColors = listOf(Color(0xFF6599B8), Color(0xFF74C69D))
    ),
    SWEET_ROSE(
        id = "SWEET_ROSE",
        displayName = "Pastel Sweet Rose",
        primary = Color(0xFFE78EA9),
        primaryLight = Color(0xFFFFE3E8),
        primaryDark = Color(0xFF7D2E49),
        surface = Color(0xFFFFF0F3),
        background = Color(0xFFFFF7F9),
        border = Color(0xFFFFCCD5),
        gradientColors = listOf(Color(0xFFE78EA9), Color(0xFFF4A261))
    ),
    MINT_SAGE(
        id = "MINT_SAGE",
        displayName = "Pastel Mint Sage",
        primary = Color(0xFF52B788),
        primaryLight = Color(0xFFD8F3DC),
        primaryDark = Color(0xFF1B4332),
        surface = Color(0xFFE8F5E9),
        background = Color(0xFFF1F8F4),
        border = Color(0xFFB7E4C7),
        gradientColors = listOf(Color(0xFF52B788), Color(0xFF95D5B2))
    ),
    LAVENDER_DREAM(
        id = "LAVENDER_DREAM",
        displayName = "Pastel Lavender",
        primary = Color(0xFFA594F9),
        primaryLight = Color(0xFFEDE7F6),
        primaryDark = Color(0xFF4A148C),
        surface = Color(0xFFF3E8FF),
        background = Color(0xFFF9F5FF),
        border = Color(0xFFDDD6FE),
        gradientColors = listOf(Color(0xFFA594F9), Color(0xFF7091F5))
    ),
    SUNSET_PEACH(
        id = "SUNSET_PEACH",
        displayName = "Pastel Sunset Peach",
        primary = Color(0xFFF4A261),
        primaryLight = Color(0xFFFFE8D6),
        primaryDark = Color(0xFF8D4B1C),
        surface = Color(0xFFFFF3E0),
        background = Color(0xFFFFF9F2),
        border = Color(0xFFFFD8B8),
        gradientColors = listOf(Color(0xFFF4A261), Color(0xFFE76F51))
    ),
    DARK_SLATE(
        id = "DARK_SLATE",
        displayName = "Pastel Midnight Slate",
        primary = Color(0xFF7E8CE0),
        primaryLight = Color(0xFF2A3447),
        primaryDark = Color(0xFFE2E8F0),
        surface = Color(0xFF1E293B),
        background = Color(0xFF0F172A),
        border = Color(0xFF475569),
        gradientColors = listOf(Color(0xFF6366F1), Color(0xFFA855F7))
    );

    companion object {
        fun fromId(id: String): ThemePreset = entries.find { it.id.equals(id, ignoreCase = true) } ?: SHARK_BLUE
    }
}

// ==========================================
// 2. FONT COLOR PRESETS
// ==========================================

enum class FontColorPreset(
    val id: String,
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val mutedColor: Color
) {
    DEEP_CHARCOAL(
        id = "DEEP_CHARCOAL",
        displayName = "Deep Charcoal (Modern)",
        primaryColor = Color(0xFF1E293B),
        secondaryColor = Color(0xFF475569),
        mutedColor = Color(0xFF94A3B8)
    ),
    NAVY_MIDNIGHT(
        id = "NAVY_MIDNIGHT",
        displayName = "Navy Midnight",
        primaryColor = Color(0xFF0F172A),
        secondaryColor = Color(0xFF334155),
        mutedColor = Color(0xFF64748B)
    ),
    ESPRESSO(
        id = "ESPRESSO",
        displayName = "Warm Espresso",
        primaryColor = Color(0xFF382218),
        secondaryColor = Color(0xFF5C3D2E),
        mutedColor = Color(0xFF8C6D58)
    ),
    PLUM_VIOLET(
        id = "PLUM_VIOLET",
        displayName = "Plum Velvet",
        primaryColor = Color(0xFF3B0764),
        secondaryColor = Color(0xFF581C87),
        mutedColor = Color(0xFF865DAB)
    ),
    TEAL_FOREST(
        id = "TEAL_FOREST",
        displayName = "Forest Teal",
        primaryColor = Color(0xFF064E3B),
        secondaryColor = Color(0xFF047857),
        mutedColor = Color(0xFF4B9080)
    );

    companion object {
        fun fromId(id: String): FontColorPreset = entries.find { it.id.equals(id, ignoreCase = true) } ?: DEEP_CHARCOAL
    }
}

// ==========================================
// 3. FONT SIZE PRESETS
// ==========================================

enum class FontSizePreset(val label: String, val scale: Float) {
    SMALL("Kecil (0.88x)", 0.88f),
    NORMAL("Sedang / Standar (1.0x)", 1.0f),
    LARGE("Besar (1.15x)", 1.15f),
    EXTRA_LARGE("Sangat Besar (1.30x)", 1.30f);

    companion object {
        fun fromScale(scale: Float): FontSizePreset = when {
            scale <= 0.90f -> SMALL
            scale <= 1.05f -> NORMAL
            scale <= 1.20f -> LARGE
            else -> EXTRA_LARGE
        }
    }
}

// ==========================================
// 4. BUDGET CATEGORY PALETTES
// ==========================================

val PastelMintSavings = Color(0xFF74C69D)    // Green/Mint for Savings & Invest
val PastelMintLight = Color(0xFFD8F3DC)
val PastelCoralFixed = Color(0xFFE2847A)     // Coral/Red for Fixed Cost
val PastelCoralLight = Color(0xFFFFECEB)
val PastelPeachVar = Color(0xFFF4A261)       // Peach/Yellow for Variable Cost
val PastelPeachLight = Color(0xFFFFF1E6)
val PastelLilacSub = Color(0xFFA594F9)       // Lavender for Subscriptions
val PastelLilacLight = Color(0xFFF3E8FF)
val PastelGoldIncome = Color(0xFF4895EF)     // Blue/Gold for Income
val PastelGoldLight = Color(0xFFE0EEFD)

// Default / Fallback Colors
val PastelSkyPrimary = Color(0xFF6599B8)
val PastelSkyDark = Color(0xFF2B536E)
val PastelSkyLight = Color(0xFFD6EAF8)
val PastelSkySurface = Color(0xFFEBF5FB)
val PastelCardBg = Color(0xFFFFFFFF)
val PastelCardBorder = Color(0xFFBCE0FD)

val TextPrimaryDark = Color(0xFF1E293B)
val TextSecondaryMuted = Color(0xFF64748B)
val TextCaption = Color(0xFF94A3B8)
val BackgroundCream = Color(0xFFF4F9FC)
val SurfaceContainer = Color(0xFFE8F1F8)
val SurfaceBorder = Color(0xFFD1E3F0)

// Priority Badges
val PriorityHighBg = Color(0xFFFFD8D8)
val PriorityHighText = Color(0xFFC53030)
val PriorityMedBg = Color(0xFFFFF0D4)
val PriorityMedText = Color(0xFFB7791F)
val PriorityLowBg = Color(0xFFE2F7E2)
val PriorityLowText = Color(0xFF276749)

// Helper Gradient Brush
fun getThemeGradientBrush(preset: ThemePreset): Brush {
    return Brush.horizontalGradient(preset.gradientColors)
}
