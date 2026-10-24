package com.t3codes.mobile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Difference
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.t3codes.core.DiffPreviewSource
import com.t3codes.core.DiffFileStat

private val AdditionText = Color(0xFF4FB585)
private val DeletionText = Color(0xFFE06C75)
private val AdditionBackground = Color(0x1F4FB585)
private val DeletionBackground = Color(0x1FE06C75)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiffScreen(state: UiState, viewModel: MainViewModel) {
    val diff = state.diff ?: return
    val sources = diff.preview?.sources.orEmpty()
    val selected = sources.getOrNull(diff.sourceIndex.coerceIn(0, (sources.size - 1).coerceAtLeast(0)))
    val cwdLabel = diff.cwd.substringAfterLast('/').ifBlank { diff.cwd }
    val lines = remember(selected?.diffHash, selected?.diff) {
        selected?.diff?.split('\n').orEmpty()
    }
    val horizontal = rememberScrollState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    IconButton(onClick = viewModel::closeDiff) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text("Changes", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = cwdLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.openDiff(diff.threadId) }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh changes")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                diff.loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                diff.error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        ErrorBanner(
                            message = diff.error,
                            onRetry = { viewModel.openDiff(diff.threadId) },
                        )
                    }
                }
                selected == null || (selected.diff.isBlank() && selected.files.isEmpty()) -> {
                    EmptyState(
                        icon = Icons.Rounded.CheckCircle,
                        title = "Working tree clean",
                        message = "There are no changes to review in this workspace.",
                    )
                }
                else -> {
                    if (sources.size > 1) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(sources.size) { index ->
                                FilterChip(
                                    selected = index == diff.sourceIndex,
                                    onClick = { viewModel.selectDiffSource(index) },
                                    label = {
                                        Text(
                                            text = sources[index].title.ifBlank { "Changes ${index + 1}" },
                                            maxLines = 1,
                                        )
                                    },
                                    shape = CircleShape,
                                )
                            }
                        }
                    } else {
                        SourceSummary(selected)
                    }
                    if (selected.truncated) {
                        Text(
                            text = "This diff is truncated. Open the full review in T3.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    if (selected.files.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            items(selected.files) { file ->
                                FileStatChip(file)
                            }
                        }
                    }
                    if (lines.isEmpty()) {
                        EmptyState(
                            icon = Icons.Rounded.Difference,
                            title = "No textual diff",
                            message = "The selected change has no diff content to display.",
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 30.dp),
                        ) {
                            items(lines.size) { index ->
                                DiffLine(lines[index], horizontal)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceSummary(source: DiffPreviewSource) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
            Text(
                text = source.title.ifBlank { "Working tree" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${source.files.size} ${if (source.files.size == 1) "file" else "files"}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "+${source.files.sumOf { it.additions }}  −${source.files.sumOf { it.deletions }}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FileStatChip(file: DiffFileStat) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = file.path.substringAfterLast('/'),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp),
            )
            if (file.additions > 0) {
                Text("+${file.additions}", style = MaterialTheme.typography.labelSmall, color = AdditionText)
            }
            if (file.deletions > 0) {
                Text("−${file.deletions}", style = MaterialTheme.typography.labelSmall, color = DeletionText)
            }
        }
    }
}

@Composable
private fun DiffLine(line: String, horizontal: androidx.compose.foundation.ScrollState) {
    val background = when {
        line.startsWith("diff --git") || line.startsWith("index ") -> MaterialTheme.colorScheme.surfaceContainerLow
        line.startsWith("+++") || line.startsWith("---") -> MaterialTheme.colorScheme.surfaceContainerLow
        line.startsWith("@@") -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
        line.startsWith("+") -> AdditionBackground
        line.startsWith("-") -> DeletionBackground
        else -> Color.Transparent
    }
    val textColor = when {
        line.startsWith("diff --git") || line.startsWith("index ") -> MaterialTheme.colorScheme.onSurfaceVariant
        line.startsWith("+++") || line.startsWith("---") -> MaterialTheme.colorScheme.onSurfaceVariant
        line.startsWith("@@") -> MaterialTheme.colorScheme.secondary
        line.startsWith("+") -> AdditionText
        line.startsWith("-") -> DeletionText
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .horizontalScroll(horizontal)
            .padding(horizontal = 12.dp, vertical = 1.dp),
    ) {
        Text(
            text = line.ifEmpty { " " },
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            ),
            color = textColor,
            softWrap = false,
        )
    }
}
