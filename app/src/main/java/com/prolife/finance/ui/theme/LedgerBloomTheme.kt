package com.prolife.finance.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.prolife.finance.model.ThemePreference

@Immutable
data class LedgerPalette(
    val income: Color,
    val expense: Color,
    val hero: Color,
    val heroAlt: Color,
    val chartAccent: Color,
    val chartMuted: Color,
    val ringColors: List<Color>
)

private val LedgerLightPalette = LedgerPalette(
    income = Color(0xFF34C759),
    expense = Color(0xFFFF3B30),
    hero = Color(0xFFEAF4FF),
    heroAlt = Color(0xFFFFFFFF),
    chartAccent = Color(0xFF0A84FF),
    chartMuted = Color(0xFF8E8E93),
    ringColors = listOf(
        Color(0xFF0A84FF),
        Color(0xFF34C759),
        Color(0xFFFF9F0A),
        Color(0xFFFF2D55)
    )
)

private val LocalLedgerPalette = staticCompositionLocalOf { LedgerLightPalette }

private val LedgerLightColorScheme = lightColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCEEFF),
    onPrimaryContainer = Color(0xFF082742),
    secondary = Color(0xFF5AC8FA),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8F7FF),
    onSecondaryContainer = Color(0xFF143B4B),
    tertiary = Color(0xFFFF9F0A),
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF6D6D72),
    outline = Color(0xFFD1D1D6),
    outlineVariant = Color(0xFFE5E5EA),
    error = Color(0xFFFF3B30),
    onError = Color(0xFFFFFFFF)
)

object LedgerBloomTokens {
    val palette: LedgerPalette
        @Composable get() = LocalLedgerPalette.current
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun LedgerBloomTheme(
    preference: ThemePreference,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLedgerPalette provides LedgerLightPalette) {
        MaterialTheme(
            colorScheme = LedgerLightColorScheme,
            typography = Typography,
            content = content
        )
    }
}
