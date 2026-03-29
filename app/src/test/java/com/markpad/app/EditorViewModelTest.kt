package com.markpad.app

import com.markpad.app.ui.editor.EditorViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EditorViewModelTest {

    private lateinit var viewModel: EditorViewModel

    @Before
    fun setup() {
        viewModel = EditorViewModel()
    }

    @Test
    fun `test word count logic`() {
        viewModel.onContentChange("Hello World")
        assertEquals(2, viewModel.state.value.wordCount)

        viewModel.onContentChange("This is a markdown editor.")
        assertEquals(5, viewModel.state.value.wordCount)

        viewModel.onContentChange("")
        assertEquals(0, viewModel.state.value.wordCount)
    }

    @Test
    fun `test undo and redo logic`() {
        viewModel.onContentChange("First")
        viewModel.onContentChange("Second")
        assertEquals("Second", viewModel.state.value.content)

        viewModel.undo()
        assertEquals("First", viewModel.state.value.content)

        viewModel.redo()
        assertEquals("Second", viewModel.state.value.content)
    }

    @Test
    fun `test save status logic`() {
        viewModel.onContentChange("Initial Content")
        assertFalse(viewModel.state.value.isSaved)

        // Simulate saving (note: real saving would require a file, but we're testing state logic)
        // Since viewModel.saveFile() uses File(path), we won't call it here but check the property
        // In a real test we'd mock the file system
    }
}
