package com.example

import com.example.data.DesignJsonCodec
import com.example.data.DesignWorkspaceStore
import com.example.export.DesignOutput
import com.example.export.DesignOutputSettings
import com.example.model.PolylineElement
import com.example.model.design.*
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
class StraightSideDocumentTest {
    @get:Rule val temp=TemporaryFolder()
    private fun document()=ProjectDesign("side",listOf(DesignObject("p","Pool",DesignObjectKind.POOL,DesignFixtures.rectangle(),coping=CopingSpec())))
    @Test fun `whole-side result saves and reopens exact geometry with no interaction state`() {
        val next=DesignCommands.apply(document(),DesignCommand.MoveSide("p","edge-1",-1.0))
        val json=DesignJsonCodec.encode(next)
        assertFalse(json.contains("sideEditing"));assertEquals(next,DesignJsonCodec.decode(json))
        val store=DesignWorkspaceStore(File(temp.root,"draft.json"));store.save(next)
        assertEquals(next,DesignWorkspaceStore(File(temp.root,"draft.json")).load())
        assertEquals(next.objectById("p").copingFootprint!!.outerBoundary,store.load()!!.objectById("p").copingFootprint!!.outerBoundary)
    }
    @Test fun `output uses moved waterline and regenerated coping without writing samples back`() {
        val next=DesignCommands.apply(document(),DesignCommand.MoveSide("p","edge-1",-1.0))
        val json=DesignJsonCodec.encode(next)
        val elements=DesignOutput.drawing(next,DesignOutputSettings(includeMeasurements=false)).elements.filterIsInstance<PolylineElement>()
        assertEquals(1100f,elements.single { it.id=="p:outline" }.points.maxOf { it.x },0.001f)
        assertTrue(elements.single { it.id=="p:coping-outer" }.points.maxOf { it.x }>1100f)
        assertEquals(json,DesignJsonCodec.encode(next))
    }
}
