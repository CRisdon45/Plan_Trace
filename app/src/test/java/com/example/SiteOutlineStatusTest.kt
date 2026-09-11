package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.model.design.*
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
class SiteOutlineStatusTest {
    @get:Rule val ui = createComposeRule()
    private fun fixture(): ProjectDesign {
        val source = SiteImage(SiteImageAsset("a".repeat(64),1000,1000), DesignPoint(0.0,20.0),0.02,
            ImageCalibration(ImagePoint(0.0,0.0),ImagePoint(500.0,0.0),10.0))
        var draft = SiteOutlineDraft(SiteOutlineRole.HOUSE,source,"site",0,"house")
        listOf(DesignPoint(2.0,18.0),DesignPoint(12.0,18.0),DesignPoint(12.0,8.0),DesignPoint(2.0,8.0))
            .forEach { draft = draft.append(it) }
        return ProjectDesign("site",listOf(draft.finish()),siteImage=source)
    }
    @Test fun `source preview does not resize the drawing area and committed mismatch does warn`() {
        val document = fixture()
        var state by mutableStateOf(WorkspaceState(document=document,selectedId="house",loading=false))
        ui.setContent {
            MaterialTheme {
                Column(Modifier.size(600.dp,500.dp)) {
                    Box(Modifier.weight(1f).fillMaxWidth().testTag("canvas-bounds"))
                    WorkspaceTraceStatus(state)
                }
            }
        }
        val before = ui.onNodeWithTag("canvas-bounds").fetchSemanticsNode().boundsInRoot
        val changed = DesignCommands.apply(document,DesignCommand.SetSiteImage(document.siteImage!!.moved(1.0,0.0),document.siteImage))
        ui.runOnIdle { state = state.copy(preview=changed) }
        ui.onNodeWithTag("workspace-registration-warning").assertDoesNotExist()
        assertEquals(before,ui.onNodeWithTag("canvas-bounds").fetchSemanticsNode().boundsInRoot)
        ui.runOnIdle { state = state.copy(document=changed,preview=null) }
        ui.onNodeWithTag("workspace-registration-warning").assertIsDisplayed()
        assertEquals(document.objects,changed.objects)
    }
    @Test fun `an adjusted-geometry preview retains the committed provenance status until completion`() {
        val document = fixture()
        val unlocked = DesignCommands.apply(document,DesignCommand.SetLocked("house",false))
        val changed = DesignCommands.apply(unlocked,DesignCommand.Translate("house",1.0,0.0))
        var state by mutableStateOf(WorkspaceState(document=unlocked,preview=changed,selectedId="house",loading=false))
        ui.setContent { MaterialTheme { WorkspaceTraceStatus(state) } }
        ui.onNodeWithTag("workspace-trace-status").assertTextEquals("Traced, not field-verified")
        ui.runOnIdle { state=state.copy(document=changed,preview=null) }
        ui.onNodeWithTag("workspace-trace-status").assertTextContains("adjusted after tracing",substring=true)
    }
}
