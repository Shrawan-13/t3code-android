package com.t3codes.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val WORKTREE_KEYS = listOf(
    "newWorktreesStartFromOrigin",
    "worktreeSubmodules",
    "branchNamingMode",
    "branchNamePrefix",
    "branchNameInstructions",
)

@Composable
fun SettingsWorktreesScreen(state: UiState, viewModel: MainViewModel) {
    val projectId = scopeKey(state.serverScopeProjectId)

    SettingsScaffold(
        title = "Worktrees",
        onBack = viewModel::closeSettingsPage,
    ) {
        ServerScopeChips(state, viewModel)
        if (projectId != null) {
            SettingsNote("Overrides for this project. Values fall back to All projects when not overridden.")
        }

        SettingsSection(title = "New worktrees") {
            Column {
                val locked = ManagedPolicy.isLocked(state.config, "newWorktreesStartFromOrigin")
                SettingsSwitchRow(
                    icon = Icons.Rounded.AccountTree,
                    title = "Start from remote",
                    checked = scopeBool(state, "newWorktreesStartFromOrigin", true),
                    enabled = !locked && !state.serverSettingsWriting,
                    onCheckedChange = { viewModel.writeServerSetting("newWorktreesStartFromOrigin", JsonPrimitive(it), projectId) },
                )
                ManagedNote(state, "newWorktreesStartFromOrigin")
            }
        }

        SettingsSection(title = "Worktree submodules") {
            Column {
                triStateChoice(
                    state = state,
                    key = "worktreeSubmodules",
                    projectId = projectId,
                    onSelect = { value -> viewModel.writeServerSetting("worktreeSubmodules", value, projectId) },
                    choices = listOf(
                        Triple(null, "Inherit", "Use the repository's t3.json, or initialize recursively."),
                        Triple("recursive", "Recursive", "Initialize nested submodules too."),
                        Triple("top-level", "Top level only", "Skip submodules declared inside other submodules."),
                        Triple("none", "Skip", "Leave submodules empty for a setup script."),
                    ),
                )
                ManagedNote(state, "worktreeSubmodules")
            }
        }

        SettingsSection(title = "Branch naming") {
            Column {
                val lockedMode = ManagedPolicy.isLocked(state.config, "branchNamingMode")
                val current = scopeString(state, "branchNamingMode", "static")
                val prefix = scopeString(state, "branchNamePrefix", "t3") ?: ""
                val instructions = scopeString(state, "branchNameInstructions", "") ?: ""

                SettingsChoiceRow(
                    title = "Static",
                    description = "Branches use the prefix below, like t3/my-task.",
                    selected = current == "static",
                    enabled = !lockedMode && !state.serverSettingsWriting,
                    onClick = { viewModel.writeServerSetting("branchNamingMode", JsonPrimitive("static"), projectId) },
                )
                SettingsChoiceRow(
                    title = "Semantic",
                    description = "Branch names generated from the task.",
                    selected = current == "semantic",
                    separated = true,
                    enabled = !lockedMode && !state.serverSettingsWriting,
                    onClick = { viewModel.writeServerSetting("branchNamingMode", JsonPrimitive("semantic"), projectId) },
                )
                SettingsChoiceRow(
                    title = "Custom",
                    description = "Your instructions steer the branch name.",
                    selected = current == "custom",
                    separated = true,
                    enabled = !lockedMode && !state.serverSettingsWriting,
                    onClick = { viewModel.writeServerSetting("branchNamingMode", JsonPrimitive("custom"), projectId) },
                )
                ManagedNote(state, "branchNamingMode")

                if (current == "static") {
                    InlineTextFieldRow(
                        label = "Prefix",
                        value = prefix,
                        placeholder = "t3",
                        enabled = !ManagedPolicy.isLocked(state.config, "branchNamePrefix") && !state.serverSettingsWriting,
                        onSave = { viewModel.writeServerSetting("branchNamePrefix", JsonPrimitive(it.trim()), projectId) },
                    )
                }
                if (current == "custom") {
                    InlineTextFieldRow(
                        label = "Instructions",
                        value = instructions,
                        placeholder = "e.g. issue number first, kebab case, max 40 chars",
                        singleLine = false,
                        enabled = !ManagedPolicy.isLocked(state.config, "branchNameInstructions") && !state.serverSettingsWriting,
                        onSave = { viewModel.writeServerSetting("branchNameInstructions", JsonPrimitive(it), projectId) },
                    )
                }
                ManagedNote(state, "branchNamePrefix")
                ManagedNote(state, "branchNameInstructions")
            }
        }

        ClearProjectOverridesRow(state, viewModel, WORKTREE_KEYS)
    }
}

/** Inline single- or multi-line editable field that saves on blur/action. */
@Composable
internal fun InlineTextFieldRow(
    label: String,
    value: String,
    placeholder: String,
    enabled: Boolean,
    singleLine: Boolean = true,
    onSave: (String) -> Unit,
) {
    var draft by remember(value) { mutableStateOf(value) }
    LaunchedEffect(value) { draft = value }
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        androidx.compose.material3.Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
        )
        androidx.compose.foundation.layout.Spacer(Modifier.padding(2.dp))
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            placeholder = { androidx.compose.material3.Text(placeholder) },
            singleLine = singleLine,
            enabled = enabled,
            trailingIcon = if (draft != value) {
                {
                    androidx.compose.material3.TextButton(onClick = { onSave(draft) }) {
                        androidx.compose.material3.Text("Save")
                    }
                }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

