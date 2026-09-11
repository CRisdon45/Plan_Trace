package com.example

import com.example.model.design.*
import org.junit.Assert.*
import org.junit.Test

class SiteRegistrationTest {
    private fun source() = SiteImage.unscaled(SiteImageAsset("a".repeat(64),1200,900))
    @Test fun `image down coordinates roundtrip through metric up coordinates`() {
        val s=source();val pixel=ImagePoint(205.2,650.4);val back=s.toImage(s.toWorld(pixel))
        assertEquals(pixel.x,back.x,1e-9);assertEquals(pixel.y,back.y,1e-9)
        assertTrue(s.toWorld(ImagePoint(0.0,1.0)).y<s.topLeft.y)
        assertNull(s.calibration)
    }
    @Test fun `known distance calibration preserves the first world anchor and records evidence`() {
        val s=source();val a=ImagePoint(250.0,650.0);val b=ImagePoint(850.0,650.0)
        val calibrated=s.calibrated(a,b,40*0.3048)
        assertEquals(s.toWorld(a),calibrated.toWorld(a))
        assertEquals(40*0.3048,calibrated.toWorld(a).distanceTo(calibrated.toWorld(b)),1e-9)
        assertEquals(a,calibrated.calibration!!.first);assertEquals(s.asset,calibrated.asset)
    }
    @Test fun `invalid calibration and mismatched saved scale fail without changing source`() {
        val s=source()
        assertThrows(IllegalArgumentException::class.java) { s.calibrated(ImagePoint(0.0,0.0),ImagePoint(3.0,0.0),5.0) }
        assertThrows(IllegalArgumentException::class.java) { s.calibrated(ImagePoint(-10.0,0.0),ImagePoint(30.0,0.0),5.0) }
        assertThrows(IllegalArgumentException::class.java) { s.copy(metresPerPixel=Double.NaN) }
        val c=s.calibrated(ImagePoint(100.0,100.0),ImagePoint(900.0,100.0),6.0)
        assertThrows(IllegalArgumentException::class.java) { c.copy(metresPerPixel=c.metresPerPixel*2) }
        assertNull(s.calibration)
    }
    @Test fun `source attachment calibration and undo never rescale or reidentify proposed objects`() {
        val initial=DesignFixtures.document();val session=DesignSession(initial);val image=source()
        session.execute(DesignCommand.SetSiteImage(image,null))
        val calibrated=image.calibrated(ImagePoint(100.0,100.0),ImagePoint(900.0,100.0),6.0)
        session.execute(DesignCommand.SetSiteImage(calibrated,image))
        assertEquals(initial.objects,session.document.objects)
        assertEquals(image,session.undo().siteImage)
        assertEquals(calibrated,session.redo().siteImage)
        session.undo();session.undo();assertNull(session.document.siteImage)
        assertEquals(initial.objects,session.document.objects)
    }
    @Test fun `stale source operation is rejected atomically without consuming redo`() {
        val s=source();val session=DesignSession(ProjectDesign("site",siteImage=s))
        session.execute(DesignCommand.SetSiteImage(s.moved(1.0,0.0),s));session.undo()
        val before=session.document
        assertThrows(IllegalArgumentException::class.java) { session.execute(DesignCommand.SetSiteImage(s,null)) }
        assertEquals(before,session.document);assertTrue(session.canRedo)
    }
    @Test fun `ordinary geometry commands preserve source registration and exact pixels`() {
        val initial=DesignFixtures.document();val s=source()
        val doc=ProjectDesign(initial.id,initial.objects,0,s)
        val moved=DesignCommands.apply(doc,DesignCommand.Translate("pool",3.0,2.0))
        assertEquals(s,moved.siteImage)
        val session=DesignSession(moved);session.execute(DesignCommand.SetSiteImage(s.moved(-2.0,1.0),s))
        assertEquals(moved.objects,session.document.objects)
        assertEquals(s.metresPerPixel,session.document.siteImage!!.metresPerPixel,0.0)
    }
    @Test fun `source pixels cannot be resolved using a path or unsupported image dimensions`() {
        assertThrows(IllegalArgumentException::class.java) { SiteImageAsset("../workspace.json",100,100) }
        assertThrows(IllegalArgumentException::class.java) { SiteImageAsset("a".repeat(64),100000,100000) }
    }
    @Test fun `fit includes the registered image only when it is visible`() {
        val initial=DesignFixtures.document();val s=source().moved(50.0,0.0)
        val d=ProjectDesign(initial.id,initial.objects,0,s)
        val fit=DesignViewport.fit(d,1200.0,700.0)
        s.corners().forEach { val p=fit.toScreen(it);assertTrue(p.x in 0.0..1200.0 && p.y in 0.0..700.0) }
        assertNotEquals(fit,DesignViewport.fit(ProjectDesign(d.id,d.objects,0,s.copy(visible=false)),1200.0,700.0))
    }
}
