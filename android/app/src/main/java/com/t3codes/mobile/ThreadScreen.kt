package com.t3codes.mobile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Difference
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Terminal
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
import com.t3codes.core.T3Core
import com.t3codes.core.ThreadShell
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.intOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadScreen(state: UiState, viewModel: MainViewModel) {
    val thread = state.thread ?: return
    val summary = thread.summary ?: state.summaryFor(thread.threadId)
    val projection = thread.projection
    val effective = if (projection.thread.id.isNotEmpty()) projection.thread else summary
    val status = threadStatus(effective)
    val project = state.projectFor(effective ?: summary)
    val rows = remember(projection.visibleTurnItems, thread.olderItems) {
        projection.timeline(thread.olderItems)
    }
    val running = projection.thread.activeRunId != null ||
        summary?.activeRunId != null ||
        projection.thread.status == "running"
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
    var modelPickerOpen by remember { mutableStateOf(false) }
    val modelChoices = remember(state.config) { state.config?.modelChoices().orEmpty() }
    val currentSelection = summary?.modelSelection

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
                                text = { Text("Change model") },
                                leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    modelPickerOpen = true
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
                onSend = viewModel::sendMessage,
                onStop = viewModel::stopRun,
                onAttach = viewModel::attach,
                onRemoveAttachment = viewModel::removeAttachment,
                onRespond = viewModel::respond,
                onDismissError = viewModel::dismissThreadError,
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                    )
                }
            }
        }
    }

    if (modelPickerOpen) {
        AlertDialog(
            onDismissRequest = { modelPickerOpen = false },
            title = { Text("Change model") },
            text = {
                if (modelChoices.isEmpty()) {
                    Text(
                        text = "No models are available from the connected providers.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(modelChoices, key = { it.key }) { choice ->
                            val selected = currentSelection?.instanceId == choice.instanceId &&
                                currentSelection?.model == choice.model.key
                            Surface(
                                onClick = {
                                    val selection = ModelSelection(
                                        instanceId = choice.instanceId,
                                        model = choice.model.key,
                                    )
                                    viewModel.threadAction(
                                        thread.threadId,
                                        "setModel",
                                        T3Core.json.encodeToJsonElement(selection).toString(),
                                    )
                                    modelPickerOpen = false
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
                                        Text(
                                            text = choice.model.label,
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                        Text(
                                            text = choice.providerName,
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
                }
            },
            confirmButton = {
                TextButton(onClick = { modelPickerOpen = false }) { Text("Done") }
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
) {
    val listState = rememberLazyListState()
    val itemCount = rows.size +
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
    LaunchedEffect(thread.threadId, thread.loading, itemCount) {
        if (!initialScrollDone && itemCount > 0) {
            listState.scrollToItem(itemCount - 1)
            initialScrollDone = true
        }
    }
    LaunchedEffect(rows.size, rows.lastOrNull()?.text, rows.lastOrNull()?.output, running) {
        if (initialScrollDone && atBottom && itemCount > 0) {
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
            PullRequestCard(pullRequest)
        }
        items(rows, key = { it.key }) { entry ->
            when (entry.kind) {
                TimelineKind.User -> UserBubble(entry)
                TimelineKind.Assistant -> AssistantMessage(entry)
                TimelineKind.Reasoning -> ThinkingCard(entry)
                TimelineKind.Command -> CommandCard(entry)
                TimelineKind.FileChange -> FileChangeCard(entry)
                TimelineKind.Tool -> ToolCard(entry)
                TimelineKind.Notice -> NoticeRow(entry)
                TimelineKind.Checkpoint -> CheckpointRow(entry)
                TimelineKind.Unknown -> UnknownCard(entry)
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
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 11.dp)) {
                    SelectionContainer {
                        Text(
                            text = entry.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    if (entry.attachmentCount > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${entry.attachmentCount} attachment${if (entry.attachmentCount == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark(size = 20.dp)
            Spacer(Modifier.width(7.dp))
            Text(
                text = "T3",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (entry.streaming) {
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "writing…",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
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
private fun PullRequestCard(pullRequest: PullRequestInfo) {
    val uriHandler = LocalUriHandler.current
    val clickable = pullRequest.url.isNotBlank()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        onClick = { if (clickable) uriHandler.openUri(pullRequest.url) },
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
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onPickAttachment: () -> Unit,
    onRemoveAttachment: (com.t3codes.core.Attachment) -> Unit,
) {
    var text by rememberSaveable(thread.threadId) { mutableStateOf("") }
    val canSend = text.isNotBlank() || thread.pendingAttachments.isNotEmpty()
    val model = summary?.modelSelection?.model?.takeIf { it.isNotBlank() }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
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

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(if (running) "Queue a message for T3…" else "Message T3…")
                },
                minLines = 1,
                maxLines = 6,
                shape = RoundedCornerShape(20.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default,
                ),
                keyboardActions = KeyboardActions(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            )

            Spacer(Modifier.height(7.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPickAttachment, enabled = !thread.uploading) {
                    Icon(
                        Icons.Rounded.AttachFile,
                        contentDescription = "Attach file",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!model.isNullOrBlank()) {
                    Pill(
                        text = model.substringAfterLast('/'),
                        container = MaterialTheme.colorScheme.surfaceContainerHigh,
                        content = MaterialTheme.colorScheme.onSurfaceVariant,
                        icon = Icons.Rounded.AutoAwesome,
                    )
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
