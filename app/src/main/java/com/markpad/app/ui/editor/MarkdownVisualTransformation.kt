package com.markpad.app.ui.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
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

    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text.text)
        val document = parser.parse(text.text)

        for (node in document.children) {
            applyStyle(node, builder)
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    private fun applyStyle(node: Node, builder: AnnotatedString.Builder) {
        val start = node.startOffset
        val end = node.endOffset

        when (node) {
            is Heading -> {
                val style = when (node.level) {
                    1 -> theme.h1
                    2 -> theme.h2
                    3 -> theme.h3
                    4 -> theme.h4
                    5 -> theme.h5
                    else -> theme.h6
                }
                builder.addStyle(style, start, end)
            }
            is StrongEmphasis -> builder.addStyle(theme.bold, start, end)
            is Emphasis -> builder.addStyle(theme.italic, start, end)
            is Strikethrough -> builder.addStyle(theme.strikethrough, start, end)
            is Code -> builder.addStyle(theme.code, start, end)
            is FencedCodeBlock -> builder.addStyle(theme.code, start, end)
            is Link -> builder.addStyle(theme.link, start, end)
            is BlockQuote -> builder.addStyle(theme.quote, start, end)
        }

        // Recursively apply to children
        for (child in node.children) {
            applyStyle(child, builder)
        }
    }
}
