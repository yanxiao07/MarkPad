package com.markpad.app.ui.filemanager

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.Date

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val lastModified: Long,
    val size: Long
)

class FileManagerViewModel : ViewModel() {
    private val _currentPath = MutableStateFlow(File("/").absolutePath)
    val currentPath: StateFlow<String> = _currentPath

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files

    fun loadFiles(directory: File) {
        _currentPath.value = directory.absolutePath
        val fileList = directory.listFiles()?.map {
            FileItem(it.name, it.absolutePath, it.isDirectory, it.lastModified(), it.length())
        }?.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name.lowercase() }) ?: emptyList()
        _files.value = fileList
    }

    fun navigateUp() {
        val parent = File(_currentPath.value).parentFile
        if (parent != null) {
            loadFiles(parent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    viewModel: FileManagerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onFileSelected: (File) -> Unit = {},
    onNewFile: () -> Unit = {},
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentPath by viewModel.currentPath.collectAsState()
    val files by viewModel.files.collectAsState()

    LaunchedEffect(Unit) {
        // 初始加载应用内部存储目录
        viewModel.loadFiles(context.filesDir)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文件") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.navigateUp() }) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "向上")
                    }
                    IconButton(onClick = onNewFile) {
                        Icon(Icons.Default.NoteAdd, contentDescription = "新建文件")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                PathBreadcrumbs(currentPath)
            }
            items(files) { file ->
                FileListItem(file) {
                    if (file.isDirectory) {
                        viewModel.loadFiles(File(file.path))
                    } else if (file.name.endsWith(".md")) {
                        onFileSelected(File(file.path))
                    }
                }
            }
        }
    }
}

@Composable
fun FileListItem(file: FileItem, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(file.name, fontWeight = if (file.isDirectory) FontWeight.Bold else FontWeight.Normal) },
        supportingContent = {
            if (!file.isDirectory) {
                Text("${Date(file.lastModified)} • ${file.size / 1024} KB")
            }
        },
        leadingContent = {
            Icon(
                if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                contentDescription = null,
                tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
fun PathBreadcrumbs(path: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = path,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            maxLines = 1
        )
    }
}
