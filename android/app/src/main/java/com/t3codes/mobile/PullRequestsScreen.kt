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
import androidx.compose.material.icons.rounded.CallMerge
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.t3codes.core.PullRequestEntry

@Composable
fun PullRequestsScreen(state: UiState, viewModel: MainViewModel) {
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var projectFilter by rememberSaveable { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current
    val pullRequests = state.pullRequests
    val filtered = pullRequests.entries.filter { entry ->
        val matchesProject = projectFilter == null || entry.projectId == projectFilter
        val needle = query.trim()
        val matchesQuery = needle.isEmpty() ||
            entry.title.contains(needle, ignoreCase = true) ||
            entry.repository.contains(needle, ignoreCase = true) ||
            entry.author?.login?.contains(needle, ignoreCase = true) == true ||
            entry.number.toString() == needle
        matchesProject && matchesQuery
    }
    val projectsWithPullRequests = remember(pullRequests.entries, state.projects) {
        state.projects.filter { project ->
            pullRequests.entries.any { it.projectId == project.id }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Pull requests",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {
                if (searchOpen) query = ""
                searchOpen = !searchOpen
            }) {
                Icon(
                    if (searchOpen) Icons.Rounded.Close else Icons.Rounded.Search,
                    contentDescription = if (searchOpen) "Close search" else "Search pull requests",
                    modifier = Modifier.size(21.dp),
                )
            }
        }

        if (searchOpen) {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Search pull requests") },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val states = listOf(
                "open" to "Open",
                "all" to "All",
                "merged" to "Merged",
                "closed" to "Closed",
            )
            items(states) { (value, label) ->
                FilterChip(
                    selected = pullRequests.state == value,
                    onClick = { viewModel.setPullRequestsFilter(value) },
                    label = { Text(label) },
                    shape = CircleShape,
                )
            }
            if (projectsWithPullRequests.size > 1) {
                item {
                    FilterChip(
                        selected = projectFilter != null,
                        onClick = {
                            val index = projectsWithPullRequests.indexOfFirst {
                                it.id == projectFilter
                            }
                            projectFilter = if (index == -1) {
                                projectsWithPullRequests.first().id
                            } else if (index == projectsWithPullRequests.lastIndex) {
                                null
                            } else {
                                projectsWithPullRequests[index + 1].id
                            }
                        },
                        label = {
                            Text(
                                text = projectFilter?.let { id ->
                                    state.projects.firstOrNull { it.id == id }?.title
                                } ?: "Projects",
                                maxLines = 1,
                            )
                        },
                        shape = CircleShape,
                    )
                }
            }
        }

        when {
            pullRequests.loading && pullRequests.entries.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 2.5.dp,
                    )
                }
            }

            pullRequests.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                ) {
                    ErrorBanner(
                        message = pullRequests.error,
                        onRetry = { viewModel.loadPullRequests(force = true) },
                    )
                }
            }

            filtered.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 40.dp, vertical = 72.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            Icon(
                                Icons.Rounded.CallMerge,
                                contentDescription = null,
                                modifier = Modifier.padding(19.dp).size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(17.dp))
                        Text(
                            text = if (query.isNotBlank() || projectFilter != null) {
                                "No matching pull requests"
                            } else {
                                "No pull requests"
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Pull requests from your projects will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (pullRequests.errors.isNotEmpty()) {
                        item {
                            ErrorBanner(
                                message = pullRequests.errors.joinToString("\n") {
                                    "${it.projectTitle}: ${it.message}"
                                },
                            )
                        }
                    }
                    items(filtered, key = { "${it.projectId}-${it.number}" }) { entry ->
                        PullRequestRow(
                            entry = entry,
                            onOpen = { viewModel.openPullRequest(entry) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PullRequestRow(entry: PullRequestEntry, onOpen: () -> Unit) {
    val accent = when {
        entry.state == "merged" -> MaterialTheme.colorScheme.tertiary
        entry.isDraft -> MaterialTheme.colorScheme.onSurfaceVariant
        entry.state == "closed" -> MaterialTheme.colorScheme.error
        entry.checksState == "failing" -> MaterialTheme.colorScheme.error
        entry.mergeability == "conflicting" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val status = when {
        entry.isDraft -> "Draft"
        entry.state == "merged" -> "Merged"
        entry.state == "closed" -> "Closed"
        entry.checksState == "failing" -> "Checks failing"
        entry.checksState == "pending" -> "Checks running"
        entry.mergeability == "conflicting" -> "Conflicts"
        else -> "Open"
    }
    Surface(
        onClick = onOpen,
        enabled = entry.url.isNotBlank(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.CallMerge,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = accent,
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = entry.projectTitle.ifBlank { entry.repository },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = entry.title.ifBlank { "Pull request #${entry.number}" },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (entry.url.isNotBlank()) {
                    Spacer(Modifier.width(7.dp))
                    Icon(
                        Icons.Rounded.OpenInNew,
                        contentDescription = "Open on GitHub",
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    )
                }
            }
            Spacer(Modifier.height(7.dp))
            Text(
                text = buildString {
                    append("#")
                    append(entry.number)
                    if (entry.repository.isNotBlank()) {
                        append(" · ")
                        append(entry.repository)
                    }
                    entry.author?.login?.takeIf { it.isNotBlank() }?.let {
                        append(" · ")
                        append(it)
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.headBranch.isNotBlank() && entry.baseBranch.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${entry.headBranch} → ${entry.baseBranch}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (entry.mergeability == "conflicting" || entry.checksState == "failing") {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (entry.mergeability == "conflicting") {
                            "This branch has conflicts"
                        } else {
                            "Some checks are failing"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
