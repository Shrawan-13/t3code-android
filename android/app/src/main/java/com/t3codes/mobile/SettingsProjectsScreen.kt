package com.t3codes.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.t3codes.core.Project

@Composable
fun SettingsProjectsScreen(state: UiState, viewModel: MainViewModel) {
    var renaming by remember { mutableStateOf<Project?>(null) }
    var deleting by remember { mutableStateOf<Project?>(null) }
    var draftTitle by remember { mutableStateOf("") }

    SettingsScaffold(
        title = "Projects",
        onBack = viewModel::closeSettingsPage,
    ) {
        if (state.projects.isEmpty()) {
            SettingsSection {
                Text(
                    text = "No projects on this server.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            state.projects.forEach { project ->
                SettingsSection(title = project.title.ifBlank { project.workspaceRoot.substringAfterLast('/') }) {
                    Column {
                        ProjectActionRow(
                            icon = Icons.Rounded.Edit,
                            title = "Rename",
                            onClick = {
                                draftTitle = project.title
                                renaming = project
                            },
                        )
                        InsetDivider(start = 46.dp)
                        ProjectActionRow(
                            icon = Icons.Rounded.PushPin,
                            title = if (project.pinned) "Unpin" else "Pin",
                            onClick = { viewModel.setProjectPinned(project.id, !project.pinned) },
                        )
                        InsetDivider(start = 46.dp)
                        ProjectActionRow(
                            icon = if (project.archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                            title = if (project.archived) "Unarchive" else "Archive",
                            onClick = { viewModel.setProjectArchived(project.id, !project.archived) },
                        )
                        InsetDivider(start = 46.dp)
                        ProjectActionRow(
                            icon = Icons.Rounded.Delete,
                            title = "Remove project",
                            danger = true,
                            onClick = { deleting = project },
                        )
                    }
                }
            }
        }
    }

    renaming?.let { project ->
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Rename project") },
            text = {
                OutlinedTextField(
                    value = draftTitle,
                    onValueChange = { draftTitle = it },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameProject(project.id, draftTitle)
                        renaming = null
                    },
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renaming = null }) { Text("Cancel") }
            },
        )
    }

    deleting?.let { project ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Remove project?") },
            text = { Text("The workspace is removed from T3. Files on disk are not deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeProject(project.id)
                        deleting = null
                    },
                ) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ProjectActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(onClick = onClick) {
            Text(if (danger) "Remove" else "Change")
        }
    }
}
