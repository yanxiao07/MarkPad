package com.markpad.app.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

data class MarkdownTheme(
    val h1: SpanStyle = SpanStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold),
    val h2: SpanStyle = SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    val h3: SpanStyle = SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    val h4: SpanStyle = SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    val h5: SpanStyle = SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
    val h6: SpanStyle = SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
    val bold: SpanStyle = SpanStyle(fontWeight = FontWeight.Bold),
    val italic: SpanStyle = SpanStyle(fontStyle = FontStyle.Italic),
    val strikethrough: SpanStyle = SpanStyle(textDecoration = TextDecoration.LineThrough),
    val code: SpanStyle = SpanStyle(
        fontFamily = FontFamily.Monospace,
        background = Color.LightGray.copy(alpha = 0.2f)
    ),
    val link: SpanStyle = SpanStyle(color = Color(0xFF2196F3), textDecoration = TextDecoration.Underline),
    val quote: SpanStyle = SpanStyle(color = Color.Gray, fontStyle = FontStyle.Italic),
    val listMarker: SpanStyle = SpanStyle(color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold),
    val tableHeader: SpanStyle = SpanStyle(fontWeight = FontWeight.Bold, background = Color.LightGray.copy(alpha = 0.3f)),
    val taskChecked: SpanStyle = SpanStyle(color = Color.Gray, textDecoration = TextDecoration.LineThrough),
)

object MarkdownThemes {
    val Default = MarkdownTheme()
    val GitHub = MarkdownTheme(
        link = SpanStyle(color = Color(0xFF0366d6))
    )
}
