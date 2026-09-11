package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.model.design.*
import com.example.ui.workspace.LiveMeasureOverlay
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w800dp-h1280dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LiveMeasureOverlayTest {
    @get:Rule val ui=createComposeRule()
    @Test fun `badge stays in bounds at each canvas corner without moving artwork`() {
        var target by mutableStateOf(Offset.Zero)
        ui.setContent { MaterialTheme {
            Box(Modifier.size(480.dp,320.dp).testTag("bounds")) {
                Canvas(Modifier.fillMaxSize().testTag("artwork")) {}
                LiveMeasureOverlay(LiveMeasure(DesignPoint(0.0,0.0),listOf("Line · 12′ 4 1/2″")),target)
            }
        } }
        val canvas=ui.onNodeWithTag("bounds").fetchSemanticsNode().boundsInRoot
        val original=ui.onNodeWithTag("artwork").fetchSemanticsNode().boundsInRoot
        listOf(Offset(1f,1f),Offset(canvas.width-1,1f),Offset(1f,canvas.height-1),Offset(canvas.width-1,canvas.height-1)).forEach { p ->
            ui.runOnIdle { target=p }
            val b=ui.onNodeWithTag("drawing-live-measure").fetchSemanticsNode().boundsInRoot
            assertTrue(b.left>=canvas.left && b.right<=canvas.right && b.top>=canvas.top && b.bottom<=canvas.bottom)
            assertEquals(original,ui.onNodeWithTag("artwork").fetchSemanticsNode().boundsInRoot)
        }
    }
    @Test fun `readout changes in place without creating an input blocking control`() {
        var taps=0
        var text by mutableStateOf("Line · 10′ 0″")
        ui.setContent { MaterialTheme {
            Box(Modifier.size(480.dp,320.dp).testTag("bounds")) {
                Canvas(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { taps++ } }) {}
                LiveMeasureOverlay(LiveMeasure(DesignPoint(0.0,0.0),listOf(text)),Offset(120f,130f))
            }
        } }
        ui.runOnIdle { text="Line · 12′ 0″" }
        ui.onNodeWithTag("drawing-live-measure").assertContentDescriptionEquals(text)
        val b=ui.onNodeWithTag("drawing-live-measure").fetchSemanticsNode().boundsInRoot
        val canvas=ui.onNodeWithTag("bounds").fetchSemanticsNode().boundsInRoot
        ui.onNodeWithTag("bounds").performTouchInput { click(b.center-canvas.topLeft) }
        ui.runOnIdle { assertEquals(1,taps) }
    }
}
