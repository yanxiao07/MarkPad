package com.markpad.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = file.readText()
                withContext(Dispatchers.Main) {
                    _state.value = EditorState(
                        content = content,
                        filePath = file.absolutePath,
                        isSaved = true,
                        wordCount = countWords(content)
                    )
                    undoStack.clear()
                    redoStack.clear()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun importContent(name: String, content: String) {
        _state.value = EditorState(
            content = content,
            filePath = null, // Imported content needs to be saved first
            isSaved = false,
            wordCount = countWords(content)
        )
        undoStack.clear()
        redoStack.clear()
    }

    fun saveFile(context: android.content.Context, explicitFile: File? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val targetFile = explicitFile ?: _state.value.filePath?.let { File(it) }
                if (targetFile != null) {
                    targetFile.writeText(_state.value.content)
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            isSaved = true,
                            filePath = targetFile.absolutePath
                        )
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(Regex("\\s+")).size
    }
}
