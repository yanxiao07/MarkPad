package com.markpad.app.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
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
    val activeTab = state.activeTab ?: return
    val context = LocalContext.current
    var textFieldValue by remember { mutableStateOf(TextFieldValue(activeTab.content)) }
    var showMenu by remember { mutableStateOf(false) }
    var showOutline by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var isFocusMode by remember { mutableStateOf(false) } // 专注模式

    val handleShortcut: (KeyEvent) -> Boolean = { keyEvent ->
        if (keyEvent.isCtrlPressed && keyEvent.type == KeyEventType.KeyDown) {
            val action = when (keyEvent.key) {
                Key.B -> "bold"
                Key.I -> "italic"
                Key.S -> { onSave(); null }
                Key.O -> { onImport(); null }
                Key.N -> { viewModel.createNewFile(); null }
                else -> null
            }
            if (action != null) {
                val (newText, newSelection) = applyMarkdownAction(textFieldValue, action)
                textFieldValue = TextFieldValue(newText, newSelection)
                viewModel.onContentChange(newText, context)
                true
            } else false
        } else false
    }

    // Sync state to local value ONLY if changed from outside (undo/redo/load/tab switch)
    LaunchedEffect(activeTab.id, activeTab.content) {
        if (activeTab.content != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(
                text = activeTab.content,
                selection = if (textFieldValue.text.isEmpty() || activeTab.id != state.activeTabId) {
                    androidx.compose.ui.text.TextRange(activeTab.content.length)
                } else {
                    textFieldValue.selection
                }
            )
        }
    }

    if (showOutline) {
        OutlineDialog(
            outlineItems = activeTab.outline,
            onDismiss = { showOutline = false },
            onNavigate = { offset ->
                textFieldValue = textFieldValue.copy(
                    selection = androidx.compose.ui.text.TextRange(offset)
                )
                showOutline = false
            }
        )
    }

    if (showHistory) {
        HistoryDialog(
            historyItems = activeTab.history,
            onDismiss = { showHistory = false },
            onRestore = { versionId ->
                viewModel.restoreVersion(versionId)
                showHistory = false
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { 
                        Column {
                            Text(
                                activeTab.filePath?.let { java.io.File(it).name } ?: activeTab.title,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "版本: 1.3.0-PREMIUM",
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
                        IconButton(onClick = { 
                            isFocusMode = !isFocusMode
                            if (isFocusMode) onToggleSidebar() // 专注模式自动隐藏侧边栏
                        }) {
                            Icon(
                                if (isFocusMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, 
                                contentDescription = "专注模式"
                            )
                        }
                        IconButton(onClick = onSave) {
                            Icon(Icons.Default.Save, contentDescription = "保存")
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多")
                        }
                        
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("导出 PDF") },
                                onClick = { 
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("导出 HTML") },
                                onClick = { 
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Html, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("导出 Word (Doc)") },
                                onClick = { 
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Description, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("新建标签页") },
                                onClick = { 
                                    showMenu = false
                                    viewModel.createNewFile()
                                },
                                leadingIcon = { Icon(Icons.Default.Add, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("历史版本") },
                                onClick = { 
                                    showMenu = false
                                    showHistory = true
                                },
                                leadingIcon = { Icon(Icons.Default.History, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("导入 .md 文件") },
                                onClick = { 
                                    showMenu = false
                                    onImport()
                                },
                                leadingIcon = { Icon(Icons.Default.FileOpen, null) }
                            )
                        }
                    }
                )
                
                // Tab Bar
                ScrollableTabRow(
                    selectedTabIndex = state.tabs.indexOfFirst { it.id == state.activeTabId },
                    edgePadding = 0.dp,
                    divider = {},
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[state.tabs.indexOfFirst { it.id == state.activeTabId }])
                        )
                    }
                ) {
                    state.tabs.forEach { tab ->
                        Tab(
                            selected = tab.id == state.activeTabId,
                            onClick = { viewModel.switchTab(tab.id) },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = tab.filePath?.let { java.io.File(it).name } ?: tab.title,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    if (!tab.isSaved) {
                                        Surface(
                                            modifier = Modifier.padding(start = 4.dp).size(6.dp),
                                            shape = androidx.compose.foundation.shape.CircleShape,
                                            color = MaterialTheme.colorScheme.error
                                        ) {}
                                    }
                                    IconButton(
                                        onClick = { viewModel.closeTab(tab.id) },
                                        modifier = Modifier.size(18.dp).padding(start = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        )
                    }
                }
            }
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
                        text = stringResource(R.string.word_count, activeTab.wordCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (!activeTab.isSaved) {
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
                    .onKeyEvent { handleShortcut(it) }
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
    outlineItems: List<OutlineItem>,
    onDismiss: () -> Unit,
    onNavigate: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("文档大纲") },
        text = {
            if (outlineItems.isEmpty()) {
                Text("暂无标题")
            } else {
                LazyColumn {
                    items(outlineItems) { item ->
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

@Composable
fun HistoryDialog(
    historyItems: List<VersionItem>,
    onDismiss: () -> Unit,
    onRestore: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("历史版本") },
        text = {
            if (historyItems.isEmpty()) {
                Text("暂无保存记录")
            } else {
                LazyColumn {
                    val reversedList: List<VersionItem> = historyItems.reversed()
                    items(reversedList) { item ->
                        ListItem(
                            headlineContent = { 
                                Text(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(item.timestamp))) 
                            },
                            supportingContent = { Text("内容长度: ${item.content.length} 字符") },
                            modifier = Modifier.clickable { onRestore(item.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
