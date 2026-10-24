package com.t3codes.mobile

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.t3codes.core.ProjectEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(state: UiState, viewModel: MainViewModel) {
    val files = state.files ?: return
    val file = files.file

    if (file != null) {
        FileViewer(files, file, viewModel)
        return
    }

    val entries = remember(files.entries) {
        files.entries.sortedWith(
            compareByDescending<ProjectEntry> { it.kind == "directory" }
                .thenBy { it.path.lowercase() },
        )
    }
    val cwdLabel = files.cwd.substringAfterLast('/').ifBlank { files.cwd }
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    IconButton(onClick = viewModel::closeFiles) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text("Files", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (files.directory.isBlank()) cwdLabel else files.directory,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            clipboard.setPrimaryClip(ClipData.newPlainText("Path", files.cwd))
                            Toast.makeText(context, "Workspace path copied", Toast.LENGTH_SHORT).show()
                        },
                    ) {
                        Icon(
                            Icons.Rounded.ContentCopy,
                            contentDescription = "Copy workspace path",
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    IconButton(onClick = viewModel::refreshFiles) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (files.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Breadcrumbs(files.directory, viewModel::navigateFiles)
            if (files.error != null) {
                ErrorBanner(
                    message = files.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    onRetry = viewModel::refreshFiles,
                )
            }
            when {
                files.loading && entries.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                entries.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Rounded.FolderOpen,
                        title = "Empty folder",
                        message = "There are no files to show here.",
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        if (files.directory.isNotBlank()) {
                            item {
                                EntryRow(
                                    icon = Icons.Rounded.ArrowUpward,
                                    name = "Parent folder",
                                    detail = files.directory.substringBeforeLast('/', "").ifBlank { "root" },
                                    isDirectory = true,
                                    onClick = {
                                        viewModel.navigateFiles(files.directory.substringBeforeLast('/', ""))
                                    },
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 52.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                )
                            }
                        }
                        itemsIndexed(entries, key = { _, entry -> entry.path }) { index, entry ->
                            val name = entry.path.substringAfterLast('/').ifBlank { entry.path }
                            EntryRow(
                                icon = if (entry.kind == "directory") Icons.Rounded.Folder
                                else Icons.Rounded.InsertDriveFile,
                                name = name,
                                detail = if (entry.ignored) "ignored" else null,
                                isDirectory = entry.kind == "directory",
                                onClick = {
                                    if (entry.kind == "directory") {
                                        viewModel.navigateFiles(entry.path)
                                    } else {
                                        viewModel.openFile(entry.path)
                                    }
                                },
                            )
                            if (index < entries.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 52.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                )
                            }
                        }
                        if (files.truncated) {
                            item {
                                Text(
                                    text = "This folder has more entries than can be shown.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Breadcrumbs(directory: String, onNavigate: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = { onNavigate("") }) {
            Text("root", style = MaterialTheme.typography.labelMedium)
        }
        var prefix = ""
        directory.split('/').filter { it.isNotBlank() }.forEach { segment ->
            prefix = if (prefix.isBlank()) segment else "$prefix/$segment"
            val target = prefix
            Icon(
                Icons.Rounded.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { onNavigate(target) }) {
                Text(segment, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun EntryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    name: String,
    detail: String?,
    isDirectory: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = if (isDirectory) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!detail.isNullOrBlank()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (isDirectory) {
            Icon(
                Icons.Rounded.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileViewer(
    files: FilesUi,
    file: FileView,
    viewModel: MainViewModel,
) {
    val context = LocalContext.current
    val lines = remember(file.relativePath, file.contents) {
        if (file.contents.isEmpty()) emptyList() else file.contents.split('\n')
    }
    val fileName = file.relativePath.substringAfterLast('/').ifBlank { file.relativePath }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    IconButton(onClick = viewModel::closeFile) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back to files")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = file.relativePath,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText(file.relativePath, file.contents),
                            )
                            Toast.makeText(context, "File copied", Toast.LENGTH_SHORT).show()
                        },
                    ) {
                        Icon(
                            Icons.Rounded.ContentCopy,
                            contentDescription = "Copy file",
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    IconButton(onClick = viewModel::refreshFiles) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh file")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (files.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (files.error != null) {
                ErrorBanner(
                    message = files.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    onRetry = viewModel::refreshFiles,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${lines.size} lines · ${file.byteLength} bytes" +
                        if (file.truncated) " · truncated" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        text = "read-only",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
            if (lines.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.InsertDriveFile,
                    title = "Empty file",
                    message = "This file has no contents.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLowest),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 6.dp),
                ) {
                    itemsIndexed(lines) { index, line ->
                        SelectionContainer {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "${index + 1}",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 18.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.width(42.dp).padding(end = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                )
                                Text(
                                    text = line.ifEmpty { " " },
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
