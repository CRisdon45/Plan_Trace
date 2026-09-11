package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.fetchSemanticsNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.ui.workspace.OutlineControlStrip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w800dp-h1280dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OutlineControlStripTest {
    @get:Rule val ui = createComposeRule()

    @Test fun active_drawing_keeps_all_commands_in_one_compact_accessible_strip() {
        val calls = mutableListOf<String>()
        ui.setContent { MaterialTheme {
            Box(Modifier.width(800.dp).testTag("bounds")) {
                OutlineControlStrip(
                    label = "Pool",
                    countText = "3 corners",
                    canClose = true,
                    canBack = true,
                    closeLabel = "Close",
                    tag = "proposed-outline",
                    modeLabel = "Right angles",
                    modeSelected = true,
                    onMode = { calls += "mode" },
                    onClose = { calls += "close" },
                    onBack = { calls += "back" },
                    onCancel = { calls += "cancel" },
                )
            }
        } }

        ui.onNodeWithTag("proposed-outline-count").assertTextEquals("Pool · 3 corners")
        ui.onNodeWithTag("proposed-outline-ortho")
            .assertContentDescriptionEquals("Right angles, on").performClick()
        ui.onNodeWithTag("proposed-outline-finish")
            .assertContentDescriptionEquals("Close outline").assertIsEnabled().performClick()
        ui.onNodeWithTag("proposed-outline-back")
            .assertContentDescriptionEquals("Back one point").assertIsEnabled().performClick()
        ui.onNodeWithTag("proposed-outline-cancel")
            .assertContentDescriptionEquals("Cancel drawing").assertIsEnabled().performClick()
        assertEquals(listOf("mode", "close", "back", "cancel"), calls)

        ui.onNodeWithText("Tap or drag the pen", substring = true).assertDoesNotExist()
        val strip = ui.onNodeWithTag("proposed-outline-controls").fetchSemanticsNode().boundsInRoot
        val bounds = ui.onNodeWithTag("bounds").fetchSemanticsNode().boundsInRoot
        assertTrue(strip.height < bounds.width / 8f)
    }
}
