package com.t3codes.mobile

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.MarkEmailUnread
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.t3codes.core.Project
import com.t3codes.core.ThreadShell

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadsScreen(state: UiState, viewModel: MainViewModel) {
    var renameThread by remember { mutableStateOf<ThreadShell?>(null) }
    var deleteThread by remember { mutableStateOf<ThreadShell?>(null) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }

    // Opening the Chats tab (or returning from a thread) should always show
    // what is on the server right now, even if the live feed went silent while
    // the app was in the background.
    LaunchedEffect(Unit) { viewModel.refresh() }

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
    val chats = filtered.filter { it.pinnedAt == null && it.settledAt == null && !it.isArchived }
    val archived = state.archivedThreads.filter { thread ->
        val matchesProject = state.projectFilter == null ||
            thread.projectId == state.projectFilter
        val matchesSearch = query.isEmpty() ||
            thread.title.contains(query, ignoreCase = true) ||
            thread.latestVisibleMessage?.text?.contains(query, ignoreCase = true) == true
        matchesProject && matchesSearch
    }
    val settled = (
        filtered.filter { it.settledAt != null && !it.isArchived } +
            if (state.showArchived) archived else emptyList()
        ).distinctBy { it.id }
    val selectedProject = state.projectFilter?.let { projectsById[it] }
    val filterCount = listOf(
        state.projectFilter != null,
        !state.showSettled,
        state.showArchived,
    ).count { it }

    Column(modifier = Modifier.fillMaxSize()) {
        HomeTopBar(
            query = state.search,
            searchOpen = searchOpen,
            filterCount = filterCount,
            onToggleSearch = {
                if (searchOpen) viewModel.setSearch("")
                searchOpen = !searchOpen
            },
            onSettings = { viewModel.selectTab(AppTab.Settings) },
        )

        if (state.connectionStatus != ConnectionStatus.Live) {
            ConnectionBanner(
                status = state.connectionStatus,
                environment = state.config?.environment?.label?.ifBlank { "the machine" } ?: "the machine",
                note = state.connectionNote,
                onRetry = viewModel::retryConnection,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }

        AnimatedVisibility(visible = searchOpen || state.search.isNotBlank()) {
            TextField(
                value = state.search,
                onValueChange = viewModel::setSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Search chats") },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    if (state.search.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearch("") }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            selectedProject?.let { project ->
                item {
                    ActiveFilter(
                        label = project.title.ifBlank { project.workspaceRoot.substringAfterLast('/') },
                        onClear = { viewModel.setProjectFilter(null) },
                    )
                }
            }

            if (pinned.isNotEmpty()) {
                item { ChatSectionLabel("Pinned", pinned.size) }
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

            if (chats.isNotEmpty()) {
                item { ChatSectionLabel(if (pinned.isEmpty()) "Chats" else "All chats", chats.size) }
                item {
                    ThreadGroup(
                        threads = chats,
                        projects = projectsById,
                        archived = false,
                        onOpen = viewModel::openThread,
                        onAction = { thread, action, value -> viewModel.threadAction(thread.id, action, value) },
                        onRename = { renameThread = it },
                        onDelete = { deleteThread = it },
                    )
                }
            }

            if (!state.shellLoaded && state.threads.isEmpty() && state.archivedThreads.isEmpty()) {
                item { WorkingRow("Loading chats from ${state.config?.environment?.label ?: "the machine"}…") }
            } else if (filtered.isEmpty() && settled.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.Forum,
                        title = if (query.isNotEmpty() || state.projectFilter != null) {
                            "No matching chats"
                        } else {
                            "Start a conversation"
                        },
                        message = if (query.isNotEmpty() || state.projectFilter != null) {
                            "Try a different search or project filter."
                        } else {
                            "Threads you start will show up here with live progress, approvals, and diffs."
                        },
                        actionLabel = if (query.isNotEmpty() || state.projectFilter != null) {
                            "Clear filters"
                        } else {
                            "New thread"
                        },
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

            if (settled.isNotEmpty()) {
                item {
                    ChatSectionLabel(
                        text = "Settled",
                        count = settled.size,
                        trailing = {
                            TextButton(onClick = viewModel::toggleSettled) {
                                Text(if (state.showSettled) "Hide" else "Show")
                            }
                        },
                    )
                }
                if (state.showSettled) {
                    item {
                        ThreadGroup(
                            threads = settled,
                            projects = projectsById,
                            archived = true,
                            onOpen = viewModel::openThread,
                            onAction = { thread, action, value ->
                                viewModel.threadAction(thread.id, action, value)
                            },
                            onRename = { renameThread = it },
                            onDelete = { deleteThread = it },
                        )
                    }
                }
            }
        }
    }

    if (state.homeFilterOpen) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setHomeFilterOpen(false) },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Filter chats",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Project",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.projectFilter == null,
                            onClick = { viewModel.setProjectFilter(null) },
                            label = { Text("All projects") },
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
                                    text = project.title.ifBlank {
                                        project.workspaceRoot.substringAfterLast('/')
                                    },
                                    maxLines = 1,
                                )
                            },
                            shape = CircleShape,
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Settled chats", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "Show completed conversations in the list",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.showSettled,
                        onCheckedChange = { viewModel.toggleSettled() },
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Archived chats", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "Show archived conversations in the list",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.showArchived,
                        onCheckedChange = { viewModel.toggleArchived() },
                    )
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            viewModel.setProjectFilter(null)
                            viewModel.setSearch("")
                            if (!state.showSettled) viewModel.toggleSettled()
                            if (state.showArchived) viewModel.toggleArchived()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                    ) { Text("Clear") }
                    TextButton(
                        onClick = { viewModel.setHomeFilterOpen(false) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Done") }
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
private fun HomeTopBar(
    query: String,
    searchOpen: Boolean,
    filterCount: Int,
    onToggleSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "T3 Code",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.6).sp,
        )
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
        ) {
            Text(
                text = "ALPHA",
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                letterSpacing = 1.1.sp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.weight(1f))
        if (filterCount > 0) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            ) {
                Text(
                    text = filterCount.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(4.dp))
        }
        IconButton(onClick = onToggleSearch) {
            Icon(
                if (searchOpen || query.isNotBlank()) Icons.Rounded.Close else Icons.Rounded.Search,
                contentDescription = if (searchOpen) "Close search" else "Search",
                modifier = Modifier.size(21.dp),
            )
        }
        IconButton(onClick = onSettings) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = "Settings",
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

@Composable
private fun ActiveFilter(label: String, onClear: () -> Unit) {
    Surface(
        onClick = onClear,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    ) {
        Row(
            modifier = Modifier.padding(start = 11.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Clear project filter",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ChatSectionLabel(
    text: String,
    count: Int? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.2.sp,
            modifier = Modifier.weight(1f),
        )
        if (count != null) {
            Text(
                text = "($count)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
        trailing?.invoke()
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
        threads.forEach { thread ->
            ThreadRow(
                thread = thread,
                project = projects[thread.projectId],
                archived = archived,
                onOpen = { onOpen(thread.id) },
                onAction = { action, value -> onAction(thread, action, value) },
                onRename = { onRename(thread) },
                onDelete = { onDelete(thread) },
            )
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
    val settled = archived || thread.settledAt != null || thread.isArchived
    val projectTitle = project?.title?.ifBlank { project.workspaceRoot.substringAfterLast('/') }
        ?: "Project"
    val rightLabel = when {
        settled -> "Settled"
        status.tone == StatusTone.Error -> "Error"
        status.tone == StatusTone.Attention -> "Needs you"
        status.tone == StatusTone.Active -> "Working"
        else -> relativeTime(thread.updatedAt.ifEmpty { thread.createdAt })
    }
    val rightColor = when {
        settled -> MaterialTheme.colorScheme.tertiary
        status.tone == StatusTone.Error -> MaterialTheme.colorScheme.error
        status.tone == StatusTone.Attention -> MaterialTheme.colorScheme.tertiary
        status.tone == StatusTone.Active -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 17.dp, top = 13.dp, bottom = 13.dp, end = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = projectTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (status.tone == StatusTone.Active) {
                        StatusDot(
                            tone = StatusTone.Active,
                            pulsing = true,
                            size = 5.dp,
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = rightLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = rightColor,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = thread.title.ifBlank { "Untitled thread" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (thread.pinnedAt != null) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Rounded.PushPin,
                        contentDescription = "Pinned",
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(13.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Box {
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = "Thread actions",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
                if (!thread.isArchived) {
                    DropdownMenuItem(
                        text = { Text(if (thread.settledAt != null) "Unsettle" else "Settle") },
                        leadingIcon = {
                            Icon(
                                if (thread.settledAt != null) Icons.Rounded.Unarchive else Icons.Rounded.Inbox,
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onAction(if (thread.settledAt != null) "unsettle" else "settle", "")
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(if (thread.isArchived) "Unarchive" else "Archive") },
                    leadingIcon = { Icon(Icons.Rounded.Archive, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onAction(if (thread.isArchived) "unarchive" else "archive", "")
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
