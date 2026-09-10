package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.ProjectJsonConverter
import com.example.data.db.TraceDatabase
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PageDocumentsTest {
    private fun original() = TraceProject(backgroundType = BackgroundType.PDF_URI,
        backgroundResourceOrUri = "content://qa/drawing", pdfTotalPages = 2,
        layers = listOf(DrawingLayer(id = "patio", name = "Patio", isLocked = true)),
        activeLayerId = "patio", scaleCalibration = ScaleCalibration(true, 400f, 20f, "ft"),
        elements = listOf(LineElement(id = "first", layerId = "patio", start = Point2D(20f, 40f), end = Point2D(420f, 40f))))

    @Test fun `sheets retain separate objects layers scale opacity and active layer`() {
        val first = original().copy(backgroundOpacity = 0.4f)
        val second = first.openSheet(first.backgroundType, first.backgroundResourceOrUri, 1, 2)
        assertTrue(second.elements.isEmpty())
        assertFalse(second.scaleCalibration.isCalibrated)
        assertEquals(TraceProject.defaultLayers(), second.layers)
        val edited = second.copy(elements = listOf(TextElement(id = "note", layerId = second.activeLayerId,
            position = Point2D(100f, 100f), text = "PAGE TWO")), scaleCalibration = ScaleCalibration(true, 100f, 5f, "m"))
        val back = edited.openSheet(first.backgroundType, first.backgroundResourceOrUri, 0, 2)
        assertEquals(first.currentPageDrawing(), back.currentPageDrawing())
        assertEquals(edited.currentPageDrawing(), back.openSheet(first.backgroundType, first.backgroundResourceOrUri, 1, 2).currentPageDrawing())
    }

    @Test fun `save and reopen persist inactive pages and the latest active edit`() {
        val first = original()
        val second = first.openSheet(first.backgroundType, first.backgroundResourceOrUri, 1, 2)
            .copy(elements = listOf(TextElement(id = "latest", layerId = TraceProject.DEFAULT_LAYER_1_ID,
                position = Point2D(10f, 30f), text = "Latest edit")))
        val reopened = ProjectJsonConverter.fromEntity(ProjectJsonConverter.toEntity(second))
        assertEquals(second.currentPageDrawing(), reopened.currentPageDrawing())
        assertEquals(first.currentPageDrawing(), reopened.openSheet(first.backgroundType, first.backgroundResourceOrUri, 0, 2).currentPageDrawing())
    }

    @Test fun `legacy shared annotations belong only to the saved active page`() {
        val legacy = original().copy(pdfPageNumber = 1)
        val entity = ProjectJsonConverter.toEntity(legacy).copy(pageDrawingsJson = "{}")
        val upgraded = ProjectJsonConverter.fromEntity(entity)
        val other = upgraded.openSheet(legacy.backgroundType, legacy.backgroundResourceOrUri, 0, 2)
        assertTrue(other.elements.isEmpty())
        assertFalse(other.scaleCalibration.isCalibrated)
        assertEquals(legacy.currentPageDrawing(), other.openSheet(legacy.backgroundType, legacy.backgroundResourceOrUri, 1, 2).currentPageDrawing())
    }

    @Test fun `different sources never inherit drawings and reopening a source restores them`() {
        val original = original()
        val different = original.openSheet(BackgroundType.PDF_URI, "content://qa/other", 0, 3)
        assertTrue(different.elements.isEmpty())
        val image = different.openSheet(BackgroundType.IMAGE_URI, "content://qa/image")
        assertTrue(image.elements.isEmpty())
        assertEquals(original.currentPageDrawing(), image.openSheet(original.backgroundType, original.backgroundResourceOrUri, 0, 2).currentPageDrawing())
    }

    @Test fun `version one database migrates without losing legacy project fields`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "pages-migration-test"
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name).callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""CREATE TABLE projects (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL,
                        createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, backgroundType TEXT NOT NULL,
                        backgroundResourceOrUri TEXT NOT NULL, backgroundOpacity REAL NOT NULL, isBackgroundLocked INTEGER NOT NULL,
                        pdfPageNumber INTEGER NOT NULL, pdfTotalPages INTEGER NOT NULL, isCalibrated INTEGER NOT NULL,
                        pixelDistance REAL NOT NULL, realWorldUnits REAL NOT NULL, unit TEXT NOT NULL, layersJson TEXT NOT NULL,
                        activeLayerId TEXT NOT NULL, elementsJson TEXT NOT NULL)""")
                    val old = ProjectJsonConverter.toEntity(original().copy(id = "legacy", title = "Keep me", pdfPageNumber = 1))
                    db.execSQL("INSERT INTO projects VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", arrayOf(old.id, old.title,
                        old.createdAt, old.updatedAt, old.backgroundType, old.backgroundResourceOrUri, old.backgroundOpacity,
                        1, old.pdfPageNumber, old.pdfTotalPages, 1, old.pixelDistance, old.realWorldUnits,
                        old.unit, old.layersJson, old.activeLayerId, old.elementsJson))
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build())
        helper.writableDatabase
        helper.close()
        val migrated = Room.databaseBuilder(context, TraceDatabase::class.java, name)
            .addMigrations(TraceDatabase.MIGRATION_1_2).allowMainThreadQueries().build()
        try {
            migrated.openHelper.writableDatabase.query("SELECT * FROM projects WHERE id='legacy'").use { row ->
                assertTrue(row.moveToFirst())
                assertEquals("Keep me", row.getString(row.getColumnIndexOrThrow("title")))
                assertEquals(1, row.getInt(row.getColumnIndexOrThrow("pdfPageNumber")))
                assertEquals(ProjectJsonConverter.serializeElements(original().elements), row.getString(row.getColumnIndexOrThrow("elementsJson")))
                assertEquals("{}", row.getString(row.getColumnIndexOrThrow("pageDrawingsJson")))
            }
            assertEquals(2, migrated.openHelper.writableDatabase.version)
        } finally { migrated.close(); context.deleteDatabase(name) }
    }
}
