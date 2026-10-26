package com.t3codes.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import kotlinx.coroutines.delay
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.ins.Ins
import org.commonmark.ext.ins.InsExtension
import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.HtmlInline
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListBlock
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text as MarkdownTextNode
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser

private val markdownParser: Parser = Parser.builder()
    .extensions(
        listOf(
            TablesExtension.create(),
            StrikethroughExtension.create(),
            AutolinkExtension.create(),
            TaskListItemsExtension.create(),
            InsExtension.create(),
        ),
    )
    .build()

/**
 * CommonMark + GFM renderer for modal transcripts and pull request descriptions.
 *
 * Supported: ATX/setext headings, emphasis, strong emphasis, strikethrough, inserted
 * text, inline code, fenced/indented code, block quotes, nested ordered/unordered
 * lists, task lists, links, autolinks, images, thematic breaks, tables, HTML, and
 * common GitHub-style chat markdown.
 */
@Composable
fun RichMarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val document = remember(text) { markdownParser.parse(text) }
    SelectionContainer {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            document.nodes().forEach { node ->
                MarkdownBlockNode(node = node, style = style, color = color)
            }
        }
    }
}

@Composable
private fun MarkdownBlockNode(
    node: Node,
    style: TextStyle,
    color: Color,
) {
    when (node) {
        is Paragraph -> MarkdownParagraph(node, style, color)
        is Heading -> MarkdownHeading(node, style, color)
        is BulletList -> MarkdownList(node, style, color)
        is OrderedList -> MarkdownList(node, style, color)
        is BlockQuote -> MarkdownBlockQuote(node, style, color)
        is FencedCodeBlock -> MarkdownCodeBlock(
            code = node.literal.orEmpty(),
            language = node.info.orEmpty().substringBefore(' ').trim(),
            color = color,
        )
        is IndentedCodeBlock -> MarkdownCodeBlock(
            code = node.literal.orEmpty(),
            language = "",
            color = color,
        )
        is TableBlock -> MarkdownTable(node, style, color)
        is ThematicBreak -> HorizontalDivider(
            modifier = Modifier.padding(vertical = 2.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        is HtmlBlock -> MarkdownHtmlBlock(node, style, color)
        is ListItem -> MarkdownChildren(node, style, color)
        else -> {
            if (node.firstChild != null) {
                MarkdownChildren(node, style, color)
            }
        }
    }
}

@Composable
private fun MarkdownChildren(node: Node, style: TextStyle, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        node.nodes().forEach { child ->
            MarkdownBlockNode(child, style, color)
        }
    }
}

@Composable
private fun MarkdownParagraph(node: Paragraph, style: TextStyle, color: Color) {
    val images = remember(node) { node.imageNodes() }
    val annotated = markdownInline(node)
    if (annotated.isNotBlank()) {
        Text(
            text = annotated,
            style = style,
            color = color,
        )
    }
    images.forEach { image ->
        MarkdownImage(image)
    }
}

@Composable
private fun MarkdownHeading(node: Heading, style: TextStyle, color: Color) {
    val headingStyle = when (node.level) {
        1 -> MaterialTheme.typography.headlineSmall
        2 -> MaterialTheme.typography.titleLarge
        3 -> MaterialTheme.typography.titleMedium
        4 -> MaterialTheme.typography.titleSmall
        else -> MaterialTheme.typography.labelLarge
    }
    Text(
        text = markdownInline(node),
        style = headingStyle.merge(TextStyle(fontWeight = FontWeight.SemiBold)),
        color = color,
    )
}

@Composable
private fun MarkdownList(node: ListBlock, style: TextStyle, color: Color) {
    val items = node.nodes().filterIsInstance<ListItem>()
    val ordered = node is OrderedList
    val start = (node as? OrderedList)?.startNumber ?: 1
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (node.isTight) 3.dp else 7.dp),
    ) {
        items.forEachIndexed { index, item ->
            val marker = item.taskMarker()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when {
                    marker != null -> Icon(
                        imageVector = if (marker.isChecked) {
                            Icons.Rounded.CheckBox
                        } else {
                            Icons.Rounded.CheckBoxOutlineBlank
                        },
                        contentDescription = if (marker.isChecked) "Completed" else "Not completed",
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(18.dp),
                        tint = if (marker.isChecked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    else -> Text(
                        text = if (ordered) "${start + index}." else "•",
                        modifier = Modifier.widthIn(min = 18.dp),
                        style = style,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = if (ordered) TextAlign.End else TextAlign.Start,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item.nodes()
                        .filterNot { it is TaskListItemMarker }
                        .forEach { child -> MarkdownBlockNode(child, style, color) }
                }
            }
        }
    }
}

@Composable
private fun MarkdownBlockQuote(node: BlockQuote, style: TextStyle, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            node.nodes().forEach { child ->
                MarkdownBlockNode(child, style, color)
            }
        }
    }
}

@Composable
private fun MarkdownCodeBlock(code: String, language: String, color: Color) {
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }
    val clipboard = LocalClipboardManager.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = language.ifBlank { "Code" }.uppercase(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(code))
                        copied = true
                    },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (copied) "Copied" else "Copy",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                Text(
                    text = code,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun MarkdownTable(node: TableBlock, style: TextStyle, color: Color) {
    val rows = node.nodes().filterIsInstance<TableHead>().flatMap { it.nodes() }
        .filterIsInstance<TableRow>() +
        node.nodes().filterIsInstance<org.commonmark.ext.gfm.tables.TableBody>().flatMap { it.nodes() }
            .filterIsInstance<TableRow>()
    if (rows.isEmpty()) return
    val outline = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        Column {
            rows.forEach { row ->
                Row {
                    row.nodes().filterIsInstance<TableCell>().forEach { cell ->
                        val header = cell.isHeader
                        Box(
                            modifier = Modifier
                                .width(168.dp)
                                .border(0.5.dp, outline)
                                .background(
                                    if (header) {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    } else {
                                        Color.Transparent
                                    },
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = markdownInline(cell),
                                style = if (header) {
                                    style.copy(fontWeight = FontWeight.SemiBold)
                                } else {
                                    style
                                },
                                color = color,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownHtmlBlock(node: HtmlBlock, style: TextStyle, color: Color) {
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = remember(node.literal, linkColor) {
        htmlToAnnotated(node.literal, linkColor)
    }
    if (annotated.isNotBlank()) {
        Text(text = annotated, style = style, color = color)
    }
}

@Composable
private fun MarkdownImage(node: Image) {
    val url = node.destination.trim()
    if (!isRemoteImageUrl(url)) {
        Text(
            text = "[Image: ${imageAlt(node).ifBlank { "image" }}]",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val alt = imageAlt(node)
    val uriHandler = LocalUriHandler.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { uriHandler.openUri(url) },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = alt.ifBlank { "Markdown image" },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp, max = 420.dp)
                .wrapContentHeight(),
            contentScale = ContentScale.Fit,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                    )
                }
            },
            success = {
                SubcomposeAsyncImageContent()
            },
            error = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Image,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = alt.ifBlank { "Image unavailable" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
    }
}

@Composable
private fun markdownInline(node: Node?): AnnotatedString {
    if (node == null) return AnnotatedString("")
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBackground = MaterialTheme.colorScheme.surfaceContainerHighest
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    return remember(node, linkColor, codeBackground, mutedColor) {
        buildAnnotatedString {
            appendInline(node, linkColor, codeBackground, mutedColor)
        }
    }
}

private fun AnnotatedString.Builder.appendInline(
    parent: Node,
    linkColor: Color,
    codeBackground: Color,
    mutedColor: Color,
) {
    parent.nodes().forEach { node ->
        when (node) {
            is MarkdownTextNode -> append(node.literal)
            is SoftLineBreak -> append('\n')
            is HardLineBreak -> append('\n')
            is Emphasis -> {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                appendInline(node, linkColor, codeBackground, mutedColor)
                pop()
            }
            is StrongEmphasis -> {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                appendInline(node, linkColor, codeBackground, mutedColor)
                pop()
            }
            is Strikethrough -> {
                pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                appendInline(node, linkColor, codeBackground, mutedColor)
                pop()
            }
            is Ins -> {
                pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                appendInline(node, linkColor, codeBackground, mutedColor)
                pop()
            }
            is Code -> {
                pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBackground,
                    ),
                )
                append(node.literal)
                pop()
            }
            is Link -> {
                val start = length
                appendInline(node, linkColor, codeBackground, mutedColor)
                val destination = clickableLink(node.destination)
                if (destination != null && length > start) {
                    addLink(
                        LinkAnnotation.Url(
                            url = destination,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline,
                                ),
                            ),
                        ),
                        start = start,
                        end = length,
                    )
                }
            }
            is Image -> {
                val destination = node.destination.trim()
                val alt = imageAlt(node)
                if (!isRemoteImageUrl(destination)) {
                    pushStyle(SpanStyle(color = mutedColor))
                    append("[Image: ${alt.ifBlank { "image" }}]")
                    pop()
                }
            }
            is HtmlInline -> append(htmlToAnnotated(node.literal, linkColor))
            is TaskListItemMarker -> Unit
            else -> appendInline(node, linkColor, codeBackground, mutedColor)
        }
    }
}

private fun Node.nodes(): List<Node> {
    val result = ArrayList<Node>()
    var child = firstChild
    while (child != null) {
        result += child
        child = child.next
    }
    return result
}

private fun Node.imageNodes(): List<Image> {
    val result = ArrayList<Image>()
    nodes().forEach { child ->
        if (child is Image) {
            result += child
        } else {
            result += child.imageNodes()
        }
    }
    return result
}

private fun ListItem.taskMarker(): TaskListItemMarker? =
    nodes().filterIsInstance<TaskListItemMarker>().firstOrNull()

private fun imageAlt(node: Image): String {
    val builder = StringBuilder()
    node.nodes().forEach { child ->
        when (child) {
            is MarkdownTextNode -> builder.append(child.literal)
            is Code -> builder.append(child.literal)
            else -> builder.append(child.imageNodes().joinToString(" ") { it.destination })
        }
    }
    return builder.toString().trim()
}

private fun htmlToAnnotated(html: String, linkColor: Color): AnnotatedString =
    runCatching {
        AnnotatedString.fromHtml(
            html,
            TextLinkStyles(
                style = SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline,
                ),
            ),
        )
    }.getOrElse { AnnotatedString(html) }

private fun clickableLink(destination: String): String? {
    val value = destination.trim()
    return when {
        value.startsWith("http://", ignoreCase = true) -> value
        value.startsWith("https://", ignoreCase = true) -> value
        value.startsWith("mailto:", ignoreCase = true) -> value
        value.startsWith("tel:", ignoreCase = true) -> value
        value.startsWith("www.", ignoreCase = true) -> "https://$value"
        else -> null
    }
}

private fun isRemoteImageUrl(destination: String): Boolean =
    destination.startsWith("http://", ignoreCase = true) ||
        destination.startsWith("https://", ignoreCase = true)
