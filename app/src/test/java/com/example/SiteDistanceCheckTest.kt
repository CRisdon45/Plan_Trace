package com.example

import com.example.data.DesignJsonCodec
import com.example.data.DesignWorkspaceStore
import com.example.export.DesignOutput
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
@Config(sdk = [35])
class SiteDistanceCheckTest {
    @get:Rule val temp = TemporaryFolder()
    private fun source() = SiteImage.unscaled(SiteImageAsset("a".repeat(64), 1200, 900))
        .calibrated(ImagePoint(200.0, 650.0), ImagePoint(800.0, 650.0), 40 * 0.3048)
    private fun checked(distanceFeet: Double = 20.0) = source().checked(
        ImagePoint(200.0, 700.0), ImagePoint(500.0, 700.0), distanceFeet * 0.3048)

    @Test fun `second reference compares distance without rescaling or repositioning the source`() {
        val original = source(); val result = checked()
        assertEquals(original.metresPerPixel, result.metresPerPixel, 0.0)
        assertEquals(original.topLeft, result.topLeft)
        assertEquals(original.calibration, result.calibration)
        assertEquals(20 * 0.3048, result.distanceReading!!.measuredMetres, 1e-10)
        assertEquals(0.0, result.distanceReading!!.differenceMetres, 1e-10)
    }
    @Test fun `a mismatch is retained as evidence not silently corrected or declared verified`() {
        val result = checked(30.0).distanceReading!!
        assertEquals(-10 * 0.3048, result.differenceMetres, 1e-10)
        assertEquals(-100.0 / 3, result.differencePercent, 1e-10)
        assertTrue(result.summary().contains("reference 30.00 ft"))
        assertTrue(result.summary().contains("-33.33%"))
        assertEquals(source().metresPerPixel, checked(30.0).metresPerPixel, 0.0)
    }
    @Test fun `reusing calibration endpoints is rejected in either direction`() {
        val s = source(); val c = s.calibration!!
        assertThrows(IllegalArgumentException::class.java) { s.checked(c.first, c.second, c.distanceMetres) }
        assertThrows(IllegalArgumentException::class.java) { s.checked(c.second, c.first, c.distanceMetres) }
    }
    @Test fun `uncalibrated outside tiny and nonfinite checks are rejected`() {
        val raw = SiteImage.unscaled(source().asset)
        assertThrows(IllegalArgumentException::class.java) { raw.checked(ImagePoint(20.0, 20.0), ImagePoint(200.0, 20.0), 4.0) }
        assertThrows(IllegalArgumentException::class.java) { source().checked(ImagePoint(-1.0, 20.0), ImagePoint(200.0, 20.0), 4.0) }
        assertThrows(IllegalArgumentException::class.java) { source().checked(ImagePoint(20.0, 20.0), ImagePoint(22.0, 20.0), 4.0) }
        assertThrows(IllegalArgumentException::class.java) { source().checked(ImagePoint(20.0, 20.0), ImagePoint(200.0, 20.0), Double.NaN) }
    }
    @Test fun `recalibration clears stale checking evidence while movement and visibility preserve it`() {
        val s = checked()
        assertEquals(s.distanceReading, s.moved(9.0, -12.0).distanceReading)
        assertEquals(s.distanceCheck, s.copy(visible = false).distanceCheck)
        assertNull(s.calibrated(ImagePoint(100.0, 100.0), ImagePoint(500.0, 100.0), 50 * 0.3048).distanceCheck)
    }
    @Test fun `check command undo redo preserve all proposed objects and exact source placement`() {
        val original = ProjectDesign("check", DesignFixtures.document().objects, siteImage = source())
        val session = DesignSession(original)
        val next = session.execute(DesignCommand.SetSiteImage(checked(), original.siteImage))
        assertEquals(original.objects, next.objects)
        assertEquals(original.siteImage, session.undo().siteImage)
        assertEquals(next.siteImage, session.redo().siteImage)
        assertEquals(original.objects, session.document.objects)
    }
    @Test fun `saved source check roundtrips and derived readings are not duplicate authority`() {
        val d = ProjectDesign("check", DesignFixtures.document().objects, siteImage = checked(30.0))
        val json = DesignJsonCodec.encode(d)
        assertEquals(d, DesignJsonCodec.decode(json))
        assertFalse(json.contains("differencePercent"))
        val store = DesignWorkspaceStore(File(temp.root, "draft.json"))
        store.save(d)
        assertEquals(d, DesignWorkspaceStore(File(temp.root, "draft.json")).load())
    }
    @Test fun `older source documents stay unchecked and cannot masquerade as containing new evidence`() {
        val old = ProjectDesign("old", siteImage = source())
        val v3 = JSONObject(DesignJsonCodec.encode(old)).put("version", 3)
        assertEquals(old, DesignJsonCodec.decode(v3.toString()))
        val falselyOld = JSONObject(DesignJsonCodec.encode(ProjectDesign("bad", siteImage = checked()))).put("version", 3)
        assertThrows(IllegalArgumentException::class.java) { DesignJsonCodec.decode(falselyOld.toString()) }
    }
    @Test fun `corrupt check stops the whole read rather than stripping evidence`() {
        val root = JSONObject(DesignJsonCodec.encode(ProjectDesign("bad", siteImage = checked())))
        root.getJSONObject("siteImage").getJSONObject("distanceCheck").put("distanceMetres", -1.0)
        assertThrows(IllegalArgumentException::class.java) { DesignJsonCodec.decode(root.toString()) }
    }
    @Test fun `export notice includes disagreement without changing objects or implying field verification`() {
        val doc = ProjectDesign("export", DesignFixtures.document().objects, siteImage = checked(30.0))
        val notice = DesignOutput.drawing(doc).elements.filterIsInstance<TextElement>().single { it.id == "site-source-notice" }
        assertTrue(notice.text.contains("site accuracy unverified"))
        assertTrue(notice.text.contains("-10.00 ft (-33.33%)"))
        assertEquals(DesignFixtures.document().objects, doc.objects)
    }
}
