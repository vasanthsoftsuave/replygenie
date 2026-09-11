package com.example.rephrasegenie.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.rephrasegenie.domain.model.ThemeMode

val LocalRephraseColors = staticCompositionLocalOf {
    rephraseColors(isDark = true, accent = DefaultAccent)
}

/** Shorthand: `AppTheme.colors.accent` from any composable. */
object AppTheme {
    val colors: RephraseColors
        @Composable get() = LocalRephraseColors.current
}

@Composable
fun RephraseGenieTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    accentHex: String = DEFAULT_ACCENT_HEX,
    lightBackdrop: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }

    val colors = rephraseColors(isDark, parseHexColor(accentHex))

    // The window is edge-to-edge with transparent system bars, so the clock and the battery icon
    // are drawn by the system on top of whatever we paint. Without this they stay dark on the dark
    // theme and are unreadable. Driven from [isDark] rather than set once at startup, so switching
    // the theme in Settings fixes them straight away.
    //
    // [lightBackdrop] overrides it for content that is light whatever the theme says — the intro
    // video, which is near-white and made the white icons disappear into it.
    val darkIcons = lightBackdrop || !isDark
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = darkIcons
                isAppearanceLightNavigationBars = darkIcons
            }
        }
    }

    // Material 3 still drives built-in components, so keep its scheme in step with ours.
    val materialScheme = if (isDark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.card,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.input,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.inputBorder,
            error = colors.danger,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.card,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.input,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.inputBorder,
            error = colors.danger,
        )
    }

    CompositionLocalProvider(LocalRephraseColors provides colors) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = RephraseTypography,
            shapes = RephraseShapes,
            content = content,
        )
    }
}

/** Convenience for previews and the overlay, which builds views outside the Compose tree. */
fun overlayColors(isDark: Boolean, accentHex: String): RephraseColors =
    rephraseColors(isDark, parseHexColor(accentHex))

internal val Transparent = Color.Transparent
