package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextContains
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w800dp-h1280dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WorkspaceContextBarTest {
    @get:Rule val ui = createComposeRule()

    @Test fun `normal context is one compact selected-object row without permanent instructions`() {
        val selected = DesignFixtures.document().objects.first()
        ui.setContent { MaterialTheme {
            Box(Modifier.width(800.dp).testTag("bounds")) { WorkspaceContextBar(selected, null) }
        } }
        ui.onNodeWithTag("workspace-selection").assertTextContains(selected.name)
            .assertTextContains("ft perimeter")
        ui.onNodeWithTag("workspace-message").assertDoesNotExist()
        val context = ui.onNodeWithTag("workspace-context").fetchSemanticsNode().boundsInRoot
        val bounds = ui.onNodeWithTag("bounds").fetchSemanticsNode().boundsInRoot
        assertTrue(context.height < bounds.width / 8f)
    }

    @Test fun `consequential feedback shares context without replacing selection`() {
        val selected = DesignFixtures.document().objects.first()
        ui.setContent { MaterialTheme { WorkspaceContextBar(selected, "Edit kept the existing pool") } }
        ui.onNodeWithTag("workspace-selection").assertTextContains(selected.name)
        ui.onNodeWithTag("workspace-message").assertTextContains("Edit kept the existing pool")
    }
}
