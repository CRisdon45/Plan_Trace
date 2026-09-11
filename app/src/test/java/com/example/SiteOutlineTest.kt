package com.example

import com.example.data.DesignJsonCodec
import com.example.data.DesignWorkspaceStore
import com.example.export.DesignOutput
import com.example.export.DesignOutputSettings
import com.example.model.TextElement
import com.example.model.design.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class SiteOutlineTest {
    @get:Rule val temp=TemporaryFolder()
    private fun source()=SiteImage(SiteImageAsset("a".repeat(64),1000,1000),DesignPoint(0.0,20.0),0.02,
        ImageCalibration(ImagePoint(0.0,0.0),ImagePoint(500.0,0.0),10.0))
    private fun draft(role:SiteOutlineRole=SiteOutlineRole.HOUSE)=SiteOutlineDraft(role,source(),"site",0,"outline")
    private fun ready(role:SiteOutlineRole=SiteOutlineRole.HOUSE):SiteOutlineDraft {
        var d=draft(role)
        listOf(100.0 to 100.0,600.0 to 100.0,600.0 to 600.0,100.0 to 600.0).forEach { (x,y) -> d=d.append(source().toWorld(ImagePoint(x,y))) }
        return d
    }
    @Test fun `closed site object keeps trace provenance and is protected by default`() {
        val obj=ready().finish()
        assertEquals(DesignObjectKind.SITE_OUTLINE,obj.kind);assertEquals(GeometryConfidence.TRACED,obj.confidence)
        assertTrue(obj.locked);assertNull(obj.coping);assertEquals(source().asset.sha256,obj.sourceReference)
        assertEquals(source(),obj.siteTrace!!.source);assertEquals(4,obj.boundary.nodes.size)
    }
    @Test fun `corners stay transient and closure is one reversible project addition`() {
        val original=ProjectDesign("site",DesignFixtures.document().objects,siteImage=source())
        val d=ready(); val session=DesignSession(original)
        assertFalse(session.canUndo)
        val committed=session.execute(DesignCommand.Add(d.finish()))
        assertEquals(1L,committed.revision);assertEquals(original.objects,committed.objects.dropLast(1))
        assertEquals(original.objects,session.undo().objects)
        assertEquals(committed.objects,session.redo().objects)
    }
    @Test fun `back a corner never modifies earlier draft instances`() {
        val full=ready();val shortened=full.back()
        assertEquals(4,full.points.size);assertEquals(3,shortened.points.size)
        assertEquals(full.objectId,shortened.objectId)
        assertEquals(0,draft().back().points.size)
        assertThrows(IllegalArgumentException::class.java) { draft().finish() }
    }
    @Test fun `right angles follow the first segment not an assumed screen axis`() {
        var d=draft().append(DesignPoint(2.0,18.0)).append(DesignPoint(6.0,14.0))
        val candidate=d.candidate(DesignPoint(9.0,15.0),true,false)
        val dx=candidate.x-6.0;val dy=candidate.y-14.0
        assertEquals(dx,dy,1e-9)
        assertEquals(DesignPoint(9.0,15.0),d.candidate(DesignPoint(9.0,15.0),false,false))
    }
    @Test fun `snapping is explicit and candidate generation does not add corners`() {
        val d=draft();val p=DesignPoint(1.1,1.7)
        assertEquals(p,d.candidate(p,false,false));assertEquals(GridAssist.snapPoint(p),d.candidate(p,false,true))
        assertTrue(d.points.isEmpty())
    }
    @Test fun `concave house outline is accepted and crossing outline is not repaired`() {
        var concave=draft()
        listOf(2.0 to 18.0,8.0 to 18.0,8.0 to 16.0,12.0 to 16.0,12.0 to 10.0,2.0 to 10.0)
            .forEach { (x,y) -> concave=concave.append(DesignPoint(x,y)) }
        assertEquals(6,concave.finish().boundary.nodes.size)
        var crossing=draft()
        listOf(2.0 to 18.0,8.0 to 12.0,2.0 to 12.0,8.0 to 18.0)
            .forEach { (x,y) -> crossing=crossing.append(DesignPoint(x,y)) }
        assertThrows(IllegalArgumentException::class.java) { crossing.finish() }
    }
    @Test fun `outside and duplicate corners leave the draft intact`() {
        val d=draft().append(DesignPoint(2.0,18.0))
        assertThrows(IllegalArgumentException::class.java) { d.append(DesignPoint(-1.0,18.0)) }
        assertThrows(IllegalArgumentException::class.java) { d.append(DesignPoint(2.0,18.0)) }
        assertEquals(1,d.points.size)
    }
    @Test fun `uncalibrated or hidden source cannot start a measured trace`() {
        assertThrows(IllegalArgumentException::class.java) { SiteOutlineDraft(SiteOutlineRole.HOUSE,SiteImage.unscaled(source().asset),"s",0) }
        assertThrows(IllegalArgumentException::class.java) { SiteOutlineDraft(SiteOutlineRole.HOUSE,source().copy(visible=false),"s",0) }
    }
    @Test fun `all supported mutation routes respect lock until explicitly unlocked`() {
        val obj=ready().finish();val session=DesignSession(ProjectDesign("site",listOf(obj)))
        val commands=listOf(DesignCommand.Remove(obj.id),DesignCommand.Translate(obj.id,1.0,0.0),
            DesignCommand.MoveVertex(obj.id,obj.boundary.nodes[0].vertexId,DesignPoint(3.0,18.0)),
            DesignCommand.SetDimension(obj.id,SizeAxis.LENGTH,9.0),DesignCommand.ReplaceBoundary(obj.id,DesignFixtures.rectangle()))
        commands.forEach { assertThrows(IllegalArgumentException::class.java) { session.execute(it) } }
        assertFalse(session.canUndo)
        session.execute(DesignCommand.SetLocked(obj.id,false))
        val changed=session.execute(DesignCommand.Translate(obj.id,1.0,0.0)).objectById(obj.id)
        assertTrue(changed.siteTrace!!.adjusted);assertEquals(obj.siteTrace!!.source,changed.siteTrace!!.source)
        assertEquals(obj.boundary.nodes.map{it.edgeId},changed.boundary.nodes.map{it.edgeId})
        assertEquals(obj.copy(locked=false),session.undo().objectById(obj.id))
        assertEquals(obj,session.undo().objectById(obj.id))
    }
    @Test fun `invalid edit does not damage the unlocked trace or its history`() {
        val obj=ready().finish().copy(locked=false);val session=DesignSession(ProjectDesign("site",listOf(obj)))
        assertThrows(IllegalArgumentException::class.java) { session.execute(DesignCommand.MoveVertex(obj.id,obj.boundary.nodes[1].vertexId,DesignPoint(1.0,16.0))) }
        assertEquals(obj,session.document.objectById(obj.id));assertFalse(session.canUndo)
        assertThrows(IllegalArgumentException::class.java) { session.execute(DesignCommand.ChangeBulge(obj.id,obj.boundary.nodes[0].edgeId,0.2)) }
    }
    @Test fun `source movement recalibration and removal flag provenance without moving objects`() {
        val obj=ready().finish();val original=ProjectDesign("site",listOf(obj),siteImage=source())
        val session=DesignSession(original)
        val moved=source().moved(1.0,0.0)
        val next=session.execute(DesignCommand.SetSiteImage(moved,source()))
        assertEquals(original.objects,next.objects);assertEquals(TraceRegistration.CHANGED,obj.siteTrace!!.registration(next.siteImage))
        assertEquals(TraceRegistration.CURRENT,obj.siteTrace!!.registration(session.undo().siteImage))
        assertEquals(TraceRegistration.CURRENT,obj.siteTrace!!.registration(source().copy(visible=false)))
        assertEquals(TraceRegistration.UNAVAILABLE,obj.siteTrace!!.registration(null))
        val scaled=source().calibrated(ImagePoint(0.0,0.0),ImagePoint(500.0,0.0),12.0)
        assertEquals(TraceRegistration.CHANGED,obj.siteTrace!!.registration(scaled))
    }
    @Test fun `source provenance roundtrips after the source is detached`() {
        val obj=ready().finish();val doc=ProjectDesign("site",listOf(obj))
        val json=DesignJsonCodec.encode(doc);assertEquals(doc,DesignJsonCodec.decode(json))
        assertFalse(json.contains("source-file-name"))
        val store=DesignWorkspaceStore(File(temp.root,"draft.json"));store.save(doc)
        assertEquals(doc,DesignWorkspaceStore(File(temp.root,"draft.json")).load())
    }
    @Test fun `older documents do not acquire site identities or accept unsupported provenance`() {
        val old=ProjectDesign("old",DesignFixtures.document().objects,siteImage=source())
        val json=JSONObject(DesignJsonCodec.encode(old)).put("version",4).toString()
        assertEquals(old,DesignJsonCodec.decode(json))
        val falseOld=JSONObject(DesignJsonCodec.encode(ProjectDesign("site",listOf(ready().finish())))).put("version",4)
        assertThrows(IllegalArgumentException::class.java) { DesignJsonCodec.decode(falseOld.toString()) }
    }
    @Test fun `corrupt or falsely verified trace metadata rejects the complete document`() {
        val root=JSONObject(DesignJsonCodec.encode(ProjectDesign("site",listOf(ready().finish()))))
        root.getJSONArray("objects").getJSONObject(0).put("confidence","FIELD_VERIFIED")
        assertThrows(IllegalArgumentException::class.java) { DesignJsonCodec.decode(root.toString()) }
    }
    @Test fun `traced outlines retain truthful export notice with the raster hidden`() {
        val obj=ready().finish();val doc=ProjectDesign("site",listOf(obj),siteImage=source().moved(1.0,0.0))
        val output=DesignOutput.drawing(doc,DesignOutputSettings(includeMeasurements=false,includeSourceImage=false))
        val text=output.elements.filterIsInstance<TextElement>().single().text
        assertTrue(text.contains("not field-verified"));assertTrue(text.contains("source registration changed"))
        assertEquals(doc.objects.single(),obj)
    }
}
