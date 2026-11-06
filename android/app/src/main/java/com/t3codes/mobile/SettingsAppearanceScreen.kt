package com.t3codes.mobile

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.WrapText
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class ThemeOption(val id: String, val label: String)

private val THEME_OPTIONS = listOf(
    ThemeOption("t3-code", "T3 Code"),
    ThemeOption("t3-chat", "T3 Chat"),
    ThemeOption("grove", "Grove"),
    ThemeOption("ocean", "Ocean"),
    ThemeOption("ember", "Ember"),
    ThemeOption("iris", "Iris"),
)

@Composable
fun SettingsAppearanceScreen(state: UiState, viewModel: MainViewModel) {
    val supportsMaterialYou = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    SettingsScaffold(
        title = "Appearance",
        onBack = viewModel::closeSettingsPage,
    ) {
        // Color scheme
        SettingsSection(title = "Color scheme") {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEachIndexed { index, (mode, label) ->
                    SettingsChoiceRow(
                        title = label,
                        description = null,
                        selected = state.themeMode == mode,
                        separated = index > 0,
                        onClick = { viewModel.setThemeMode(mode) },
                    )
                }
            }
        }

        // Theme pickers, one per appearance
        SettingsSection(title = "Theme") {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Light",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ThemeGrid(state, viewModel, supportsMaterialYou, dark = false)
                Text(
                    text = "Dark",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ThemeGrid(state, viewModel, supportsMaterialYou, dark = true)
                if (state.lightThemeId != state.darkThemeId) {
                    androidx.compose.material3.TextButton(
                        onClick = { viewModel.setThemeIdBoth(state.lightThemeId) },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("Use the same theme for both")
                    }
                }
            }
        }

        // Android navigation style
        SettingsSection(title = "Navigation") {
            Column {
                SettingsChoiceRow(
                    title = "One UI",
                    description = "Floating navigation with a separate compose button.",
                    selected = state.androidNavStyle == "one-ui",
                    onClick = { viewModel.setAndroidNavStyle("one-ui") },
                )
                SettingsChoiceRow(
                    title = "Material",
                    description = "Standard Android navigation bar and side rail.",
                    selected = state.androidNavStyle == "material",
                    separated = true,
                    onClick = { viewModel.setAndroidNavStyle("material") },
                )
            }
        }

        // Text
        SettingsSection(title = "Text") {
            Column {
                SettingsSliderRow(
                    icon = Icons.Rounded.TextFields,
                    title = "Text size",
                    value = state.baseFontSize.toFloat(),
                    valueLabel = "${state.baseFontSize} pt",
                    min = AppSettings.MIN_BASE_FONT_SIZE.toFloat(),
                    max = AppSettings.MAX_BASE_FONT_SIZE.toFloat(),
                    step = 1f,
                    onChange = { viewModel.setBaseFontSize(it.toInt()) },
                )
                SettingsSwitchRow(
                    icon = Icons.Rounded.Calculate,
                    title = "Render math",
                    checked = state.mathRenderingEnabled,
                    onCheckedChange = viewModel::setMathRenderingEnabled,
                )
            }
        }

        // Terminal
        SettingsSection(title = "Terminal") {
            Column {
                SettingsSwitchRow(
                    icon = Icons.Rounded.Terminal,
                    title = "Custom font size",
                    checked = state.terminalFontSize != null,
                    onCheckedChange = { enabled ->
                        viewModel.setTerminalFontSize(if (enabled) 10.5f else null)
                    },
                )
                if (state.terminalFontSize != null) {
                    SettingsSliderRow(
                        icon = Icons.Rounded.TextFields,
                        title = "Font size",
                        value = state.terminalFontSize,
                        valueLabel = "${"%.1f".format(state.terminalFontSize)} pt",
                        min = 6f,
                        max = 14f,
                        step = 0.5f,
                        onChange = { viewModel.setTerminalFontSize(it) },
                    )
                }
            }
        }

        // Code & Diffs
        SettingsSection(title = "Code & Diffs") {
            Column {
                SettingsSwitchRow(
                    icon = Icons.Rounded.Code,
                    title = "Custom font size",
                    checked = state.codeFontSize != null,
                    onCheckedChange = { enabled ->
                        viewModel.setCodeFontSize(if (enabled) 12 else null)
                    },
                )
                if (state.codeFontSize != null) {
                    SettingsSliderRow(
                        icon = Icons.Rounded.TextFields,
                        title = "Font size",
                        value = state.codeFontSize.toFloat(),
                        valueLabel = "${state.codeFontSize} pt",
                        min = 8f,
                        max = 18f,
                        step = 1f,
                        onChange = { viewModel.setCodeFontSize(it.toInt()) },
                    )
                }
                SettingsSwitchRow(
                    icon = Icons.Rounded.WrapText,
                    title = "Word break",
                    checked = state.codeWordBreak,
                    onCheckedChange = viewModel::setCodeWordBreak,
                )
            }
        }
    }
}

@Composable
private fun ThemeGrid(state: UiState, viewModel: MainViewModel, supportsMaterialYou: Boolean, dark: Boolean) {
    val options = buildList {
        if (supportsMaterialYou) add(ThemeOption("material-you", "Material You"))
        addAll(THEME_OPTIONS)
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { option ->
                    ThemeTile(
                        option = option,
                        dark = dark,
                        selected = (if (dark) state.darkThemeId else state.lightThemeId) == option.id,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.setThemeId(if (dark) "dark" else "light", option.id)
                        },
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ThemeTile(
    option: ThemeOption,
    dark: Boolean,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = if (option.id == "material-you") null else ThemePalettes.byId(option.id, dark)
    val canvas = palette?.canvas ?: MaterialTheme.colorScheme.surface
    val accent = palette?.accent ?: MaterialTheme.colorScheme.primary
    val muted = palette?.mutedForeground ?: MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .then(
                if (selected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)),
            )
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(canvas),
            contentAlignment = Alignment.CenterStart,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(modifier = Modifier.height(6.dp).width(84.dp).clip(RoundedCornerShape(3.dp)).background(muted.copy(alpha = 0.55f)))
                Box(modifier = Modifier.height(14.dp).width(58.dp).clip(RoundedCornerShape(7.dp)).background(accent))
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = option.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}
