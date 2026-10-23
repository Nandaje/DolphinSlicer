package com.orcaslicer.android.model

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class Vec2(val x: Float, val y: Float) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vec2(x * scalar, y * scalar)

    fun length(): Float = sqrt(x * x + y * y)
    fun distanceTo(other: Vec2): Float = (this - other).length()

    fun normalized(): Vec2 {
        val len = length()
        return if (len > 1e-6f) Vec2(x / len, y / len) else Vec2(0f, 0f)
    }

    fun perp(): Vec2 = Vec2(-y, x)
}

data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vec3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float): Vec3 =
        if (abs(scalar) > 1e-8f) Vec3(x / scalar, y / scalar, z / scalar) else Vec3(0f, 0f, 0f)

    fun dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vec3): Vec3 = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun distanceTo(other: Vec3): Float = (this - other).length()

    fun normalized(): Vec3 {
        val len = length()
        return if (len > 1e-7f) Vec3(x / len, y / len, z / len) else Vec3(0f, 0f, 1f)
    }

    fun isFinite(): Boolean = x.isFinite() && y.isFinite() && z.isFinite()
}

data class BoundingBox(
    val min: Vec3,
    val max: Vec3
) {
    val size: Vec3 get() = max - min
    val center: Vec3 get() = (min + max) * 0.5f

    fun merge(other: BoundingBox): BoundingBox = BoundingBox(
        min = Vec3(min(min.x, other.min.x), min(min.y, other.min.y), min(min.z, other.min.z)),
        max = Vec3(max(max.x, other.max.x), max(max.y, other.max.y), max(max.z, other.max.z))
    )

    fun intersectRay(rayOrigin: Vec3, rayDir: Vec3): Float? {
        var tMin = Float.NEGATIVE_INFINITY
        var tMax = Float.POSITIVE_INFINITY

        val origins = floatArrayOf(rayOrigin.x, rayOrigin.y, rayOrigin.z)
        val dirs = floatArrayOf(rayDir.x, rayDir.y, rayDir.z)
        val mins = floatArrayOf(min.x, min.y, min.z)
        val maxs = floatArrayOf(max.x, max.y, max.z)

        for (i in 0..2) {
            if (abs(dirs[i]) < 1e-7f) {
                if (origins[i] < mins[i] || origins[i] > maxs[i]) return null
            } else {
                val invD = 1.0f / dirs[i]
                var t0 = (mins[i] - origins[i]) * invD
                var t1 = (maxs[i] - origins[i]) * invD
                if (t0 > t1) {
                    val tmp = t0
                    t0 = t1
                    t1 = tmp
                }
                tMin = max(tMin, t0)
                tMax = min(tMax, t1)
                if (tMax < tMin) return null
            }
        }
        return if (tMax >= 0f) max(0f, tMin) else null
    }
}

data class Triangle(
    val v0: Vec3,
    val v1: Vec3,
    val v2: Vec3,
    val normal: Vec3 = computeNormal(v0, v1, v2)
) {
    companion object {
        fun computeNormal(a: Vec3, b: Vec3, c: Vec3): Vec3 =
            (b - a).cross(c - a).normalized()
    }
}

/**
 * Stores 3D triangle mesh data in local centered coordinates (centered in X/Y, base at Z=0).
 * Packed vertex array layout for GPU VBO: [px, py, pz, nx, ny, nz] per vertex (6 floats/vertex).
 */
class MeshData(
    val triangles: List<Triangle>,
    val localBounds: BoundingBox
) {
    val triangleCount: Int get() = triangles.size
    val vertexCount: Int get() = triangles.size * 3

    // Lazily packed interleaved [px, py, pz, nx, ny, nz] array for OpenGL ES 3.0 VBO upload
    val gpuVertexBufferData: FloatArray by lazy {
        val out = FloatArray(triangles.size * 18)
        var idx = 0
        for (tri in triangles) {
            val n = tri.normal
            out[idx++] = tri.v0.x; out[idx++] = tri.v0.y; out[idx++] = tri.v0.z
            out[idx++] = n.x; out[idx++] = n.y; out[idx++] = n.z

            out[idx++] = tri.v1.x; out[idx++] = tri.v1.y; out[idx++] = tri.v1.z
            out[idx++] = n.x; out[idx++] = n.y; out[idx++] = n.z

            out[idx++] = tri.v2.x; out[idx++] = tri.v2.y; out[idx++] = tri.v2.z
            out[idx++] = n.x; out[idx++] = n.y; out[idx++] = n.z
        }
        out
    }

    companion object {
        fun fromRawTriangles(rawTriangles: List<Triangle>): MeshData {
            if (rawTriangles.isEmpty()) {
                return MeshData(emptyList(), BoundingBox(Vec3(0f, 0f, 0f), Vec3(1f, 1f, 1f)))
            }
            var minX = Float.POSITIVE_INFINITY
            var minY = Float.POSITIVE_INFINITY
            var minZ = Float.POSITIVE_INFINITY
            var maxX = Float.NEGATIVE_INFINITY
            var maxY = Float.NEGATIVE_INFINITY
            var maxZ = Float.NEGATIVE_INFINITY

            for (t in rawTriangles) {
                for (v in arrayOf(t.v0, t.v1, t.v2)) {
                    if (!v.isFinite()) continue
                    minX = min(minX, v.x); minY = min(minY, v.y); minZ = min(minZ, v.z)
                    maxX = max(maxX, v.x); maxY = max(maxY, v.y); maxZ = max(maxZ, v.z)
                }
            }

            if (!minX.isFinite() || !maxX.isFinite()) {
                return MeshData(emptyList(), BoundingBox(Vec3(0f, 0f, 0f), Vec3(1f, 1f, 1f)))
            }

            val centerX = (minX + maxX) * 0.5f
            val centerY = (minY + maxY) * 0.5f
            val baseZ = minZ
            val offset = Vec3(centerX, centerY, baseZ)

            val normalized = ArrayList<Triangle>(rawTriangles.size)
            for (t in rawTriangles) {
                if (!t.v0.isFinite() || !t.v1.isFinite() || !t.v2.isFinite()) continue
                val nv0 = t.v0 - offset
                val nv1 = t.v1 - offset
                val nv2 = t.v2 - offset
                normalized.add(Triangle(nv0, nv1, nv2, Triangle.computeNormal(nv0, nv1, nv2)))
            }

            val bounds = BoundingBox(
                min = Vec3(minX - centerX, minY - centerY, 0f),
                max = Vec3(maxX - centerX, maxY - centerY, maxZ - baseZ)
            )
            return MeshData(normalized, bounds)
        }
    }
}

data class SceneObject(
    val id: String,
    val name: String,
    val sourceFormat: String,
    val mesh: MeshData,
    val position: Vec3 = Vec3(0f, 0f, 0f),
    val rotationDeg: Vec3 = Vec3(0f, 0f, 0f),
    val scale: Vec3 = Vec3(1f, 1f, 1f),
    val filamentIndex: Int = 0,
    val visible: Boolean = true,
    val wallLoopsOverride: Int? = null,
    val infillDensityOverride: Int? = null,
    val supportEnabledOverride: Boolean? = null
) {
    fun transformPoint(local: Vec3): Vec3 {
        // Center pivot at mesh geometric center so rotations happen in-place around the object's center
        val lc = mesh.localBounds.center
        var x = local.x - lc.x
        var y = local.y - lc.y
        var z = local.z - lc.z

        // Global Euler rotation X -> Y -> Z (degrees to radians)
        val rx = rotationDeg.x * (PI.toFloat() / 180f)
        val ry = rotationDeg.y * (PI.toFloat() / 180f)
        val rz = rotationDeg.z * (PI.toFloat() / 180f)

        if (abs(rx) > 1e-5f) {
            val c = cos(rx); val s = sin(rx)
            val ny = y * c - z * s
            val nz = y * s + z * c
            y = ny; z = nz
        }
        if (abs(ry) > 1e-5f) {
            val c = cos(ry); val s = sin(ry)
            val nx = x * c + z * s
            val nz = -x * s + z * c
            x = nx; z = nz
        }
        if (abs(rz) > 1e-5f) {
            val c = cos(rz); val s = sin(rz)
            val nx = x * c - y * s
            val ny = x * s + y * c
            x = nx; y = ny
        }

        // Global-orientation scale along world X, Y, Z axes (matching OrcaSlicer global coordinate system)
        x *= scale.x
        y *= scale.y
        z *= scale.z

        return Vec3(x + position.x, y + position.y, z + lc.z + position.z)
    }

    fun worldBoundingBox(): BoundingBox {
        val b = mesh.localBounds
        val corners = arrayOf(
            Vec3(b.min.x, b.min.y, b.min.z),
            Vec3(b.max.x, b.min.y, b.min.z),
            Vec3(b.min.x, b.max.y, b.min.z),
            Vec3(b.max.x, b.max.y, b.min.z),
            Vec3(b.min.x, b.min.y, b.max.z),
            Vec3(b.max.x, b.min.y, b.max.z),
            Vec3(b.min.x, b.max.y, b.max.z),
            Vec3(b.max.x, b.max.y, b.max.z)
        )
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY

        for (c in corners) {
            val wc = transformPoint(c)
            minX = min(minX, wc.x); minY = min(minY, wc.y); minZ = min(minZ, wc.z)
            maxX = max(maxX, wc.x); maxY = max(maxY, wc.y); maxZ = max(maxZ, wc.z)
        }
        return BoundingBox(Vec3(minX, minY, minZ), Vec3(maxX, maxY, maxZ))
    }

    /**
     * Returns a copy of this SceneObject shifted in Z so its lowest transformed vertex sits exactly at Z = 0.
     */
    fun snappedToBed(): SceneObject {
        val wb = worldBoundingBox()
        val dz = -wb.min.z
        return if (abs(dz) > 1e-4f) {
            copy(position = position.copy(z = position.z + dz))
        } else {
            this
        }
    }

    /**
     * Generates world-space triangles with Z snapped to bed, used by SlicingEngine and Exporter.
     */
    fun worldTriangles(): List<Triangle> {
        val wb = worldBoundingBox()
        val zLift = -wb.min.z
        val out = ArrayList<Triangle>(mesh.triangles.size)
        for (t in mesh.triangles) {
            val v0 = transformPoint(t.v0).let { Vec3(it.x, it.y, it.z + zLift) }
            val v1 = transformPoint(t.v1).let { Vec3(it.x, it.y, it.z + zLift) }
            val v2 = transformPoint(t.v2).let { Vec3(it.x, it.y, it.z + zLift) }
            out.add(Triangle(v0, v1, v2, Triangle.computeNormal(v0, v1, v2)))
        }
        return out
    }
}
