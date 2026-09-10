package com.example

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.example.model.design.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.workspace.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SiteHeaderTest {
    @get:Rule val ui=createComposeRule()
    @Test fun `a site-only draft can export without first creating a proposed object`() {
        val doc=ProjectDesign("site-only",siteImage=SiteImage.unscaled(SiteImageAsset("a".repeat(64),400,300)))
        ui.setContent { MyApplicationTheme { WorkspaceHeader(WorkspaceState(document=doc,loading=false),false,{},{},{},{},{}) } }
        ui.onNodeWithTag("workspace-export").assertIsEnabled()
    }
    @Test fun `an empty draft is not presented as an exportable site`() {
        ui.setContent { MyApplicationTheme { WorkspaceHeader(WorkspaceState(document=ProjectDesign("empty"),loading=false),false,{},{},{},{},{}) } }
        ui.onNodeWithTag("workspace-export").assertIsNotEnabled()
    }
}
