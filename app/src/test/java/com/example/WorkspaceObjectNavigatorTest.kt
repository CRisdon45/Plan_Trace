package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.model.design.DesignFixtures
import com.example.ui.workspace.WorkspaceObjectNavigator
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w800dp-h1280dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WorkspaceObjectNavigatorTest {
    @get:Rule val ui = createComposeRule()

    @Test fun `compact navigator keeps direct object access and full accessible names`() {
        val objects = DesignFixtures.document().objects
        var selected: String? = null
        ui.setContent { MaterialTheme {
            WorkspaceObjectNavigator(objects, selected) { selected = it }
        } }
        ui.onNodeWithTag("workspace-object-count").assertTextEquals("${objects.size} objects")
        objects.forEachIndexed { index, obj ->
            ui.onNodeWithTag("workspace-object-$index")
                .assertContentDescriptionEquals(obj.name + if (obj.locked) ", locked" else "")
        }
        ui.onNodeWithTag("workspace-object-0").performClick()
        assertEquals(objects[0].id, selected)
    }
}
