package com.markpad.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Stack

data class VersionItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class TabItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "未命名",
    val content: String = "",
    val filePath: String? = null,
    val isSaved: Boolean = true,
    val wordCount: Int = 0,
    val outline: List<OutlineItem> = emptyList(),
    val history: List<VersionItem> = emptyList()
)

data class EditorState(
    val tabs: List<TabItem> = listOf(TabItem()),
    val activeTabId: String = tabs[0].id,
    val theme: MarkdownTheme = MarkdownThemes.Default
) {
    val activeTab: TabItem? get() = tabs.find { it.id == activeTabId }
}

data class OutlineItem(
    val title: String,
    val level: Int,
    val offset: Int
)

class EditorViewModel : ViewModel() {
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val undoStacks = mutableMapOf<String, Stack<String>>()
    private val redoStacks = mutableMapOf<String, Stack<String>>()
    private var lastSaveTimes = mutableMapOf<String, Long>()
    private val UNDO_DEBOUNCE_MS = 1000L
    
    private var autoSaveJob: Job? = null
    private val AUTO_SAVE_DELAY_MS = 2000L

    fun onContentChange(newContent: String, context: android.content.Context? = null) {
        val activeTabId = _state.value.activeTabId
        val activeTab = _state.value.activeTab ?: return
        val oldContent = activeTab.content

        if (oldContent != newContent) {
            val currentTime = System.currentTimeMillis()
            val lastSaveTime = lastSaveTimes[activeTabId] ?: 0L
            
            if (currentTime - lastSaveTime > UNDO_DEBOUNCE_MS) {
                undoStacks.getOrPut(activeTabId) { Stack() }.push(oldContent)
                redoStacks[activeTabId]?.clear()
                lastSaveTimes[activeTabId] = currentTime
            }

            val updatedTabs = _state.value.tabs.map {
                if (it.id == activeTabId) {
                    it.copy(
                        content = newContent,
                        isSaved = false,
                        wordCount = countWords(newContent),
                        outline = extractOutline(newContent)
                    )
                } else it
            }
            _state.value = _state.value.copy(tabs = updatedTabs)
            
            // Auto Save
            autoSaveJob?.cancel()
            if (context != null && activeTab.filePath != null) {
                autoSaveJob = viewModelScope.launch {
                    delay(AUTO_SAVE_DELAY_MS)
                    saveFile(context)
                }
            }
        }
    }

    fun switchTab(tabId: String) {
        _state.value = _state.value.copy(activeTabId = tabId)
    }

    fun closeTab(tabId: String) {
        val currentTabs = _state.value.tabs
        if (currentTabs.size <= 1) {
            createNewFile()
            return
        }
        
        val updatedTabs = currentTabs.filter { it.id != tabId }
        val newActiveId = if (tabId == _state.value.activeTabId) {
            updatedTabs.first().id
        } else {
            _state.value.activeTabId
        }
        
        _state.value = _state.value.copy(tabs = updatedTabs, activeTabId = newActiveId)
        undoStacks.remove(tabId)
        redoStacks.remove(tabId)
        lastSaveTimes.remove(tabId)
    }

    private fun extractOutline(content: String): List<OutlineItem> {
        val outline = mutableListOf<OutlineItem>()
        content.lines().forEachIndexed { index, line ->
            if (line.startsWith("#")) {
                val match = Regex("^(#+)\\s+(.*)$").find(line)
                match?.let {
                    val level = it.groupValues[1].length
                    val title = it.groupValues[2]
                    // 计算字符偏移量
                    var offset = 0
                    for (i in 0 until index) {
                        offset += content.lines()[i].length + 1
                    }
                    outline.add(OutlineItem(title, level, offset))
                }
            }
        }
        return outline
    }

    fun createNewFile() {
        val newTab = TabItem()
        val updatedTabs = _state.value.tabs + newTab
        _state.value = _state.value.copy(tabs = updatedTabs, activeTabId = newTab.id)
    }

    fun undo() {
        val activeId = _state.value.activeTabId
        val stack = undoStacks[activeId]
        if (stack != null && stack.isNotEmpty()) {
            val activeTab = _state.value.activeTab ?: return
            val currentContent = activeTab.content
            redoStacks.getOrPut(activeId) { Stack() }.push(currentContent)
            val previousContent = stack.pop()
            
            val updatedTabs = _state.value.tabs.map {
                if (it.id == activeId) {
                    it.copy(
                        content = previousContent,
                        isSaved = false,
                        wordCount = countWords(previousContent),
                        outline = extractOutline(previousContent)
                    )
                } else it
            }
            _state.value = _state.value.copy(tabs = updatedTabs)
        }
    }

    fun redo() {
        val activeId = _state.value.activeTabId
        val stack = redoStacks[activeId]
        if (stack != null && stack.isNotEmpty()) {
            val activeTab = _state.value.activeTab ?: return
            val currentContent = activeTab.content
            undoStacks.getOrPut(activeId) { Stack() }.push(currentContent)
            val nextContent = stack.pop()
            
            val updatedTabs = _state.value.tabs.map {
                if (it.id == activeId) {
                    it.copy(
                        content = nextContent,
                        isSaved = false,
                        wordCount = countWords(nextContent),
                        outline = extractOutline(nextContent)
                    )
                } else it
            }
            _state.value = _state.value.copy(tabs = updatedTabs)
        }
    }

    fun loadFile(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = file.readText()
                withContext(Dispatchers.Main) {
                    val newTab = TabItem(
                        title = file.name,
                        content = content,
                        filePath = file.absolutePath,
                        isSaved = true,
                        wordCount = countWords(content),
                        outline = extractOutline(content)
                    )
                    val updatedTabs = _state.value.tabs + newTab
                    _state.value = _state.value.copy(tabs = updatedTabs, activeTabId = newTab.id)
                }
            } catch (e: Exception) {}
        }
    }

    fun importContent(name: String, content: String, uri: String? = null) {
        viewModelScope.launch(Dispatchers.Main) {
            val newTab = TabItem(
                title = name,
                content = content,
                filePath = uri,
                isSaved = true,
                wordCount = countWords(content),
                outline = extractOutline(content)
            )
            val updatedTabs = _state.value.tabs + newTab
            _state.value = _state.value.copy(tabs = updatedTabs, activeTabId = newTab.id)
        }
    }

    fun saveFile(context: android.content.Context, uri: android.net.Uri? = null) {
        val activeTab = _state.value.activeTab ?: return
        val pathOrUri = activeTab.filePath ?: uri?.toString() ?: return
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (pathOrUri.startsWith("content://")) {
                    val targetUri = android.net.Uri.parse(pathOrUri)
                    context.contentResolver.openOutputStream(targetUri, "rwt")?.use { 
                        it.write(activeTab.content.toByteArray()) 
                    }
                } else {
                    File(pathOrUri).writeText(activeTab.content)
                }
                
                withContext(Dispatchers.Main) {
                    val newVersion = VersionItem(content = activeTab.content)
                    val updatedTabs = _state.value.tabs.map {
                        if (it.id == activeTab.id) {
                            it.copy(
                                isSaved = true, 
                                filePath = pathOrUri,
                                history = (it.history + newVersion).takeLast(10) // Keep last 10 versions
                            )
                        } else it
                    }
                    _state.value = _state.value.copy(tabs = updatedTabs)
                }
            } catch (e: Exception) {}
        }
    }

    fun restoreVersion(versionId: String) {
        val activeTab = _state.value.activeTab ?: return
        val version = activeTab.history.find { it.id == versionId } ?: return
        
        onContentChange(version.content)
    }

    private fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(Regex("\\s+")).size
    }
}
