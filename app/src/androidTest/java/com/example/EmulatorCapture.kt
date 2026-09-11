package com.example

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/** A green Compose assertion cannot certify an unobstructed Android screen. Never dismiss an ANR. */
object EmulatorCapture {
    fun save(context: Context, directory: File, name: String) {
        directory.mkdirs()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val deadline = SystemClock.uptimeMillis() + 3000
        var activePackage: String?
        do {
            activePackage = automation.rootInActiveWindow?.packageName?.toString()
            if (activePackage == context.packageName) break
            SystemClock.sleep(100)
        } while (SystemClock.uptimeMillis() < deadline)
        val root = automation.rootInActiveWindow
        val windowText = buildString {
            append("Expected application: ").append(context.packageName).append('\n')
            append("Active package: ").append(root?.packageName).append('\n')
            fun describe(node: AccessibilityNodeInfo?, depth: Int = 0) {
                if (node == null || depth > 20) return
                append("  ".repeat(depth)).append(node.packageName).append(" | ")
                    .append(node.className).append(" | ").append(node.text).append(" | ")
                    .append(node.contentDescription).append('\n')
                for (i in 0 until node.childCount) describe(node.getChild(i), depth + 1)
            }
            describe(root)
        }
        File(directory, "$name-active-window.txt").writeText(windowText)
        val bitmap = checkNotNull(automation.takeScreenshot()) { "Android did not return a screenshot" }
        try {
            File(directory, "$name.png").outputStream().use {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) { "Screenshot encoding failed" }
            }
        } finally { bitmap.recycle() }
        check(root?.packageName?.toString() == context.packageName) {
            "Screenshot blocked by another Android window ($activePackage). Raw screenshot and window tree retained; no dialog dismissed."
        }
    }
}
