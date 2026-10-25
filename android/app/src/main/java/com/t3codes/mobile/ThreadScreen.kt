package com.t3codes.mobile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Difference
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.t3codes.core.ModelSelection
import com.t3codes.core.Project
import com.t3codes.core.RuntimeRequest
import com.t3codes.core.ServerConfig
import com.t3codes.core.T3Core
import com.t3codes.core.ThreadShell
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.intOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadScreen(state: UiState, viewModel: MainViewModel) {
    val uriHandler = LocalUriHandler.current
    val thread = state.thread ?: return
    val summary = thread.summary ?: state.summaryFor(thread.threadId)
    val projection = thread.projection
    val effective = if (projection.thread.id.isNotEmpty()) projection.thread else summary
    val status = threadStatus(effective)
    val project = state.projectFor(effective ?: summary)
    val rows = remember(projection.visibleTurnItems, thread.olderItems, thread.pendingUserItems) {
        projection.timeline(thread.olderItems, thread.pendingUserItems)
    }
    val running = projection.thread.activeRunId != null ||
        summary?.activeRunId != null ||
        projection.thread.status == "running" ||
        projection.isRunning()
    val title = effective?.title?.ifBlank { "Untitled thread" }
        ?: summary?.title?.ifBlank { "Untitled thread" }
        ?: "Thread"
    val projectTitle = project?.title?.ifBlank { project.workspaceRoot.substringAfterLast('/') } ?: "Project"
    val pullRequests = remember(effective?.pullRequests, effective?.linkedPullRequest) {
        parsePullRequests(effective?.pullRequests.orEmpty(), effective?.linkedPullRequest)
    }

    var menuOpen by remember { mutableStateOf(false) }
    var renameOpen by remember { mutableStateOf(false) }
    var deleteOpen by remember { mutableStateOf(false) }
    var runtimePickerOpen by remember { mutableStateOf(false) }
    val runtimeMode = summary?.runtimeMode.orEmpty()
    val planMode = summary?.interactionMode == "plan"

    LaunchedEffect(thread.threadId) {
        viewModel.ensureModelsLoaded(force = false)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                navigationIcon = {
                    IconButton(onClick = viewModel::closeThread) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            StatusDot(
                                tone = status.tone,
                                pulsing = status.tone == StatusTone.Active,
                                size = 6.dp,
                            )
                            Text(
                                text = status.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusToneColor(status.tone),
                            )
                            if (state.connectionStatus == ConnectionStatus.Reconnecting) {
                                Text(
                                    text = "· reconnecting",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            } else if (state.connectionStatus == ConnectionStatus.Offline) {
                                Text(
                                    text = "· offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            } else if (!thread.live && !thread.loading) {
                                Text(
                                    text = "· syncing",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                            Text(
                                text = "· $projectTitle",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "Thread actions")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Refresh thread") },
                                leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    viewModel.refreshThread()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Review changes") },
                                leadingIcon = { Icon(Icons.Rounded.Difference, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    viewModel.openDiff(thread.threadId)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Terminal") },
                                leadingIcon = { Icon(Icons.Rounded.Terminal, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    viewModel.openTerminal(thread.threadId)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Browse files") },
                                leadingIcon = { Icon(Icons.Rounded.FolderOpen, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    viewModel.openFiles(thread.threadId)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Run permissions") },
                                leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    runtimePickerOpen = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(if (planMode) "Leave plan mode" else "Plan mode") },
                                leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    viewModel.threadAction(
                                        thread.threadId,
                                        "setInteractionMode",
                                        if (planMode) "default" else "plan",
                                    )
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    renameOpen = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(if (summary?.pinnedAt != null) "Unpin" else "Pin") },
                                leadingIcon = { Icon(Icons.Rounded.PushPin, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    viewModel.threadAction(
                                        thread.threadId,
                                        if (summary?.pinnedAt != null) "unpin" else "pin",
                                    )
                                },
                            )
                            if (summary?.isArchived != true) {
                                DropdownMenuItem(
                                    text = {
                                        Text(if (summary?.settledAt != null) "Unsettle" else "Settle")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (summary?.settledAt != null) Icons.Rounded.Unarchive
                                            else Icons.Rounded.Inbox,
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        menuOpen = false
                                        viewModel.threadAction(
                                            thread.threadId,
                                            if (summary?.settledAt != null) "unsettle" else "settle",
                                        )
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(if (summary?.isArchived == true) "Unarchive" else "Archive")
                                },
                                leadingIcon = { Icon(Icons.Rounded.Archive, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    viewModel.threadAction(
                                        thread.threadId,
                                        if (summary?.isArchived == true) "unarchive" else "archive",
                                    )
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
                                    deleteOpen = true
                                },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            ComposerColumn(
                thread = thread,
                summary = summary,
                running = running,
                config = state.config,
                modelsRefreshing = state.modelsRefreshing,
                onRefreshModels = { viewModel.ensureModelsLoaded(force = true) },
                onSelectModel = { choice ->
                    val selection = ModelSelection(
                        instanceId = choice.instanceId,
                        model = choice.model.key,
                    )
                    viewModel.threadAction(
                        thread.threadId,
                        "setModel",
                        T3Core.json.encodeToJsonElement(selection).toString(),
                    )
                },
                onSend = viewModel::sendMessage,
                onStop = viewModel::stopRun,
                onAttach = viewModel::attach,
                onRemoveAttachment = viewModel::removeAttachment,
                onRespond = viewModel::respond,
                onDismissError = viewModel::dismissThreadError,
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.connectionStatus != ConnectionStatus.Live) {
                ConnectionBanner(
                    status = state.connectionStatus,
                    environment = state.config?.environment?.label?.ifBlank { "the machine" }
                        ?: "the machine",
                    note = state.connectionNote,
                    onRetry = viewModel::retryConnection,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when {
                    thread.loading && rows.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    thread.error != null && rows.isEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            ErrorBanner(message = thread.error, onRetry = viewModel::retryThread)
                        }
                    }
                    rows.isEmpty() && !running -> {
                        EmptyState(
                            icon = Icons.Rounded.AutoAwesome,
                            title = "Say hello",
                            message = "This thread is ready. Send a message to start the agent.",
                        )
                    }
                    else -> {
                        TimelineContent(
                            thread = thread,
                            rows = rows,
                            running = running,
                            pullRequests = pullRequests,
                            onLoadEarlier = viewModel::loadEarlier,
                            onOpenPullRequest = { pullRequest ->
                                val projectId = effective?.projectId.orEmpty()
                                if (projectId.isBlank()) {
                                    if (pullRequest.url.isNotBlank()) {
                                        uriHandler.openUri(pullRequest.url)
                                    }
                                } else {
                                    viewModel.openPullRequestFromThread(
                                        url = pullRequest.url,
                                        projectId = projectId,
                                        title = pullRequest.title,
                                        stateName = pullRequest.state,
                                        number = pullRequest.number,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (runtimePickerOpen) {
        AlertDialog(
            onDismissRequest = { runtimePickerOpen = false },
            title = { Text("Run permissions") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(runtimeModes, key = { it.value }) { mode ->
                        val selected = runtimeMode == mode.value
                        Surface(
                            onClick = {
                                viewModel.threadAction(
                                    thread.threadId,
                                    "setRuntimeMode",
                                    mode.value,
                                )
                                runtimePickerOpen = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(mode.label, style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        text = mode.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { runtimePickerOpen = false }) { Text("Cancel") }
            },
        )
    }

    if (renameOpen) {
        var value by remember(thread.threadId, summary?.title) {
            mutableStateOf(summary?.title ?: title)
        }
        AlertDialog(
            onDismissRequest = { renameOpen = false },
            title = { Text("Rename thread") },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val next = value.trim()
                        if (next.isNotEmpty()) viewModel.threadAction(thread.threadId, "rename", next)
                        renameOpen = false
                    },
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { renameOpen = false }) { Text("Cancel") }
            },
        )
    }

    if (deleteOpen) {
        AlertDialog(
            onDismissRequest = { deleteOpen = false },
            title = { Text("Delete thread?") },
            text = { Text("This conversation will be permanently removed. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.threadAction(thread.threadId, "delete")
                        deleteOpen = false
                    },
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteOpen = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun TimelineContent(
    thread: ThreadUi,
    rows: List<TimelineEntry>,
    running: Boolean,
    pullRequests: List<PullRequestInfo>,
    onLoadEarlier: () -> Unit,
    onOpenPullRequest: (PullRequestInfo) -> Unit,
) {
    val listState = rememberLazyListState()
    val blocks = remember(rows) { groupTimeline(rows) }
    val itemCount = blocks.size +
        (if (thread.hasMoreHistory) 1 else 0) +
        (if (running) 1 else 0) +
        pullRequests.size
    val atBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible >= info.totalItemsCount - 2
        }
    }
    var initialScrollDone by remember(thread.threadId) { mutableStateOf(false) }
    var followTail by remember(thread.threadId) { mutableStateOf(true) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress to atBottom }.collect { (scrolling, bottom) ->
            if (bottom) followTail = true
            else if (scrolling) followTail = false
        }
    }
    LaunchedEffect(thread.threadId, thread.loading, itemCount) {
        if (!initialScrollDone && itemCount > 0 && !thread.loading) {
            listState.scrollToItem(itemCount - 1)
            initialScrollDone = true
            followTail = true
        }
    }
    LaunchedEffect(thread.pendingUserItems.size, thread.sending) {
        if (thread.pendingUserItems.isNotEmpty()) {
            followTail = true
            if (itemCount > 0) listState.scrollToItem(itemCount - 1)
        }
    }
    LaunchedEffect(
        itemCount,
        blocks.lastOrNull()?.key,
        rows.lastOrNull()?.text,
        rows.lastOrNull()?.output,
        running,
    ) {
        if (initialScrollDone && followTail && itemCount > 0) {
            listState.scrollToItem(itemCount - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (thread.hasMoreHistory) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (thread.loadingEarlier) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        TextButton(onClick = onLoadEarlier) { Text("Load earlier messages") }
                    }
                }
            }
        }
        items(pullRequests) { pullRequest ->
            PullRequestCard(pullRequest, onOpen = { onOpenPullRequest(pullRequest) })
        }
        items(blocks, key = { it.key }) { block ->
            when (block) {
                is TimelineBlock.WorkGroup -> WorkGroupCard(block.entries)
                is TimelineBlock.Single -> {
                    val entry = block.entry
                    when (entry.kind) {
                        TimelineKind.User -> UserBubble(entry)
                        TimelineKind.Assistant -> AssistantMessage(entry)
                        TimelineKind.Reasoning,
                        TimelineKind.Command,
                        TimelineKind.FileChange,
                        TimelineKind.Tool,
                        -> WorkEntryRow(entry)
                        TimelineKind.Notice -> NoticeRow(entry)
                        TimelineKind.Checkpoint -> CheckpointRow(entry)
                        TimelineKind.Unknown -> UnknownCard(entry)
                    }
                }
            }
        }
        if (running) {
            item {
                WorkingRow("T3 is working…")
            }
        }
    }
}

@Composable
private fun UserBubble(entry: TimelineEntry) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxBubble = maxWidth * 0.86f
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Surface(
                modifier = Modifier.widthIn(max = maxBubble),
                shape = RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 11.dp)) {
                    SelectionContainer {
                        Text(
                            text = entry.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    if (entry.attachmentCount > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${entry.attachmentCount} attachment${if (entry.attachmentCount == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistantMessage(entry: TimelineEntry) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (entry.text.isBlank()) {
            WorkingRow("Thinking…")
        } else {
            MarkdownText(text = entry.text + if (entry.streaming) " ▍" else "")
        }
    }
}

@Composable
private fun UnknownCard(entry: TimelineEntry) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (entry.secondary.isNotBlank()) {
                Text(
                    text = entry.secondary.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(5.dp))
            }
            MarkdownText(text = entry.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PullRequestCard(pullRequest: PullRequestInfo, onOpen: () -> Unit) {
    val clickable = pullRequest.url.isNotBlank() || pullRequest.number != null
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        onClick = { if (clickable) onOpen() },
        enabled = clickable,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pullRequest.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        pullRequest.number?.let { "#$it" },
                        pullRequest.state.takeIf { it.isNotBlank() },
                    ).joinToString(" · ").ifBlank { "Pull request" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                )
            }
            if (clickable) {
                Icon(Icons.Rounded.ArrowUpward, contentDescription = "Open", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ComposerColumn(
    thread: ThreadUi,
    summary: ThreadShell?,
    running: Boolean,
    config: ServerConfig?,
    modelsRefreshing: Boolean,
    onRefreshModels: () -> Unit,
    onSelectModel: (ModelChoice) -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onAttach: (android.net.Uri) -> Unit,
    onRemoveAttachment: (com.t3codes.core.Attachment) -> Unit,
    onRespond: (String, String) -> Unit,
    onDismissError: () -> Unit,
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(onAttach)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        thread.projection.pendingRequests.firstOrNull()?.let { request ->
            ApprovalCard(request = request, onRespond = onRespond)
        }
        AnimatedVisibility(visible = thread.error != null) {
            thread.error?.let {
                ErrorBanner(
                    message = it,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    onDismiss = onDismissError,
                )
            }
        }
        Composer(
            thread = thread,
            summary = summary,
            running = running,
            config = config,
            modelsRefreshing = modelsRefreshing,
            onRefreshModels = onRefreshModels,
            onSelectModel = onSelectModel,
            onSend = onSend,
            onStop = onStop,
            onPickAttachment = { picker.launch("*/*") },
            onRemoveAttachment = onRemoveAttachment,
        )
    }
}

@Composable
private fun ApprovalCard(
    request: RuntimeRequest,
    onRespond: (String, String) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = approvalTitle(request.kind),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = approvalMessage(request.kind),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { onRespond("decline", "") }) {
                    Text("Decline", color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
                Spacer(Modifier.width(4.dp))
                Button(onClick = { onRespond("accept", "") }) { Text("Approve") }
            }
        }
    }
}

private fun approvalTitle(kind: String): String = when (kind) {
    "command" -> "Run this command?"
    "file-read" -> "Read this file?"
    "file-change" -> "Change this file?"
    "mcp-elicitation" -> "MCP request"
    "user-input" -> "Input needed"
    else -> "Approval needed"
}

private fun approvalMessage(kind: String): String = when (kind) {
    "command" -> "The agent is waiting to run a command in your workspace."
    "file-read" -> "The agent wants to read a file outside its usual scope."
    "file-change" -> "The agent wants to modify a file in your workspace."
    "user-input" -> "The agent asked a question. Answer it from the full T3 client for now."
    "mcp-elicitation" -> "An MCP server is requesting input."
    else -> "The agent is waiting for your decision before it continues."
}

@Composable
private fun Composer(
    thread: ThreadUi,
    summary: ThreadShell?,
    running: Boolean,
    config: ServerConfig?,
    modelsRefreshing: Boolean,
    onRefreshModels: () -> Unit,
    onSelectModel: (ModelChoice) -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onPickAttachment: () -> Unit,
    onRemoveAttachment: (com.t3codes.core.Attachment) -> Unit,
) {
    var text by rememberSaveable(thread.threadId) { mutableStateOf("") }
    var modelMenuOpen by remember(thread.threadId) { mutableStateOf(false) }
    val canSend = text.isNotBlank() || thread.pendingAttachments.isNotEmpty()
    val modelChoices = remember(config) { config?.modelChoices().orEmpty() }
    val currentSelection = summary?.modelSelection
    val selectedChoice = modelChoices.firstOrNull { choice ->
        choice.instanceId == currentSelection?.instanceId &&
            (choice.model.key == currentSelection?.model ||
                choice.model.id == currentSelection?.model ||
                choice.model.slug == currentSelection?.model)
    }
    val modelLabel = selectedChoice?.model?.label
        ?: currentSelection?.model?.substringAfterLast('/')?.takeIf { it.isNotBlank() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f),
        ),
    ) {
        Column(modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 5.dp, bottom = 8.dp)) {
            if (thread.pendingAttachments.isNotEmpty() || thread.uploading) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    contentPadding = PaddingValues(bottom = 7.dp),
                ) {
                    if (thread.uploading) {
                        item {
                            AssistChip(
                                onClick = {},
                                label = { Text("Uploading…") },
                                shape = CircleShape,
                            )
                        }
                    }
                    items(thread.pendingAttachments, key = { it.id }) { attachment ->
                        AssistChip(
                            onClick = { onRemoveAttachment(attachment) },
                            label = {
                                Text(
                                    text = attachment.name.ifBlank { "Attachment" },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            trailingIcon = {
                                Icon(Icons.Rounded.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                            },
                            shape = CircleShape,
                        )
                    }
                }
            }

            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = if (running) "Queue a message for T3…" else "Message T3…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                minLines = 1,
                maxLines = 6,
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default,
                ),
                keyboardActions = KeyboardActions(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )

            Spacer(Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPickAttachment, enabled = !thread.uploading) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Attach file",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    Surface(
                        onClick = {
                            modelMenuOpen = true
                            onRefreshModels()
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (modelsRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.6.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = modelLabel ?: if (modelsRefreshing) {
                                    "Loading models…"
                                } else {
                                    "Model"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.width(3.dp))
                            Icon(
                                Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = modelMenuOpen,
                        onDismissRequest = { modelMenuOpen = false },
                    ) {
                        if (modelsRefreshing) {
                            DropdownMenuItem(
                                text = { Text("Loading models…") },
                                leadingIcon = {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(15.dp),
                                        strokeWidth = 1.8.dp,
                                    )
                                },
                                onClick = {},
                                enabled = false,
                            )
                        }
                        if (modelChoices.isEmpty() && !modelsRefreshing) {
                            DropdownMenuItem(
                                text = { Text("Refresh models") },
                                leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                                onClick = onRefreshModels,
                            )
                        }
                        modelChoices.forEach { choice ->
                            val selected = currentSelection?.instanceId == choice.instanceId &&
                                (currentSelection?.model == choice.model.key ||
                                    currentSelection?.model == choice.model.slug)
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = choice.model.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = choice.providerName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (selected) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = "Selected",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                },
                                onClick = {
                                    modelMenuOpen = false
                                    onSelectModel(choice)
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                if (running) {
                    Surface(
                        onClick = onStop,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ) {
                        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Stop, contentDescription = "Stop", modifier = Modifier.size(21.dp))
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
                if (!running || canSend) {
                    Surface(
                        onClick = {
                            if (canSend) {
                                onSend(text)
                                text = ""
                            }
                        },
                        enabled = canSend && !thread.sending,
                        shape = CircleShape,
                        color = if (canSend) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = if (canSend) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    ) {
                        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                            if (thread.sending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.ArrowUpward,
                                    contentDescription = if (running) "Queue message" else "Send",
                                    modifier = Modifier.size(21.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class RuntimeModeOption(
    val value: String,
    val label: String,
    val description: String,
)

private val runtimeModes = listOf(
    RuntimeModeOption(
        value = "approval-required",
        label = "Ask first",
        description = "Approve commands and file changes",
    ),
    RuntimeModeOption(
        value = "auto-accept-edits",
        label = "Auto-accept edits",
        description = "Approve file edits, ask before commands",
    ),
    RuntimeModeOption(
        value = "auto",
        label = "Auto",
        description = "Ask only when T3 thinks it is necessary",
    ),
    RuntimeModeOption(
        value = "full-access",
        label = "Full access",
        description = "Run commands and edit files without asking",
    ),
)

private data class PullRequestInfo(
    val title: String,
    val url: String,
    val state: String,
    val number: Int?,
)

private fun parsePullRequests(
    list: List<JsonElement>,
    linked: JsonElement?,
): List<PullRequestInfo> {
    val all = list + listOfNotNull(linked)
    return all.mapNotNull { element ->
        val obj = element as? JsonObject ?: return@mapNotNull null
        val url = obj.stringValue("url").orEmpty()
        val title = obj.stringValue("title").orEmpty()
        val state = obj.stringValue("state").orEmpty()
        val number = (obj["number"] as? JsonPrimitive)?.intOrNull
        if (url.isBlank() && title.isBlank()) return@mapNotNull null
        PullRequestInfo(
            title = title.ifBlank { "Pull request" },
            url = url,
            state = state,
            number = number,
        )
    }.distinctBy { it.url.ifBlank { it.title + it.number } }
}

private fun JsonObject.stringValue(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

@Composable
private fun statusToneColor(tone: StatusTone): Color = when (tone) {
    StatusTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    StatusTone.Active -> MaterialTheme.colorScheme.primary
    StatusTone.Attention -> MaterialTheme.colorScheme.tertiary
    StatusTone.Error -> MaterialTheme.colorScheme.error
}
