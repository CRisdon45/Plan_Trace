package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfManager {

    data class PdfInfo(
        val pageCount: Int,
        val firstPageBitmap: Bitmap?
    )

    suspend fun getPdfPageCount(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    return@withContext renderer.pageCount
                }
            } ?: 1
        } catch (e: Exception) {
            e.printStackTrace()
            1
        }
    }

    suspend fun renderPdfPage(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        targetWidth: Int = 1800
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null
                    renderer.openPage(pageIndex).use { page ->
                        val ratio = page.height.toFloat() / page.width.toFloat()
                        val width = targetWidth
                        val height = (targetWidth * ratio).toInt().coerceAtLeast(100)

                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        // Fill white background for PDF page
                        val canvas = android.graphics.Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)

                        page.render(
                            bitmap,
                            null,
                            null,
                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                        )
                        return@withContext bitmap
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun copyUriToInternalFile(context: Context, uri: Uri, fileName: String): File? =
        withContext(Dispatchers.IO) {
            try {
                val destFile = File(context.filesDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                destFile
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
}
