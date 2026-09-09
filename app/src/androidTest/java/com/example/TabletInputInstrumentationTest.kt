package com.example

import android.graphics.Color
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * Emulator coverage for the tablet input and Perspective contracts.
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
        waitForPlan()

        composeRule
            .onNodeWithContentDescription("S Pen Only (Palm Rejection Active)")
            .assertIsDisplayed()

        // Undo history is session-local, so a newly launched test activity begins with no undo
        // operation even if another test saved vector elements in the project database.
        val undo = composeRule.onNodeWithContentDescription("Undo")
        undo.assertIsNotEnabled()

        val (width, height) = activitySize()
        val startX = width * 0.55f
        val startY = height * 0.52f

        // In Pen-only mode a finger swipe is navigation, never a new drawing operation.
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
        undo.assertIsNotEnabled()

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
        undo.assertIsEnabled()
    }

    @Test
    fun filamentPerspective_presentsBackgroundAndGeneratedGeometry() {
        waitForPlan()
        val (width, height) = activitySize()

        // Produce a known closed vector primitive through the actual UI/input path. This means
        // the 3D assertion exercises: toolbar -> stylus routing -> project vector model ->
        // Architectural3DEngine -> Filament mesh -> GPU-presented Android window.
        composeRule.onNodeWithTag("tool_rect").performClick()
        injectSinglePointerStroke(
            toolType = MotionEvent.TOOL_TYPE_STYLUS,
            source = InputDevice.SOURCE_STYLUS,
            startX = width * 0.42f,
            startY = height * 0.38f,
            endX = width * 0.66f,
            endY = height * 0.62f
        )
        composeRule.waitForIdle()
        SystemClock.sleep(350)
        composeRule.onNodeWithContentDescription("Undo").assertIsEnabled()

        composeRule
            .onNodeWithContentDescription("Open Perspective preview")
            .performClick()

        composeRule.waitUntil(timeoutMillis = 12_000) {
            runCatching {
                composeRule.onNodeWithText("Perspective · Filament preview").fetchSemanticsNode()
                true
            }.getOrDefault(false)
        }

        composeRule.onNodeWithText("Perspective · Filament preview").assertIsDisplayed()
        composeRule.onNodeWithText("Drag to orbit · Pinch to zoom").assertIsDisplayed()

        SystemClock.sleep(2200)

        // A Compose Dialog owns a separate Android window, so inspecting only Activity.decorView
        // can miss the renderer entirely. UiAutomation captures the real composited display and
        // therefore verifies exactly what a tablet user would see.
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        assertNotNull("Expected a composited emulator screenshot", bitmap)
        bitmap!!

        // Black was the original failure mode. Sample the central viewport, well away from the
        // Compose header/footer overlays.
        val center = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
        val centerLuminance = (Color.red(center) + Color.green(center) + Color.blue(center)) / 3
        assertTrue(
            "Perspective viewport remained black: center=#${Integer.toHexString(center)} luminance=$centerLuminance",
            centerLuminance > 40
        )

        // Also require visible pixels that differ meaningfully from the intended paper-tone clear.
        // This prevents a false pass where the swap chain clears correctly but traced vector
        // geometry never makes it through the mesh/material/render pipeline.
        val background = intArrayOf(246, 244, 236)
        var geometrySamples = 0
        val minX = (bitmap.width * 0.16f).toInt()
        val maxX = (bitmap.width * 0.84f).toInt()
        val minY = (bitmap.height * 0.24f).toInt()
        val maxY = (bitmap.height * 0.76f).toInt()
        val stepX = ((maxX - minX) / 60).coerceAtLeast(1)
        val stepY = ((maxY - minY) / 40).coerceAtLeast(1)
        var y = minY
        while (y < maxY) {
            var x = minX
            while (x < maxX) {
                val pixel = bitmap.getPixel(x, y)
                val distance = abs(Color.red(pixel) - background[0]) +
                    abs(Color.green(pixel) - background[1]) +
                    abs(Color.blue(pixel) - background[2])
                if (distance > 90) geometrySamples++
                x += stepX
            }
            y += stepY
        }
        assertTrue(
            "Filament presented its background but no visible generated geometry (samples=$geometrySamples)",
            geometrySamples >= 8
        )
        bitmap.recycle()

        composeRule.onNodeWithContentDescription("Close Perspective").performClick()
    }

    private fun waitForPlan() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule
                .onAllNodes(hasContentDescription("S Pen Only (Palm Rejection Active)"))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
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
