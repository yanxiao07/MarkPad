package com.markpad.app.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp
import com.vladsch.flexmark.ast.*
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.data.MutableDataSet

class MarkdownVisualTransformation(private val theme: MarkdownTheme) : VisualTransformation {

    private val options = MutableDataSet().apply {
        set(Parser.EXTENSIONS, listOf(
            TablesExtension.create(),
            StrikethroughExtension.create(),
            TaskListExtension.create()
        ))
    }
    private val parser = Parser.builder(options).build()

    // 符号隐藏样式：设为透明且字号极小
    private val hideStyle = SpanStyle(color = Color.Transparent, fontSize = 1.sp)

    override fun filter(text: AnnotatedString): TransformedText {
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

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
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
                node.openingMarker.let { 
                    if (it.isNotNull) builder.addStyle(hideStyle, it.startOffset, it.endOffset)
                }
            }
            is StrongEmphasis -> {
                builder.addStyle(theme.bold, start, end)
                // 隐藏 ** 符号
                builder.addStyle(hideStyle, start, start + 2)
                builder.addStyle(hideStyle, end - 2, end)
            }
            is Emphasis -> {
                builder.addStyle(theme.italic, start, end)
                // 隐藏 * 或 _ 符号
                builder.addStyle(hideStyle, start, start + 1)
                builder.addStyle(hideStyle, end - 1, end)
            }
            is Code -> {
                builder.addStyle(theme.code, start, end)
                // 隐藏 ` 符号
                builder.addStyle(hideStyle, start, start + 1)
                builder.addStyle(hideStyle, end - 1, end)
            }
            is BlockQuote -> {
                builder.addStyle(theme.quote, start, end)
                // 隐藏 > 符号
                node.openingMarker.let {
                    if (it.isNotNull) builder.addStyle(hideStyle, it.startOffset, it.endOffset)
                }
            }
            is FencedCodeBlock -> {
                builder.addStyle(theme.code, start, end)
                // 隐藏代码块标记
                node.openingMarker.let {
                    if (it.isNotNull) builder.addStyle(hideStyle, it.startOffset, it.endOffset)
                }
                node.closingMarker.let {
                    if (it.isNotNull) builder.addStyle(hideStyle, it.startOffset, it.endOffset)
                }
            }
        }

        // 递归处理子节点
        for (child in node.children) {
            applyStyle(child, builder)
        }
    }
}
