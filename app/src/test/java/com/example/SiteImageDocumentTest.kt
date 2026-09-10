package com.example

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.export.*
import com.example.model.TextElement
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
class SiteImageDocumentTest {
    @get:Rule val temp=TemporaryFolder()
    private fun png(): ByteArray {
        val bitmap=Bitmap.createBitmap(400,300,Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply { drawColor(0xFFDDD8C8.toInt());drawRect(50f,190f,70f,210f,Paint().apply{color=0xFFCC4422.toInt()}) }
        val bytes=java.io.ByteArrayOutputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it);it.toByteArray() }
        bitmap.recycle();return bytes
    }
    private fun store()=SiteImageStore(File(temp.root,"assets"))
    @Test fun `image intake owns exact normalized pixels and retains original without user filenames`() {
        val store=store();val bytes=png();val asset=store.ingest(bytes.inputStream())
        assertEquals(400,asset.width);assertEquals(300,asset.height)
        val bitmap=store.load(asset);assertEquals(0xFFCC4422.toInt(),bitmap.getPixel(60,200));bitmap.recycle()
        assertEquals(asset,store.ingest(bytes.inputStream()))
        assertTrue(File(temp.root,"assets").listFiles()!!.any { it.extension=="source" && it.readBytes().contentEquals(bytes) })
        assertTrue(File(temp.root,"assets").listFiles()!!.none { it.extension=="tmp" })
    }
    @Test fun `jpeg orientation is normalized before reference pixels are assigned`() {
        val input=Bitmap.createBitmap(80,40,Bitmap.Config.ARGB_8888)
        input.eraseColor(0xFF123456.toInt());val f=File(temp.root,"rotated.jpg")
        f.outputStream().use { input.compress(Bitmap.CompressFormat.JPEG,95,it) };input.recycle()
        ExifInterface(f.path).apply { setAttribute(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_ROTATE_90.toString());saveAttributes() }
        val asset=f.inputStream().use { store().ingest(it) }
        assertEquals(40,asset.width);assertEquals(80,asset.height)
    }
    @Test fun `corrupt image leaves existing asset unchanged and clears intake temporary files`() {
        val s=store();val a=s.ingest(png().inputStream());val before=s.file(a).readBytes()
        assertThrows(IllegalArgumentException::class.java) { s.ingest("not a picture".byteInputStream()) }
        assertArrayEquals(before,s.file(a).readBytes())
        assertTrue(File(temp.root,"assets").listFiles()!!.none { it.extension=="tmp" })
    }
    @Test fun `missing and checksum damaged sources are not silently replaced`() {
        val s=store();val a=s.ingest(png().inputStream())
        s.file(a).writeText("damaged")
        assertThrows(IllegalArgumentException::class.java) { s.load(a) }
        assertThrows(IllegalArgumentException::class.java) { s.ingest(png().inputStream()) }
        s.file(a).delete();assertThrows(IllegalArgumentException::class.java) { s.load(a) }
    }
    @Test fun `registration and evidence survive versioned JSON and the real atomic draft`() {
        val a=store().ingest(png().inputStream());val image=SiteImage.unscaled(a).calibrated(ImagePoint(30.0,250.0),ImagePoint(370.0,250.0),6.096)
        val initial=DesignFixtures.document();val doc=ProjectDesign(initial.id,initial.objects,3,image)
        val json=DesignJsonCodec.encode(doc)
        assertEquals(doc,DesignJsonCodec.decode(json))
        assertFalse(json.contains("file://"));assertFalse(json.contains("source"+".png"))
        val draft=DesignWorkspaceStore(File(temp.root,"workspace.json"));draft.save(doc)
        assertEquals(doc,draft.load())
        val bad=JSONObject(json);bad.getJSONObject("siteImage").put("metresPerPixel",3.0)
        assertThrows(IllegalArgumentException::class.java) { DesignJsonCodec.decode(bad.toString()) }
    }
    @Test fun `old documents stay source free and cannot conceal newer registration`() {
        val original=DesignFixtures.document()
        for(v in 1..2) assertEquals(original,DesignJsonCodec.decode(JSONObject(DesignJsonCodec.encode(original)).put("version",v).toString()))
        val a=store().ingest(png().inputStream())
        val doc=ProjectDesign("new",siteImage=SiteImage.unscaled(a))
        val json=JSONObject(DesignJsonCodec.encode(doc)).put("version",2)
        assertThrows(IllegalArgumentException::class.java) { DesignJsonCodec.decode(json.toString()) }
    }
    @Test fun `actual PNG shares image placement with geometry and refuses missing requested pixels`() = runBlocking {
        val base=ApplicationProvider.getApplicationContext<Context>()
        val context=object: ContextWrapper(base) {
            override fun getFilesDir()=File(temp.root,"files").apply{mkdirs()}
            override fun getCacheDir()=File(temp.root,"cache").apply{mkdirs()}
        }
        val store=SiteImageStore.inFiles(context.filesDir);val asset=store.ingest(png().inputStream())
        val source=SiteImage(asset,DesignPoint(1.0,5.0),0.01)
        val doc=ProjectDesign("synthetic-source-output",siteImage=source)
        val settings=DesignOutputSettings(includeMeasurements=false)
        val projection=DesignOutput.drawing(doc,settings)
        assertTrue(projection.elements.filterIsInstance<TextElement>().single().text.contains("SCALE NOT SET"))
        val rect=DesignOutput.sourceRect(source,settings)
        val fit=ExportGeometry.fit(ExportGeometry.contentBounds(projection,asset.width,asset.height,rect),android.graphics.RectF(0f,0f,800f,600f))
        val world=source.toWorld(ImagePoint(60.0,200.0))
        val x=(world.x*100*fit.scale+fit.translateX).toInt();val y=(-world.y*100*fit.scale+fit.translateY).toInt()
        val bytes=DesignOutput.png(context,doc,800,600,settings)!!.readBytes()
        val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size)
        assertEquals(0xFFCC4422.toInt(),bitmap.getPixel(x,y));bitmap.recycle()
        assertEquals(doc,DesignJsonCodec.decode(DesignJsonCodec.encode(doc)))
        store.file(asset).delete()
        var failed=false
        try { DesignOutput.png(context,doc) } catch(e:IllegalArgumentException) { failed=true }
        assertTrue(failed)
        assertNotNull(DesignOutput.png(context,doc,settings=settings.copy(includeSourceImage=false)))
    }
    @Test fun `registered export bounds use source world placement rather than bitmap pixel origin`() {
        val source=SiteImage(SiteImageAsset("a".repeat(64),400,300),DesignPoint(-2.0,7.0),0.02)
        val r=DesignOutput.sourceRect(source)
        assertEquals(-200f,r.left,0f);assertEquals(-700f,r.top,0f)
        assertEquals(600f,r.right,0f);assertEquals(-100f,r.bottom,0f)
    }
}
