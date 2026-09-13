package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.example.model.design.DesignFixtures
import com.example.ui.workspace.WorkspaceContextBar
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w800dp-h1280dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WorkspaceContextBarTest {
    @get:Rule val ui = createComposeRule()

    @Test fun `idle workspace reserves no empty context row`() {
        ui.setContent { MaterialTheme { WorkspaceContextBar(null, null) } }
        ui.onNodeWithTag("workspace-context").assertDoesNotExist()
        ui.onNodeWithTag("workspace-selection").assertDoesNotExist()
    }

    @Test fun `normal context is one compact selected-object row without permanent instructions`() {
        val selected = DesignFixtures.document().objects.first()
        ui.setContent { MaterialTheme {
            Box(Modifier.width(800.dp).testTag("bounds")) { WorkspaceContextBar(selected, null) }
        } }
        val selectionText = String.format(
            Locale.US,
            "%s · %.2f ft perimeter",
            selected.name,
            selected.boundary.perimeterMetres / 0.3048,
        )
        ui.onNodeWithTag("workspace-selection").assertTextEquals(selectionText)
        ui.onNodeWithTag("workspace-message").assertDoesNotExist()
        val context = ui.onNodeWithTag("workspace-context").fetchSemanticsNode().boundsInRoot
        val bounds = ui.onNodeWithTag("bounds").fetchSemanticsNode().boundsInRoot
        assertTrue(context.height < bounds.width / 8f)
    }

    @Test fun `consequential feedback shares context without replacing selection`() {
        val selected = DesignFixtures.document().objects.first()
        ui.setContent { MaterialTheme { WorkspaceContextBar(selected, "Edit kept the existing pool") } }
        val selectionText = String.format(
            Locale.US,
            "%s · %.2f ft perimeter",
            selected.name,
            selected.boundary.perimeterMetres / 0.3048,
        )
        ui.onNodeWithTag("workspace-selection").assertTextEquals(selectionText)
        ui.onNodeWithTag("workspace-message").assertTextEquals("Edit kept the existing pool")
    }

    @Test fun `feedback can stand alone without inventing a selection`() {
        ui.setContent { MaterialTheme { WorkspaceContextBar(null, "Source image not scaled") } }
        ui.onNodeWithTag("workspace-selection").assertDoesNotExist()
        ui.onNodeWithTag("workspace-message").assertTextEquals("Source image not scaled")
    }
}
