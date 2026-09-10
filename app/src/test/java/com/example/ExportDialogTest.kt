package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.export.PdfExportOptions
import com.example.ui.components.ExportDialog
import com.example.ui.theme.MyApplicationTheme
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
class ExportDialogTest {
    @get:Rule val compose = createComposeRule()
    private val exports = mutableListOf<Pair<PdfExportOptions, Boolean>>()
    private var dismissals = 0

    private fun show() {
        compose.setContent {
            MyApplicationTheme {
                ExportDialog("Synthetic export fixture", { dismissals++ }) { options, asPdf ->
                    exports.add(options to asPdf)
                }
            }
        }
    }

    private fun tap(tag: String) = compose.onNodeWithTag(tag).performScrollTo().performClick()

    @Test fun `PNG exposes a labeled measurement toggle and delivers its value`() {
        show()
        tap("export_format_png")
        compose.onNodeWithTag("switch_include_dimensions")
            .assertIsOn().assertTextContains("Include measurements")
        tap("switch_include_dimensions")
        compose.onNodeWithTag("switch_include_dimensions").assertIsOff()
        compose.onNodeWithText("Sheet number").assertDoesNotExist()
        tap("btn_confirm_export")
        compose.runOnIdle {
            assertEquals(1, exports.size)
            assertFalse(exports.single().second)
            assertFalse(exports.single().first.includeDimensions)
            assertTrue(exports.single().first.includeBackground)
        }
    }

    @Test fun `measurement and underlay choices survive switching formats both ways`() {
        show()
        tap("export_format_png")
        tap("switch_include_dimensions")
        tap("switch_include_bg")
        tap("export_format_pdf")
        compose.onNodeWithTag("switch_include_dimensions").assertIsOff()
        compose.onNodeWithTag("switch_include_bg").assertIsOff()
        tap("btn_confirm_export")
        tap("export_format_png")
        tap("btn_confirm_export")
        compose.runOnIdle {
            assertEquals(listOf(true, false), exports.map { it.second })
            assertTrue(exports.none { it.first.includeDimensions || it.first.includeBackground })
            assertEquals(exports[0].first, exports[1].first)
        }
    }

    @Test fun `export choices survive saved-state recreation`() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            MyApplicationTheme {
                ExportDialog("Synthetic state fixture", {}) { options, asPdf ->
                    exports.add(options to asPdf)
                }
            }
        }
        tap("export_format_png")
        tap("switch_include_dimensions")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("export_format_png").assertIsSelected()
        compose.onNodeWithTag("switch_include_dimensions").assertIsOff()
        tap("btn_confirm_export")
        compose.runOnIdle {
            assertFalse(exports.single().second)
            assertFalse(exports.single().first.includeDimensions)
        }
    }

    @Test fun `cancel does not emit an export`() {
        show()
        tap("export_format_png")
        tap("switch_include_dimensions")
        tap("btn_cancel_export")
        compose.runOnIdle {
            assertEquals(1, dismissals)
            assertTrue(exports.isEmpty())
        }
    }
}
