package com.clairjour.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.clairjour.app.data.prefs.ThemeMode

private val LightScheme = lightColorScheme(
    primary = ClairjourBlue,
    onPrimary = ClairjourIvory,
    primaryContainer = ClairjourBlue,
    onPrimaryContainer = ClairjourIvory,
    secondary = ClairjourGold,
    onSecondary = ClairjourInk,
    secondaryContainer = ClairjourGoldMuted,
    onSecondaryContainer = ClairjourInk,
    tertiary = ClairjourGold,
    onTertiary = ClairjourInk,
    background = ClairjourIvory,
    onBackground = ClairjourInk,
    surface = ClairjourIvory,
    onSurface = ClairjourInk,
    surfaceVariant = ClairjourIvoryDim,
    onSurfaceVariant = ClairjourInkSoft,
    outline = ClairjourGoldMuted,
    outlineVariant = ClairjourIvoryDim,
    surfaceContainerLowest = ClairjourIvoryLightest,
    surfaceContainerLow = ClairjourIvoryLow,
    surfaceContainer = ClairjourIvoryContainer,
    surfaceContainerHigh = ClairjourIvoryDim,
    surfaceContainerHighest = ClairjourIvoryHighest,
    error = ClairjourError,
    onError = ClairjourIvory
)

private val DarkScheme = darkColorScheme(
    primary = ClairjourGold,
    onPrimary = ClairjourBlueDark,
    primaryContainer = ClairjourBlue,
    onPrimaryContainer = ClairjourIvory,
    secondary = ClairjourGold,
    onSecondary = ClairjourBlueDark,
    secondaryContainer = ClairjourBlueDark,
    onSecondaryContainer = ClairjourIvory,
    tertiary = ClairjourGold,
    onTertiary = ClairjourBlueDark,
    background = ClairjourBlueDeep,
    onBackground = ClairjourIvory,
    surface = ClairjourBlueDark,
    onSurface = ClairjourIvory,
    surfaceVariant = ClairjourBlue,
    onSurfaceVariant = ClairjourIvoryDim,
    outline = ClairjourGoldMuted,
    outlineVariant = ClairjourBlue,
    surfaceContainerLowest = ClairjourBlueLowest,
    surfaceContainerLow = ClairjourBlueLow,
    surfaceContainer = ClairjourBlueDark,
    surfaceContainerHigh = ClairjourBlueHigh,
    surfaceContainerHighest = ClairjourBlue,
    error = ClairjourError,
    onError = ClairjourIvory
)

@Composable
fun ClairjourTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme = if (useDark) DarkScheme else LightScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !useDark
            controller.isAppearanceLightNavigationBars = !useDark
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = ClairjourTypography,
        content = content
    )
}

fun Color.contentColorFor(): Color =
    if (luminance() > 0.5f) ClairjourInk else ClairjourIvory
