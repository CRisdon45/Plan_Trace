package com.example.ui.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.model.design.LiveMeasure
import kotlin.math.roundToInt

/** A measured overlay, not a canvas child that changes available drawing space. No input handlers. */
@Composable
fun LiveMeasureOverlay(measure: LiveMeasure, target: Offset, modifier: Modifier = Modifier) {
    Layout(modifier = modifier.fillMaxSize(), content = {
        Box((if(measure.plain && !measure.invalid) Modifier else Modifier.shadow(3.dp,MaterialTheme.shapes.medium)
            .background(if(measure.invalid) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,MaterialTheme.shapes.medium))
            .testTag("drawing-live-measure").semantics {
                contentDescription = (if (measure.invalid) listOf("Invalid edit") + measure.lines else measure.lines)
                    .joinToString(". ")
            }) {
            Column(Modifier.padding(horizontal=12.dp,vertical=8.dp)) {
                measure.lines.forEachIndexed { i,line -> Text(line,
                    style=if(i==0) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
                    color=if(measure.invalid) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface) }
            }
        }
    }) { children, constraints ->
        val margin=8.dp.roundToPx();val gap=22.dp.roundToPx()
        val width=constraints.maxWidth;val height=constraints.maxHeight
        val badge=children.single().measure(Constraints(maxWidth=minOf(270.dp.roundToPx(),(width-2*margin).coerceAtLeast(1)),
            maxHeight=(height-2*margin).coerceAtLeast(1)))
        val preferredX=if(target.x+gap+badge.width <= width-margin) target.x+gap else target.x-gap-badge.width
        val preferredY=if(target.y-gap-badge.height >= margin) target.y-gap-badge.height else target.y+gap
        val x=preferredX.roundToInt().coerceIn(margin.coerceAtMost(width-badge.width),(width-badge.width-margin).coerceAtLeast(margin.coerceAtMost(width-badge.width)))
        val y=preferredY.roundToInt().coerceIn(margin.coerceAtMost(height-badge.height),(height-badge.height-margin).coerceAtLeast(margin.coerceAtMost(height-badge.height)))
        layout(width,height) { badge.place(x,y) }
    }
}
