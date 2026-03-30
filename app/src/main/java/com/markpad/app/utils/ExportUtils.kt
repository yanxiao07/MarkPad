package com.markpad.app.utils

import android.content.Context
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import android.os.Environment
import android.widget.Toast
import com.itextpdf.html2pdf.HtmlConverter
import java.io.File
import java.io.FileOutputStream

import android.content.ContentValues
import android.provider.MediaStore
import android.net.Uri
import android.os.Build

object ExportUtils {

    private val options = MutableDataSet().apply {
        set(Parser.EXTENSIONS, listOf(
            TablesExtension.create(),
            StrikethroughExtension.create()
        ))
    }
    private val parser = Parser.builder(options).build()
    private val renderer = HtmlRenderer.builder(options).build()

    private fun saveToPublicDownloads(context: Context, fileName: String, mimeType: String, contentWriter: (java.io.OutputStream) -> Unit) {
        try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MarkPad")
                }
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    contentWriter(outputStream)
                }
                Toast.makeText(context, "导出成功: Downloads/MarkPad/$fileName", Toast.LENGTH_LONG).show()
            } else {
                throw Exception("无法创建文件")
            }
        } catch (e: Exception) {
            Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun markdownToHtml(markdown: String): String {
        val document = parser.parse(markdown)
        val contentHtml = renderer.render(document)
        
        // Wrap in professional GitHub-style HTML structure
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style>
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif; 
                        padding: 45px; 
                        line-height: 1.6; 
                        color: #24292e; 
                        max-width: 800px;
                        margin: 0 auto;
                        background-color: #ffffff;
                    }
                    h1, h2, h3, h4, h5, h6 { 
                        margin-top: 24px; 
                        margin-bottom: 16px; 
                        font-weight: 600; 
                        line-height: 1.25; 
                        border-bottom: 1px solid #eaecef;
                        padding-bottom: .3em;
                    }
                    code { 
                        background-color: rgba(27,31,35,.05); 
                        padding: .2em .4em; 
                        border-radius: 3px; 
                        font-family: SFMono-Regular,Consolas,Liberation Mono,Menlo,monospace;
                        font-size: 85%;
                    }
                    pre { 
                        background-color: #f6f8fa; 
                        padding: 16px; 
                        overflow: auto; 
                        border-radius: 6px;
                        line-height: 1.45;
                    }
                    table { 
                        border-collapse: collapse; 
                        width: 100%; 
                        margin-bottom: 16px;
                    }
                    th, td { 
                        border: 1px solid #dfe2e5; 
                        padding: 6px 13px; 
                    }
                    tr:nth-child(2n) { background-color: #f6f8fa; }
                    blockquote { 
                        border-left: .25em solid #dfe2e5; 
                        padding: 0 1em; 
                        color: #6a737d; 
                        margin: 0 0 16px 0;
                    }
                    img { max-width: 100%; box-sizing: content-box; background-color: #fff; }
                    hr { height: .25em; padding: 0; margin: 24px 0; background-color: #e1e4e8; border: 0; }
                </style>
            </head>
            <body>
                <div class="markdown-body">
                    $contentHtml
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    fun exportToPdf(context: Context, markdown: String, fileName: String) {
        val finalName = if (fileName.endsWith(".pdf")) fileName else "$fileName.pdf"
        val html = markdownToHtml(markdown)
        saveToPublicDownloads(context, finalName, "application/pdf") { outputStream ->
            HtmlConverter.convertToPdf(html, outputStream)
        }
    }

    fun exportToHtml(context: Context, markdown: String, fileName: String) {
        val finalName = if (fileName.endsWith(".html")) fileName else "$fileName.html"
        val html = markdownToHtml(markdown)
        saveToPublicDownloads(context, finalName, "text/html") { outputStream ->
            outputStream.write(html.toByteArray())
        }
    }

    fun exportToDocx(context: Context, markdown: String, fileName: String) {
        val finalName = if (fileName.endsWith(".docx")) fileName else "$fileName.docx"
        val html = markdownToHtml(markdown)
        saveToPublicDownloads(context, finalName, "application/vnd.openxmlformats-officedocument.wordprocessingml.document") { outputStream ->
            outputStream.write(html.toByteArray())
        }
    }
}
