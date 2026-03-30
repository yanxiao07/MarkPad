package com.markpad.app.ui.editor

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.markpad.app.utils.ExportUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownPreviewScreen(
    markdown: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("预览") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        val html = ExportUtils.markdownToHtml(markdown)
        
        // 使用更强大的预览配置，支持 Mermaid, MathJax, Prism.js
        val enhancedHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <meta charset="UTF-8">
                <!-- Prism.js 代码高亮 -->
                <link href="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/themes/prism-tomorrow.min.css" rel="stylesheet" />
                <!-- MathJax 数学公式 -->
                <script src="https://polyfill.io/v3/polyfill.min.js?features=es6"></script>
                <script id="MathJax-script" async src="https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js"></script>
                <!-- Mermaid 流程图 -->
                <script src="https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js"></script>
                <script>
                    mermaid.initialize({
                        startOnLoad: true,
                        theme: 'default',
                        securityLevel: 'loose'
                    });
                </script>
                <style>
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; 
                        padding: 20px; 
                        line-height: 1.6; 
                        color: #333;
                        max-width: 900px;
                        margin: 0 auto;
                    }
                    img { max-width: 100%; height: auto; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                    table { border-collapse: collapse; width: 100%; margin: 1.5em 0; border: 1px solid #eee; }
                    th, td { border: 1px solid #eee; padding: 12px; text-align: left; }
                    th { background-color: #f8f9fa; font-weight: 600; }
                    pre { border-radius: 8px; padding: 1em; overflow: auto; background: #2d2d2d !important; }
                    blockquote { 
                        border-left: 4px solid #42b983; 
                        color: #666; 
                        padding: 10px 20px; 
                        margin: 1.5em 0;
                        background: #f3f5f7;
                    }
                    hr { border: 0; border-top: 1px solid #eee; margin: 2em 0; }
                    .task-list-item { list-style-type: none; }
                    .task-list-item input { margin-right: 8px; }
                </style>
            </head>
            <body>
                <div class="markdown-body">
                    $html
                </div>
                <script src="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/prism.min.js"></script>
                <script src="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/plugins/autoloader/prism-autoloader.min.js"></script>
            </body>
            </html>
        """.trimIndent()

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        allowContentAccess = true
                        allowFileAccess = true
                    }
                    loadDataWithBaseURL("https://local.markpad", enhancedHtml, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL("https://local.markpad", enhancedHtml, "text/html", "UTF-8", null)
            }
        )
    }
}
