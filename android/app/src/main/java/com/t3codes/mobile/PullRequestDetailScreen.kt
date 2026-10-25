package com.t3codes.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CallMerge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.t3codes.core.PullRequestActor
import com.t3codes.core.PullRequestCheck
import com.t3codes.core.PullRequestDetail
import com.t3codes.core.PullRequestEntry
import java.util.Locale

@Composable
fun PullRequestDetailScreen(state: UiState, viewModel: MainViewModel) {
    val ui = state.pullRequestDetail ?: return
    val uriHandler = LocalUriHandler.current
    var menuOpen by remember { mutableStateOf(false) }
    var mergeDialog by remember { mutableStateOf(false) }
    val detail = ui.detail

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 4.dp, top = 6.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = viewModel::closePullRequest) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(21.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "#${ui.entry.number}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = ui.entry.repository.ifBlank { "Pull request" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (ui.entry.url.isNotBlank()) {
                IconButton(onClick = { uriHandler.openUri(ui.entry.url) }) {
                    Icon(
                        Icons.Rounded.OpenInNew,
                        contentDescription = "Open on GitHub",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            IconButton(onClick = {
                viewModel.loadPullRequestDetail()
                viewModel.loadPullRequestDiff()
            }) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = "Refresh pull request",
                    modifier = Modifier.size(20.dp),
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = "Pull request actions",
                        modifier = Modifier.size(20.dp),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Open on GitHub") },
                        leadingIcon = { Icon(Icons.Rounded.OpenInNew, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            if (ui.entry.url.isNotBlank()) uriHandler.openUri(ui.entry.url)
                        },
                    )
                    if (detail != null) {
                        val actions = detail.viewerPermissions.actions
                        if (detail.state == "open" && "draft" in actions && !detail.isDraft) {
                            DropdownMenuItem(
                                text = { Text("Convert to draft") },
                                onClick = {
                                    menuOpen = false
                                    viewModel.runPullRequestAction("draft")
                                },
                            )
                        }
                        if (detail.state == "open" && "ready" in actions && detail.isDraft) {
                            DropdownMenuItem(
                                text = { Text("Mark ready for review") },
                                onClick = {
                                    menuOpen = false
                                    viewModel.runPullRequestAction("ready")
                                },
                            )
                        }
                        if (detail.state == "open" && "close" in actions) {
                            DropdownMenuItem(
                                text = { Text("Close pull request", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuOpen = false
                                    viewModel.runPullRequestAction("close")
                                },
                            )
                        }
                        if (detail.state == "closed" && "reopen" in actions) {
                            DropdownMenuItem(
                                text = { Text("Reopen pull request") },
                                onClick = {
                                    menuOpen = false
                                    viewModel.runPullRequestAction("reopen")
                                },
                            )
                        }
                        if (detail.state == "open" && "update-branch" in actions) {
                            DropdownMenuItem(
                                text = { Text("Update branch") },
                                leadingIcon = { Icon(Icons.Rounded.Sync, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    viewModel.runPullRequestAction("update-branch")
                                },
                            )
                        }
                    }
                }
            }
        }

        if (ui.error != null) {
            ErrorBanner(
                message = ui.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                onRetry = viewModel::loadPullRequestDetail,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = ui.tab == "overview",
                onClick = { viewModel.setPullRequestTab("overview") },
                label = { Text("Overview") },
                shape = CircleShape,
            )
            FilterChip(
                selected = ui.tab == "files",
                onClick = { viewModel.setPullRequestTab("files") },
                label = {
                    Text(
                        if (detail != null && detail.changedFiles > 0) {
                            "Files (${detail.changedFiles})"
                        } else {
                            "Files"
                        },
                    )
                },
                shape = CircleShape,
            )
        }

        when (ui.tab) {
            "files" -> PullRequestFilesTab(ui, viewModel)
            else -> PullRequestOverviewTab(
                ui = ui,
                detail = detail,
                onOpenUrl = { url -> if (url.isNotBlank()) uriHandler.openUri(url) },
                onSelectMethod = viewModel::setPullRequestMergeMethod,
                onRetry = viewModel::loadPullRequestDetail,
                onMerge = { mergeDialog = true },
            )
        }
    }

    if (mergeDialog && detail != null) {
        MergeConfirmDialog(
            detail = detail,
            method = ui.selectedMethod,
            busy = ui.actionBusy,
            onMethod = viewModel::setPullRequestMergeMethod,
            onConfirm = {
                mergeDialog = false
                viewModel.mergePullRequest(ui.selectedMethod)
            },
            onDismiss = { mergeDialog = false },
        )
    }
}

@Composable
private fun PullRequestOverviewTab(
    ui: PullRequestDetailUi,
    detail: PullRequestDetail?,
    onOpenUrl: (String) -> Unit,
    onSelectMethod: (String) -> Unit,
    onRetry: () -> Unit,
    onMerge: () -> Unit,
) {
    if (detail == null) {
        if (ui.loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Loading pull request…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            EmptyState(
                icon = Icons.Rounded.ErrorOutline,
                title = "Could not load this pull request",
                message = ui.error ?: "The environment did not return pull request details.",
                actionLabel = "Try again",
                onAction = onRetry,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PullRequestSummaryCard(detail)
        }
        item {
            PullRequestMergeCard(
                detail = detail,
                selectedMethod = ui.selectedMethod,
                busy = ui.actionBusy,
                onSelectMethod = onSelectMethod,
                onMerge = onMerge,
            )
        }
        if (detail.checks.isNotEmpty()) {
            item { PullRequestChecksCard(detail.checks) }
        }
        if (detail.labels.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    detail.labels.forEach { label ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ) {
                            Text(
                                text = label.name,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
        if (detail.body.isNotBlank()) {
            item {
                GroupedCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        MarkdownText(
                            text = detail.body,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
        if (detail.reviewers.isNotEmpty()) {
            item {
                GroupedCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Reviewers",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        detail.reviewers.forEach { reviewer ->
                            Row(
                                modifier = Modifier.padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(actorName(reviewer), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
        item {
            TextButton(onClick = { onOpenUrl(detail.url) }) {
                Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(7.dp))
                Text("Open full pull request on GitHub")
            }
        }
    }
}

@Composable
private fun PullRequestSummaryCard(detail: PullRequestDetail) {
    GroupedCard {
        Column(modifier = Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PrStatusPill(
                    text = when {
                        detail.state == "merged" -> "Merged"
                        detail.state == "closed" -> "Closed"
                        detail.isDraft -> "Draft"
                        else -> "Open"
                    },
                    color = when {
                        detail.state == "merged" -> MaterialTheme.colorScheme.tertiary
                        detail.state == "closed" -> MaterialTheme.colorScheme.error
                        detail.isDraft -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.primary
                    },
                )
                Spacer(Modifier.width(8.dp))
                PrStatusPill(
                    text = when (detail.mergeability) {
                        "mergeable" -> "Mergeable"
                        "conflicting" -> "Conflicts"
                        else -> "Checking merge"
                    },
                    color = when (detail.mergeability) {
                        "mergeable" -> MaterialTheme.colorScheme.tertiary
                        "conflicting" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "+${detail.additions}  -${detail.deletions}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = detail.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = buildString {
                    append(detail.author?.let(::actorName) ?: "unknown")
                    append(" wants to merge ")
                    append(detail.headBranch)
                    append(" into ")
                    append(detail.baseBranch)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${detail.changedFiles} files",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (detail.baseComparison == "behind" && (detail.behindBy ?: 0) > 0) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "${detail.behindBy} behind",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun PullRequestMergeCard(
    detail: PullRequestDetail,
    selectedMethod: String,
    busy: Boolean,
    onSelectMethod: (String) -> Unit,
    onMerge: () -> Unit,
) {
    val methods = detail.capabilities.mergeMethods
        .ifEmpty { listOf("squash", "merge", "rebase") }
    val reason = mergeUnavailableReason(detail, selectedMethod)
    GroupedCard {
        Column(modifier = Modifier.padding(17.dp)) {
            Text(
                text = "Merge",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Complete this pull request from here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                methods.forEach { method ->
                    FilterChip(
                        selected = selectedMethod == method,
                        onClick = { onSelectMethod(method) },
                        label = { Text(mergeMethodLabel(method)) },
                        enabled = busy.not() && method in detail.capabilities.mergeMethods,
                        shape = CircleShape,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onMerge,
                enabled = reason == null && !busy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Icon(Icons.Rounded.CallMerge, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(mergeMethodLabel(selectedMethod))
            }
            if (reason != null) {
                Spacer(Modifier.height(9.dp))
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (detail.autoMergeEnabled == true) {
                Spacer(Modifier.height(7.dp))
                Text(
                    text = "Auto-merge is already enabled on the host.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

@Composable
private fun PullRequestChecksCard(checks: List<PullRequestCheck>) {
    GroupedCard {
        Column(modifier = Modifier.padding(17.dp)) {
            Text(
                text = "Checks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(9.dp))
            checks.forEachIndexed { index, check ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 7.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (icon, tint) = when (check.status) {
                        "success" -> Icons.Rounded.CheckCircle to MaterialTheme.colorScheme.tertiary
                        "failure", "cancelled" -> Icons.Rounded.ErrorOutline to
                            MaterialTheme.colorScheme.error
                        "skipped", "neutral" -> Icons.Rounded.RemoveCircleOutline to
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else -> Icons.Rounded.HourglassTop to MaterialTheme.colorScheme.tertiary
                    }
                    Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp), tint = tint)
                    Spacer(Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = check.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!check.description.isNullOrBlank()) {
                            Text(
                                text = check.description.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (check.required == true) {
                        Text(
                            text = "required",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MergeConfirmDialog(
    detail: PullRequestDetail,
    method: String,
    busy: Boolean,
    onMethod: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(mergeMethodLabel(method)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Merge ${detail.headBranch} into ${detail.baseBranch}? " +
                        mergeMethodDescription(method),
                )
                Text(
                    text = "This cannot be undone from the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !busy) {
                Text(if (busy) "Merging…" else "Merge")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun PullRequestFilesTab(ui: PullRequestDetailUi, viewModel: MainViewModel) {
    val diff = ui.diff
    val files = remember(diff?.patch) { parseUnifiedDiff(diff?.patch.orEmpty()) }
    if (diff == null && ui.diffLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(strokeWidth = 2.dp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Loading changed files…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }
    if (ui.diffError != null && diff == null) {
        Column(modifier = Modifier.padding(16.dp)) {
            ErrorBanner(
                message = ui.diffError,
                onRetry = { viewModel.loadPullRequestDiff() },
            )
        }
        return
    }
    if (files.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.FolderOpen,
            title = "No changed files",
            message = "This pull request has no textual changes, or the host did not return a diff.",
            actionLabel = "Reload",
            onAction = { viewModel.loadPullRequestDiff() },
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${files.size} changed file${if (files.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (diff?.truncated == true) {
                    Text(
                        text = "Some files hidden by the host",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(files, key = { "${it.previousPath ?: ""}:${it.path}" }) { file ->
            PullRequestFileCard(file)
        }
        if (ui.diffError != null) {
            item { ErrorBanner(message = ui.diffError) }
        }
        val cursor = diff?.nextCursor
        if (!cursor.isNullOrBlank()) {
            item {
                TextButton(
                    onClick = { viewModel.loadPullRequestDiff(cursor) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !ui.diffLoading,
                ) {
                    Text(if (ui.diffLoading) "Loading…" else "Load more changed files")
                }
            }
        }
    }
}

@Composable
private fun PullRequestFileCard(file: PullRequestDiffFile) {
    var expanded by rememberSaveable(file.path) { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(start = 14.dp, top = 11.dp, bottom = 11.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Collapse file" else "Expand file",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = file.path,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                if (file.additions > 0) {
                    Text(
                        text = "+${file.additions}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                if (file.deletions > 0) {
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "-${file.deletions}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (expanded) {
                if (file.isBinary || file.lines.isEmpty()) {
                    Text(
                        text = "No textual diff for this file.",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    ) {
                        file.lines.take(MAX_DIFF_LINES).forEach { line ->
                            DiffLineRow(line)
                        }
                        if (file.lines.size > MAX_DIFF_LINES) {
                            Text(
                                text = "… ${file.lines.size - MAX_DIFF_LINES} more lines",
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiffLineRow(line: PullRequestDiffLine) {
    val background = when (line.kind) {
        "addition" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
        "deletion" -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
        "hunk" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
        else -> Color.Transparent
    }
    val color = when (line.kind) {
        "addition" -> MaterialTheme.colorScheme.tertiary
        "deletion" -> MaterialTheme.colorScheme.error
        "hunk" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 10.dp, vertical = 1.dp),
    ) {
        Text(
            text = line.oldNumber?.toString()?.padStart(4) ?: "    ",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
        )
        Text(
            text = line.newNumber?.toString()?.padStart(4) ?: "    ",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = line.text.ifEmpty { " " },
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = color,
            softWrap = false,
        )
    }
}

@Composable
private fun PrStatusPill(text: String, color: Color) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
        )
    }
}

private fun actorName(actor: PullRequestActor): String =
    actor.name?.takeIf { it.isNotBlank() } ?: actor.login.ifBlank { "someone" }

private fun mergeMethodLabel(method: String): String = when (method) {
    "merge" -> "Create a merge commit"
    "rebase" -> "Rebase and merge"
    else -> "Squash and merge"
}

private fun mergeMethodDescription(method: String): String = when (method) {
    "merge" -> "The commits will be kept and a merge commit added."
    "rebase" -> "The commits will be added without a merge commit."
    else -> "The commits will be combined into one commit."
}

private fun mergeUnavailableReason(detail: PullRequestDetail, method: String): String? {
    val capabilities = detail.capabilities
    val permissions = detail.viewerPermissions
    return when {
        detail.state == "merged" -> "This pull request is already merged."
        detail.state != "open" -> "Reopen this pull request before merging."
        detail.isDraft -> "Mark this pull request ready for review before merging."
        detail.mergeability == "conflicting" -> "Resolve the conflicts with the base branch first."
        detail.mergeability != "mergeable" -> "The host is still checking whether this can merge."
        "merge" !in capabilities.actions -> "This host does not allow merging from T3 Code."
        "merge" !in permissions.actions -> "You do not have permission to merge this pull request."
        method !in capabilities.mergeMethods -> "This merge method is not available."
        method == "squash" && !detail.mergeCapabilities.squash -> "Squash merging is unavailable."
        method == "rebase" && !detail.mergeCapabilities.rebase -> "Rebase merging is unavailable."
        method == "merge" && !detail.mergeCapabilities.merge -> "Merge commits are unavailable."
        else -> null
    }
}

data class PullRequestDiffFile(
    val path: String,
    val previousPath: String?,
    val additions: Int,
    val deletions: Int,
    val lines: List<PullRequestDiffLine>,
    val isBinary: Boolean,
)

data class PullRequestDiffLine(
    val kind: String,
    val text: String,
    val oldNumber: Int?,
    val newNumber: Int?,
)

private class DiffFileBuilder(var path: String) {
    var previousPath: String? = null
    var additions: Int = 0
    var deletions: Int = 0
    var isBinary: Boolean = false
    val lines = mutableListOf<PullRequestDiffLine>()
}

private fun parseUnifiedDiff(patch: String): List<PullRequestDiffFile> {
    if (patch.isBlank()) return emptyList()
    val files = mutableListOf<PullRequestDiffFile>()
    var builder: DiffFileBuilder? = null
    var oldLine = 0
    var newLine = 0

    fun flush() {
        builder?.let { current ->
            files += PullRequestDiffFile(
                path = current.path,
                previousPath = current.previousPath?.takeIf { it != current.path },
                additions = current.additions,
                deletions = current.deletions,
                lines = current.lines.toList(),
                isBinary = current.isBinary,
            )
        }
        builder = null
    }

    patch.lineSequence().forEach { raw ->
        val line = raw.removeSuffix("\r")
        when {
            line.startsWith("diff --git ") -> {
                flush()
                val match = Regex("""^diff --git a/(.*?) b/(.*)$""").find(line)
                val oldPath = match?.groupValues?.getOrNull(1).orEmpty()
                val newPath = match?.groupValues?.getOrNull(2).orEmpty()
                    .ifBlank { line.removePrefix("diff --git ").trim() }
                builder = DiffFileBuilder(newPath).also {
                    it.previousPath = oldPath.takeIf { path -> path.isNotBlank() && path != newPath }
                }
            }
            builder == null -> Unit
            line.startsWith("new file mode") ||
                line.startsWith("deleted file mode") ||
                line.startsWith("similarity index") ||
                line.startsWith("index ") ||
                line.startsWith("old mode") ||
                line.startsWith("new mode") -> Unit
            line.startsWith("rename from ") -> {
                builder?.previousPath = line.removePrefix("rename from ").trim()
            }
            line.startsWith("rename to ") -> {
                val path = line.removePrefix("rename to ").trim()
                if (path.isNotBlank()) builder?.let { current ->
                    builder = DiffFileBuilder(path).also { replacement ->
                        replacement.previousPath = current.previousPath
                        replacement.additions = current.additions
                        replacement.deletions = current.deletions
                        replacement.isBinary = current.isBinary
                        replacement.lines.addAll(current.lines)
                    }
                }
            }
            line.startsWith("Binary files ") || line.startsWith("GIT binary patch") -> {
                builder?.isBinary = true
            }
            line.startsWith("--- ") -> {
                val path = line.removePrefix("--- ").removePrefix("a/").trim()
                if (path != "/dev/null" && path.isNotBlank()) {
                    builder?.previousPath = path
                }
            }
            line.startsWith("+++ ") -> {
                val path = line.removePrefix("+++ ").removePrefix("b/").trim()
                if (path != "/dev/null" && path.isNotBlank()) {
                    builder?.let { it.path = path }
                }
            }
            line.startsWith("@@") -> {
                val match = Regex("""@@ -(\d+)(?:,\d+)? \+(\d+)(?:,\d+)? @@""").find(line)
                oldLine = match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                newLine = match?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0
                builder?.lines?.add(PullRequestDiffLine("hunk", line, null, null))
            }
            line.startsWith("+") -> {
                builder?.let { it.additions += 1 }
                builder?.lines?.add(PullRequestDiffLine("addition", line, null, newLine))
                newLine++
            }
            line.startsWith("-") -> {
                builder?.let { it.deletions += 1 }
                builder?.lines?.add(PullRequestDiffLine("deletion", line, oldLine, null))
                oldLine++
            }
            line.startsWith("\\") -> {
                builder?.lines?.add(PullRequestDiffLine("meta", line, null, null))
            }
            else -> {
                builder?.lines?.add(PullRequestDiffLine("context", line, oldLine, newLine))
                oldLine++
                newLine++
            }
        }
    }
    flush()
    return files
}

private const val MAX_DIFF_LINES = 600
