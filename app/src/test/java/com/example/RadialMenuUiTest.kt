package com.example

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.model.design.DesignFixtures
import com.example.ui.workspace.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w800dp-h1280dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RadialMenuUiTest {
    @get:Rule val ui = createComposeRule()
    private val actions = mutableListOf<RadialAction>()
    private var dismissals = 0
    private var underneath = 0
    private val enabled = RadialAvailability(true, true, true, true, true, false, true)

    private fun menu(width: Int = 600, height: Int = 500, fontScale: Float = 1f,
        availability: RadialAvailability = enabled) {
        ui.setContent {
            val d = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(d, fontScale)) {
                MaterialTheme {
                    var open by remember { mutableStateOf(true) }
                    Box(Modifier.size(width.dp, height.dp).testTag("underneath").pointerInput(Unit) { detectTapGestures { underneath++ } }) {
                        if (open) WorkspaceRadialMenu(Offset(width * d / 2, height * d / 2),
                            IntSize((width * d).toInt(), (height * d).toInt()), availability,
                            onDismiss = { dismissals++; open = false }, onAction = { actions.add(it) })
                    }
                }
            }
        }
    }
    private fun tap(tag: String) { ui.onNodeWithTag(tag).performTouchInput { click(center) } }

    @Test fun `actual sector taps expand and execute exactly one action`() {
        menu(); tap("radial-category-edit"); tap("radial-action-size")
        ui.runOnIdle { assertEquals(listOf(RadialAction.SIZE), actions); assertEquals(1, dismissals); assertEquals(0, underneath) }
        ui.onNodeWithTag("workspace-radial").assertDoesNotExist()
    }
    @Test fun `disabled sector stays present and cannot execute through touch`() {
        menu(availability = enabled.copy(editable = false))
        tap("radial-category-edit")
        ui.onNodeWithTag("radial-action-delete").assertIsNotEnabled()
        tap("radial-action-delete")
        ui.runOnIdle { assertTrue(actions.isEmpty()); assertEquals(0, dismissals); assertEquals(0, underneath) }
    }
    @Test fun `center first returns to main wheel then closes without editing underneath`() {
        menu(); tap("radial-category-edit"); tap("radial-centre")
        ui.onNodeWithTag("radial-action-size").assertDoesNotExist()
        ui.onNodeWithTag("radial-category-edit").assertExists()
        tap("radial-centre")
        ui.runOnIdle { assertEquals(1, dismissals); assertTrue(actions.isEmpty()); assertEquals(0, underneath) }
    }
    @Test fun `outside tap dismisses without passing the same tap into the canvas`() {
        menu()
        ui.onNodeWithTag("workspace-radial").performTouchInput { click(Offset(3f, 3f)) }
        ui.runOnIdle { assertEquals(1, dismissals); assertTrue(actions.isEmpty()); assertEquals(0, underneath) }
    }
    @Test fun `canceled contact on delete never executes a command`() {
        menu(); tap("radial-category-edit")
        ui.onNodeWithTag("radial-action-delete").performTouchInput { down(center); cancel() }
        ui.runOnIdle { assertTrue(actions.isEmpty()); assertEquals(0, dismissals); assertEquals(0, underneath) }
    }
    @Test fun `small-space list keeps navigation reachable and invokes the same command`() {
        menu(width = 300, height = 350)
        ui.onNodeWithText("Commands").assertExists()
        ui.onNodeWithTag("radial-category-history").performScrollTo().performClick()
        ui.onNodeWithTag("radial-action-undo").performClick()
        ui.runOnIdle { assertEquals(listOf(RadialAction.UNDO), actions); assertEquals(1, dismissals) }
    }
    @Test fun `large text uses list without shrinking the radial labels`() {
        menu(fontScale = 1.6f)
        ui.onNodeWithText("Commands").assertExists()
        ui.onNodeWithTag("radial-category-view").performClick()
        ui.onNodeWithTag("radial-action-grid").performClick()
        ui.runOnIdle { assertEquals(listOf(RadialAction.GRID), actions) }
    }
    @Test fun `compact center back retains the menu instead of falling through to outside dismiss`() {
        menu(width = 300, height = 350)
        ui.onNodeWithTag("radial-category-edit").performClick()
        ui.onNodeWithTag("radial-centre").performClick()
        ui.onNodeWithTag("radial-category-edit").assertExists()
        ui.runOnIdle { assertEquals(0, dismissals); assertTrue(actions.isEmpty()); assertEquals(0, underneath) }
    }
    private fun header(width: Int, fontScale: Float) {
        ui.setContent {
            val d = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(d, fontScale)) {
                MaterialTheme {
                    Box(Modifier.width(width.dp).testTag("header-bounds")) {
                        val doc = DesignFixtures.document()
                        WorkspaceHeader(WorkspaceState(document = doc, loading = false, savedRevision = doc.revision),
                            false, {}, {}, {}, {}, {})
                    }
                }
            }
        }
        for (tag in listOf("workspace-commands", "workspace-back", "workspace-export", "workspace-save-status")) {
            ui.onNodeWithTag(tag).assertIsDisplayed()
            val box = ui.onNodeWithTag("header-bounds").fetchSemanticsNode().boundsInRoot
            val node = ui.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            assertTrue("$tag must remain in the visible header", node.left >= box.left && node.right <= box.right)
        }
    }
    @Test fun `narrow header exposes Commands without scrolling`() { header(320, 1f) }
    @Test fun `large-text narrow header retains Commands back export and save status`() { header(360, 1.6f) }
}
