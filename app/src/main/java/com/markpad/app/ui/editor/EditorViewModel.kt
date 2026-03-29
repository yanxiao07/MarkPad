package com.markpad.app.ui.editor

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Stack

data class EditorState(
    val content: String = "",
    val filePath: String? = null,
    val isSaved: Boolean = true,
    val wordCount: Int = 0,
    val theme: MarkdownTheme = MarkdownThemes.Default
)

class EditorViewModel : ViewModel() {
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val undoStack = Stack<String>()
    private val redoStack = Stack<String>()

    fun onContentChange(newContent: String) {
        val oldContent = _state.value.content
        if (oldContent != newContent) {
            undoStack.push(oldContent)
            redoStack.clear()
            _state.value = _state.value.copy(
                content = newContent,
                isSaved = false,
                wordCount = countWords(newContent)
            )
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val currentContent = _state.value.content
            redoStack.push(currentContent)
            val previousContent = undoStack.pop()
            _state.value = _state.value.copy(
                content = previousContent,
                isSaved = false,
                wordCount = countWords(previousContent)
            )
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val currentContent = _state.value.content
            undoStack.push(currentContent)
            val nextContent = redoStack.pop()
            _state.value = _state.value.copy(
                content = nextContent,
                isSaved = false,
                wordCount = countWords(nextContent)
            )
        }
    }

    fun loadFile(file: File) {
        viewModelScope.launch {
            try {
                val content = file.readText()
                _state.value = EditorState(
                    content = content,
                    filePath = file.absolutePath,
                    isSaved = true,
                    wordCount = countWords(content)
                )
                undoStack.clear()
                redoStack.clear()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun saveFile() {
        val currentPath = _state.value.filePath
        if (currentPath != null) {
            val file = File(currentPath)
            file.writeText(_state.value.content)
            _state.value = _state.value.copy(isSaved = true)
        }
    }

    private fun countWords(text: String): Int {
        return text.split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }
}
