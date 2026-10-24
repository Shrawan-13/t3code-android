package com.t3codes.mobile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.MarkEmailUnread
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.t3codes.core.Project
import com.t3codes.core.ThreadShell

@Composable
fun ThreadsScreen(state: UiState, viewModel: MainViewModel) {
    var renameThread by remember { mutableStateOf<ThreadShell?>(null) }
    var deleteThread by remember { mutableStateOf<ThreadShell?>(null) }

    val projectsById = state.projects.associateBy { it.id }
    val query = state.search.trim()
    val filtered = state.threads.filter { thread ->
        val matchesProject = state.projectFilter == null || thread.projectId == state.projectFilter
        val matchesSearch = query.isEmpty() ||
            thread.title.contains(query, ignoreCase = true) ||
            thread.latestVisibleMessage?.text?.contains(query, ignoreCase = true) == true ||
            projectsById[thread.projectId]?.title?.contains(query, ignoreCase = true) == true
        matchesProject && matchesSearch
    }
    val pinned = filtered.filter { it.pinnedAt != null }
    val active = filtered.filter {
        it.pinnedAt == null && (it.activeRunId != null || it.pendingRuntimeRequest != null || it.status == "running")
    }
    val recent = filtered.filter {
        it.pinnedAt == null && it.activeRunId == null && it.pendingRuntimeRequest == null && it.status != "running"
    }
    val archived = state.archivedThreads.filter { thread ->
        val matchesProject = state.projectFilter == null || thread.projectId == state.projectFilter
        val matchesSearch = query.isEmpty() ||
            thread.title.contains(query, ignoreCase = true) ||
            thread.latestVisibleMessage?.text?.contains(query, ignoreCase = true) == true
        matchesProject && matchesSearch
    }
    val runningCount = state.threads.count { it.activeRunId != null || it.status == "running" }
    val subtitle = when {
        runningCount > 0 -> "$runningCount working · ${state.threads.size} threads"
        state.threads.isEmpty() -> "Your workspace is quiet"
        else -> "${state.threads.size} threads · ${state.projects.size} projects"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(
                title = "Threads",
                subtitle = subtitle,
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { viewModel.selectTab(AppTab.Settings) }) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                },
            )
        }

        item {
            OutlinedTextField(
                value = state.search,
                onValueChange = viewModel::setSearch,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search threads") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.search.isNotBlank()) {
                        TextButton(onClick = { viewModel.setSearch("") }) { Text("Clear") }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            )
        }

        if (state.projects.isNotEmpty()) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 1.dp),
                ) {
                    item {
                        FilterChip(
                            selected = state.projectFilter == null,
                            onClick = { viewModel.setProjectFilter(null) },
                            label = { Text("All") },
                            shape = CircleShape,
                        )
                    }
                    items(state.projects, key = { it.id }) { project ->
                        FilterChip(
                            selected = state.projectFilter == project.id,
                            onClick = {
                                viewModel.setProjectFilter(
                                    if (state.projectFilter == project.id) null else project.id,
                                )
                            },
                            label = {
                                Text(
                                    text = project.title.ifBlank { project.workspaceRoot.substringAfterLast('/') },
                                    maxLines = 1,
                                )
                            },
                            shape = CircleShape,
                        )
                    }
                }
            }
        }

        if (pinned.isNotEmpty()) {
            item { SectionLabel("Pinned · ${pinned.size}") }
            item {
                ThreadGroup(
                    threads = pinned,
                    projects = projectsById,
                    archived = false,
                    onOpen = viewModel::openThread,
                    onAction = { thread, action, value -> viewModel.threadAction(thread.id, action, value) },
                    onRename = { renameThread = it },
                    onDelete = { deleteThread = it },
                )
            }
        }

        if (active.isNotEmpty()) {
            item { SectionLabel("Active · ${active.size}") }
            item {
                ThreadGroup(
                    threads = active,
                    projects = projectsById,
                    archived = false,
                    onOpen = viewModel::openThread,
                    onAction = { thread, action, value -> viewModel.threadAction(thread.id, action, value) },
                    onRename = { renameThread = it },
                    onDelete = { deleteThread = it },
                )
            }
        }

        if (recent.isNotEmpty()) {
            item {
                SectionLabel(
                    text = if (active.isEmpty() && pinned.isEmpty()) "Recent" else "Recent · ${recent.size}",
                )
            }
            item {
                ThreadGroup(
                    threads = recent,
                    projects = projectsById,
                    archived = false,
                    onOpen = viewModel::openThread,
                    onAction = { thread, action, value -> viewModel.threadAction(thread.id, action, value) },
                    onRename = { renameThread = it },
                    onDelete = { deleteThread = it },
                )
            }
        }

        if (filtered.isEmpty() && archived.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.Forum,
                    title = if (query.isNotEmpty() || state.projectFilter != null) {
                        "No matching threads"
                    } else {
                        "Start a conversation"
                    },
                    message = if (query.isNotEmpty() || state.projectFilter != null) {
                        "Try a different search or project filter."
                    } else {
                        "Threads you start will show up here with live progress, approvals, and diffs."
                    },
                    actionLabel = if (query.isNotEmpty() || state.projectFilter != null) "Clear filters" else "New thread",
                    onAction = {
                        if (query.isNotEmpty() || state.projectFilter != null) {
                            viewModel.setSearch("")
                            viewModel.setProjectFilter(null)
                        } else {
                            viewModel.openNewThread()
                        }
                    },
                )
            }
        }

        if (archived.isNotEmpty()) {
            item {
                SectionLabel(
                    text = "Archived · ${archived.size}",
                    trailing = {
                        FilterChip(
                            selected = state.showArchived,
                            onClick = viewModel::toggleArchived,
                            label = { Text(if (state.showArchived) "Hide" else "Show") },
                            shape = CircleShape,
                        )
                    },
                )
            }
            if (state.showArchived) {
                item {
                    ThreadGroup(
                        threads = archived,
                        projects = projectsById,
                        archived = true,
                        onOpen = viewModel::openThread,
                        onAction = { thread, action, value -> viewModel.threadAction(thread.id, action, value) },
                        onRename = { renameThread = it },
                        onDelete = { deleteThread = it },
                    )
                }
            }
        }
    }

    renameThread?.let { thread ->
        var title by remember(thread.id) { mutableStateOf(thread.title) }
        AlertDialog(
            onDismissRequest = { renameThread = null },
            title = { Text("Rename thread") },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val value = title.trim()
                        if (value.isNotEmpty()) viewModel.threadAction(thread.id, "rename", value)
                        renameThread = null
                    },
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { renameThread = null }) { Text("Cancel") }
            },
        )
    }

    deleteThread?.let { thread ->
        AlertDialog(
            onDismissRequest = { deleteThread = null },
            title = { Text("Delete thread?") },
            text = {
                Text(
                    "“${thread.title}” and its conversation will be permanently removed. " +
                        "This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.threadAction(thread.id, "delete")
                        deleteThread = null
                    },
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteThread = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ThreadGroup(
    threads: List<ThreadShell>,
    projects: Map<String, Project>,
    archived: Boolean,
    onOpen: (String) -> Unit,
    onAction: (ThreadShell, String, String) -> Unit,
    onRename: (ThreadShell) -> Unit,
    onDelete: (ThreadShell) -> Unit,
) {
    GroupedCard {
        threads.forEachIndexed { index, thread ->
            ThreadRow(
                thread = thread,
                project = projects[thread.projectId],
                archived = archived,
                onOpen = { onOpen(thread.id) },
                onAction = { action, value -> onAction(thread, action, value) },
                onRename = { onRename(thread) },
                onDelete = { onDelete(thread) },
            )
            if (index < threads.lastIndex) InsetDivider(start = 72.dp)
        }
    }
}

@Composable
private fun ThreadRow(
    thread: ThreadShell,
    project: Project?,
    archived: Boolean,
    onOpen: () -> Unit,
    onAction: (String, String) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val status = threadStatus(thread)
    val tone = statusToneColor(status.tone)
    val preview = thread.latestVisibleMessage?.text
        ?.replace('\n', ' ')
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    val projectTitle = project?.title?.ifBlank { project.workspaceRoot.substringAfterLast('/') }
        ?: "Project"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 14.dp, top = 13.dp, bottom = 13.dp, end = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        ProjectAvatar(
            title = projectTitle,
            key = thread.projectId,
            size = 44.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f).padding(top = 1.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = thread.title.ifBlank { "Untitled thread" },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (thread.pinnedAt != null) {
                    Spacer(Modifier.width(5.dp))
                    Icon(
                        Icons.Rounded.PushPin,
                        contentDescription = "Pinned",
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = preview ?: projectTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(
                    tone = status.tone,
                    pulsing = status.tone == StatusTone.Active,
                    size = 6.dp,
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = status.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = tone,
                )
                Text(
                    text = "  ·  $projectTitle  ·  ${modelLabel(thread)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = relativeTime(thread.updatedAt.ifEmpty { thread.createdAt }),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp, end = 4.dp),
            )
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = "Thread actions",
                        modifier = Modifier.size(19.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(if (thread.pinnedAt != null) "Unpin" else "Pin") },
                        leadingIcon = { Icon(Icons.Rounded.PushPin, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onAction(if (thread.pinnedAt != null) "unpin" else "pin", "")
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(if (archived) "Unarchive" else "Archive") },
                        leadingIcon = { Icon(Icons.Rounded.Archive, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onAction(if (archived) "unarchive" else "archive", "")
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Mark as read") },
                        leadingIcon = { Icon(Icons.Rounded.MarkEmailRead, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onAction("markRead", "")
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Mark as unread") },
                        leadingIcon = { Icon(Icons.Rounded.MarkEmailUnread, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onAction("markUnread", "")
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun statusToneColor(tone: StatusTone): Color = when (tone) {
    StatusTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    StatusTone.Active -> MaterialTheme.colorScheme.primary
    StatusTone.Attention -> MaterialTheme.colorScheme.tertiary
    StatusTone.Error -> MaterialTheme.colorScheme.error
}
