package com.markpad.app.ui.editor

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.markpad.app.utils.ExportUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownPreviewScreen(
    markdown: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
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
        val html = remember(markdown) { ExportUtils.markdownToHtml(markdown) }
        
        val enhancedHtml = remember(html) {
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
                <meta charset="UTF-8">
                <link href="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/themes/prism-tomorrow.min.css" rel="stylesheet" />
                <script src="https://polyfill.io/v3/polyfill.min.js?features=es6"></script>
                <script id="MathJax-script" async src="https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js"></script>
                <script src="https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js"></script>
                <script>
                    window.onload = function() {
                        mermaid.initialize({ startOnLoad: true, theme: 'default' });
                    };
                </script>
                <style>
                    body { 
                        font-family: -apple-system, system-ui, sans-serif; 
                        padding: 16px; 
                        line-height: 1.6; 
                        color: #333;
                        word-wrap: break-word;
                    }
                    img { max-width: 100%; height: auto; display: block; margin: 10px auto; }
                    table { border-collapse: collapse; width: 100%; overflow-x: auto; display: block; }
                    th, td { border: 1px solid #eee; padding: 8px; }
                    pre { background: #2d2d2d !important; padding: 12px; border-radius: 4px; overflow-x: auto; }
                    blockquote { border-left: 4px solid #4CAF50; padding: 8px 16px; background: #f9f9f9; margin: 16px 0; }
                </style>
            </head>
            <body>
                <div class="markdown-body">$html</div>
                <script src="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/prism.min.js"></script>
            </body>
            </html>
            """.trimIndent()
        }

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                        }
                    }
                    loadDataWithBaseURL("file:///android_asset/", enhancedHtml, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL("file:///android_asset/", enhancedHtml, "text/html", "UTF-8", null)
            }
        )
    }
}
