package com.t3codes.mobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun BrandMark(size: Dp = 56.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "T3",
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.34f).sp,
            letterSpacing = (-0.5).sp,
        )
    }
}

@Composable
fun ProjectAvatar(
    title: String,
    key: String,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(avatarColor(key).copy(alpha = 0.18f))
            .border(1.dp, avatarColor(key).copy(alpha = 0.35f), RoundedCornerShape(size * 0.32f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials(title),
            color = avatarColor(key),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.3f).sp,
        )
    }
}

@Composable
fun StatusDot(
    tone: StatusTone,
    modifier: Modifier = Modifier,
    pulsing: Boolean = false,
    size: Dp = 8.dp,
) {
    val color = when (tone) {
        StatusTone.Neutral -> MaterialTheme.colorScheme.outline
        StatusTone.Active -> MaterialTheme.colorScheme.primary
        StatusTone.Attention -> MaterialTheme.colorScheme.tertiary
        StatusTone.Error -> MaterialTheme.colorScheme.error
    }
    val alpha = if (pulsing) {
        val transition = rememberInfiniteTransition(label = "status-pulse")
        val value by transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "status-alpha",
        )
        value
    } else {
        1f
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha)),
    )
}

@Composable
fun StatusPill(info: StatusInfo, modifier: Modifier = Modifier) {
    val container = when (info.tone) {
        StatusTone.Neutral -> MaterialTheme.colorScheme.surfaceContainerHighest
        StatusTone.Active -> MaterialTheme.colorScheme.primaryContainer
        StatusTone.Attention -> MaterialTheme.colorScheme.tertiaryContainer
        StatusTone.Error -> MaterialTheme.colorScheme.errorContainer
    }
    val content = when (info.tone) {
        StatusTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
        StatusTone.Active -> MaterialTheme.colorScheme.onPrimaryContainer
        StatusTone.Attention -> MaterialTheme.colorScheme.onTertiaryContainer
        StatusTone.Error -> MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(shape = CircleShape, color = container, modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            StatusDot(info.tone, pulsing = info.tone == StatusTone.Active, size = 6.dp)
            Text(
                text = info.label,
                style = MaterialTheme.typography.labelSmall,
                color = content,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    icon: ImageVector? = null,
) {
    Surface(shape = CircleShape, color = container, modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp), tint = content)
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun BackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Rounded.ArrowBack,
            contentDescription = "Back",
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        actions()
    }
}

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.1.sp,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
fun GroupedCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(content = { content() })
    }
}

@Composable
fun InsetDivider(start: Dp = 72.dp) {
    HorizontalDivider(
        modifier = Modifier.padding(start = start),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
    )
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(18.dp).size(34.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(18.dp))
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            if (onRetry != null) {
                TextButton(onClick = onRetry) { Text("Retry") }
            }
            if (onDismiss != null) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Dismiss", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun ConnectionBanner(
    status: ConnectionStatus,
    environment: String,
    note: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (status == ConnectionStatus.Live) return
    val offline = status == ConnectionStatus.Offline
    val container = if (offline) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val content = if (offline) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    val title = when (status) {
        ConnectionStatus.Offline -> "Can't reach $environment"
        ConnectionStatus.Connecting -> "Connecting to $environment…"
        else -> "Reconnecting to $environment…"
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(start = 13.dp, top = 8.dp, bottom = 8.dp, end = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (offline) {
                Icon(
                    Icons.Rounded.CloudOff,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = content,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                )
                if (!note.isNullOrBlank() && note != title) {
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = content.copy(alpha = 0.78f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            TextButton(onClick = onRetry) {
                Text(if (offline) "Retry" else "Retry now", color = content)
            }
        }
    }
}

@Composable
fun WorkingRow(label: String = "Working…") {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(15.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// --------------------------------------------------------------------- markdown

private sealed interface MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock
    data class Heading(val text: String, val level: Int) : MarkdownBlock
    data class Bullets(val items: List<String>) : MarkdownBlock
    data class Code(val text: String, val language: String) : MarkdownBlock
}

private val bulletPattern = Regex("^\\d+\\. ")

private fun parseMarkdown(source: String): List<MarkdownBlock> {
    val lines = source.replace("\r\n", "\n").split('\n')
    val blocks = mutableListOf<MarkdownBlock>()
    var index = 0
    while (index < lines.size) {
        val line = lines[index]
        val trimmed = line.trimStart()
        when {
            line.isBlank() -> index++
            trimmed.startsWith("```") -> {
                val language = trimmed.removePrefix("```").trim()
                index++
                val code = StringBuilder()
                while (index < lines.size && !lines[index].trimStart().startsWith("```")) {
                    code.appendLine(lines[index])
                    index++
                }
                if (index < lines.size) index++
                blocks += MarkdownBlock.Code(code.toString().trimEnd(), language)
            }
            trimmed.startsWith("#") -> {
                val level = line.takeWhile { it == '#' }.length.coerceAtMost(6)
                blocks += MarkdownBlock.Heading(line.drop(level).trim(), level)
                index++
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") || bulletPattern.containsMatchIn(trimmed) -> {
                val items = mutableListOf<String>()
                while (index < lines.size) {
                    val candidate = lines[index].trimStart()
                    when {
                        candidate.startsWith("- ") -> {
                            items += candidate.drop(2)
                            index++
                        }
                        candidate.startsWith("* ") -> {
                            items += candidate.drop(2)
                            index++
                        }
                        bulletPattern.containsMatchIn(candidate) -> {
                            items += candidate.substringAfter(". ")
                            index++
                        }
                        else -> break
                    }
                }
                blocks += MarkdownBlock.Bullets(items)
            }
            else -> {
                val paragraph = mutableListOf<String>()
                while (index < lines.size) {
                    val candidate = lines[index]
                    val candidateTrimmed = candidate.trimStart()
                    if (candidate.isBlank() ||
                        candidateTrimmed.startsWith("```") ||
                        candidateTrimmed.startsWith("#") ||
                        candidateTrimmed.startsWith("- ") ||
                        candidateTrimmed.startsWith("* ") ||
                        bulletPattern.containsMatchIn(candidateTrimmed)
                    ) {
                        break
                    }
                    paragraph += candidate
                    index++
                }
                if (paragraph.isNotEmpty()) blocks += MarkdownBlock.Paragraph(paragraph.joinToString("\n"))
            }
        }
    }
    return blocks
}

private val inlinePattern = Regex(
    "\\*\\*([^*]+)\\*\\*|`([^`]+)`|\\*([^*]+)\\*|\\[([^\\]]+)]\\(([^)]+)\\)",
)

@Composable
private fun inlineMarkdown(text: String): AnnotatedString {
    val linkColor = MaterialTheme.colorScheme.primary
    val codeColor = MaterialTheme.colorScheme.surfaceContainerHighest
    return remember(text, linkColor, codeColor) {
        buildAnnotatedString {
            var last = 0
            inlinePattern.findAll(text).forEach { match ->
                append(text.substring(last, match.range.first))
                val start = length
                when {
                    match.groups[1] != null -> {
                        append(match.groupValues[1])
                        addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), start, length)
                    }
                    match.groups[2] != null -> {
                        append(match.groupValues[2])
                        addStyle(
                            SpanStyle(fontFamily = FontFamily.Monospace, background = codeColor),
                            start,
                            length,
                        )
                    }
                    match.groups[3] != null -> {
                        append(match.groupValues[3])
                        addStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), start, length)
                    }
                    match.groups[4] != null -> {
                        append(match.groupValues[4])
                        addStyle(
                            SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                            start,
                            length,
                        )
                    }
                }
                last = match.range.last + 1
            }
            append(text.substring(last))
        }
    }
}

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val blocks = remember(text) { parseMarkdown(text) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Paragraph -> SelectionContainer {
                    Text(
                        text = inlineMarkdown(block.text),
                        style = style,
                        color = color,
                    )
                }
                is MarkdownBlock.Heading -> Text(
                    text = inlineMarkdown(block.text),
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineSmall
                        2 -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.titleMedium
                    },
                    color = color,
                )
                is MarkdownBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    block.items.forEach { item ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("•", style = style, color = MaterialTheme.colorScheme.primary)
                            Text(inlineMarkdown(item), style = style, color = color)
                        }
                    }
                }
                is MarkdownBlock.Code -> Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp),
                    ) {
                        if (block.language.isNotBlank()) {
                            Text(
                                text = block.language.uppercase(Locale.getDefault()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(5.dp))
                        }
                        Text(
                            text = block.text,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = color,
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ timeline rows

@Composable
fun ThinkingCard(entry: TimelineEntry) {
    var expanded by rememberSaveable(entry.key) { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = if (entry.streaming) "Thinking…" else "Reasoning",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.size(18.dp).rotate(if (expanded) 180f else 0f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                text = entry.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun CommandCard(entry: TimelineEntry) {
    var expanded by rememberSaveable(entry.key) { mutableStateOf(false) }
    val failed = entry.exitCode != null && entry.exitCode != 0
    val statusColor = when {
        failed -> MaterialTheme.colorScheme.error
        entry.streaming -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Rounded.Terminal,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = entry.command.ifBlank { "Command" },
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = if (expanded) 40 else 5,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                when {
                    entry.streaming -> CircularProgressIndicator(
                        modifier = Modifier.size(13.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    failed -> Text(
                        "exit ${entry.exitCode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    entry.exitCode == 0 -> Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Succeeded",
                        modifier = Modifier.size(15.dp),
                        tint = statusColor,
                    )
                }
            }
            if (entry.output.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                Spacer(Modifier.height(9.dp))
                Text(
                    text = entry.output,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) 200 else 8,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry.output.length > 320) {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "Show less" else "Show more")
                    }
                }
            }
        }
    }
}

@Composable
fun ToolCard(entry: TimelineEntry) {
    var expanded by rememberSaveable(entry.key) { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.text,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (entry.secondary.isNotBlank()) {
                        Text(
                            text = entry.secondary,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (entry.streaming) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(13.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (entry.command.isNotBlank() && expanded) {
                Spacer(Modifier.height(7.dp))
                Text(
                    text = entry.command,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 20,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (entry.output.isNotBlank() && expanded) {
                Spacer(Modifier.height(7.dp))
                Text(
                    text = entry.output,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 20,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun FileChangeCard(entry: TimelineEntry) {
    val path = entry.text
    val fileName = path.substringAfterLast('/').ifBlank { path }
    val directory = path.substringBeforeLast('/', "").removePrefix("/")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName.ifBlank { "File changed" },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (directory.isNotBlank()) {
                    Text(
                        text = directory,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (entry.status.isNotBlank()) {
                Pill(entry.status)
            }
        }
    }
}

@Composable
fun NoticeRow(entry: TimelineEntry) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = entry.text,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun CheckpointRow(entry: TimelineEntry) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Bookmark,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = entry.text.ifBlank { "Checkpoint" },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

// ------------------------------------------------------------- compact work log
//
// The official client treats tool calls as quiet one-line activity. Only the
// summary is visible by default; details live one tap deeper. Groups keep long
// runs of work from pushing the conversation off screen.

@Composable
fun WorkGroupCard(entries: List<TimelineEntry>) {
    val firstKey = entries.firstOrNull()?.key ?: "work"
    var expanded by rememberSaveable(firstKey) { mutableStateOf(false) }
    val streaming = entries.any { it.streaming }
    val failed = entries.any(::workEntryFailed)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.62f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WorkGroupIcon(entries)
            Spacer(Modifier.width(9.dp))
            Text(
                text = workGroupSummary(entries),
                style = MaterialTheme.typography.labelLarge,
                color = if (failed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (streaming) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.8.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(6.dp))
            }
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Collapse work" else "Expand work",
                modifier = Modifier.size(17.dp).rotate(if (expanded) 180f else 0f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 5.dp)) {
                entries.forEach { entry ->
                    WorkEntryRow(entry = entry, nested = true)
                }
            }
        }
    }
}

@Composable
fun WorkEntryRow(entry: TimelineEntry, nested: Boolean = false) {
    var expanded by rememberSaveable(entry.key) { mutableStateOf(false) }
    val failed = workEntryFailed(entry)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { expanded = !expanded }
                .padding(
                    start = if (nested) 8.dp else 5.dp,
                    end = 5.dp,
                    top = 7.dp,
                    bottom = 7.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WorkEntryIcon(entry)
            Spacer(Modifier.width(8.dp))
            Text(
                text = workEntryLabel(entry),
                style = MaterialTheme.typography.labelLarge,
                fontFamily = if (entry.kind == TimelineKind.Command) {
                    FontFamily.Monospace
                } else {
                    FontFamily.Default
                },
                color = if (failed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(7.dp))
            when {
                entry.streaming -> CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.8.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                failed && entry.exitCode != null -> Text(
                    text = "exit ${entry.exitCode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
                failed -> Text(
                    text = "failed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Collapse details" else "Show details",
                modifier = Modifier.size(15.dp).rotate(if (expanded) 180f else 0f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            )
        }
        AnimatedVisibility(visible = expanded) {
            WorkEntryDetails(entry)
        }
    }
}

@Composable
private fun WorkGroupIcon(entries: List<TimelineEntry>) {
    val actions = entries.map(::workAction).toSet()
    val icon = when {
        actions.size == 1 && actions.first() == WorkAction.Command -> Icons.Rounded.Terminal
        actions.size == 1 && actions.first() == WorkAction.Edit -> Icons.Rounded.Description
        actions.size == 1 && actions.first() == WorkAction.Thought -> Icons.Rounded.Psychology
        actions.size == 1 && actions.first() == WorkAction.Search -> Icons.Rounded.Search
        actions.size == 1 && actions.first() == WorkAction.Browse -> Icons.Rounded.Language
        actions.size == 1 && actions.first() == WorkAction.Read -> Icons.Rounded.Description
        else -> Icons.Rounded.AutoAwesome
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(15.dp),
        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
    )
}

@Composable
private fun WorkEntryIcon(entry: TimelineEntry) {
    val icon = when (workAction(entry)) {
        WorkAction.Thought -> Icons.Rounded.Psychology
        WorkAction.Command -> Icons.Rounded.Terminal
        WorkAction.Edit -> Icons.Rounded.Description
        WorkAction.Read -> Icons.Rounded.Description
        WorkAction.Search -> Icons.Rounded.Search
        WorkAction.Browse -> Icons.Rounded.Language
        WorkAction.Tool -> Icons.Rounded.Build
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(15.dp),
        tint = when (workAction(entry)) {
            WorkAction.Thought -> MaterialTheme.colorScheme.primary
            WorkAction.Command -> MaterialTheme.colorScheme.onSurfaceVariant
            WorkAction.Edit -> MaterialTheme.colorScheme.tertiary
            WorkAction.Read -> MaterialTheme.colorScheme.onSurfaceVariant
            WorkAction.Search -> MaterialTheme.colorScheme.secondary
            WorkAction.Browse -> MaterialTheme.colorScheme.secondary
            WorkAction.Tool -> MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

@Composable
private fun WorkEntryDetails(entry: TimelineEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 31.dp, end = 8.dp, bottom = 7.dp),
    ) {
        when (entry.kind) {
            TimelineKind.Reasoning -> {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
                ) {
                    SelectionContainer {
                        Text(
                            text = entry.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(11.dp),
                        )
                    }
                }
            }

            TimelineKind.Command -> CodeDetails(entry.command, entry.output)

            TimelineKind.Tool -> {
                if (entry.command.isNotBlank() || entry.output.isNotBlank()) {
                    CodeDetails(entry.command, entry.output)
                } else if (entry.secondary.isNotBlank()) {
                    Text(
                        text = entry.secondary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            TimelineKind.FileChange -> {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
                ) {
                    Column(modifier = Modifier.padding(11.dp)) {
                        Text(
                            text = entry.text.ifBlank { "File changed" },
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (entry.secondary.isNotBlank()) {
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = entry.secondary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            else -> {
                if (entry.output.isNotBlank()) CodeDetails("", entry.output)
            }
        }
    }
}

@Composable
private fun CodeDetails(command: String, output: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
    ) {
        Column(modifier = Modifier.padding(11.dp)) {
            if (command.isNotBlank()) {
                SelectionContainer {
                    Text(
                        text = command,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (command.isNotBlank() && output.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                )
                Spacer(Modifier.height(8.dp))
            }
            if (output.isNotBlank()) {
                SelectionContainer {
                    Text(
                        text = output,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 200,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun workEntryFailed(entry: TimelineEntry): Boolean =
    (entry.exitCode != null && entry.exitCode != 0) ||
        entry.status.equals("failed", ignoreCase = true) ||
        entry.status.equals("error", ignoreCase = true)
