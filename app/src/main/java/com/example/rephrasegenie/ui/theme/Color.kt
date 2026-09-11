package com.example.rephrasegenie.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Colour tokens ported from the Windows app's ThemeManager.
 *
 * The palette is worked out at runtime from two inputs: whether we are in dark mode, and the accent
 * colour the user picked. Everything accent-derived (hover, tint, button text) is calculated rather
 * than hard-coded, so any accent stays readable.
 */
@Immutable
data class RephraseColors(
    val background: Color,
    val header: Color,
    val card: Color,
    val cardBorder: Color,
    val input: Color,
    val inputBorder: Color,
    val inputFocusBorder: Color,
    val accent: Color,
    val accentHover: Color,
    val accentTint: Color,
    val onAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val danger: Color,
    val dangerHover: Color,
    val dangerBackground: Color,
    val dangerBorder: Color,
    val success: Color,
    val isDark: Boolean,
)

/** The eight accent colours offered in Settings. */
val PresetAccents: List<Pair<String, Color>> = listOf(
    "Electric Blue" to Color(0xFF8AB4FF),
    "Emerald Green" to Color(0xFF34D399),
    "Vivid Purple" to Color(0xFFA78BFA),
    "Coral Rose" to Color(0xFFFB7185),
    "Amber Sun" to Color(0xFFF59E0B),
    "Cyan Sky" to Color(0xFF38BDF8),
    "Sunset Orange" to Color(0xFFFB923C),
    "Hot Pink" to Color(0xFFF472B6),
)

const val DEFAULT_ACCENT_HEX = "#8AB4FF"
val DefaultAccent = Color(0xFF8AB4FF)

/**
 * Builds the full palette. Mirrors ThemeManager.ApplyTheme in the Windows app.
 */
fun rephraseColors(isDark: Boolean, accent: Color): RephraseColors {
    val accentHover = if (isDark) accent.lighten(0.15f) else accent.darken(0.12f)
    val accentTint = accent.copy(alpha = if (isDark) 0.208f else 0.145f)

    // A bright accent needs dark text on top of it, otherwise the label disappears.
    val onAccent = if (accent.perceivedLuminance() > 0.55f) Color(0xFF0A0D14) else Color.White

    return if (isDark) {
        RephraseColors(
            background = Color(0xFF0E1017),
            header = Color(0xFF131622),
            card = Color(0xFF161927),
            cardBorder = Color(0xFF252B3E),
            input = Color(0xFF1C2032),
            inputBorder = Color(0xFF2F3750),
            inputFocusBorder = accent,
            accent = accent,
            accentHover = accentHover,
            accentTint = accentTint,
            onAccent = onAccent,
            textPrimary = Color(0xFFEEF0F5),
            textSecondary = Color(0xFF949DB0),
            textMuted = Color(0xFF626B80),
            danger = Color(0xFFFF5C7A),
            dangerHover = Color(0xFFFF7A94),
            dangerBackground = Color(0xFF28151D),
            dangerBorder = Color(0xFF5A1E2B),
            success = Color(0xFF34D399),
            isDark = true,
        )
    } else {
        RephraseColors(
            background = Color(0xFFF1F4F9),
            header = Color(0xFFFFFFFF),
            card = Color(0xFFFFFFFF),
            cardBorder = Color(0xFFD4DCE8),
            input = Color(0xFFF6F8FC),
            inputBorder = Color(0xFFC4CFDE),
            inputFocusBorder = accent,
            accent = accent,
            accentHover = accentHover,
            accentTint = accentTint,
            onAccent = onAccent,
            textPrimary = Color(0xFF0F172A),
            textSecondary = Color(0xFF475569),
            textMuted = Color(0xFF64748B),
            danger = Color(0xFFDC2626),
            dangerHover = Color(0xFFEF4444),
            dangerBackground = Color(0xFFFEF2F2),
            dangerBorder = Color(0xFFFCA5A5),
            success = Color(0xFF059669),
            isDark = false,
        )
    }
}

fun Color.lighten(amount: Float): Color = Color(
    red = red + (1f - red) * amount,
    green = green + (1f - green) * amount,
    blue = blue + (1f - blue) * amount,
    alpha = alpha,
)

fun Color.darken(amount: Float): Color = Color(
    red = red * (1f - amount),
    green = green * (1f - amount),
    blue = blue * (1f - amount),
    alpha = alpha,
)

fun Color.perceivedLuminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

/** Parses "#RRGGBB", "RRGGBB" or "#RGB". Returns [fallback] for anything it cannot read. */
fun parseHexColor(hex: String?, fallback: Color = DefaultAccent): Color {
    if (hex.isNullOrBlank()) return fallback
    val cleaned = hex.trim().removePrefix("#")
    val expanded = when (cleaned.length) {
        3 -> cleaned.map { "$it$it" }.joinToString("")
        6 -> cleaned
        else -> return fallback
    }
    val value = expanded.toLongOrNull(16) ?: return fallback
    return Color(0xFF000000 or value)
}

/** Formats a colour back to "#RRGGBB" for storage. */
fun Color.toHexString(): String {
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(r, g, b)
}
