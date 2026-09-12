package com.example

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.DesignJsonCodec
import com.example.data.DesignWorkspaceStore
import com.example.export.DesignOutput
import com.example.model.PolylineElement
import com.example.model.SurfaceMaterial
import com.example.model.design.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CopingDocumentTest {
    @get:Rule val temp=TemporaryFolder()
    private fun document()=ProjectDesign("synthetic-coping",listOf(
        DesignObject("pool","Curved pool",DesignObjectKind.POOL,DesignFixtures.organic(),coping=CopingSpec())))
    @Test fun `saved intent roundtrips and regenerates the same band without storing sampled vertices`() {
        val d=document();val json=DesignJsonCodec.encode(d);val r=DesignJsonCodec.decode(json)
        assertEquals(d,r)
        assertEquals(d.objectById("pool").copingFootprint!!.outerBoundary,r.objectById("pool").copingFootprint!!.outerBoundary)
        assertFalse(json.contains("outerBoundary"));assertFalse(json.contains("coping-outer"))
        assertEquals(6,JSONObject(json).getJSONArray("objects").getJSONObject(0).getJSONArray("nodes").length())
    }
    @Test fun `version one files remain outline only without implicit promotion`() {
        val old=DesignFixtures.document()
        val json=JSONObject(DesignJsonCodec.encode(old)).put("version",1).toString()
        assertEquals(old,DesignJsonCodec.decode(json))
        assertTrue(DesignJsonCodec.decode(json).objects.all { it.coping==null })
    }
    @Test fun `unsupported or corrupt coupling fails the entire decode`() {
        val root=JSONObject(DesignJsonCodec.encode(document()))
        root.getJSONArray("objects").getJSONObject(0).getJSONObject("coping").put("generatorVersion",2)
        assertThrows(IllegalArgumentException::class.java) { DesignJsonCodec.decode(root.toString()) }
        root.getJSONArray("objects").getJSONObject(0).getJSONObject("coping").put("generatorVersion",1).put("widthMetres",-1.0)
        assertThrows(IllegalArgumentException::class.java) { DesignJsonCodec.decode(root.toString()) }
    }
    @Test fun `invalid reshaping cannot replace a previously saved valid pool`() {
        val store=DesignWorkspaceStore(File(temp.root,"draft.json"));val s=DesignSession(document())
        store.save(s.document);val before=File(temp.root,"draft.json").readBytes()
        assertThrows(IllegalArgumentException::class.java) {
            s.execute(DesignCommand.MoveVertex("pool","vertex-1",DesignPoint(-1.0,2.0)))
        }
        store.save(s.document)
        assertArrayEquals(before,File(temp.root,"draft.json").readBytes())
    }
    @Test fun `width and derived footprint survive disk save after edit and undo`() {
        val store=DesignWorkspaceStore(File(temp.root,"draft.json"));val s=DesignSession(document())
        s.execute(DesignCommand.SetCoping("pool",CopingSpec(16*CopingSpec.INCH)));store.save(s.document)
        assertEquals(s.document,store.load())
        assertEquals(s.document.objectById("pool").copingFootprint!!.outerBoundary,store.load()!!.objectById("pool").copingFootprint!!.outerBoundary)
        s.undo();store.save(s.document);assertEquals(12.0,store.load()!!.objectById("pool").coping!!.widthInches,1e-9)
    }
    @Test fun `actual output includes coping and water through the existing material renderer`() = runBlocking {
        val doc=document();val elements=DesignOutput.drawing(doc).elements.filterIsInstance<PolylineElement>()
        assertEquals(listOf("pool:coping-outer","pool:outline"),elements.map { it.id })
        assertEquals(listOf(SurfaceMaterial.PAVING,SurfaceMaterial.WATER),elements.map { it.material })
        assertEquals(listOf(1.5f,2.35f),elements.map { it.strokeWidth })
        val context=ApplicationProvider.getApplicationContext<Context>()
        val a=DesignOutput.png(context,doc,1000,700)!!.readBytes()
        val b=DesignOutput.png(context,doc,1000,700)!!.readBytes()
        assertArrayEquals(a,b)
        val bitmap=BitmapFactory.decodeByteArray(a,0,a.size)
        val pixels=IntArray(bitmap.width*bitmap.height);bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
        assertTrue(pixels.count { it==SurfaceMaterial.PAVING.fill.toInt() }>100)
        assertTrue(pixels.count { it==SurfaceMaterial.WATER.fill.toInt() }>1000)
        bitmap.recycle()
        File("build/reports/design-geometry").mkdirs()
        File("build/reports/design-geometry/connected-coping.png").writeBytes(a)
        File("build/reports/design-geometry/connected-coping.json").writeText(DesignJsonCodec.encode(doc))
    }
}
