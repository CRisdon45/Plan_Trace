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
import androidx.compose.ui.test.onNodeWithTag
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

        val undo = composeRule.onNodeWithContentDescription("Undo")
        val hadExistingUndo = runCatching { undo.assertIsEnabled(); true }.getOrDefault(false)

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
        if (!hadExistingUndo) undo.assertIsNotEnabled()

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
        // Architectural3DEngine -> Filament mesh -> GPU-presented TextureView.
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
            composeRule
                .onAllNodes(hasText("Perspective · Filament preview"))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }

        composeRule.onNodeWithText("Perspective · Filament preview").assertIsDisplayed()
        composeRule.onNodeWithText("Drag to orbit · Pinch to zoom").assertIsDisplayed()

        SystemClock.sleep(2200)
        val surface = findView<FilamentPlanSurface>(composeRule.activity.window.decorView)
        assertNotNull("Expected the Filament TextureView inside the Perspective dialog", surface)

        val captured = AtomicReference<Bitmap?>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            captured.set(surface?.bitmap)
        }
        val bitmap = captured.get()
        assertNotNull("Expected TextureView bitmap after Filament rendered", bitmap)
        bitmap!!

        // Black was the original failure mode. The explicit paper-tone clear must always present.
        val center = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
        val centerLuminance = (Color.red(center) + Color.green(center) + Color.blue(center)) / 3
        assertTrue(
            "Perspective surface remained black: center=#${Integer.toHexString(center)} luminance=$centerLuminance",
            centerLuminance > 40
        )

        // Also require pixels that differ meaningfully from the paper background. That prevents a
        // false pass where only the clear color works but traced vector geometry never reaches the GPU.
        val background = intArrayOf(246, 244, 236)
        var geometrySamples = 0
        val stepX = (bitmap.width / 60).coerceAtLeast(1)
        val stepY = (bitmap.height / 40).coerceAtLeast(1)
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
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
            "Filament presented the background but no visible generated geometry (samples=$geometrySamples)",
            geometrySamples >= 8
        )
        bitmap.recycle()

        composeRule.onNodeWithContentDescription("Close Perspective").performClick()
    }

    private fun waitForPlan() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule
                .onAllNodes(hasText("Set Scale ⌖"))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty() ||
                composeRule
                    .onAllNodes(hasText("Scale:"))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
        }
        composeRule
            .onNodeWithContentDescription("S Pen Only (Palm Rejection Active)")
            .assertIsDisplayed()
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
