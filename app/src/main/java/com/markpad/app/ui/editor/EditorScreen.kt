package com.markpad.app.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markpad.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onToggleSidebar: () -> Unit = {},
    onPreview: () -> Unit = {},
    onImport: () -> Unit = {},
    onSave: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var textFieldValue by remember { mutableStateOf(TextFieldValue(state.content)) }
    var showMenu by remember { mutableStateOf(false) }
    var showOutline by remember { mutableStateOf(false) } // 大纲控制

    // Sync state to local value ONLY if changed from outside (undo/redo/load)
    LaunchedEffect(state.content) {
        if (state.content != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(
                text = state.content,
                // Only move cursor to end if it's a completely new file/load
                selection = if (textFieldValue.text.isEmpty()) {
                    androidx.compose.ui.text.TextRange(state.content.length)
                } else {
                    textFieldValue.selection
                }
            )
        }
    }

    if (showOutline) {
        OutlineDialog(
            items = state.outline,
            onDismiss = { showOutline = false },
            onNavigate = { offset ->
                textFieldValue = textFieldValue.copy(
                    selection = androidx.compose.ui.text.TextRange(offset)
                )
                showOutline = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            state.filePath?.let { java.io.File(it).name } ?: stringResource(R.string.untitled),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "版本: 1.2.0-ULTRA",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onToggleSidebar) {
                        Icon(Icons.Default.Menu, contentDescription = "菜单")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.undo() }) {
                        Icon(Icons.Default.Undo, contentDescription = "撤销")
                    }
                    IconButton(onClick = { viewModel.redo() }) {
                        Icon(Icons.Default.Redo, contentDescription = "重做")
                    }
                    IconButton(onClick = onPreview) {
                        Icon(Icons.Default.Visibility, contentDescription = "预览")
                    }
                    IconButton(onClick = { showOutline = true }) {
                        Icon(Icons.Default.List, contentDescription = "大纲")
                    }
                    IconButton(onClick = onSave) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                    
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.export_pdf)) },
                            onClick = { 
                                showMenu = false
                                // Export logic handled by MainActivity or a dedicated handler
                            },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("导入 .md 文件") },
                            onClick = { 
                                showMenu = false
                                onImport()
                            },
                            leadingIcon = { Icon(Icons.Default.FileOpen, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("保存") },
                            onClick = { 
                                showMenu = false
                                onSave()
                            },
                            leadingIcon = { Icon(Icons.Default.Save, null) }
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.word_count, state.wordCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (!state.isSaved) {
                        Text(
                            text = stringResource(R.string.not_saved),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.saved),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Editor Toolbar
            MarkdownToolbar(
                onAction = { action ->
                    val (newText, newSelection) = applyMarkdownAction(textFieldValue, action)
                    textFieldValue = TextFieldValue(newText, newSelection)
                    viewModel.onContentChange(newText, context)
                }
            )

            // Main Editor
            BasicTextField(
                value = textFieldValue,
                onValueChange = {
                    textFieldValue = it
                    viewModel.onContentChange(it.text, context)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 28.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                visualTransformation = MarkdownVisualTransformation(state.theme),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                )
            )
        }
    }
}

@Composable
fun MarkdownToolbar(onAction: (String) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex = -1,
        divider = {},
        edgePadding = 8.dp,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        indicator = {}
    ) {
        ToolbarButton(Icons.Default.FormatBold, "加粗") { onAction("bold") }
        ToolbarButton(Icons.Default.FormatItalic, "斜体") { onAction("italic") }
        ToolbarButton(Icons.Default.StrikethroughS, "删除线") { onAction("strikethrough") }
        ToolbarButton(Icons.Default.FormatListBulleted, "无序列表") { onAction("list") }
        ToolbarButton(Icons.Default.FormatListNumbered, "有序列表") { onAction("ordered_list") }
        ToolbarButton(Icons.Default.CheckBox, "待办列表") { onAction("task") }
        ToolbarButton(Icons.Default.FormatQuote, "引用") { onAction("quote") }
        ToolbarButton(Icons.Default.Code, "行内代码") { onAction("code") }
        ToolbarButton(Icons.Default.IntegrationInstructions, "代码块") { onAction("fenced_code") }
        ToolbarButton(Icons.Default.Link, "链接") { onAction("link") }
        ToolbarButton(Icons.Default.TableChart, "表格") { onAction("table") }
        ToolbarButton(Icons.Default.Title, "标题") { onAction("h1") }
        ToolbarButton(Icons.Default.HorizontalRule, "分割线") { onAction("hr") }
    }
}

@Composable
fun ToolbarButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(22.dp))
    }
}

private fun applyMarkdownAction(current: TextFieldValue, action: String): Pair<String, androidx.compose.ui.text.TextRange> {
    val text = current.text
    val selection = current.selection
    val selectedText = text.substring(selection.start, selection.end)

    val (newText, offset) = when (action) {
        "bold" -> "**$selectedText**" to 2
        "italic" -> "_${selectedText}_" to 1
        "strikethrough" -> "~~$selectedText~~" to 2
        "code" -> "`$selectedText`" to 1
        "fenced_code" -> "\n```\n$selectedText\n```" to 5
        "link" -> "[$selectedText](url)" to 1
        "h1" -> "\n# $selectedText" to 3
        "quote" -> "\n> $selectedText" to 3
        "list" -> "\n- $selectedText" to 3
        "ordered_list" -> "\n1. $selectedText" to 4
        "task" -> "\n- [ ] $selectedText" to 6
        "hr" -> "\n---\n" to 5
        "table" -> "\n| Header | Header |\n| --- | --- |\n| Cell | Cell |\n" to 3
        else -> selectedText to 0
    }

    val updatedText = text.replaceRange(selection.start, selection.end, newText)
    val newCursorPos = if (selectedText.isEmpty()) {
        selection.start + offset
    } else {
        selection.start + newText.length
    }

    return updatedText to androidx.compose.ui.text.TextRange(newCursorPos)
}

@Composable
fun OutlineDialog(
    items: List<OutlineItem>,
    onDismiss: () -> Unit,
    onNavigate: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("文档大纲") },
        text = {
            if (items.isEmpty()) {
                Text("暂无标题")
            } else {
                LazyColumn {
                    items(items) { item ->
                        Text(
                            text = item.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate(item.offset) }
                                .padding(vertical = 8.dp, horizontal = (item.level * 8).dp),
                            style = when (item.level) {
                                1 -> MaterialTheme.typography.titleMedium
                                2 -> MaterialTheme.typography.titleSmall
                                else -> MaterialTheme.typography.bodyMedium
                            },
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}
