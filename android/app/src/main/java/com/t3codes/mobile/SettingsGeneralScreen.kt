package com.t3codes.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.History
import androidx.compose.runtime.Composable

@Composable
fun SettingsGeneralScreen(state: UiState, viewModel: MainViewModel) {
    SettingsScaffold(
        title = "General",
        onBack = viewModel::closeSettingsPage,
    ) {
        SettingsSection(title = "Project grouping") {
            Column {
                SettingsChoiceRow(
                    title = "Group by repository",
                    description = "Matching repositories appear as one project.",
                    selected = state.projectGroupingMode == "repository",
                    onClick = { viewModel.setProjectGroupingMode("repository") },
                )
                SettingsChoiceRow(
                    title = "Group by repository path",
                    description = "Keep monorepo paths separate.",
                    selected = state.projectGroupingMode == "repository_path",
                    separated = true,
                    onClick = { viewModel.setProjectGroupingMode("repository_path") },
                )
                SettingsChoiceRow(
                    title = "Keep separate",
                    description = "Show every workspace as its own project.",
                    selected = state.projectGroupingMode == "separate",
                    separated = true,
                    onClick = { viewModel.setProjectGroupingMode("separate") },
                )
            }
        }
        SettingsSection(title = "Sidebar") {
            Column {
                SettingsSwitchRow(
                    icon = Icons.Rounded.Bolt,
                    title = "Working section",
                    checked = state.workingShelfEnabled,
                    onCheckedChange = viewModel::setWorkingShelfEnabled,
                )
            }
        }
        SettingsSection(title = "New threads") {
            Column {
                SettingsSwitchRow(
                    icon = Icons.Rounded.History,
                    title = "Use last project",
                    checked = state.useLastProjectForNewThreads,
                    onCheckedChange = viewModel::setUseLastProjectForNewThreads,
                )
            }
        }
    }
}
