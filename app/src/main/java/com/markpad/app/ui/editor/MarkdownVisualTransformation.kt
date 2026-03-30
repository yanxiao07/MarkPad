package com.markpad.app.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp
import com.vladsch.flexmark.ast.*
import com.vladsch.flexmark.ast.ThematicBreak
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListItem
import com.vladsch.flexmark.ext.gitlab.GitLabExtension
import com.vladsch.flexmark.ext.gitlab.GitLabInlineMath
import com.vladsch.flexmark.ext.gitlab.GitLabBlockQuote
import com.vladsch.flexmark.ext.admonition.AdmonitionExtension
import com.vladsch.flexmark.ext.admonition.AdmonitionBlock
import com.vladsch.flexmark.ext.tables.*
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.data.MutableDataSet

class MarkdownVisualTransformation(private val theme: MarkdownTheme) : VisualTransformation {

    private val options = MutableDataSet().apply {
        set(Parser.EXTENSIONS, listOf(
            TablesExtension.create(),
            StrikethroughExtension.create(),
            TaskListExtension.create(),
            GitLabExtension.create(),
            AdmonitionExtension.create()
        ))
    }
    private val parser = Parser.builder(options).build()

    // 缓存解析结果，避免重复解析
    private var lastText: String? = null
    private var lastTransformedText: TransformedText? = null

    // 符号隐藏样式：透明且极小，接近隐藏效果
    private val hideStyle = SpanStyle(
        color = Color.Transparent, 
        fontSize = 0.1.sp, // 极小字号
        letterSpacing = (-2).sp // 负间距，进一步压缩空间
    )

    override fun filter(text: AnnotatedString): TransformedText {
        if (text.text == lastText && lastTransformedText != null) {
            return lastTransformedText!!
        }

        if (text.text.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val builder = AnnotatedString.Builder(text.text)
        
        try {
            val document = parser.parse(text.text)
            for (node in document.children) {
                applyStyle(node, builder)
            }
        } catch (e: Exception) {
            // Error safety
        }

        val result = TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
        lastText = text.text
        lastTransformedText = result
        return result
    }

    private fun applyStyle(node: Node, builder: AnnotatedString.Builder) {
        val start = node.startOffset
        val end = node.endOffset
        
        if (start < 0 || end > builder.length) return

        when (node) {
            is Heading -> {
                val style = when (node.level) {
                    1 -> theme.h1; 2 -> theme.h2; 3 -> theme.h3
                    else -> theme.h4
                }
                builder.addStyle(style, start, end)
                // 隐藏 # 符号
                val marker = node.openingMarker
                if (marker.isNotNull) {
                    builder.addStyle(hideStyle, marker.startOffset, marker.endOffset)
                }
            }
            is StrongEmphasis -> {
                builder.addStyle(theme.bold, start, end)
                // 隐藏 ** 或 __ 符号
                val opening = node.openingMarker
                val closing = node.closingMarker
                if (opening.isNotNull) builder.addStyle(hideStyle, opening.startOffset, opening.endOffset)
                if (closing.isNotNull) builder.addStyle(hideStyle, closing.startOffset, closing.endOffset)
            }
            is Emphasis -> {
                builder.addStyle(theme.italic, start, end)
                // 隐藏 * 或 _ 符号
                val opening = node.openingMarker
                val closing = node.closingMarker
                if (opening.isNotNull) builder.addStyle(hideStyle, opening.startOffset, opening.endOffset)
                if (closing.isNotNull) builder.addStyle(hideStyle, closing.startOffset, closing.endOffset)
            }
            is Code -> {
                builder.addStyle(theme.code, start, end)
                // 隐藏 ` 符号
                val opening = node.openingMarker
                val closing = node.closingMarker
                if (opening.isNotNull) builder.addStyle(hideStyle, opening.startOffset, opening.endOffset)
                if (closing.isNotNull) builder.addStyle(hideStyle, closing.startOffset, closing.endOffset)
            }
            is BlockQuote -> {
                builder.addStyle(theme.quote, start, end)
                // 隐藏 > 符号
                val marker = node.openingMarker
                if (marker.isNotNull) {
                    builder.addStyle(hideStyle, marker.startOffset, marker.endOffset)
                }
            }
            is FencedCodeBlock -> {
                builder.addStyle(theme.code, start, end)
                // 隐藏代码块标记 ``` 和 语言标识
                val opening = node.openingMarker
                val closing = node.closingMarker
                val info = node.info
                if (opening.isNotNull) builder.addStyle(hideStyle, opening.startOffset, opening.endOffset)
                if (info.isNotNull) builder.addStyle(hideStyle, info.startOffset, info.endOffset)
                if (closing.isNotNull) builder.addStyle(hideStyle, closing.startOffset, closing.endOffset)
            }
            is Strikethrough -> {
                builder.addStyle(theme.strikethrough, start, end)
                // 隐藏 ~~ 符号
                val opening = node.openingMarker
                val closing = node.closingMarker
                if (opening.isNotNull) builder.addStyle(hideStyle, opening.startOffset, opening.endOffset)
                if (closing.isNotNull) builder.addStyle(hideStyle, closing.startOffset, closing.endOffset)
            }
            is TaskListItem -> {
                // 待办列表处理
                val marker = node.openingMarker
                if (marker.isNotNull) {
                    builder.addStyle(hideStyle, marker.startOffset, marker.endOffset)
                }
                // 如果已完成，应用删除线样式
                if (node.isItemDoneMarker()) {
                    builder.addStyle(theme.taskChecked, start, end)
                }
            }
            is BulletListItem -> {
                val marker = node.openingMarker
                if (marker.isNotNull) {
                    builder.addStyle(theme.listMarker, marker.startOffset, marker.endOffset)
                }
            }
            is OrderedListItem -> {
                val marker = node.openingMarker
                if (marker.isNotNull) {
                    builder.addStyle(theme.listMarker, marker.startOffset, marker.endOffset)
                }
            }
            is TableBlock -> {
                builder.addStyle(theme.code.copy(background = Color.LightGray.copy(alpha = 0.1f)), start, end)
            }
            is TableHead -> {
                builder.addStyle(theme.tableHeader, start, end)
            }
            is ThematicBreak -> {
                builder.addStyle(theme.code.copy(color = Color.Gray), start, end)
            }
            is Link -> {
                builder.addStyle(theme.link, start, end)
                // 隐藏 [ ] ( ) 和 URL
                val opening = node.textOpeningMarker
                val closing = node.textClosingMarker
                val urlOpening = node.linkOpeningMarker
                val urlClosing = node.linkClosingMarker
                if (opening.isNotNull) builder.addStyle(hideStyle, opening.startOffset, opening.endOffset)
                if (closing.isNotNull) builder.addStyle(hideStyle, closing.startOffset, closing.endOffset)
                if (urlOpening.isNotNull && urlClosing.isNotNull) {
                    builder.addStyle(hideStyle, urlOpening.startOffset, urlClosing.endOffset)
                }
            }
            is Image -> {
                builder.addStyle(theme.link.copy(color = Color(0xFF4CAF50)), start, end)
            }
            is GitLabInlineMath -> {
                builder.addStyle(theme.code.copy(color = Color(0xFF9C27B0)), start, end)
                // 隐藏 $ 符号
                val opening = node.openingMarker
                val closing = node.closingMarker
                if (opening.isNotNull) builder.addStyle(hideStyle, opening.startOffset, opening.endOffset)
                if (closing.isNotNull) builder.addStyle(hideStyle, closing.startOffset, closing.endOffset)
            }
            is AdmonitionBlock -> {
                builder.addStyle(theme.quote.copy(color = Color(0xFF607D8B)), start, end)
            }
        }

        // 递归处理子节点
        for (child in node.children) {
            applyStyle(child, builder)
        }
    }
}
