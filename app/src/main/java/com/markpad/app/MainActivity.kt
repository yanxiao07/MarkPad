package com.markpad.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.markpad.app.ui.filemanager.FileManagerScreen
import com.markpad.app.ui.filemanager.FileManagerViewModel
import com.markpad.app.ui.theme.MarkPadTheme
import java.io.File

class MainActivity : ComponentActivity() {
    private val editorViewModel: EditorViewModel by viewModels()
    private val fileManagerViewModel: FileManagerViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize file manager with internal directory or storage root
        fileManagerViewModel.loadFiles(filesDir)

        setContent {
            MarkPadTheme {
                val windowSize = calculateWindowSizeClass(this)
                val isExpanded = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
                
                MainLayout(
                    isExpanded = isExpanded,
                    editorViewModel = editorViewModel,
                    fileManagerViewModel = fileManagerViewModel
                )
            }
        }
    }
}

import com.markpad.app.ui.editor.MarkdownPreviewScreen

@Composable
fun MainLayout(
    isExpanded: Boolean,
    editorViewModel: EditorViewModel,
    fileManagerViewModel: FileManagerViewModel
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
                    PermanentDrawerSheet(
                        modifier = Modifier.width(300.dp)
                    ) {
                        FileManagerScreen(
                            viewModel = fileManagerViewModel,
                            onFileSelected = { file ->
                                editorViewModel.loadFile(file)
                            }
                        )
                    }
                }
            ) {
                EditorScreen(
                    viewModel = editorViewModel,
                    onToggleSidebar = { /* Sidebar is permanent in expanded mode */ },
                    onPreview = { showPreview = true }
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
                                // scope.launch { drawerState.close() }
                            }
                        )
                    }
                }
            ) {
                EditorScreen(
                    viewModel = editorViewModel,
                    onToggleSidebar = {
                        // scope.launch { if (drawerState.isClosed) drawerState.open() else drawerState.close() }
                    },
                    onPreview = { showPreview = true }
                )
            }
        }
    }
}
