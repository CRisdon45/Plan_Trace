package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.export.DesignAppearance
import com.example.ui.workspace.WorkspaceAppearanceDialog
import com.example.ui.workspace.WorkspaceAppearancePreference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WorkspaceAppearanceTest {
    @get:Rule val ui=createComposeRule()

    @Test fun `missing or unknown preference safely uses northstar`() {
        assertEquals(DesignAppearance.NORTHSTAR,WorkspaceAppearancePreference.decode(null))
        assertEquals(DesignAppearance.NORTHSTAR,WorkspaceAppearancePreference.decode("future-mode"))
        DesignAppearance.values().forEach { appearance ->
            assertEquals(appearance,WorkspaceAppearancePreference.decode(appearance.name))
        }
    }

    @Test fun `one direct choice commits and dismisses without apply ceremony`() {
        val choices=mutableListOf<DesignAppearance>()
        ui.setContent {
            MaterialTheme {
                var open by remember { mutableStateOf(true) }
                if(open) WorkspaceAppearanceDialog(DesignAppearance.NORTHSTAR,
                    onSelect={ choices.add(it);open=false },onDismiss={ open=false })
            }
        }
        ui.onNodeWithTag("workspace-appearance-northstar").assertIsSelected()
        ui.onNodeWithTag("workspace-appearance-graphic").assertIsNotSelected().performClick()
        ui.runOnIdle { assertEquals(listOf(DesignAppearance.GRAPHIC),choices) }
        ui.onNodeWithTag("workspace-appearance-dialog").assertDoesNotExist()
    }
}
