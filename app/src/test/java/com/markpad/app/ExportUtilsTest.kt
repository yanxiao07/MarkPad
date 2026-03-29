package com.markpad.app

import com.markpad.app.utils.ExportUtils
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportUtilsTest {

    @Test
    fun `test markdown to html conversion`() {
        val markdown = "# Hello\n\n**Bold Text**"
        val html = ExportUtils.markdownToHtml(markdown)
        
        // Basic checks for expected HTML output
        assertTrue(html.contains("<h1>Hello</h1>"))
        assertTrue(html.contains("<strong>Bold Text</strong>"))
        assertTrue(html.contains("<!DOCTYPE html>"))
    }

    @Test
    fun `test markdown with table to html`() {
        val markdown = """
            | Header 1 | Header 2 |
            | -------- | -------- |
            | Cell 1   | Cell 2   |
        """.trimIndent()
        val html = ExportUtils.markdownToHtml(markdown)
        
        assertTrue(html.contains("<table>"))
        assertTrue(html.contains("<th>Header 1</th>"))
        assertTrue(html.contains("<td>Cell 1</td>"))
    }
}
