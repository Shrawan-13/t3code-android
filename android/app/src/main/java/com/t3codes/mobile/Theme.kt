package com.t3codes.mobile

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/** Extra theme colors the stock Material 3 scheme does not carry. */
data class T3Colors(
    val userBubble: Color,
    val onUserBubble: Color,
    val userBubbleAction: Color,
    val onUserBubbleAction: Color,
    val codeBackground: Color,
    val onCodeBackground: Color,
    val terminalBackground: Color,
    val onTerminalBackground: Color,
    val drawer: Color,
    val onDrawer: Color,
    val drawerMuted: Color,
    val drawerControl: Color,
    val drawerBorder: Color,
    val searchMatchBackground: Color,
    val searchMatchForeground: Color,
    val warningSurface: Color,
    val onWarningSurface: Color,
    val updateSurface: Color,
    val onUpdateSurface: Color,
)

val LocalT3Colors = staticCompositionLocalOf {
    T3Colors(
        userBubble = Color(0xFFECECF1),
        onUserBubble = Color(0xFF1B1B1F),
        userBubbleAction = Color(0xFF1B4ED8),
        onUserBubbleAction = Color(0xFFFFFFFF),
        codeBackground = Color(0xFFF4F4F5),
        onCodeBackground = Color(0xFF27272A),
        terminalBackground = Color(0xFF27272A),
        onTerminalBackground = Color(0xFFF4F4F5),
        drawer = Color(0xFFFAFAFA),
        onDrawer = Color(0xFF1B1B1F),
        drawerMuted = Color(0xFF71717B),
        drawerControl = Color(0xFFF4F4F5),
        drawerBorder = Color(0xFFE4E4E7),
        searchMatchBackground = Color(0xFFFFDE8B),
        searchMatchForeground = Color(0xFF21180A),
        warningSurface = Color(0xFFFFF3D6),
        onWarningSurface = Color(0xFF5C4300),
        updateSurface = Color(0xFFDCEAFF),
        onUpdateSurface = Color(0xFF1B4ED8),
    )
}

private fun sharedSchemeArgs(palette: ThemePalette) = object {
    val primary = palette.accent
    val onPrimary = palette.accentForeground
    val primaryContainer = palette.accentSurface
    val onPrimaryContainer = palette.accentSurfaceForeground
    val secondary = palette.secondary
    val onSecondary = palette.secondaryForeground
    val secondaryContainer = palette.muted
    val onSecondaryContainer = palette.mutedForeground
    val tertiary = palette.update
    val onTertiary = palette.updateForeground
    val tertiaryContainer = palette.updateSurface
    val onTertiaryContainer = palette.updateForeground
    val error = palette.error
    val onError = palette.errorForeground
    val errorContainer = palette.errorSurface
    val onErrorContainer = palette.error
    val background = palette.canvas
    val onBackground = palette.text
    val surface = palette.surface
    val onSurface = palette.text
    val surfaceVariant = palette.muted
    val onSurfaceVariant = palette.mutedForeground
    val surfaceContainerLowest = palette.canvas
    val surfaceContainerLow = palette.surfaceRaised
    val surfaceContainer = palette.surfaceRaised
    val surfaceContainerHigh = palette.surfaceOverlay
    val surfaceContainerHighest = palette.surfaceOverlay
    val surfaceDim = palette.surface
    val surfaceBright = palette.canvas
    val outline = palette.border
    val outlineVariant = palette.border
    val inverseSurface = palette.toolbar
    val inverseOnSurface = palette.toolbarForeground
    val inversePrimary = palette.accent
    val scrim = Color(0x66000000)
}

private fun colorSchemeFor(palette: ThemePalette, dark: Boolean): androidx.compose.material3.ColorScheme {
    val a = sharedSchemeArgs(palette)
    return if (dark) {
        darkColorScheme(
            primary = a.primary, onPrimary = a.onPrimary,
            primaryContainer = a.primaryContainer, onPrimaryContainer = a.onPrimaryContainer,
            secondary = a.secondary, onSecondary = a.onSecondary,
            secondaryContainer = a.secondaryContainer, onSecondaryContainer = a.onSecondaryContainer,
            tertiary = a.tertiary, onTertiary = a.onTertiary,
            tertiaryContainer = a.tertiaryContainer, onTertiaryContainer = a.onTertiaryContainer,
            error = a.error, onError = a.onError,
            errorContainer = a.errorContainer, onErrorContainer = a.onErrorContainer,
            background = a.background, onBackground = a.onBackground,
            surface = a.surface, onSurface = a.onSurface,
            surfaceVariant = a.surfaceVariant, onSurfaceVariant = a.onSurfaceVariant,
            surfaceContainerLowest = a.surfaceContainerLowest,
            surfaceContainerLow = a.surfaceContainerLow,
            surfaceContainer = a.surfaceContainer,
            surfaceContainerHigh = a.surfaceContainerHigh,
            surfaceContainerHighest = a.surfaceContainerHighest,
            surfaceDim = a.surfaceDim, surfaceBright = a.surfaceBright,
            outline = a.outline, outlineVariant = a.outlineVariant,
            inverseSurface = a.inverseSurface, inverseOnSurface = a.inverseOnSurface,
            inversePrimary = a.inversePrimary, scrim = a.scrim,
        )
    } else {
        lightColorScheme(
            primary = a.primary, onPrimary = a.onPrimary,
            primaryContainer = a.primaryContainer, onPrimaryContainer = a.onPrimaryContainer,
            secondary = a.secondary, onSecondary = a.onSecondary,
            secondaryContainer = a.secondaryContainer, onSecondaryContainer = a.onSecondaryContainer,
            tertiary = a.tertiary, onTertiary = a.onTertiary,
            tertiaryContainer = a.tertiaryContainer, onTertiaryContainer = a.onTertiaryContainer,
            error = a.error, onError = a.onError,
            errorContainer = a.errorContainer, onErrorContainer = a.onErrorContainer,
            background = a.background, onBackground = a.onBackground,
            surface = a.surface, onSurface = a.onSurface,
            surfaceVariant = a.surfaceVariant, onSurfaceVariant = a.onSurfaceVariant,
            surfaceContainerLowest = a.surfaceContainerLowest,
            surfaceContainerLow = a.surfaceContainerLow,
            surfaceContainer = a.surfaceContainer,
            surfaceContainerHigh = a.surfaceContainerHigh,
            surfaceContainerHighest = a.surfaceContainerHighest,
            surfaceDim = a.surfaceDim, surfaceBright = a.surfaceBright,
            outline = a.outline, outlineVariant = a.outlineVariant,
            inverseSurface = a.inverseSurface, inverseOnSurface = a.inverseOnSurface,
            inversePrimary = a.inversePrimary, scrim = a.scrim,
        )
    }
}

private fun extendedColorsFor(palette: ThemePalette) = T3Colors(
    userBubble = palette.messageSurface,
    onUserBubble = palette.messageForeground,
    userBubbleAction = palette.messageAction,
    onUserBubbleAction = palette.messageActionForeground,
    codeBackground = palette.codeBackground,
    onCodeBackground = palette.codeForeground,
    terminalBackground = palette.terminalBackground,
    onTerminalBackground = palette.terminalForeground,
    drawer = palette.sidebar,
    onDrawer = palette.sidebarForeground,
    drawerMuted = palette.sidebarMutedForeground,
    drawerControl = palette.sidebarControlSurface,
    drawerBorder = palette.sidebarBorder,
    searchMatchBackground = palette.searchMatchBackground,
    searchMatchForeground = palette.searchMatchForeground,
    warningSurface = palette.warningSurface,
    onWarningSurface = palette.warningForeground,
    updateSurface = palette.updateSurface,
    onUpdateSurface = palette.updateForeground,
)

private fun extendedColorsForSystem(dark: Boolean) = T3Colors(
    userBubble = if (dark) Color(0xFFECECF1) else Color(0xFFECECF1),
    onUserBubble = if (dark) Color(0xFF1B1B1F) else Color(0xFF1B1B1F),
    userBubbleAction = Color(0xFF1B4ED8),
    onUserBubbleAction = Color(0xFFFFFFFF),
    codeBackground = if (dark) Color(0xFF27272A) else Color(0xFFF4F4F5),
    onCodeBackground = if (dark) Color(0xFFF4F4F5) else Color(0xFF27272A),
    terminalBackground = Color(0xFF27272A),
    onTerminalBackground = Color(0xFFF4F4F5),
    drawer = if (dark) Color(0xFF1B1B1F) else Color(0xFFFAFAFA),
    onDrawer = if (dark) Color(0xFFECE9EE) else Color(0xFF1B1B1F),
    drawerMuted = if (dark) Color(0xFFB4B2BC) else Color(0xFF71717B),
    drawerControl = if (dark) Color(0xFF2B2B32) else Color(0xFFF4F4F5),
    drawerBorder = if (dark) Color(0xFF2E2E36) else Color(0xFFE4E4E7),
    searchMatchBackground = Color(0xFFFFDE8B),
    searchMatchForeground = Color(0xFF21180A),
    warningSurface = if (dark) Color(0xFF3B2F00) else Color(0xFFFFF3D6),
    onWarningSurface = if (dark) Color(0xFFFFDE8B) else Color(0xFF5C4300),
    updateSurface = if (dark) Color(0xFF1B2A4A) else Color(0xFFDCEAFF),
    onUpdateSurface = if (dark) Color(0xFF9DBBFF) else Color(0xFF1B4ED8),
)

private val BaseTypography = Typography(
    displayLarge = TextStyle(FontFamily.SansSerif, FontWeight.Bold, 52.sp, 58.sp, letterSpacing = (-1.5).sp),
    displaySmall = TextStyle(FontFamily.SansSerif, FontWeight.Bold, 34.sp, 40.sp, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(FontFamily.SansSerif, FontWeight.SemiBold, 28.sp, 34.sp, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(FontFamily.SansSerif, FontWeight.SemiBold, 24.sp, 30.sp, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(FontFamily.SansSerif, FontWeight.SemiBold, 20.sp, 26.sp),
    titleMedium = TextStyle(FontFamily.SansSerif, FontWeight.SemiBold, 16.sp, 22.sp, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(FontFamily.SansSerif, FontWeight.SemiBold, 14.sp, 20.sp),
    bodyLarge = TextStyle(FontFamily.SansSerif, FontWeight.Normal, 16.sp, 24.sp, letterSpacing = 0.15.sp),
    bodyMedium = TextStyle(FontFamily.SansSerif, FontWeight.Normal, 14.sp, 21.sp, letterSpacing = 0.15.sp),
    bodySmall = TextStyle(FontFamily.SansSerif, FontWeight.Normal, 12.sp, 17.sp, letterSpacing = 0.2.sp),
    labelLarge = TextStyle(FontFamily.SansSerif, FontWeight.Medium, 14.sp, 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(FontFamily.SansSerif, FontWeight.Medium, 12.sp, 16.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(FontFamily.SansSerif, FontWeight.Medium, 11.sp, 15.sp, letterSpacing = 0.4.sp),
)

/** Scales every text style by [scale]; line heights scale with glyphs. */
private fun scaledTypography(scale: Float): Typography {
    if (scale == 1f) return BaseTypography
    fun TextStyle.scaled(): TextStyle = copy(
        fontSize = fontSize * scale,
        lineHeight = lineHeight * scale,
    )
    return with(BaseTypography) {
        Typography(
            displayLarge = displayLarge.scaled(),
            displayMedium = displayMedium.scaled(),
            displaySmall = displaySmall.scaled(),
            headlineLarge = headlineLarge.scaled(),
            headlineMedium = headlineMedium.scaled(),
            headlineSmall = headlineSmall.scaled(),
            titleLarge = titleLarge.scaled(),
            titleMedium = titleMedium.scaled(),
            titleSmall = titleSmall.scaled(),
            bodyLarge = bodyLarge.scaled(),
            bodyMedium = bodyMedium.scaled(),
            bodySmall = bodySmall.scaled(),
            labelLarge = labelLarge.scaled(),
            labelMedium = labelMedium.scaled(),
            labelSmall = labelSmall.scaled(),
        )
    }
}

@Composable
fun T3Theme(
    themeMode: String = "system",
    lightThemeId: String = "t3-code",
    darkThemeId: String = "t3-code",
    baseFontSize: Int = AppSettings.DEFAULT_BASE_FONT_SIZE,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val themeId = if (dark) darkThemeId else lightThemeId
    val context = LocalView.current.context
    val materialYou = themeId == "material-you" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        materialYou -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> colorSchemeFor(ThemePalettes.byId(themeId, dark), dark)
    }
    val extended = if (materialYou) extendedColorsForSystem(dark) else extendedColorsFor(ThemePalettes.byId(themeId, dark))
    val scale = baseFontSize.coerceIn(AppSettings.MIN_BASE_FONT_SIZE, AppSettings.MAX_BASE_FONT_SIZE).toFloat() / AppSettings.DEFAULT_BASE_FONT_SIZE

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
        colorScheme = colorScheme,
        typography = scaledTypography(scale),
        content = { androidx.compose.runtime.CompositionLocalProvider(LocalT3Colors provides extended, content = content) },
    )
}
