package com.example.model.design

import kotlin.math.abs

/** Coordinates refer to the normalized, application-owned image, not the original camera EXIF frame. */
data class ImagePoint(val x: Double, val y: Double) {
    init { require(x.isFinite() && y.isFinite()) { "Image coordinates must be finite" } }
    fun distanceTo(other: ImagePoint) = kotlin.math.hypot(x - other.x, y - other.y)
}

data class SiteImageAsset(val sha256: String, val width: Int, val height: Int) {
    init {
        require(sha256.matches(Regex("[a-f0-9]{64}"))) { "Invalid image asset identity" }
        require(width in 1..4096 && height in 1..4096 && width.toLong() * height <= 12_000_000) { "Image exceeds the supported pixel budget" }
    }
}

/** A chosen reference distance establishes scale, NOT surveyed/field-verified site accuracy. */
data class ImageCalibration(val first: ImagePoint, val second: ImagePoint, val distanceMetres: Double) {
    init {
        require(first.distanceTo(second) >= 8.0) { "Choose reference points at least 8 image pixels apart" }
        require(distanceMetres.isFinite() && distanceMetres in 0.0254..304.8) { "Reference distance must be 1 inch to 1000 feet" }
    }
    val metresPerPixel: Double get() = distanceMetres / first.distanceTo(second)
}

/** The source is protected from ordinary design gestures. Changing it never changes proposed objects.
 * First slice: upright, uniform scale + translation only. No inferred georeferencing or perspective repair.
 */
data class SiteImage(
    val asset: SiteImageAsset,
    val topLeft: DesignPoint,
    val metresPerPixel: Double,
    val calibration: ImageCalibration? = null,
    val visible: Boolean = true
) {
    init {
        require(metresPerPixel.isFinite() && metresPerPixel in 0.000001..10.0) { "Unsupported image scale" }
        require(asset.width * metresPerPixel <= 2000.0 && asset.height * metresPerPixel <= 2000.0) { "Image extent exceeds the site preview limit" }
        calibration?.let {
            require(contains(it.first) && contains(it.second)) { "Reference points must be inside the image" }
            require(abs(it.metresPerPixel - metresPerPixel) <= 1e-10 * metresPerPixel) { "Calibration and placement scale disagree" }
        }
    }
    fun contains(p: ImagePoint) = p.x in 0.0..asset.width.toDouble() && p.y in 0.0..asset.height.toDouble()
    fun toWorld(p: ImagePoint) = DesignPoint(topLeft.x + p.x * metresPerPixel, topLeft.y - p.y * metresPerPixel)
    fun toImage(p: DesignPoint) = ImagePoint((p.x - topLeft.x) / metresPerPixel, (topLeft.y - p.y) / metresPerPixel)
    fun corners() = listOf(topLeft, toWorld(ImagePoint(asset.width.toDouble(), asset.height.toDouble())))
    fun calibrated(first: ImagePoint, second: ImagePoint, distanceMetres: Double): SiteImage {
        val next = ImageCalibration(first, second, distanceMetres)
        val anchor = toWorld(first)
        return copy(topLeft = DesignPoint(anchor.x - first.x * next.metresPerPixel,
            anchor.y + first.y * next.metresPerPixel), metresPerPixel = next.metresPerPixel, calibration = next)
    }
    fun moved(dx: Double, dy: Double) = copy(topLeft = topLeft.translated(dx, dy))
    companion object {
        /** Temporary framing only, prominently uncalibrated. Not a claimed physical image width. */
        fun unscaled(asset: SiteImageAsset) = SiteImage(asset, DesignPoint(-5.0, 15.0), 30.0 / asset.width)
    }
}
