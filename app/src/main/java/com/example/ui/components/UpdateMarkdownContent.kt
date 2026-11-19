package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

sealed class UpdateMarkdownBlock {
    data class TextBlock(val spans: List<UpdateMarkdownSpan>) : UpdateMarkdownBlock()
    data class CodeBlock(val code: String, val language: String = "") : UpdateMarkdownBlock()
}

sealed class UpdateMarkdownSpan {
    data class PlainText(val text: String) : UpdateMarkdownSpan()
    data class Link(val title: String, val url: String) : UpdateMarkdownSpan()
    data class InlineCode(val code: String) : UpdateMarkdownSpan()
}

object UpdateMarkdownParser {
    private val INLINE_TOKEN_REGEX = Regex(
        """<a\s+[^>]*href\s*=\s*(?:"([^"]+)"|'([^']+)')[^>]*>([\s\S]*?)</a>|\[([^\]]+)\]\((https?://[^\s)]+)\)|`([^`\n]+)`""",
        RegexOption.IGNORE_CASE
    )

    fun parseBlocks(rawText: String): List<UpdateMarkdownBlock> {
        val text = rawText.replace("\r\n", "\n").replace("\r", "\n")
        if (text.isBlank()) return emptyList()

        val blocks = mutableListOf<UpdateMarkdownBlock>()
        var cursor = 0

        while (cursor < text.length) {
            val openIdx = text.indexOf("```", startIndex = cursor)
            if (openIdx < 0) {
                val tail = text.substring(cursor).trim('\n')
                if (tail.isNotBlank()) {
                    blocks.add(UpdateMarkdownBlock.TextBlock(parseInlineSpans(tail)))
                }
                break
            }

            if (openIdx > cursor) {
                val before = text.substring(cursor, openIdx).trim('\n')
                if (before.isNotBlank()) {
                    blocks.add(UpdateMarkdownBlock.TextBlock(parseInlineSpans(before)))
                }
            }

            val afterOpen = openIdx + 3
            val nextNewline = text.indexOf('\n', startIndex = afterOpen)
            val sameLineClose = text.indexOf("```", startIndex = afterOpen)

            // 1行完結の ```code``` パターン
            if (sameLineClose >= 0 && (nextNewline < 0 || sameLineClose < nextNewline)) {
                val code = text.substring(afterOpen, sameLineClose).trim()
                if (code.isNotEmpty()) {
                    blocks.add(UpdateMarkdownBlock.CodeBlock(code = code))
                }
                cursor = sameLineClose + 3
                if (cursor < text.length && text[cursor] == '\n') {
                    cursor++
                }
                continue
            }

            if (nextNewline >= 0) {
                val headerToken = text.substring(afterOpen, nextNewline).trim()
                val closeIdx = text.indexOf("```", startIndex = nextNewline + 1)
                if (closeIdx >= 0) {
                    val bodyAfterHeader = text.substring(nextNewline + 1, closeIdx).trim('\n')
                    val isLangTag = headerToken.isEmpty() ||
                        (headerToken.length <= 20 && headerToken.matches(Regex("""[a-zA-Z0-9_-]+""")))
                    val code = if (isLangTag) {
                        bodyAfterHeader
                    } else {
                        listOf(headerToken, bodyAfterHeader).filter { it.isNotEmpty() }.joinToString("\n")
                    }
                    val language = if (isLangTag) headerToken else ""
                    if (code.isNotEmpty()) {
                        blocks.add(UpdateMarkdownBlock.CodeBlock(code = code, language = language))
                    }
                    cursor = closeIdx + 3
                    if (cursor < text.length && text[cursor] == '\n') {
                        cursor++
                    }
                } else {
                    // 閉じ ``` がない場合は残りをコードブロックとして扱う
                    val remainingBody = text.substring(nextNewline + 1).trim('\n')
                    val isLangTag = headerToken.isEmpty() ||
                        (headerToken.length <= 20 && headerToken.matches(Regex("""[a-zA-Z0-9_-]+""")))
                    val code = if (isLangTag) {
                        remainingBody
                    } else {
                        listOf(headerToken, remainingBody).filter { it.isNotEmpty() }.joinToString("\n")
                    }
                    if (code.isNotEmpty()) {
                        blocks.add(
                            UpdateMarkdownBlock.CodeBlock(
                                code = code,
                                language = if (isLangTag) headerToken else ""
                            )
                        )
                    }
                    break
                }
            } else {
                val remaining = text.substring(afterOpen).trim()
                if (remaining.isNotEmpty()) {
                    blocks.add(UpdateMarkdownBlock.CodeBlock(code = remaining))
                }
                break
            }
        }

        return blocks
    }

    fun parseInlineSpans(text: String): List<UpdateMarkdownSpan> {
        if (text.isEmpty()) return emptyList()
        val normalized = text
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
        val spans = mutableListOf<UpdateMarkdownSpan>()
        var lastIndex = 0

        for (match in INLINE_TOKEN_REGEX.findAll(normalized)) {
            if (match.range.first > lastIndex) {
                spans.add(
                    UpdateMarkdownSpan.PlainText(
                        decodeBasicHtmlEntities(normalized.substring(lastIndex, match.range.first))
                    )
                )
            }
            val htmlHrefDouble = match.groups[1]?.value
            val htmlHrefSingle = match.groups[2]?.value
            val htmlTitle = match.groups[3]?.value
            val mdTitle = match.groups[4]?.value
            val mdUrl = match.groups[5]?.value
            val inlineCode = match.groups[6]?.value
            when {
                htmlTitle != null && (htmlHrefDouble != null || htmlHrefSingle != null) -> {
                    val rawUrl = (htmlHrefDouble ?: htmlHrefSingle ?: "").trim()
                    val resolvedUrl = if (rawUrl.startsWith("http://", ignoreCase = true) ||
                        rawUrl.startsWith("https://", ignoreCase = true)
                    ) {
                        rawUrl
                    } else if (rawUrl.isNotBlank()) {
                        "https://$rawUrl"
                    } else {
                        ""
                    }
                    val cleanTitle = decodeBasicHtmlEntities(htmlTitle).trim()
                    if (resolvedUrl.isNotBlank() && cleanTitle.isNotEmpty()) {
                        spans.add(UpdateMarkdownSpan.Link(title = cleanTitle, url = resolvedUrl))
                    } else if (cleanTitle.isNotEmpty()) {
                        spans.add(UpdateMarkdownSpan.PlainText(cleanTitle))
                    }
                }
                mdTitle != null && mdUrl != null -> {
                    spans.add(
                        UpdateMarkdownSpan.Link(
                            title = decodeBasicHtmlEntities(mdTitle),
                            url = mdUrl.trim()
                        )
                    )
                }
                inlineCode != null -> {
                    spans.add(UpdateMarkdownSpan.InlineCode(code = inlineCode))
                }
            }
            lastIndex = match.range.last + 1
        }

        if (lastIndex < normalized.length) {
            spans.add(
                UpdateMarkdownSpan.PlainText(
                    decodeBasicHtmlEntities(normalized.substring(lastIndex))
                )
            )
        }
        return spans
    }

    private fun decodeBasicHtmlEntities(input: String): String {
        return input
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
    }
}

@Composable
fun UpdateMarkdownContent(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    linkColor: Color = MaterialTheme.colorScheme.primary,
    fontSize: TextUnit = 13.sp,
    lineHeight: TextUnit = 19.sp
) {
    val blocks = remember(text) { UpdateMarkdownParser.parseBlocks(text) }
    val uriHandler = LocalUriHandler.current
    val inlineCodeBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is UpdateMarkdownBlock.TextBlock -> {
                    RichSpanText(
                        spans = block.spans,
                        textColor = textColor,
                        linkColor = linkColor,
                        inlineCodeBackground = inlineCodeBackground,
                        fontSize = fontSize,
                        lineHeight = lineHeight
                    )
                }

                is UpdateMarkdownBlock.CodeBlock -> {
                    MarkdownCodeBlockCard(code = block.code)
                }
            }
        }
    }
}

@Composable
fun RichLinkText(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    linkColor: Color = MaterialTheme.colorScheme.primary,
    fontSize: TextUnit = 13.sp,
    lineHeight: TextUnit = 19.sp,
    fontWeight: FontWeight = FontWeight.Normal
) {
    val spans = remember(text) { UpdateMarkdownParser.parseInlineSpans(text) }
    val inlineCodeBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    RichSpanText(
        spans = spans,
        modifier = modifier,
        textColor = textColor,
        linkColor = linkColor,
        inlineCodeBackground = inlineCodeBackground,
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontWeight = fontWeight
    )
}

@Composable
private fun RichSpanText(
    spans: List<UpdateMarkdownSpan>,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    linkColor: Color = MaterialTheme.colorScheme.primary,
    inlineCodeBackground: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    fontSize: TextUnit = 13.sp,
    lineHeight: TextUnit = 19.sp,
    fontWeight: FontWeight = FontWeight.Normal
) {
    val uriHandler = LocalUriHandler.current
    val annotated = remember(spans, linkColor, inlineCodeBackground) {
        buildAnnotatedString {
            spans.forEach { span ->
                when (span) {
                    is UpdateMarkdownSpan.PlainText -> append(span.text)
                    is UpdateMarkdownSpan.Link -> {
                        withLink(
                            LinkAnnotation.Url(
                                url = span.url,
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = linkColor,
                                        textDecoration = TextDecoration.Underline,
                                        fontWeight = FontWeight.Medium
                                    )
                                ),
                                linkInteractionListener = {
                                    try {
                                        uriHandler.openUri(span.url)
                                    } catch (_: Exception) {
                                    }
                                }
                            )
                        ) {
                            append(span.title)
                        }
                    }
                    is UpdateMarkdownSpan.InlineCode -> {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = inlineCodeBackground
                            )
                        ) {
                            append(span.code)
                        }
                    }
                }
            }
        }
    }
    Text(
        text = annotated,
        modifier = modifier,
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontWeight = fontWeight,
        color = textColor
    )
}

@Composable
private fun MarkdownCodeBlockCard(code: String) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember(code) { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("markdown_code_block")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(code))
                    copied = true
                },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("markdown_code_copy_button")
            ) {
                Icon(
                    imageVector = if (copied) Icons.Default.Check else Icons.Outlined.ContentCopy,
                    contentDescription = "コピー",
                    tint = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            VerticalDivider(
                modifier = Modifier.height(24.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = code,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
