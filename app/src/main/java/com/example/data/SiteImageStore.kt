package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.util.AtomicFile
import com.example.model.design.SiteImageAsset
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/** Own the selected bytes instead of relying on a provider URI surviving deletion/restart.
 * Normalized PNG is bounded and orientation-corrected; the original selected bytes are retained
 * locally by hash for provenance/recovery. Neither original filename nor location tags enter JSON.
 */
class SiteImageStore(private val directory: File) {
    private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    fun ingest(context: Context, uri: Uri): SiteImageAsset {
        require(uri.scheme == "content" || uri.scheme == "file") { "Choose a local image through the file picker" }
        return context.contentResolver.openInputStream(uri)?.use { ingest(it) }
            ?: throw IllegalArgumentException("The selected image could not be read")
    }
    fun ingest(input: InputStream): SiteImageAsset {
        directory.mkdirs()
        val raw = File.createTempFile("intake-", ".tmp", directory)
        try {
            raw.outputStream().use { out ->
                val buffer = ByteArray(8192); var total = 0L
                while (true) {
                    val count = input.read(buffer); if (count < 0) break
                    total += count; require(total <= 32L * 1024 * 1024) { "Choose a PNG or JPEG under 32 MiB" }
                    out.write(buffer, 0, count)
                }
                out.fd.sync()
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(raw.path, bounds)
            require(bounds.outMimeType in setOf("image/png", "image/jpeg") && bounds.outWidth > 0 && bounds.outHeight > 0 &&
                bounds.outWidth <= 20000 && bounds.outHeight <= 20000 && bounds.outWidth.toLong() * bounds.outHeight <= 100_000_000) {
                "This site intake supports PNG/JPEG images up to 100 megapixels, not PDFs or other formats yet"
            }
            var sample = 1
            while (bounds.outWidth / sample > 4096 || bounds.outHeight / sample > 4096 ||
                (bounds.outWidth.toLong() / sample) * (bounds.outHeight / sample) > 4_000_000) sample *= 2
            val decoded = BitmapFactory.decodeFile(raw.path, BitmapFactory.Options().apply {
                inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888
            }) ?: throw IllegalArgumentException("Image decoding failed")
            var bitmap = decoded
            try {
                val orientation = if (bounds.outMimeType == "image/jpeg") ExifInterface(raw.path)
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) else ExifInterface.ORIENTATION_NORMAL
                val matrix = Matrix().apply {
                    when (orientation) {
                        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                        ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                        ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                        ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                        ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
                        ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
                        ExifInterface.ORIENTATION_NORMAL, ExifInterface.ORIENTATION_UNDEFINED -> Unit
                        else -> throw IllegalArgumentException("Unsupported image orientation")
                    }
                }
                if (!matrix.isIdentity) bitmap = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                val bytes = java.io.ByteArrayOutputStream().use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)); output.toByteArray()
                }
                val asset = SiteImageAsset(hash(bytes), bitmap.width, bitmap.height)
                // Content addressed originals and normalized pixels are retained; Undo never deletes assets.
                val original = raw.readBytes()
                writeOnce(File(directory, "${hash(original)}.source"), original)
                writeOnce(file(asset), bytes)
                load(asset).recycle() // Verify byte identity, image readability and dimensions before attachment.
                return asset
            } finally { if (bitmap !== decoded) bitmap.recycle(); decoded.recycle() }
        } finally { raw.delete() }
    }
    private fun writeOnce(file: File, bytes: ByteArray) = synchronized(lock) {
        if (file.exists()) {
            require(file.readBytes().contentEquals(bytes)) { "Existing image asset is damaged; it was not overwritten" }
        } else {
            val atomic = AtomicFile(file); val stream = atomic.startWrite()
            try { stream.write(bytes); atomic.finishWrite(stream) }
            catch (e: Exception) { atomic.failWrite(stream); throw e }
        }
    }
    fun file(asset: SiteImageAsset) = File(directory, "${asset.sha256}.png")
    fun load(asset: SiteImageAsset): Bitmap {
        val file = file(asset)
        require(file.isFile && file.length() <= 64L * 1024 * 1024) { "The owned source image is missing or too large. Design geometry was preserved" }
        val bytes = file.readBytes()
        require(hash(bytes) == asset.sha256) { "The source image checksum changed. It was not silently substituted" }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth == asset.width && bounds.outHeight == asset.height) { "Source pixel dimensions changed" }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: throw IllegalArgumentException("Owned source could not be decoded")
    }
    companion object {
        private val lock = Any()
        fun inFiles(filesDir: File) = SiteImageStore(File(filesDir, "project-design/assets"))
    }
}
