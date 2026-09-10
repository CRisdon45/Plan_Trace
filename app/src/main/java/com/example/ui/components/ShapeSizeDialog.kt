package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.RectangleElement
import com.example.model.ScaleCalibration
import com.example.model.withExactSize
import java.util.Locale

@Composable
fun ShapeSizeDialog(rectangle: RectangleElement, scale: ScaleCalibration, onDismiss: () -> Unit,
    onApply: (RectangleElement) -> Unit) {
    val bounds = rectangle.boundingBox()
    val factor = if (scale.isCalibrated) scale.pixelsPerUnit else 1f
    val unit = if (scale.isCalibrated) scale.unit else "px"
    var width by remember(rectangle.id) { mutableStateOf(String.format(Locale.US, "%.3f", bounds.width() / factor)) }
    var height by remember(rectangle.id) { mutableStateOf(String.format(Locale.US, "%.3f", bounds.height() / factor)) }
    fun parsed(text: String) = text.trim().toFloatOrNull()?.takeIf { it.isFinite() && it > 0f }
    val proposed = runCatching { rectangle.withExactSize(parsed(width) ?: error("width"), parsed(height) ?: error("height"), scale) }.getOrNull()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Rectangle size") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Enter decimal $unit. The top-left corner stays fixed.")
            OutlinedTextField(value = width, onValueChange = { width = it }, label = { Text("Width ($unit)") },
                singleLine = true, isError = parsed(width) == null, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Height ($unit)") },
                singleLine = true, isError = parsed(height) == null, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            if (!scale.isCalibrated) Text("Calibrate the plan to enter real-world dimensions.")
        }
    }, confirmButton = {
        TextButton(enabled = proposed != null, onClick = { proposed?.let(onApply); onDismiss() }) { Text("Apply size") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
