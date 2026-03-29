package com.markpad.app.utils

import android.content.Context
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.itextpdf.html2pdf.HtmlConverter
import java.io.File
import java.io.FileOutputStream

object ExportUtils {

    private val options = MutableDataSet().apply {
        set(Parser.EXTENSIONS, listOf(
            TablesExtension.create(),
            StrikethroughExtension.create()
        ))
    }
    private val parser = Parser.builder(options).build()
    private val renderer = HtmlRenderer.builder(options).build()

    fun markdownToHtml(markdown: String): String {
        val document = parser.parse(markdown)
        val contentHtml = renderer.render(document)
        
        // Wrap in basic HTML structure with styling
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: sans-serif; padding: 40px; line-height: 1.6; color: #333; }
                    h1, h2, h3 { color: #000; }
                    code { background: #f4f4f4; padding: 2px 4px; border-radius: 4px; }
                    pre { background: #f4f4f4; padding: 10px; overflow-x: auto; }
                    table { border-collapse: collapse; width: 100%; }
                    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                    th { background-color: #f2f2f2; }
                    blockquote { border-left: 4px solid #ddd; padding-left: 15px; color: #777; }
                </style>
            </head>
            <body>
                $contentHtml
            </body>
            </html>
        """.trimIndent()
    }

    fun exportToPdf(context: Context, markdown: String, outputFile: File) {
        val html = markdownToHtml(markdown)
        val outputStream = FileOutputStream(outputFile)
        HtmlConverter.convertToPdf(html, outputStream)
        outputStream.close()
    }

    fun exportToHtml(markdown: String, outputFile: File) {
        val html = markdownToHtml(markdown)
        outputFile.writeText(html)
    }
}
