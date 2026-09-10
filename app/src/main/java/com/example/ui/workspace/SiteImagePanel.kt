package com.example.ui.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.model.design.*

/** Source setup is deliberately separate from proposed geometry. Presets avoid obligatory typing.
 * It is a first scale-reference workflow, not the final precision interaction design.
 */
@Composable
fun SiteImagePanel(state: WorkspaceState, model: DesignWorkspaceViewModel, onImport: () -> Unit,
                   onClose: () -> Unit, onTool: (SiteTool) -> Unit) {
    val source = state.document?.siteImage
    Surface(Modifier.widthIn(max=360.dp).fillMaxWidth().testTag("workspace-site-panel"),
        shape=MaterialTheme.shapes.large, shadowElevation=5.dp, tonalElevation=3.dp) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Site image",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                TextButton(onClick=onClose,modifier=Modifier.testTag("site-close")) { Text("Close") }
            }
            if(source==null) {
                Text("Bring in a PNG/JPEG plan image or overhead screenshot. The app keeps its own copy. PDF pages and automatic site tracing come later.")
                Button(onClick=onImport,enabled=!state.siteImporting,modifier=Modifier.testTag("site-import")) {
                    Text(if(state.siteImporting) "Importing…" else "Choose image")
                }
            } else {
                Text(if(source.calibration==null) "Scale not set. The image is reference only."
                    else "Scale set from a reference distance. Site accuracy is still unverified.", modifier=Modifier.testTag("site-scale-status"))
                Text("Ordinary design gestures cannot move this source. Site actions change only the image, never your existing pool or landscape objects.",style=MaterialTheme.typography.bodySmall)
                state.siteError?.let { Text(it,color=MaterialTheme.colorScheme.error) }
                if(state.referencePoints.size==2) {
                    Text("Distance between your two marks:")
                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        listOf(10,20,30,40).forEach { feet ->
                            OutlinedButton(onClick={model.calibrateSite(feet*DesignDimensions.FOOT)},contentPadding=PaddingValues(9.dp),
                                modifier=Modifier.testTag("site-distance-$feet")) { Text("$feet′") }
                        }
                    }
                    var custom by remember { mutableStateOf(false) }
                    TextButton(onClick={custom=!custom}) { Text("Other known distance") }
                    if(custom) {
                        var value by remember { mutableStateOf("") }
                        OutlinedTextField(value=value,onValueChange={value=it.take(48)},label={Text("Reference distance (ft / in)")},singleLine=true,
                            keyboardOptions=KeyboardOptions(imeAction=ImeAction.Done),keyboardActions=KeyboardActions(onDone={
                                try { model.calibrateSite(FeetInchesInput.parseMetres(value)) }
                                catch(e:IllegalArgumentException) { model.feedback(e.message) }
                            }))
                    }
                    Text("The first mark stays in place. Existing design dimensions do not change.",style=MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(onClick={onTool(SiteTool.CALIBRATE)},enabled=state.siteBitmap!=null,modifier=Modifier.testTag("site-calibrate")) { Text("Mark a known distance") }
                OutlinedButton(onClick={onTool(SiteTool.MOVE)},enabled=state.siteBitmap!=null,modifier=Modifier.testTag("site-move")) { Text("Move source only") }
                TextButton(onClick={model.execute(DesignCommand.SetSiteImage(source.copy(visible=!source.visible),source))},modifier=Modifier.testTag("site-visibility")) { Text(if(source.visible) "Hide source" else "Show source") }
                TextButton(onClick={model.stopSiteTool();model.execute(DesignCommand.SetSiteImage(null,source))},modifier=Modifier.testTag("site-remove")) { Text("Remove source · Undo restores it") }
            }
            state.message?.let { Text(it,style=MaterialTheme.typography.bodySmall) }
        }
    }
}
