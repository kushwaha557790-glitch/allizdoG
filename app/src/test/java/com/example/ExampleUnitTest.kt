package com.example

import com.example.model.FilterPreset
import com.example.model.TextStyleColor
import com.example.ui.state.MotionStudioViewModel
import com.example.util.MediaUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `adding and updating text layer updates state cleanly`() {
        val viewModel = MotionStudioViewModel()
        viewModel.updateDraftText("Title Card")
        viewModel.updateDraftFontSize(42f)
        viewModel.updateDraftTextColor(TextStyleColor.CORAL)
        viewModel.addOrCommitTextLayer()

        val state = viewModel.uiState.value
        assertEquals(1, state.textLayers.size)
        assertEquals("Title Card", state.textLayers.first().text)
        assertEquals(42f, state.textLayers.first().fontSizeSp)
        assertEquals(TextStyleColor.CORAL, state.textLayers.first().color)

        viewModel.updateDraftText("Updated Title")
        assertEquals("Updated Title", viewModel.uiState.value.textLayers.first().text)
    }

    @Test
    fun `dragging and centering text layer clamps coordinates`() {
        val viewModel = MotionStudioViewModel()
        viewModel.updateDraftText("Centered")
        viewModel.addOrCommitTextLayer()
        val id = viewModel.uiState.value.textLayers.first().id

        viewModel.moveTextLayer(id, 2.0f, -2.0f)
        val moved = viewModel.uiState.value.textLayers.first()
        assertEquals(0.92f, moved.normalizedX, 0.001f)
        assertEquals(0.08f, moved.normalizedY, 0.001f)

        viewModel.centerSelectedTextLayer()
        val centered = viewModel.uiState.value.textLayers.first()
        assertEquals(0.5f, centered.normalizedX, 0.001f)
        assertEquals(0.5f, centered.normalizedY, 0.001f)
    }

    @Test
    fun `applying and resetting filters updates filter settings`() {
        val viewModel = MotionStudioViewModel()
        viewModel.applyPreset(FilterPreset.COLOR_BOOST)
        assertTrue(viewModel.uiState.value.filters.isModified)
        assertEquals(1.2f, viewModel.uiState.value.filters.contrast, 0.001f)
        assertEquals(1.3f, viewModel.uiState.value.filters.saturation, 0.001f)

        viewModel.resetFilters()
        assertFalse(viewModel.uiState.value.filters.isModified)
        assertEquals(FilterPreset.NONE, viewModel.uiState.value.filters.preset)
        assertEquals(1f, viewModel.uiState.value.filters.contrast, 0.001f)
    }

    @Test
    fun `formatTimecode formats milliseconds cleanly`() {
        assertEquals("00:00", MediaUtils.formatTimecode(0L))
        assertEquals("00:12", MediaUtils.formatTimecode(12_400L))
        assertEquals("01:05", MediaUtils.formatTimecode(65_000L))
    }
}
