package com.t3codes.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable

@Composable
fun SettingsLabsScreen(state: UiState, viewModel: MainViewModel) {
    SettingsScaffold(
        title = "Labs",
        onBack = viewModel::closeSettingsPage,
    ) {
        SettingsSection(title = "Experimental") {
            Column {
                SettingsSwitchRow(
                    icon = Icons.Rounded.Science,
                    title = "Legacy plan mode",
                    checked = state.planModeLegacyEnabled,
                    onCheckedChange = viewModel::setPlanModeLegacyEnabled,
                )
            }
        }
        SettingsSection(title = "Navigation") {
            Column {
                SettingsRow(
                    icon = Icons.Rounded.Tune,
                    title = "Reset navigation tabs",
                    onClick = viewModel::resetNavTabs,
                )
            }
        }
    }
}
