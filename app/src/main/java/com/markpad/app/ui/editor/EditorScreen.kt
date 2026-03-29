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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onToggleSidebar: () -> Unit = {},
    onPreview: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    var textFieldValue by remember { mutableStateOf(TextFieldValue(state.content)) }

    // Sync state to local value if changed from outside (undo/redo)
    LaunchedEffect(state.content) {
        if (state.content != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(text = state.content)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.filePath?.let { java.io.File(it).name } ?: "Untitled") },
                navigationIcon = {
                    IconButton(onClick = onToggleSidebar) {
                        Icon(Icons.Default.Menu, contentDescription = "Sidebar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.undo() }) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = { viewModel.redo() }) {
                        Icon(Icons.Default.Redo, contentDescription = "Redo")
                    }
                    IconButton(onClick = onPreview) {
                        Icon(Icons.Default.Visibility, contentDescription = "Preview")
                    }
                    IconButton(onClick = { viewModel.saveFile() }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentPadding = PaddingValues(horizontal = 16.dp),
                actions = {
                    Text(
                        text = "Words: ${state.wordCount}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (!state.isSaved) {
                        Text(
                            text = "Unsaved",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            )
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
                    viewModel.onContentChange(newText)
                }
            )

            // Main Editor
            BasicTextField(
                value = textFieldValue,
                onValueChange = {
                    textFieldValue = it
                    viewModel.onContentChange(it.text)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
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
        ToolbarButton(Icons.Default.FormatBold, "Bold") { onAction("bold") }
        ToolbarButton(Icons.Default.FormatItalic, "Italic") { onAction("italic") }
        ToolbarButton(Icons.Default.FormatListBulleted, "List") { onAction("list") }
        ToolbarButton(Icons.Default.FormatQuote, "Quote") { onAction("quote") }
        ToolbarButton(Icons.Default.Code, "Code") { onAction("code") }
        ToolbarButton(Icons.Default.Link, "Link") { onAction("link") }
        ToolbarButton(Icons.Default.Title, "H1") { onAction("h1") }
    }
}

@Composable
fun ToolbarButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(20.dp))
    }
}

private fun applyMarkdownAction(current: TextFieldValue, action: String): Pair<String, androidx.compose.ui.text.TextRange> {
    val text = current.text
    val selection = current.selection
    val selectedText = text.substring(selection.start, selection.end)

    val (newText, offset) = when (action) {
        "bold" -> "**$selectedText**" to 2
        "italic" -> "_${selectedText}_" to 1
        "code" -> "`$selectedText`" to 1
        "link" -> "[$selectedText](url)" to 1
        "h1" -> "# $selectedText" to 2
        "quote" -> "> $selectedText" to 2
        "list" -> "- $selectedText" to 2
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
