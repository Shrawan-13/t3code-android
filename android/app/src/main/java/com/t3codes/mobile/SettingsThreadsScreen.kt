package com.t3codes.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.CalendarClock
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonPrimitive

private val THREADS_KEYS = listOf(
    "defaultThreadEnvMode",
    "defaultRuntimeMode",
    "responseStreamingMode",
    "enableAgentBrowserAccess",
    "sidebarAutoSettleOnMerge",
    "sidebarAutoSettleAfterDays",
)

@Composable
fun SettingsThreadsScreen(state: UiState, viewModel: MainViewModel) {
    val projectId = scopeKey(state.serverScopeProjectId)
    val autoSettleSupported = state.config?.environment?.capabilities?.threadAutoSettlement == true
    val pageKeys = if (autoSettleSupported) THREADS_KEYS else THREADS_KEYS - listOf("sidebarAutoSettleOnMerge", "sidebarAutoSettleAfterDays")

    SettingsScaffold(
        title = "Threads",
        onBack = viewModel::closeSettingsPage,
    ) {
        ServerScopeChips(state, viewModel)
        if (state.serverSettings == null) {
            SettingsNote("Loading server settings…")
        }
        if (projectId != null) {
            SettingsNote("Overrides for this project. Values fall back to All projects when not overridden.")
        }

        SettingsSection(title = "Default workspace") {
            Column {
                triStateChoice(
                    state = state,
                    key = "defaultThreadEnvMode",
                    projectId = projectId,
                    onSelect = { value -> viewModel.writeServerSetting("defaultThreadEnvMode", value, projectId) },
                    choices = listOf(
                        Triple(null, "Inherit", "Use the repository's t3.json, or the current checkout."),
                        Triple("local", "Current checkout", "Start new threads in the existing workspace."),
                        Triple("worktree", "New worktree", "Give each new thread a separate checkout."),
                    ),
                )
                ManagedNote(state, "defaultThreadEnvMode")
            }
        }

        SettingsSection(title = "Default permissions") {
            Column {
                val locked = ManagedPolicy.isLocked(state.config, "defaultRuntimeMode")
                val current = scopeString(state, "defaultRuntimeMode", "full-access")
                val choices = listOf(
                    "approval-required" to ("Supervised" to "Ask before commands and file changes."),
                    "auto-accept-edits" to ("Auto-accept edits" to "Auto-approve edits, ask before other actions."),
                    "auto" to ("Auto" to "Supported providers approve routine actions; others still ask."),
                    "full-access" to ("Full access" to "Allow commands and edits without prompts."),
                )
                choices.forEachIndexed { index, (mode, texts) ->
                    SettingsChoiceRow(
                        title = texts.first,
                        description = texts.second,
                        selected = current == mode,
                        separated = index > 0,
                        enabled = !locked && !state.serverSettingsWriting,
                        onClick = { viewModel.writeServerSetting("defaultRuntimeMode", JsonPrimitive(mode), projectId) },
                    )
                }
                ManagedNote(state, "defaultRuntimeMode")
            }
        }

        SettingsSection(title = "Response streaming") {
            Column {
                val locked = ManagedPolicy.isLocked(state.config, "responseStreamingMode")
                val current = scopeString(state, "responseStreamingMode", "paragraph")
                SettingsChoiceRow(
                    title = "After the turn",
                    description = "Show the answer when the agent finishes.",
                    selected = current == "turn",
                    enabled = !locked && !state.serverSettingsWriting,
                    onClick = { viewModel.writeServerSetting("responseStreamingMode", JsonPrimitive("turn"), projectId) },
                )
                SettingsChoiceRow(
                    title = "Finished paragraphs",
                    description = "Show each paragraph or code block as it completes.",
                    selected = current == "paragraph",
                    separated = true,
                    enabled = !locked && !state.serverSettingsWriting,
                    onClick = { viewModel.writeServerSetting("responseStreamingMode", JsonPrimitive("paragraph"), projectId) },
                )
                ManagedNote(state, "responseStreamingMode")
            }
        }

        SettingsSection(title = "Preview browser") {
            Column {
                val locked = ManagedPolicy.isLocked(state.config, "enableAgentBrowserAccess")
                SettingsSwitchRow(
                    icon = Icons.Rounded.BlurOn,
                    title = "Agent browser access",
                    checked = scopeBool(state, "enableAgentBrowserAccess", true),
                    enabled = !locked && !state.serverSettingsWriting,
                    onCheckedChange = { viewModel.writeServerSetting("enableAgentBrowserAccess", JsonPrimitive(it), projectId) },
                )
                ManagedNote(state, "enableAgentBrowserAccess")
            }
        }

        if (autoSettleSupported) {
            SettingsSection(title = "Auto-settle") {
                Column {
                    val lockedMerge = ManagedPolicy.isLocked(state.config, "sidebarAutoSettleOnMerge")
                    val lockedDays = ManagedPolicy.isLocked(state.config, "sidebarAutoSettleAfterDays")
                    SettingsSwitchRow(
                        icon = Icons.Rounded.DownloadDone,
                        title = "Auto-settle merged threads",
                        checked = scopeBool(state, "sidebarAutoSettleOnMerge", true),
                        enabled = !lockedMerge && !state.serverSettingsWriting,
                        onCheckedChange = { viewModel.writeServerSetting("sidebarAutoSettleOnMerge", JsonPrimitive(it), projectId) },
                    )
                    val afterDays = scopeIntNullable(state, "sidebarAutoSettleAfterDays", 3)
                    SettingsSwitchRow(
                        icon = Icons.Rounded.History,
                        title = "Auto-settle inactive threads",
                        checked = afterDays != null,
                        enabled = !lockedDays && !state.serverSettingsWriting,
                        onCheckedChange = { enabled ->
                            viewModel.writeServerSetting(
                                "sidebarAutoSettleAfterDays",
                                if (enabled) JsonPrimitive(3) else kotlinx.serialization.json.JsonNull,
                                projectId,
                            )
                        },
                    )
                    if (afterDays != null) {
                        InactiveDaysRow(
                            value = afterDays,
                            enabled = !lockedDays && !state.serverSettingsWriting,
                            onChange = { days ->
                                viewModel.writeServerSetting("sidebarAutoSettleAfterDays", JsonPrimitive(days), projectId)
                            },
                        )
                    }
                    ManagedNote(state, "sidebarAutoSettleOnMerge")
                    ManagedNote(state, "sidebarAutoSettleAfterDays")
                }
            }
        }

        SettingsSection(title = "Usage limits") {
            Column {
                if (projectId != null) {
                    SettingsNote("Usage limit settings apply to every project. Switch to All projects to change them.")
                } else {
                    SettingsSwitchRow(
                        icon = Icons.Rounded.RestartAlt,
                        title = "Resume after usage limits",
                        checked = scopeBool(state, "autoResumeLimitedThreads", false),
                        enabled = !ManagedPolicy.isLocked(state.config, "autoResumeLimitedThreads") && !state.serverSettingsWriting,
                        onCheckedChange = { viewModel.writeServerSetting("autoResumeLimitedThreads", JsonPrimitive(it), null) },
                    )
                    SettingsSwitchRow(
                        icon = Icons.Rounded.CalendarClock,
                        title = "Snooze until limits reset",
                        checked = scopeBool(state, "snoozeLimitedThreads", false),
                        enabled = !ManagedPolicy.isLocked(state.config, "snoozeLimitedThreads") && !state.serverSettingsWriting,
                        onCheckedChange = { viewModel.writeServerSetting("snoozeLimitedThreads", JsonPrimitive(it), null) },
                    )
                }
            }
        }

        ClearProjectOverridesRow(state, viewModel, pageKeys)
    }
}

/** Shared helper for tri-state rows (Inherit / explicit value), used across scoped pages. */
@Composable
internal fun triStateChoice(
    state: UiState,
    key: String,
    projectId: String?,
    onSelect: (kotlinx.serialization.json.JsonElement?) -> Unit,
    choices: List<Triple<String?, String, String>>,
) {
    val locked = ManagedPolicy.isLocked(state.config, key)
    val view = ServerSettingsView(state.serverSettings)
    val override = projectId?.let { view.overrideOrNullStored(it, key) }
    val hasOverride = override?.first == true
    val explicitNullOverride = override?.third == true
    val machineValue = view.string(key)
    val overrideValue = override?.second?.let { (it as? JsonPrimitive)?.content }
    val current: String? = when {
        projectId == null -> machineValue
        hasOverride -> if (explicitNullOverride) null else overrideValue
        else -> machineValue
    }
    choices.forEachIndexed { index, (value, label, description) ->
        SettingsChoiceRow(
            title = label,
            description = description,
            selected = current == value,
            separated = index > 0,
            enabled = !locked && !state.serverSettingsWriting,
            onClick = { onSelect(value?.let(::JsonPrimitive) ?: kotlinx.serialization.json.JsonNull) },
        )
    }
}

@Composable
private fun InactiveDaysRow(
    value: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Inactive days",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = { onChange((value - 1).coerceIn(1, 90)) },
            enabled = enabled && value > 1,
        ) { Text("−") }
        Text(
            text = "$value",
            style = MaterialTheme.typography.titleMedium,
        )
        TextButton(
            onClick = { onChange((value + 1).coerceIn(1, 90)) },
            enabled = enabled && value < 90,
        ) { Text("+") }
    }
}
