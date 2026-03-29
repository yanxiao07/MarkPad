package com.markpad.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.markpad.app.ui.editor.EditorScreen
import com.markpad.app.ui.editor.EditorViewModel
import com.markpad.app.ui.editor.MarkdownPreviewScreen
import com.markpad.app.ui.filemanager.FileManagerScreen
import com.markpad.app.ui.filemanager.FileManagerViewModel
import com.markpad.app.ui.theme.MarkPadTheme
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
    private val editorViewModel: EditorViewModel by viewModels()
    private val fileManagerViewModel: FileManagerViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        fileManagerViewModel.loadFiles(filesDir)

        setContent {
            MarkPadTheme {
                val windowSize = calculateWindowSizeClass(this)
                val isExpanded = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
                
                val importLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument(),
                    onResult = { uri: Uri? ->
                        uri?.let { handleImport(it) }
                    }
                )

                MainLayout(
                    isExpanded = isExpanded,
                    editorViewModel = editorViewModel,
                    fileManagerViewModel = fileManagerViewModel,
                    onImport = {
                        importLauncher.launch(arrayOf("text/markdown", "text/plain"))
                    }
                )
            }
        }
    }

    private fun handleImport(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.readText()
                val name = uri.lastPathSegment ?: "Imported.md"
                editorViewModel.importContent(name, content)
            }
        } catch (e: Exception) {
            // Log or show error
        }
    }
}

@Composable
fun MainLayout(
    isExpanded: Boolean,
    editorViewModel: EditorViewModel,
    fileManagerViewModel: FileManagerViewModel,
    onImport: () -> Unit
) {
    var showPreview by remember { mutableStateOf(false) }
    val editorState by editorViewModel.state.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    if (showPreview) {
        MarkdownPreviewScreen(
            markdown = editorState.content,
            onBack = { showPreview = false }
        )
    } else {
        if (isExpanded) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(modifier = Modifier.width(300.dp)) {
                        FileManagerScreen(
                            viewModel = fileManagerViewModel,
                            onFileSelected = { file -> editorViewModel.loadFile(file) }
                        )
                    }
                }
            ) {
                EditorScreen(
                    viewModel = editorViewModel,
                    onToggleSidebar = {},
                    onPreview = { showPreview = true },
                    onImport = onImport
                )
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        FileManagerScreen(
                            viewModel = fileManagerViewModel,
                            onFileSelected = { file ->
                                editorViewModel.loadFile(file)
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                }
            ) {
                EditorScreen(
                    viewModel = editorViewModel,
                    onToggleSidebar = { scope.launch { drawerState.open() } },
                    onPreview = { showPreview = true },
                    onImport = onImport
                )
            }
        }
    }
}
