package com.t3codes.mobile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.t3codes.core.ThreadShell

/** Settings entry point for the archived thread list. */
@Composable
fun SettingsArchiveScreen(state: UiState, viewModel: MainViewModel) {
    ArchiveScreen(state, viewModel, onBack = viewModel::closeSettingsPage)
}

/** Archived threads are reached from home as well as from Settings. */
@Composable
fun ArchiveScreen(state: UiState, viewModel: MainViewModel, onBack: () -> Unit) {
    val archived = state.archivedThreads
        .filter { it.isListedThread }
        .sortedByDescending { it.archivedAt ?: it.updatedAt }
    SettingsScaffold(
        title = "Archive",
        onBack = onBack,
    ) {
        if (archived.isEmpty()) {
            SettingsSection {
                Text(
                    text = "No archived threads.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            archived.forEach { thread ->
                SettingsSection {
                    ArchivedThreadRow(
                        thread = thread,
                        state = state,
                        onOpen = {
                            viewModel.closeArchive()
                            viewModel.openThread(thread.id)
                        },
                        onUnarchive = { viewModel.threadAction(thread.id, "unarchive") },
                        onDelete = { viewModel.threadAction(thread.id, "delete") },
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchivedThreadRow(
    thread: ThreadShell,
    state: UiState,
    onOpen: () -> Unit,
    onUnarchive: () -> Unit,
    onDelete: () -> Unit,
) {
    val project = state.projects.firstOrNull { it.id == thread.projectId }
    val projectTitle = project?.title?.ifBlank { null }
        ?: project?.workspaceRoot?.substringAfterLast('/')?.ifBlank { null }
        ?: "Archived"
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Archive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = thread.title.ifBlank { "Untitled thread" },
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "$projectTitle · ${relativeTime(thread.archivedAt ?: thread.updatedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onUnarchive) {
                Icon(Icons.Rounded.Unarchive, contentDescription = null)
                Text("Restore")
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
