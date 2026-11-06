package com.t3codes.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.runtime.Composable
import kotlinx.serialization.json.JsonPrimitive

private val SOURCE_CONTROL_KEYS = listOf(
    "defaultAutoPull",
    "removeAgentCreditsOnMerge",
)

@Composable
fun SettingsSourceControlScreen(state: UiState, viewModel: MainViewModel) {
    val projectId = scopeKey(state.serverScopeProjectId)

    SettingsScaffold(
        title = "Source control",
        onBack = viewModel::closeSettingsPage,
    ) {
        ServerScopeChips(state, viewModel)
        if (projectId != null) {
            SettingsNote("Overrides for this project. Values fall back to All projects when not overridden.")
        }

        SettingsSection(title = "Pull requests") {
            Column {
                val locked = ManagedPolicy.isLocked(state.config, "removeAgentCreditsOnMerge")
                SettingsSwitchRow(
                    icon = Icons.Rounded.TaskAlt,
                    title = "Remove agent credits on merge",
                    checked = scopeBool(state, "removeAgentCreditsOnMerge", false),
                    enabled = !locked && !state.serverSettingsWriting,
                    onCheckedChange = { viewModel.writeServerSetting("removeAgentCreditsOnMerge", JsonPrimitive(it), projectId) },
                )
                ManagedNote(state, "removeAgentCreditsOnMerge")
            }
        }

        SettingsSection(title = "Default branch") {
            Column {
                val locked = ManagedPolicy.isLocked(state.config, "defaultAutoPull")
                SettingsSwitchRow(
                    icon = Icons.Rounded.ArrowDownward,
                    title = "Automatically pull",
                    checked = scopeBool(state, "defaultAutoPull", false),
                    enabled = !locked && !state.serverSettingsWriting,
                    onCheckedChange = { viewModel.writeServerSetting("defaultAutoPull", JsonPrimitive(it), projectId) },
                )
                ManagedNote(state, "defaultAutoPull")
            }
        }

        ClearProjectOverridesRow(state, viewModel, SOURCE_CONTROL_KEYS)
    }
}
