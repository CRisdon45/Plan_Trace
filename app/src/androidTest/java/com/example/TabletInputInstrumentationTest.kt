package com.example

import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.engine.filament.FilamentPlanSurface
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * Emulator coverage for the tablet input contract.
 *
 * Stylus tests inject MotionEvents with Android's real tool type/source metadata instead of adb's
 * generic touch input. They do not replace physical Samsung S Pen / palm-rejection testing, but
 * they catch the regression where stylus input is accidentally routed into pan.
 */
@RunWith(AndroidJUnit4::class)
class TabletInputInstrumentationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun penOnlyMode_routesFingerToNavigation_andStylusToDrawing() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule
                .onAllNodes(hasText("Set Scale ⌖"))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }

        composeRule
            .onNodeWithContentDescription("S Pen Only (Palm Rejection Active)")
            .assertIsDisplayed()

        // A fresh project has nothing to undo.
        composeRule.onNodeWithContentDescription("Undo").assertIsNotEnabled()

        val (width, height) = activitySize()
        val startX = width * 0.55f
        val startY = height * 0.52f

        // In Pen-only mode a finger swipe is navigation, not ink.
        injectSinglePointerStroke(
            toolType = MotionEvent.TOOL_TYPE_FINGER,
            source = InputDevice.SOURCE_TOUCHSCREEN,
            startX = startX,
            startY = startY,
            endX = startX + width * 0.12f,
            endY = startY + height * 0.05f
        )
        composeRule.waitForIdle()
        SystemClock.sleep(250)
        composeRule.onNodeWithContentDescription("Undo").assertIsNotEnabled()

        // The same motion as a stylus must create ink and therefore make Undo available.
        injectSinglePointerStroke(
            toolType = MotionEvent.TOOL_TYPE_STYLUS,
            source = InputDevice.SOURCE_STYLUS,
            startX = startX,
            startY = startY,
            endX = startX + width * 0.16f,
            endY = startY + height * 0.08f
        )
        composeRule.waitForIdle()
        SystemClock.sleep(350)
        composeRule.onNodeWithContentDescription("Undo").assertIsEnabled()
    }

    @Test
    fun filamentPerspective_presentsANonBlackFrame() {
        composeRule
            .onNodeWithContentDescription("Open Perspective preview")
            .performClick()

        composeRule.waitUntil(timeoutMillis = 12_000) {
            composeRule
                .onAllNodes(hasText("Perspective · Filament preview"))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }

        composeRule
            .onNodeWithText("Perspective · Filament preview")
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("Drag to orbit · Pinch to zoom")
            .assertIsDisplayed()

        SystemClock.sleep(1800)
        val surface = findView<FilamentPlanSurface>(composeRule.activity.window.decorView)
        assertNotNull("Expected the Filament TextureView inside the Perspective dialog", surface)

        val captured = AtomicReference<Bitmap?>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            captured.set(surface?.bitmap)
        }
        val bitmap = captured.get()
        assertNotNull("Expected TextureView bitmap after Filament rendered", bitmap)
        bitmap!!

        val sampleX = (bitmap.width / 2).coerceIn(0, bitmap.width - 1)
        val sampleY = (bitmap.height / 2).coerceIn(0, bitmap.height - 1)
        val center = bitmap.getPixel(sampleX, sampleY)
        val luminance = (Color.red(center) + Color.green(center) + Color.blue(center)) / 3
        assertTrue(
            "Perspective surface remained black: center=#${Integer.toHexString(center)} luminance=$luminance",
            luminance > 40
        )
        bitmap.recycle()

        composeRule
            .onNodeWithContentDescription("Close Perspective")
            .performClick()
    }

    private inline fun <reified T : View> findView(root: View): T? {
        if (root is T) return root
        if (root is ViewGroup) {
            for (index in 0 until root.childCount) {
                findView<T>(root.getChildAt(index))?.let { return it }
            }
        }
        return null
    }

    private fun activitySize(): Pair<Float, Float> {
        composeRule.waitForIdle()
        val decor = composeRule.activity.window.decorView
        assertTrue("Expected a laid-out activity", decor.width > 0 && decor.height > 0)
        return decor.width.toFloat() to decor.height.toFloat()
    }

    private fun injectSinglePointerStroke(
        toolType: Int,
        source: Int,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float
    ) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val downTime = SystemClock.uptimeMillis()

        fun inject(action: Int, x: Float, y: Float, eventTime: Long) {
            val properties = arrayOf(
                MotionEvent.PointerProperties().apply {
                    id = 0
                    this.toolType = toolType
                }
            )
            val coordinates = arrayOf(
                MotionEvent.PointerCoords().apply {
                    this.x = x
                    this.y = y
                    pressure = if (toolType == MotionEvent.TOOL_TYPE_STYLUS) 0.72f else 1f
                    size = 0.02f
                }
            )
            val event = MotionEvent.obtain(
                downTime,
                eventTime,
                action,
                1,
                properties,
                coordinates,
                0,
                0,
                1f,
                1f,
                0,
                0,
                source,
                0
            )
            try {
                assertTrue("Input injection failed for action=$action", automation.injectInputEvent(event, true))
            } finally {
                event.recycle()
            }
        }

        inject(MotionEvent.ACTION_DOWN, startX, startY, downTime)
        val moves = 8
        for (step in 1..moves) {
            val fraction = step.toFloat() / moves.toFloat()
            val time = downTime + step * 18L
            inject(
                MotionEvent.ACTION_MOVE,
                startX + (endX - startX) * fraction,
                startY + (endY - startY) * fraction,
                time
            )
        }
        inject(MotionEvent.ACTION_UP, endX, endY, downTime + (moves + 1) * 18L)
    }
}
