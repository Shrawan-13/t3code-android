package com.t3codes.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.t3codes.core.ModelSelection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewThreadSheet(state: UiState, viewModel: MainViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val choices = remember(state.config) { state.config?.modelChoices().orEmpty() }
    var projectId by remember {
        mutableStateOf(state.newThreadProjectId ?: state.projects.firstOrNull()?.id.orEmpty())
    }
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var modelKey by remember { mutableStateOf("") }

    val project = state.projects.firstOrNull { it.id == projectId }
    val selectedModel = choices.firstOrNull { it.key == modelKey }

    LaunchedEffect(projectId) {
        viewModel.setNewThreadProject(projectId)
        val preferred = project?.defaultModelSelection
        val match = choices.firstOrNull { choice ->
            choice.instanceId == preferred?.instanceId &&
                (choice.model.key == preferred.model ||
                    choice.model.id == preferred.model ||
                    choice.model.slug == preferred.model ||
                    choice.model.name == preferred.model)
        }
        modelKey = (match ?: choices.firstOrNull())?.key.orEmpty()
    }

    ModalBottomSheet(
        onDismissRequest = viewModel::closeNewThread,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(size = 38.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("New thread", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "Start an agent in one of your projects.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = viewModel::closeNewThread) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            SelectorField(
                label = "Project",
                value = project?.title?.ifBlank { project.workspaceRoot.substringAfterLast('/') }
                    ?: "Choose a project",
                enabled = state.projects.isNotEmpty() && !state.launching,
            ) { dismiss ->
                state.projects.forEach { candidate ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = candidate.title.ifBlank {
                                        candidate.workspaceRoot.substringAfterLast('/')
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = candidate.workspaceRoot,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        },
                        onClick = {
                            projectId = candidate.id
                            dismiss()
                        },
                    )
                }
            }

            SelectorField(
                label = "Model",
                value = selectedModel?.let { "${it.model.label} · ${it.providerName}" }
                    ?: "No models available",
                enabled = choices.isNotEmpty() && !state.launching,
                leading = {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
            ) { dismiss ->
                choices.forEach { choice ->
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
                        onClick = {
                            modelKey = choice.key
                            dismiss()
                        },
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title (optional)") },
                placeholder = { Text("Generated from your first message") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                enabled = !state.launching,
            )

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("First message (optional)") },
                placeholder = { Text("What should T3 work on?") },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(16.dp),
                enabled = !state.launching,
            )

            if (state.projects.isEmpty()) {
                ErrorBanner(message = "No projects are available on this server yet.")
            }

            Button(
                onClick = {
                    val choice = selectedModel ?: return@Button
                    viewModel.launchThread(
                        projectId = projectId,
                        title = title,
                        selection = ModelSelection(
                            instanceId = choice.instanceId,
                            model = choice.model.key,
                        ),
                        initialText = message,
                    )
                },
                enabled = !state.launching && projectId.isNotBlank() && selectedModel != null,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(17.dp),
            ) {
                if (state.launching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(19.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(9.dp))
                    Text("Starting…")
                } else {
                    Text("Start thread", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun SelectorField(
    label: String,
    value: String,
    enabled: Boolean,
    leading: (@Composable () -> Unit)? = null,
    content: @Composable (dismiss: () -> Unit) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { if (enabled) open = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leading != null) {
                    leading()
                    Spacer(Modifier.width(9.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    Icons.Rounded.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.fillMaxWidth(0.9f),
        ) {
            content { open = false }
        }
    }
}
