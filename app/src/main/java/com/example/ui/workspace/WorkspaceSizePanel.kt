package com.example.ui.workspace

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.design.*

/** A temporary property panel, not another permanent toolbar. Values commit only on Done. */
@Composable
fun WorkspaceSizePanel(obj:DesignObject, model:DesignWorkspaceViewModel, onDismiss:()->Unit) {
    BackHandler { onDismiss() }
    val size=DesignDimensions.measure(obj.boundary)
    Surface(Modifier.widthIn(max=320.dp).fillMaxWidth().testTag("workspace-size-panel"),
        shape=MaterialTheme.shapes.large,shadowElevation=5.dp,tonalElevation=3.dp) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Exact size",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                TextButton(onClick=onDismiss,modifier=Modifier.testTag("workspace-size-close")) { Text("Close") }
            }
            Text(if(size.rectangular) "Corner 1 stays fixed. Length and width follow the rectangle's own axes."
                else "Overall X/Y spans. Changing either scales the whole outline uniformly about vertex 1, preserving circular arcs.",
                style=MaterialTheme.typography.bodySmall)
            Text("Coping width stays unchanged.",style=MaterialTheme.typography.labelMedium)
            DimensionEntry(obj,SizeAxis.LENGTH,if(size.rectangular) "Length (ft / in)" else "X span (ft / in)",size.lengthMetres,model)
            DimensionEntry(obj,SizeAxis.WIDTH,if(size.rectangular) "Width (ft / in)" else "Y span (ft / in)",size.widthMetres,model)
            Text("Examples: 30, 30' 6\", 30' 6 1/2\". Keyboard Done commits one reversible change.",style=MaterialTheme.typography.labelSmall)
        }
    }
}
@Composable
private fun DimensionEntry(obj:DesignObject,axis:SizeAxis,label:String,metres:Double,model:DesignWorkspaceViewModel) {
    var text by remember(obj.id,obj.boundary,axis) { mutableStateOf(FeetInchesInput.format(metres)) }
    var dirty by remember(obj.id,obj.boundary,axis) { mutableStateOf(false) }
    var error by remember(obj.id,obj.boundary,axis) { mutableStateOf<String?>(null) }
    val focus=LocalFocusManager.current;val keyboard=LocalSoftwareKeyboardController.current
    OutlinedTextField(value=text,onValueChange={if(it.length<=48) { text=it;dirty=true };error=null},singleLine=true,
        label={Text(label)},enabled=!obj.locked,isError=error!=null,
        supportingText=error?.let { message -> { Text(message) } },
        modifier=Modifier.fillMaxWidth().testTag("workspace-size-${axis.name.lowercase()}"),
        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Text,imeAction=ImeAction.Done),
        keyboardActions=KeyboardActions(onDone=done@{
            if(!dirty) { focus.clearFocus();keyboard?.hide();return@done }
            try {
                val value=FeetInchesInput.parseMetres(text)
                model.execute(DesignCommand.SetDimension(obj.id,axis,value))
                val current=model.state.value.document?.objects?.firstOrNull { it.id==obj.id }
                if(current!=null) {
                    val actual=DesignDimensions.measure(current.boundary)
                    text=FeetInchesInput.format(if(axis==SizeAxis.LENGTH) actual.lengthMetres else actual.widthMetres)
                }
                error=model.state.value.message
                focus.clearFocus();keyboard?.hide()
            } catch(e:IllegalArgumentException) { error=e.message }
        }))
}
