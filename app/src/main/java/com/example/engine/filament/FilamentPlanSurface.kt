package com.example.engine.filament

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.TextureView
import com.example.engine.Architectural3DEngine
import com.example.engine.Face3D
import com.example.engine.Point3D
import com.example.model.TraceProject
import com.google.android.filament.Box
import com.google.android.filament.Camera
import com.google.android.filament.Colors
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.Filament
import com.google.android.filament.IndexBuffer
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.Skybox
import com.google.android.filament.SwapChain
import com.google.android.filament.VertexBuffer
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.ChoreographerHelper
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.FilamentHelper
import com.google.android.filament.android.UiHelper
import com.google.android.filament.filamat.MaterialBuilder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Isolated native-Android Filament preview for Plan Trace.
 *
 * TextureView is intentional here. Unlike SurfaceView, it participates in the same window
 * composition as Compose, which avoids a separate native surface being hidden or captured as
 * black when the Perspective view is hosted inside a full-screen Compose Dialog.
 *
 * The TraceProject remains authoritative. This class consumes generated preview faces and owns
 * only GPU resources / camera state. Layer-name height inference still belongs to the legacy
 * preview adapter and is intentionally not written back into the project model.
 */
class FilamentPlanSurface(context: Context) : TextureView(context) {

    companion object {
        init { Filament.init() }
    }

    private data class MeshResource(
        val entity: Int,
        val vertexBuffer: VertexBuffer,
        val indexBuffer: IndexBuffer
    )

    private val engine: Engine = Engine.create()
    private val renderer: Renderer = engine.createRenderer()
    private val scene: Scene = engine.createScene()
    private val filamentView: View = engine.createView()
    private val cameraEntity = EntityManager.get().create()
    private val camera = engine.createCamera(cameraEntity)
    private val displayHelper = DisplayHelper(context)
    private val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)

    private var swapChain: SwapChain? = null
    private var solidMesh: MeshResource? = null
    private var waterMesh: MeshResource? = null

    private val material: Material
    private val solidMaterial: MaterialInstance
    private val waterMaterial: MaterialInstance

    private var yawDegrees = -42f
    private var pitchDegrees = 36f
    private var cameraRadius = 8.5f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var destroyed = false

    private val frameScheduler = object : ChoreographerHelper() {
        override fun onFrame(frameTimeNanos: Long) {
            val chain = swapChain ?: return
            if (uiHelper.isReadyToRender && renderer.beginFrame(chain, frameTimeNanos)) {
                renderer.render(filamentView)
                renderer.endFrame()
            }
        }
    }

    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                cameraRadius = (cameraRadius / detector.scaleFactor).coerceIn(2.5f, 30f)
                updateCamera()
                return true
            }
        })

    init {
        isOpaque = true
        isClickable = true
        isFocusable = true

        uiHelper.renderCallback = object : UiHelper.RendererCallback {
            override fun onNativeWindowChanged(surface: Surface) {
                swapChain?.let { engine.destroySwapChain(it) }
                swapChain = engine.createSwapChain(surface)
                displayHelper.attach(renderer, display)
            }

            override fun onDetachedFromSurface() {
                displayHelper.detach()
                swapChain?.let {
                    engine.destroySwapChain(it)
                    engine.flushAndWait()
                    swapChain = null
                }
            }

            override fun onResized(width: Int, height: Int) {
                val safeHeight = max(height, 1)
                val aspect = width.toDouble() / safeHeight.toDouble()
                camera.setProjection(42.0, aspect, 0.1, 100.0, Camera.Fov.VERTICAL)
                filamentView.viewport = Viewport(0, 0, width, height)
                FilamentHelper.synchronizePendingFrames(engine)
                updateCamera()
            }
        }
        uiHelper.attachTo(this)

        filamentView.scene = scene
        filamentView.camera = camera
        filamentView.isPostProcessingEnabled = true
        scene.skybox = Skybox.Builder()
            .color(0.965f, 0.955f, 0.925f, 1f)
            .build(engine)

        material = buildFlatMaterial()
        solidMaterial = material.createInstance().apply {
            setParameter("baseColor", Colors.RgbType.SRGB, 0.67f, 0.64f, 0.58f)
        }
        waterMaterial = material.createInstance().apply {
            setParameter("baseColor", Colors.RgbType.SRGB, 0.10f, 0.55f, 0.78f)
        }

        frameScheduler.setRenderer(renderer)
        updateCamera()
    }

    fun setProject(project: TraceProject) {
        if (destroyed) return
        val faces = Architectural3DEngine.build3DFaces(project, heightMultiplier = 1f)
        rebuildMeshes(faces)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!destroyed) frameScheduler.post()
    }

    override fun onDetachedFromWindow() {
        frameScheduler.remove()
        if (!destroyed) destroyRenderer()
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> if (!scaleDetector.isInProgress && event.pointerCount == 1) {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY
                yawDegrees = (yawDegrees + dx * 0.30f) % 360f
                pitchDegrees = (pitchDegrees + dy * 0.22f).coerceIn(10f, 82f)
                lastTouchX = event.x
                lastTouchY = event.y
                updateCamera()
            }
            MotionEvent.ACTION_UP -> {
                performClick()
                parent?.requestDisallowInterceptTouchEvent(false)
            }
            MotionEvent.ACTION_CANCEL -> parent?.requestDisallowInterceptTouchEvent(false)
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun updateCamera() {
        val yaw = Math.toRadians(yawDegrees.toDouble())
        val pitch = Math.toRadians(pitchDegrees.toDouble())
        val horizontal = cameraRadius * cos(pitch)
        val eyeX = horizontal * cos(yaw)
        val eyeY = cameraRadius * sin(pitch) + 0.8
        val eyeZ = horizontal * sin(yaw)
        camera.lookAt(
            eyeX, eyeY, eyeZ,
            0.0, 0.0, 0.0,
            0.0, 1.0, 0.0
        )
    }

    private fun rebuildMeshes(faces: List<Face3D>) {
        destroyMesh(solidMesh)
        destroyMesh(waterMesh)
        solidMesh = null
        waterMesh = null

        if (faces.isEmpty()) return

        val allPoints = faces.flatMap { it.vertices }
        val minX = allPoints.minOf { it.x }
        val maxX = allPoints.maxOf { it.x }
        val minY = allPoints.minOf { it.y }
        val maxY = allPoints.maxOf { it.y }
        val maxZ = allPoints.maxOf { it.z }
        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f
        val planExtent = max(maxX - minX, maxY - minY).coerceAtLeast(1f)
        val worldScale = 6.0f / planExtent

        fun world(point: Point3D): Point3D = Point3D(
            (point.x - centerX) * worldScale,
            point.z * worldScale,
            (point.y - centerY) * worldScale
        )

        val solids = faces.filterNot { it.isWater }
        val water = faces.filter { it.isWater }
        solidMesh = createMesh(solids, solidMaterial, ::world)
        waterMesh = createMesh(water, waterMaterial, ::world)

        val sceneHeight = maxZ * worldScale
        cameraRadius = max(cameraRadius, 6.8f + sceneHeight * 0.6f).coerceAtMost(18f)
        updateCamera()
    }

    private fun createMesh(
        faces: List<Face3D>,
        materialInstance: MaterialInstance,
        transform: (Point3D) -> Point3D
    ): MeshResource? {
        if (faces.isEmpty()) return null

        val vertices = mutableListOf<Point3D>()
        val indices = mutableListOf<Int>()
        for (face in faces) {
            if (face.vertices.size < 3) continue
            val base = vertices.size
            face.vertices.mapTo(vertices, transform)
            for (i in 1 until face.vertices.size - 1) {
                indices += base
                indices += base + i
                indices += base + i + 1
            }
        }
        if (vertices.isEmpty() || indices.isEmpty()) return null

        val vertexBytes = ByteBuffer.allocateDirect(vertices.size * 3 * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
        vertices.forEach { point ->
            vertexBytes.putFloat(point.x)
            vertexBytes.putFloat(point.y)
            vertexBytes.putFloat(point.z)
        }
        vertexBytes.flip()

        val indexBytes = ByteBuffer.allocateDirect(indices.size * Int.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
        indices.forEach(indexBytes::putInt)
        indexBytes.flip()

        val vertexBuffer = VertexBuffer.Builder()
            .bufferCount(1)
            .vertexCount(vertices.size)
            .attribute(
                VertexBuffer.VertexAttribute.POSITION,
                0,
                VertexBuffer.AttributeType.FLOAT3,
                0,
                3 * Float.SIZE_BYTES
            )
            .build(engine)
        vertexBuffer.setBufferAt(engine, 0, vertexBytes)

        val indexBuffer = IndexBuffer.Builder()
            .indexCount(indices.size)
            .bufferType(IndexBuffer.Builder.IndexType.UINT)
            .build(engine)
        indexBuffer.setBuffer(engine, indexBytes)

        val xs = vertices.map { it.x }
        val ys = vertices.map { it.y }
        val zs = vertices.map { it.z }
        val minWx = xs.minOrNull() ?: -1f
        val maxWx = xs.maxOrNull() ?: 1f
        val minWy = ys.minOrNull() ?: 0f
        val maxWy = ys.maxOrNull() ?: 1f
        val minWz = zs.minOrNull() ?: -1f
        val maxWz = zs.maxOrNull() ?: 1f
        val cx = (minWx + maxWx) / 2f
        val cy = (minWy + maxWy) / 2f
        val cz = (minWz + maxWz) / 2f
        val hx = max((maxWx - minWx) / 2f, 0.05f)
        val hy = max((maxWy - minWy) / 2f, 0.05f)
        val hz = max((maxWz - minWz) / 2f, 0.05f)

        val entity = EntityManager.get().create()
        RenderableManager.Builder(1)
            .boundingBox(Box(cx, cy, cz, hx, hy, hz))
            .geometry(
                0,
                RenderableManager.PrimitiveType.TRIANGLES,
                vertexBuffer,
                indexBuffer,
                0,
                indices.size
            )
            .material(0, materialInstance)
            .culling(false)
            .castShadows(false)
            .receiveShadows(false)
            .build(engine, entity)
        scene.addEntity(entity)

        return MeshResource(entity, vertexBuffer, indexBuffer)
    }

    private fun buildFlatMaterial(): Material {
        MaterialBuilder.init()
        try {
            val packageBuffer = MaterialBuilder()
                .platform(MaterialBuilder.Platform.MOBILE)
                .name("Plan Trace flat preview")
                .shading(MaterialBuilder.Shading.UNLIT)
                .uniformParameter(MaterialBuilder.UniformType.FLOAT3, "baseColor")
                .material(
                    """
                    void material(inout MaterialInputs material) {
                        prepareMaterial(material);
                        material.baseColor.rgb = materialParams.baseColor;
                    }
                    """.trimIndent()
                )
                .optimization(MaterialBuilder.Optimization.NONE)
                .build(engine)
            check(packageBuffer.isValid) { "Filament preview material failed to compile" }
            val buffer = packageBuffer.buffer
            return Material.Builder().payload(buffer, buffer.remaining()).build(engine)
        } finally {
            MaterialBuilder.shutdown()
        }
    }

    private fun destroyMesh(mesh: MeshResource?) {
        mesh ?: return
        scene.removeEntity(mesh.entity)
        engine.destroyEntity(mesh.entity)
        engine.destroyVertexBuffer(mesh.vertexBuffer)
        engine.destroyIndexBuffer(mesh.indexBuffer)
        EntityManager.get().destroy(mesh.entity)
    }

    private fun destroyRenderer() {
        destroyed = true
        frameScheduler.remove()
        uiHelper.detach()
        destroyMesh(solidMesh)
        destroyMesh(waterMesh)
        solidMesh = null
        waterMesh = null

        engine.destroyMaterialInstance(solidMaterial)
        engine.destroyMaterialInstance(waterMaterial)
        engine.destroyMaterial(material)
        scene.skybox?.let { engine.destroySkybox(it) }
        engine.destroyRenderer(renderer)
        engine.destroyView(filamentView)
        engine.destroyScene(scene)
        engine.destroyCameraComponent(cameraEntity)
        EntityManager.get().destroy(cameraEntity)
        engine.destroy()
    }
}
