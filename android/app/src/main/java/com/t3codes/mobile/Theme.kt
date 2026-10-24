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

// The official mobile client uses a soft rose accent on deep, slightly
// violet surfaces. Keeping those values here makes every screen feel like one
// product instead of a collection of Material defaults.
private val T3Light = lightColorScheme(
    primary = Color(0xFF8B3C76),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD7F4),
    onPrimaryContainer = Color(0xFF390B31),
    secondary = Color(0xFF6F5B70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF6DDF4),
    onSecondaryContainer = Color(0xFF28172A),
    tertiary = Color(0xFF266B5C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB2F2DE),
    onTertiaryContainer = Color(0xFF00211A),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFDF7FC),
    onBackground = Color(0xFF1F1A1F),
    surface = Color(0xFFFDF7FC),
    onSurface = Color(0xFF1F1A1F),
    surfaceVariant = Color(0xFFEADDE9),
    onSurfaceVariant = Color(0xFF4E444D),
    outline = Color(0xFF80747D),
    outlineVariant = Color(0xFFD2C3CE),
    inverseSurface = Color(0xFF342F34),
    inverseOnSurface = Color(0xFFF8EEF5),
    inversePrimary = Color(0xFFFFAFE5),
    surfaceDim = Color(0xFFE0D8DF),
    surfaceBright = Color(0xFFFDF7FC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F0F7),
    surfaceContainer = Color(0xFFF2EAF1),
    surfaceContainerHigh = Color(0xFFECE4EB),
    surfaceContainerHighest = Color(0xFFE6DEE5),
)

private val T3Dark = darkColorScheme(
    primary = Color(0xFFF2A9DF),
    onPrimary = Color(0xFF4A103D),
    primaryContainer = Color(0xFF5D2451),
    onPrimaryContainer = Color(0xFFFFD7F4),
    secondary = Color(0xFFD5C2D4),
    onSecondary = Color(0xFF3B2D3B),
    secondaryContainer = Color(0xFF4B3A4A),
    onSecondaryContainer = Color(0xFFF2DDF0),
    tertiary = Color(0xFF8FD8C5),
    onTertiary = Color(0xFF00382D),
    tertiaryContainer = Color(0xFF155245),
    onTertiaryContainer = Color(0xFFABF5DF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF7B2827),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF17131E),
    onBackground = Color(0xFFF1EAF2),
    surface = Color(0xFF17131E),
    onSurface = Color(0xFFF1EAF2),
    surfaceVariant = Color(0xFF4B3F4D),
    onSurfaceVariant = Color(0xFFB4A7B5),
    outline = Color(0xFF827486),
    outlineVariant = Color(0xFF443A49),
    inverseSurface = Color(0xFFF1EAF2),
    inverseOnSurface = Color(0xFF342E36),
    inversePrimary = Color(0xFF8B3C76),
    surfaceDim = Color(0xFF17131E),
    surfaceBright = Color(0xFF3D3544),
    surfaceContainerLowest = Color(0xFF120F18),
    surfaceContainerLow = Color(0xFF1E1827),
    surfaceContainer = Color(0xFF231C2D),
    surfaceContainerHigh = Color(0xFF2C2437),
    surfaceContainerHighest = Color(0xFF372D43),
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
