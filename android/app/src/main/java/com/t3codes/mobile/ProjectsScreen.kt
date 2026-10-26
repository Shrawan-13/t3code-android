package com.t3codes.mobile

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.t3codes.core.Project
import com.t3codes.core.ProjectCloneSnapshot

@Composable
fun ProjectsScreen(state: UiState, viewModel: MainViewModel) {
    val subtitle = when {
        state.projects.isEmpty() -> "No workspaces yet"
        state.projects.size == 1 -> "1 workspace"
        else -> "${state.projects.size} workspaces"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Projects",
                subtitle = subtitle,
                leading = {
                    BackButton { viewModel.selectTab(AppTab.Threads) }
                },
                actions = {
                    IconButton(onClick = { viewModel.openAddProject() }) {
                        Icon(
                            Icons.Rounded.CreateNewFolder,
                            contentDescription = "Add project",
                        )
                    }
                    IconButton(onClick = { viewModel.openNewThread() }) {
                        Icon(Icons.Rounded.Add, contentDescription = "New thread")
                    }
                },
            )
        }

        val activeClones = state.projectClones.filter {
            it.phase == "running" || it.phase == "failed"
        }
        if (activeClones.isNotEmpty()) {
            item { SectionLabel("Cloning") }
            items(activeClones, key = { "clone-${it.projectId}" }) { clone ->
                ProjectCloneCard(
                    clone = clone,
                    title = state.projects.firstOrNull { it.id == clone.projectId }?.title
                        ?.takeIf { it.isNotBlank() }
                        ?: clone.remoteUrl.substringAfterLast('/').removeSuffix(".git")
                            .ifBlank { "Repository" },
                    onStop = { viewModel.cancelProjectClone(clone.projectId) },
                    onRetry = { viewModel.retryProjectClone(clone.projectId) },
                )
            }
        }

        if (state.projects.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.FolderOpen,
                    title = "No projects",
                    message = "Add a local folder, create a new Git repository, or clone one from GitHub.",
                    actionLabel = "Add project",
                    onAction = viewModel::openAddProject,
                )
            }
        } else {
            item { SectionLabel("Workspaces") }
            items(state.projects, key = { it.id }) { project ->
                ProjectCard(
                    project = project,
                    threads = state.threads.filter { it.projectId == project.id },
                    onOpen = {
                        viewModel.setProjectFilter(project.id)
                        viewModel.selectTab(AppTab.Threads)
                    },
                    onNewThread = { viewModel.openNewThread(project.id) },
                )
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    threads: List<com.t3codes.core.ThreadShell>,
    onOpen: () -> Unit,
    onNewThread: () -> Unit,
) {
    val running = threads.count { it.activeRunId != null || it.status == "running" }
    val title = project.title.ifBlank { project.workspaceRoot.substringAfterLast('/') }
    val defaultModel = modelName(project.defaultModelSelection)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProjectAvatar(title = title, key = project.id, size = 46.dp)
                Spacer(Modifier.width(13.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = project.workspaceRoot,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onNewThread) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "New thread in $title",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Pill(
                    text = "${threads.size} ${if (threads.size == 1) "thread" else "threads"}",
                    icon = Icons.Rounded.Forum,
                )
                if (running > 0) {
                    Pill(
                        text = "$running working",
                        container = MaterialTheme.colorScheme.primaryContainer,
                        content = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = defaultModel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    Icons.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProjectCloneCard(
    clone: ProjectCloneSnapshot,
    title: String,
    onStop: () -> Unit,
    onRetry: () -> Unit,
) {
    val failed = clone.phase == "failed"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = if (failed) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        contentColor = if (failed) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (failed) "Could not clone $title" else "Cloning $title",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = clone.remoteUrl,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = if (failed) {
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!failed) {
                    TextButton(onClick = onStop) { Text("Stop") }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (!failed) {
                val percent = clone.percent
                if (percent == null) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(
                        progress = (percent.coerceIn(0, 100) / 100f),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = listOfNotNull(
                        clone.stage.replaceFirstChar { it.uppercase() }.takeIf { it.isNotBlank() },
                        clone.detail,
                        clone.percent?.let { "$it%" },
                    ).joinToString(" · ").ifBlank { "Downloading repository…" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    text = clone.error ?: clone.detail ?: "The clone did not finish.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onRetry) { Text("Retry") }
                    TextButton(onClick = onStop) { Text("Cancel") }
                }
            }
        }
    }
}
