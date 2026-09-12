package com.example

import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.DesignJsonCodec
import com.example.data.DesignWorkspaceStore
import com.example.export.DesignAppearance
import com.example.export.DesignOutput
import com.example.export.DesignOutputSettings
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlinx.coroutines.runBlocking

/** Exercises appearance as disposable workspace state over the same saved synthetic geometry. */
class AppearanceDeviceTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private val store get()=DesignWorkspaceStore(DesignWorkspaceStore.fileIn(context.filesDir))
    private val evidence get()=File(context.getExternalFilesDir(null),"workspace-evidence").apply { mkdirs() }
    private val preferences get()=context.getSharedPreferences("workspace-view",0)

    @Before fun emulatorOnly() {
        assumeTrue(Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk_gphone") || Build.MODEL.contains("Emulator"))
    }
    private fun open() {
        assertNotNull("Earlier scenarios must produce the synthetic design",store.load())
        ui.onNodeWithContentDescription("Projects").performClick()
        ui.onNodeWithTag("open-design-workspace").performClick()
        ui.waitUntil(10000) { ui.onAllNodesWithTag("workspace-canvas").fetchSemanticsNodes().size==1 }
    }
    private fun choose(appearance:DesignAppearance,showSelectorEvidence:Boolean=false) {
        ui.onNodeWithTag("workspace-commands").performClick()
        ui.onNodeWithTag("radial-category-view").performTouchInput { click(center) }
        ui.onNodeWithTag("radial-action-appearance").performTouchInput { click(center) }
        ui.onNodeWithTag("workspace-appearance-dialog").assertExists()
        if(showSelectorEvidence) capture("appearance-selector","workspace-appearance-dialog")
        ui.onNodeWithTag("workspace-appearance-${appearance.name.lowercase()}").performClick()
        ui.waitUntil(5000) { preferences.getString("appearance",null)==appearance.name }
        ui.onNodeWithTag("workspace-appearance-dialog").assertDoesNotExist()
    }
    private fun capture(name:String,semanticsTag:String?=null) {
        ui.waitForIdle()
        InstrumentationRegistry.getInstrumentation().uiAutomation.waitForIdle(300,3000)
        EmulatorCapture.save(context,evidence,name)
        val semantics=if(semanticsTag==null) ui.onRoot() else ui.onNodeWithTag(semanticsTag)
        File(evidence,"$name-semantics.txt").writeText(semantics.printToString())
    }

    @Test fun switchPersistAndExportTheSameDesign()=runBlocking {
        open()
        val before=store.load()!!
        val json=DesignJsonCodec.encode(before)
        val exports=mutableListOf<File>()
        for((index,appearance) in DesignAppearance.values().withIndex()) {
            choose(appearance,showSelectorEvidence=index==0)
            assertEquals(before,store.load())
            capture("appearance-${appearance.name.lowercase()}")
            val file=DesignOutput.png(context,before,1400,1000,DesignOutputSettings(
                appearance=appearance,includeSourceImage=false,includeMeasurements=false))!!
            val retained=File(evidence,"appearance-${appearance.name.lowercase()}-export.png")
            file.copyTo(retained,overwrite=true);exports.add(retained)
            assertEquals(json,DesignJsonCodec.encode(store.load()!!))
        }
        assertFalse(exports[0].readBytes().contentEquals(exports[1].readBytes()))
        assertFalse(exports[1].readBytes().contentEquals(exports[2].readBytes()))
        assertFalse(exports[0].readBytes().contentEquals(exports[2].readBytes()))

        ui.activityRule.scenario.recreate()
        ui.waitUntil(10000) { ui.onAllNodesWithTag("workspace-canvas").fetchSemanticsNodes().size==1 }
        assertEquals(DesignAppearance.NORTHSTAR.name,preferences.getString("appearance",null))
        ui.onNodeWithTag("workspace-commands").performClick()
        ui.onNodeWithTag("radial-category-view").performTouchInput { click(center) }
        ui.onNodeWithTag("radial-action-appearance").performTouchInput { click(center) }
        ui.onNodeWithTag("workspace-appearance-northstar").assertIsSelected()
        ui.onNodeWithTag("workspace-appearance-close").performClick()
        assertEquals(before,store.load())
    }
}
