package com.example.ui.workspace

import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Two levels only. Categories are stable; the selected category reveals a short outer fan. */
@Composable
fun WorkspaceRadialMenu(anchor: Offset, bounds: IntSize, availability: RadialAvailability,
    onDismiss: () -> Unit, onAction: (RadialAction) -> Unit) {
    var expanded by remember { mutableStateOf<RadialCategory?>(null) }
    val density = LocalDensity.current
    val d=density.density
    val compact=bounds.width/d < 2*RadialGeometry.EXTENT || bounds.height/d < 2*RadialGeometry.EXTENT || density.fontScale>1.3f
    fun back() { if(expanded!=null) expanded=null else onDismiss() }
    fun choose(hit:RadialHit) { when(hit) {
        is RadialHit.Category -> expanded=hit.value
        is RadialHit.Action -> if(availability.enabled(hit.value)) { onDismiss();onAction(hit.value) }
        RadialHit.Centre -> back()
        RadialHit.Outside -> onDismiss()
        RadialHit.Gap -> Unit
    } }
    BackHandler { back() }
    if(compact) {
        Box(Modifier.fillMaxSize().testTag("workspace-radial").pointerInput(Unit) { detectTapGestures { onDismiss() } }) {
            Surface(Modifier.align(Alignment.Center).widthIn(max=320.dp).fillMaxWidth().padding(12.dp)
                .testTag("radial-compact-panel"),
                tonalElevation=4.dp,shadowElevation=4.dp,shape=MaterialTheme.shapes.large) {
                Column(Modifier.padding(12.dp)) {
                    // Keep the exit/back control outside the scrolling actions. In a short canvas
                    // it must not disappear below the last row or need a scroll to become reachable.
                    Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                        Text("Commands", Modifier.weight(1f), style=MaterialTheme.typography.titleMedium)
                        TextButton(onClick={back()}, modifier=Modifier.testTag("radial-centre")) {
                            Text(if(expanded!=null) "Back" else "Close")
                        }
                    }
                    Column(Modifier.weight(1f, fill=false).verticalScroll(rememberScrollState())) {
                        if(expanded==null) RadialCategory.values().forEach { c ->
                            TextButton(onClick={expanded=c},modifier=Modifier.fillMaxWidth().testTag("radial-category-${c.name.lowercase()}")) { Text(c.label) }
                        } else RadialCommands.actions(expanded!!).forEach { a ->
                            TextButton(onClick={choose(RadialHit.Action(a))},enabled=availability.enabled(a),
                                modifier=Modifier.fillMaxWidth().testTag("radial-action-${a.name.lowercase()}")) {
                                Text(a.label + (availability.checked(a)?.let { if(it) " · On" else " · Off" } ?: ""))
                            }
                        }
                    }
                }
            }
        }
        return
    }
    val c=RadialGeometry.centre((anchor.x/d).toDouble(),(anchor.y/d).toDouble(),(bounds.width/d).toDouble(),(bounds.height/d).toDouble())
    val centre=Offset((c.first*d).toFloat(),(c.second*d).toFloat())
    Box(Modifier.fillMaxSize().testTag("workspace-radial").semantics { paneTitle="Design commands" }
        .pointerInput(centre,expanded,availability) {
            detectTapGestures { p -> choose(RadialGeometry.hit(((p.x-centre.x)/d).toDouble(),((p.y-centre.y)/d).toDouble(),expanded)) }
        }) {
        Canvas(Modifier.fillMaxSize()) {
            fun sector(angle:Double,inner:Float,outer:Float,sweep:Float,color:Color) {
                val start=angle.toFloat()-sweep/2
                val path=Path().apply {
                    arcTo(Rect(centre-Offset(outer*d,outer*d),centre+Offset(outer*d,outer*d)),start,sweep,true)
                    arcTo(Rect(centre-Offset(inner*d,inner*d),centre+Offset(inner*d,inner*d)),start+sweep,-sweep,false)
                    close()
                }
                drawPath(path,color)
            }
            fun label(text:String,angle:Double,radius:Double,color:Color,size:Float=12f) {
                val p=RadialGeometry.point(angle,radius)
                val lines=text.split('\n')
                drawIntoCanvas { canvas ->
                    val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color=color.toArgbInt();textAlign=Paint.Align.CENTER;textSize=size*d*density.fontScale }
                    lines.forEachIndexed { i,line -> canvas.nativeCanvas.drawText(line,centre.x+(p.first*d).toFloat(),
                        centre.y+(p.second*d).toFloat()+(i-(lines.size-1)/2f)*15*d-(paint.ascent()+paint.descent())/2,paint) }
                }
            }
            drawCircle(Color(0xFF242F32),34*d,centre)
            label(if(expanded==null) "×\nClose" else "‹\nBack",0.0,0.0,Color.White,11f)
            RadialCategory.values().forEach { cat ->
                sector(RadialGeometry.angle(cat),40f,98f,56f,if(expanded==cat) Color(0xFF256B70) else Color(0xFF303B3E))
                label(cat.label,RadialGeometry.angle(cat),70.0,Color.White)
            }
            expanded?.let { category ->
                RadialCommands.actions(category).forEachIndexed { i,a ->
                    val enabled=availability.enabled(a);val checked=availability.checked(a)
                    val angle=RadialGeometry.childAngle(category,i)
                    sector(angle,110f,162f,28f,if(checked==true) Color(0xFFD8EBE3) else Color(0xFFF0EFEB))
                    label(a.label.replace("Touch edit","Touch\nedit")+(checked?.let { if(it) "\nOn" else "\nOff" }?:""),
                        angle,136.0,if(enabled) Color(0xFF233C3F) else Color(0xFF8A9090),11f)
                }
            }
        }
        fun offset(angle:Double,radius:Double): IntOffset {
            val p=RadialGeometry.point(angle,radius)
            return IntOffset((centre.x+p.first*d-24*d).roundToInt(),(centre.y+p.second*d-24*d).roundToInt())
        }
        // Actual hit testing uses the sectors above. These matching nodes expose label/state/actions
        // to accessibility and provide geometry-based test targets, without a second pointer handler.
        Box(Modifier.offset { offset(0.0,0.0) }.size(48.dp).testTag("radial-centre").semantics {
            contentDescription=if(expanded==null) "Close commands" else "Back to main commands"
            role=Role.Button; onClick { back();true }
        })
        RadialCategory.values().forEach { cat ->
            Box(Modifier.offset { offset(RadialGeometry.angle(cat),70.0) }.size(48.dp)
                .testTag("radial-category-${cat.name.lowercase()}").semantics {
                    contentDescription=cat.label;role=Role.Button;selected=expanded==cat
                    onClick { expanded=cat;true }
                })
        }
        expanded?.let { cat -> RadialCommands.actions(cat).forEachIndexed { i,a ->
            Box(Modifier.offset { offset(RadialGeometry.childAngle(cat,i),136.0) }.size(48.dp)
                .testTag("radial-action-${a.name.lowercase()}").semantics {
                    contentDescription=a.label;role=Role.Button
                    if(!availability.enabled(a)) disabled()
                    availability.checked(a)?.let { stateDescription=if(it) "On" else "Off" }
                    onClick { if(availability.enabled(a)) { choose(RadialHit.Action(a));true } else false }
                })
        } }
    }
}
private fun Color.toArgbInt():Int = ((alpha*255).roundToInt() shl 24) or ((red*255).roundToInt() shl 16) or ((green*255).roundToInt() shl 8) or (blue*255).roundToInt()
