package com.t3codes.mobile

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

enum class ThemePref { System, Light, Dark }

// The official mobile client uses a soft rose accent on deep neutral
// surfaces. Surfaces intentionally avoid any violet cast so the accent
// carries the brand instead of the background.
private val T3Light = lightColorScheme(
    primary = Color(0xFF8B3C76),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF4D6E8),
    onPrimaryContainer = Color(0xFF3B1030),
    secondary = Color(0xFF615761),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEBE0E9),
    onSecondaryContainer = Color(0xFF2A252A),
    tertiary = Color(0xFF266B5C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBCEEDA),
    onTertiaryContainer = Color(0xFF00211A),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE2E2E7),
    onSurfaceVariant = Color(0xFF47474E),
    outline = Color(0xFF73737C),
    outlineVariant = Color(0xFFD5D5DB),
    inverseSurface = Color(0xFF2E2E33),
    inverseOnSurface = Color(0xFFE6E4E9),
    inversePrimary = Color(0xFFDFA5C6),
    surfaceDim = Color(0xFFDEDEE3),
    surfaceBright = Color(0xFFFAFAFA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F3F5),
    surfaceContainer = Color(0xFFEBEBEE),
    surfaceContainerHigh = Color(0xFFE5E5E9),
    surfaceContainerHighest = Color(0xFFDFDFE4),
)

private val T3Dark = darkColorScheme(
    primary = Color(0xFFDFA5C6),
    onPrimary = Color(0xFF40182F),
    primaryContainer = Color(0xFF50304A),
    onPrimaryContainer = Color(0xFFF5D5E4),
    secondary = Color(0xFFCFC6CE),
    onSecondary = Color(0xFF322F33),
    secondaryContainer = Color(0xFF2E2E34),
    onSecondaryContainer = Color(0xFFE5E1E7),
    tertiary = Color(0xFF8FD8C5),
    onTertiary = Color(0xFF00382D),
    tertiaryContainer = Color(0xFF17493F),
    onTertiaryContainer = Color(0xFFABF5DF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF7B2827),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121215),
    onBackground = Color(0xFFECE9EE),
    surface = Color(0xFF121215),
    onSurface = Color(0xFFECE9EE),
    surfaceVariant = Color(0xFF3A3A42),
    onSurfaceVariant = Color(0xFFB4B2BC),
    outline = Color(0xFF6F6F79),
    outlineVariant = Color(0xFF2E2E36),
    inverseSurface = Color(0xFFE6E4E9),
    inverseOnSurface = Color(0xFF2E2E33),
    inversePrimary = Color(0xFF8B3C76),
    surfaceDim = Color(0xFF121215),
    surfaceBright = Color(0xFF38383F),
    surfaceContainerLowest = Color(0xFF0E0E11),
    surfaceContainerLow = Color(0xFF1E1E23),
    surfaceContainer = Color(0xFF232329),
    surfaceContainerHigh = Color(0xFF2B2B32),
    surfaceContainerHighest = Color(0xFF35353E),
)

private val T3Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 52.sp,
        lineHeight = 58.sp,
        letterSpacing = (-1.5).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-1).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.15.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.2.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.4.sp,
    ),
)

@Composable
fun T3Theme(
    themePref: ThemePref = ThemePref.System,
    content: @Composable () -> Unit,
) {
    val dark = when (themePref) {
        ThemePref.System -> isSystemInDarkTheme()
        ThemePref.Light -> false
        ThemePref.Dark -> true
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(
        colorScheme = if (dark) T3Dark else T3Light,
        typography = T3Typography,
        content = content,
    )
}
