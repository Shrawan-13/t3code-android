package com.t3codes.mobile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CallMerge
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/** Settings page ids. Null in state = the list itself. */
object SettingsPages {
    const val Environments = "environments"
    const val Notifications = "notifications"
    const val General = "general"
    const val Appearance = "appearance"
    const val Chat = "chat"
    const val NavigationTabs = "navigationTabs"
    const val Microphone = "microphone"
    const val Providers = "providers"
    const val Threads = "threads"
    const val ScheduledTasks = "scheduledTasks"
    const val Projects = "projects"
    const val SourceControl = "sourceControl"
    const val Worktrees = "worktrees"
    const val Maintenance = "maintenance"
    const val Labs = "labs"
    const val About = "about"
    const val ClientStorage = "clientStorage"
    const val Licenses = "licenses"
    const val Archive = "archive"
    const val Diagnostics = "diagnostics"
}

/** Root of the Settings tab: list or one of the pages. */
@Composable
fun SettingsRoot(state: UiState, viewModel: MainViewModel) {
    when (state.settingsPage) {
        null -> SettingsList(state, viewModel)
        SettingsPages.Environments -> SettingsEnvironmentsScreen(state, viewModel)
        SettingsPages.Notifications -> SettingsNotificationsScreen(state, viewModel)
        SettingsPages.General -> SettingsGeneralScreen(state, viewModel)
        SettingsPages.Appearance -> SettingsAppearanceScreen(state, viewModel)
        SettingsPages.Chat -> SettingsChatScreen(state, viewModel)
        SettingsPages.NavigationTabs -> SettingsNavigationTabsScreen(state, viewModel)
        SettingsPages.Microphone -> SettingsMicrophoneScreen(state, viewModel)
        SettingsPages.Providers -> SettingsProvidersScreen(state, viewModel)
        SettingsPages.Threads -> SettingsThreadsScreen(state, viewModel)
        SettingsPages.ScheduledTasks -> SettingsScheduledTasksScreen(state, viewModel)
        SettingsPages.Projects -> SettingsProjectsScreen(state, viewModel)
        SettingsPages.SourceControl -> SettingsSourceControlScreen(state, viewModel)
        SettingsPages.Worktrees -> SettingsWorktreesScreen(state, viewModel)
        SettingsPages.Maintenance -> SettingsMaintenanceScreen(state, viewModel)
        SettingsPages.Labs -> SettingsLabsScreen(state, viewModel)
        SettingsPages.About -> SettingsAboutScreen(state, viewModel)
        SettingsPages.ClientStorage -> SettingsClientStorageScreen(state, viewModel)
        SettingsPages.Licenses -> SettingsLicensesScreen(state, viewModel)
        SettingsPages.Archive -> SettingsArchiveScreen(state, viewModel)
        SettingsPages.Diagnostics -> SettingsDiagnosticsScreen(state, viewModel)
        else -> SettingsList(state, viewModel)
    }
}

private data class SettingsListEntry(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val keywords: List<String>,
    val value: String? = null,
    val enabled: Boolean = true,
)

@Composable
private fun SettingsList(state: UiState, viewModel: MainViewModel) {
    val connected = state.connected
    val environmentCount = if (state.config != null) 1 else 0
    val query = state.settingsSearch.trim().lowercase()

    fun matches(entry: SettingsListEntry): Boolean =
        query.isEmpty() ||
            entry.label.lowercase().contains(query) ||
            entry.keywords.any { it.contains(query) }

    fun section(title: String?, entries: List<SettingsListEntry>): Pair<String?, List<SettingsListEntry>>? {
        val visible = entries.filter(::matches)
        return if (visible.isEmpty()) null else title to visible
    }

    val workspace = listOf(
        SettingsListEntry("tab:projects", "Projects", Icons.Rounded.FolderOpen, listOf("workspaces", "repositories", "browse"), value = "${state.projects.size}"),
        SettingsListEntry("tab:usage", "Usage", Icons.Rounded.BarChart, listOf("tokens", "cost", "activity", "spend")),
    )
    val connections = listOf(
        SettingsListEntry(SettingsPages.Environments, "Environments", Icons.Rounded.Dns, listOf("server", "connection", "pair", "machine"), value = "$environmentCount"),
        SettingsListEntry(SettingsPages.Notifications, "Notifications", Icons.Rounded.Notifications, listOf("background sync", "alerts", "push")),
    )
    val main = listOf(
        SettingsListEntry(SettingsPages.General, "General", Icons.Rounded.Tune, listOf("project grouping", "working section", "new thread")),
        SettingsListEntry(SettingsPages.Appearance, "Appearance", Icons.Rounded.Palette, listOf("theme", "font", "text size", "math", "dark")),
        SettingsListEntry(SettingsPages.Chat, "Chat", Icons.Rounded.ChatBubble, listOf("steer", "queue", "follow-up")),
        SettingsListEntry(SettingsPages.NavigationTabs, "Navigation tabs", Icons.Rounded.Menu, listOf("tabs", "order", "hide")),
        SettingsListEntry(SettingsPages.Microphone, "Microphone", Icons.Rounded.Mic, listOf("voice", "input", "dictation")),
    )
    val agents = listOf(
        SettingsListEntry(SettingsPages.Providers, "Providers", Icons.Rounded.SmartToy, listOf("sign in", "claude", "codex", "account"), enabled = connected),
        SettingsListEntry(SettingsPages.Threads, "Threads", Icons.Rounded.Forum, listOf("workspace", "permissions", "streaming", "auto-settle"), enabled = connected),
        SettingsListEntry(SettingsPages.ScheduledTasks, "Scheduled tasks", Icons.Rounded.Schedule, listOf("schedule", "recurring", "cron", "automation"), enabled = connected),
    )
    val code = listOf(
        SettingsListEntry(SettingsPages.Projects, "Projects", Icons.Rounded.FolderOpen, listOf("pin", "archive", "remove", "checkout"), enabled = connected),
        SettingsListEntry(SettingsPages.SourceControl, "Source control", Icons.Rounded.CallMerge, listOf("git", "github", "gitlab", "auto-pull", "agent credits"), enabled = connected),
        SettingsListEntry(SettingsPages.Worktrees, "Worktrees", Icons.Rounded.Layers, listOf("branch", "submodules", "worktree"), enabled = connected),
    )
    val tail = listOf(
        SettingsListEntry(SettingsPages.Maintenance, "Maintenance", Icons.Rounded.Build, listOf("update", "database", "storage", "cleanup"), enabled = connected),
        SettingsListEntry(SettingsPages.Labs, "Labs", Icons.Rounded.Science, listOf("beta", "linked environments", "plan mode")),
        SettingsListEntry(SettingsPages.About, "About", Icons.Rounded.Info, listOf("version", "licenses", "legal", "diagnostics")),
        SettingsListEntry(SettingsPages.Archive, "Archive", Icons.Rounded.Archive, listOf("archived", "unarchive")),
    )

    val sections = listOfNotNull(
        section(null, workspace),
        section(null, connections),
        section(null, main),
        section("Agents", agents),
        section("Code", code),
        section(null, tail),
    )

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Settings",
            subtitle = state.config?.environment?.label?.ifBlank { null },
            leading = { BackButton { viewModel.selectTab(AppTab.Threads) } },
        )
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            OutlinedTextField(
                value = state.settingsSearch,
                onValueChange = viewModel::setSettingsSearch,
                placeholder = { Text("Search settings") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (sections.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Search,
                title = "No results",
                message = "No settings match \"${state.settingsSearch.trim()}\".",
                actionLabel = "Clear search",
                onAction = { viewModel.setSettingsSearch("") },
            )
            return@Column
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            sections.forEach { (title, entries) ->
                if (title != null) item(key = "header-$title") { SectionLabel(title) }
                item(key = "card-${title ?: "root"}-${entries.first().id}") {
                    GroupedCard {
                        entries.forEachIndexed { index, entry ->
                            if (index > 0) InsetDivider(start = 52.dp)
                            SettingsListRow(entry) {
                                if (entry.enabled) {
                                    when (entry.id) {
                                        "tab:projects" -> viewModel.selectTab(AppTab.Projects)
                                        "tab:usage" -> viewModel.selectTab(AppTab.Usage)
                                        else -> viewModel.openSettingsPage(entry.id)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsListRow(entry: SettingsListEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (entry.enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = entry.icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (entry.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.width(13.dp))
        Text(
            text = entry.label,
            style = MaterialTheme.typography.labelLarge,
            color = if (entry.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
            modifier = Modifier.weight(1f),
        )
        if (entry.value != null) {
            Text(
                text = entry.value,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(6.dp))
        }
        Icon(
            imageVector = Icons.Rounded.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
    }
}
