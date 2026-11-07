package com.t3codes.mobile

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
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
    val groupedCard: Color,
    val diffAddition: Color,
    val diffDeletion: Color,
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
        groupedCard = Color(0xFFF4F4F5),
        diffAddition = Color(0xFF1B7D50),
        diffDeletion = Color(0xFFC10007),
        searchMatchBackground = Color(0xFFFFDE8B),
        searchMatchForeground = Color(0xFF21180A),
        warningSurface = Color(0xFFFFF3D6),
        onWarningSurface = Color(0xFF5C4300),
        updateSurface = Color(0xFFDCEAFF),
        onUpdateSurface = Color(0xFF1B4ED8),
    )
}

/**
 * Grouped-list card color, matching the official mobile theme variables:
 * t3-code uses the toolbar hover / sidebar active tone, t3-chat uses its
 * toolbar control / surface tone, and the remaining themes use `muted`.
 */
private fun groupedCardColor(palette: ThemePalette, themeId: String, dark: Boolean): Color =
    when (themeId) {
        "t3-code" -> if (dark) palette.sidebarRowActive else palette.toolbarControlHover
        "t3-chat" -> if (dark) palette.surface else palette.toolbarControl
        else -> palette.muted
    }

private fun sharedSchemeArgs(palette: ThemePalette, groupedCard: Color) = object {
    val primary = palette.accent
    val onPrimary = palette.accentForeground
    val primaryContainer = palette.accentSurface
    val onPrimaryContainer = palette.accentSurfaceForeground
    val secondary = palette.secondary
    val onSecondary = palette.secondaryForeground
    val secondaryContainer = palette.secondary
    val onSecondaryContainer = palette.secondaryForeground
    val tertiary = palette.update
    // `updateForeground` is a tint of the update hue, so it can vanish on the
    // update surface itself (blue on blue). Use the accent foreground instead.
    val onTertiary = palette.accentForeground
    val tertiaryContainer = palette.updateSurface
    val onTertiaryContainer = palette.updateForeground
    // Destructive text and icons need the darker `errorForeground` tone to stay
    // readable on light cards; `error` itself is the pale container accent.
    val error = palette.errorForeground
    val onError = palette.canvas
    val errorContainer = palette.errorSurface
    val onErrorContainer = palette.errorForeground
    val background = palette.canvas
    val onBackground = palette.text
    val surface = palette.surface
    val onSurface = palette.text
    val surfaceVariant = palette.muted
    val onSurfaceVariant = palette.mutedForeground
    val surfaceContainerLowest = palette.canvas
    // Grouped cards, sheets and the composer use the official grouped tone so
    // they stay distinct from the canvas in every theme.
    val surfaceContainerLow = groupedCard
    val surfaceContainer = palette.surface
    // Chips and pills sit on grouped cards, so they use the plain card tone.
    val surfaceContainerHigh = palette.surface
    val surfaceContainerHighest = palette.surfaceOverlay
    val surfaceDim = palette.surface
    val surfaceBright = palette.canvas
    // `input` is the field-border tone; `border` stays on outlineVariant so
    // dividers remain subtle while text fields keep enough contrast.
    val outline = palette.input
    val outlineVariant = palette.border
    val inverseSurface = palette.toolbar
    val inverseOnSurface = palette.toolbarForeground
    val inversePrimary = palette.accent
    val scrim = Color(0x66000000)
}

private fun colorSchemeFor(
    palette: ThemePalette,
    themeId: String,
    dark: Boolean,
): androidx.compose.material3.ColorScheme {
    val a = sharedSchemeArgs(palette, groupedCardColor(palette, themeId, dark))
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

private fun extendedColorsFor(palette: ThemePalette, themeId: String, dark: Boolean) = T3Colors(
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
    groupedCard = groupedCardColor(palette, themeId, dark),
    diffAddition = if (dark) Color(0xFF6FD69A) else Color(0xFF1B7D50),
    diffDeletion = palette.errorForeground,
    searchMatchBackground = palette.searchMatchBackground,
    searchMatchForeground = palette.searchMatchForeground,
    warningSurface = palette.warningSurface,
    onWarningSurface = palette.warningForeground,
    updateSurface = palette.updateSurface,
    onUpdateSurface = palette.updateForeground,
)

private fun extendedColorsForSystem(dark: Boolean) = T3Colors(
    userBubble = if (dark) Color(0xFF2A2A31) else Color(0xFFECECF1),
    onUserBubble = if (dark) Color(0xFFF4F4F5) else Color(0xFF1B1B1F),
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
    groupedCard = if (dark) Color(0xFF1A1B1B) else Color(0xFFF4F4F5),
    diffAddition = if (dark) Color(0xFF6FD69A) else Color(0xFF1B7D50),
    diffDeletion = if (dark) Color(0xFFFF6467) else Color(0xFFC10007),
    searchMatchBackground = Color(0xFFFFDE8B),
    searchMatchForeground = Color(0xFF21180A),
    warningSurface = if (dark) Color(0xFF3B2F00) else Color(0xFFFFF3D6),
    onWarningSurface = if (dark) Color(0xFFFFDE8B) else Color(0xFF5C4300),
    updateSurface = if (dark) Color(0xFF1B2A4A) else Color(0xFFDCEAFF),
    onUpdateSurface = if (dark) Color(0xFF9DBBFF) else Color(0xFF1B4ED8),
)

private fun textStyle(
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    letterSpacing: Double = 0.0,
) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.toFloat().sp,
)

private val BaseTypography = Typography(
    displayLarge = textStyle(FontWeight.Bold, 52, 58, -1.5),
    displaySmall = textStyle(FontWeight.Bold, 34, 40, -1.0),
    headlineMedium = textStyle(FontWeight.SemiBold, 28, 34, -0.4),
    headlineSmall = textStyle(FontWeight.SemiBold, 24, 30, -0.2),
    titleLarge = textStyle(FontWeight.SemiBold, 20, 26),
    titleMedium = textStyle(FontWeight.SemiBold, 16, 22, 0.1),
    titleSmall = textStyle(FontWeight.SemiBold, 14, 20),
    bodyLarge = textStyle(FontWeight.Normal, 16, 24, 0.15),
    bodyMedium = textStyle(FontWeight.Normal, 14, 21, 0.15),
    bodySmall = textStyle(FontWeight.Normal, 12, 17, 0.2),
    labelLarge = textStyle(FontWeight.Medium, 14, 20, 0.1),
    labelMedium = textStyle(FontWeight.Medium, 12, 16, 0.4),
    labelSmall = textStyle(FontWeight.Medium, 11, 15, 0.4),
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
        else -> colorSchemeFor(ThemePalettes.byId(themeId, dark), themeId, dark)
    }
    val extended = if (materialYou) {
        extendedColorsForSystem(dark)
    } else {
        extendedColorsFor(ThemePalettes.byId(themeId, dark), themeId, dark)
    }
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
    ) {
        CompositionLocalProvider(LocalT3Colors provides extended) {
            // A root surface keeps every top-level screen on the themed canvas
            // and gives unstyled icons/text a readable content color in dark mode.
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = colorScheme.background,
                contentColor = colorScheme.onBackground,
                content = content,
            )
        }
    }
}
