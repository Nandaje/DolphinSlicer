package com.orcaslicer.android.model

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
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
    /**
     * Möller–Trumbore ray-triangle intersection.
     * Returns ray distance `t > 0` if hit, or `null` if no intersection.
     */
    fun intersectRay(rayOrigin: Vec3, rayDir: Vec3): Float? {
        val edge1 = v1 - v0
        val edge2 = v2 - v0
        val h = rayDir.cross(edge2)
        val a = edge1.dot(h)
        if (abs(a) < 1e-7f) return null
        val f = 1.0f / a
        val s = rayOrigin - v0
        val u = f * s.dot(h)
        if (u < 0.0f || u > 1.0f) return null
        val q = s.cross(edge1)
        val v = f * rayDir.dot(q)
        if (v < 0.0f || u + v > 1.0f) return null
        val t = f * edge2.dot(q)
        return if (t > 1e-4f) t else null
    }

    companion object {
        fun computeNormal(a: Vec3, b: Vec3, c: Vec3): Vec3 =
            (b - a).cross(c - a).normalized()
    }
}

/**
 * Represents a planar outer face candidate on a 3D mesh for OrcaSlicer's "Lay on Face" (Place on Face) tool.
 */
data class LayOnFaceCandidate(
    val id: Int,
    val localNormal: Vec3,
    val localCenter: Vec3,
    val localPolygon: List<Vec3>,
    val areaMm2: Float,
    val label: String
)

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

    /**
     * Compact 3D directional support hull vertices (up to ~120 extreme hull points) so `worldBoundingBox()`
     * computes exact tight world Z bounds under arbitrary 3D rotations (e.g. Lay on Face at 45°).
     */
    val supportVertices: List<Vec3> by lazy {
        val b = localBounds
        val fallbackCorners = listOf(
            Vec3(b.min.x, b.min.y, b.min.z),
            Vec3(b.max.x, b.min.y, b.min.z),
            Vec3(b.min.x, b.max.y, b.min.z),
            Vec3(b.max.x, b.max.y, b.min.z),
            Vec3(b.min.x, b.min.y, b.max.z),
            Vec3(b.max.x, b.min.y, b.max.z),
            Vec3(b.min.x, b.max.y, b.max.z),
            Vec3(b.max.x, b.max.y, b.max.z)
        )
        if (triangles.isEmpty()) return@lazy fallbackCorners

        if (triangles.size <= 160) {
            val unique = ArrayList<Vec3>(triangles.size * 3)
            val seen = HashSet<Long>(triangles.size * 3)
            for (t in triangles) {
                for (v in arrayOf(t.v0, t.v1, t.v2)) {
                    val kx = (v.x * 100f).roundToInt().toLong() and 0x1FFFFFL
                    val ky = (v.y * 100f).roundToInt().toLong() and 0x1FFFFFL
                    val kz = (v.z * 100f).roundToInt().toLong() and 0x1FFFFFL
                    val key = (kx shl 42) or (ky shl 21) or kz
                    if (seen.add(key)) unique.add(v)
                }
            }
            return@lazy if (unique.isNotEmpty()) unique else fallbackCorners
        }

        // Build 98 uniform sphere directions (26 cube-axes/diagonals + 72 Fibonacci sphere directions)
        val dirs = ArrayList<Vec3>(98)
        for (dx in -1..1) {
            for (dy in -1..1) {
                for (dz in -1..1) {
                    if (dx != 0 || dy != 0 || dz != 0) {
                        dirs.add(Vec3(dx.toFloat(), dy.toFloat(), dz.toFloat()).normalized())
                    }
                }
            }
        }
        val fibCount = 72
        val goldenAngle = PI.toFloat() * (3f - sqrt(5f))
        for (i in 0 until fibCount) {
            val z = 1f - (2f * (i + 0.5f) / fibCount)
            val r = sqrt(max(0f, 1f - z * z))
            val theta = goldenAngle * i
            dirs.add(Vec3(r * cos(theta), r * sin(theta), z))
        }

        val bestDot = FloatArray(dirs.size) { Float.NEGATIVE_INFINITY }
        val bestVert = Array(dirs.size) { fallbackCorners[0] }
        val step = max(1, triangles.size / 4000)
        var idx = 0
        while (idx < triangles.size) {
            val t = triangles[idx]
            for (v in arrayOf(t.v0, t.v1, t.v2)) {
                for (dIdx in dirs.indices) {
                    val d = dirs[dIdx]
                    val dot = v.x * d.x + v.y * d.y + v.z * d.z
                    if (dot > bestDot[dIdx]) {
                        bestDot[dIdx] = dot
                        bestVert[dIdx] = v
                    }
                }
            }
            idx += step
        }

        val result = ArrayList<Vec3>(dirs.size)
        val seen = HashSet<Long>(dirs.size * 2)
        for (v in bestVert) {
            val kx = (v.x * 100f).roundToInt().toLong() and 0x1FFFFFL
            val ky = (v.y * 100f).roundToInt().toLong() and 0x1FFFFFL
            val kz = (v.z * 100f).roundToInt().toLong() and 0x1FFFFFL
            val key = (kx shl 42) or (ky shl 21) or kz
            if (seen.add(key)) result.add(v)
        }
        if (result.isEmpty()) fallbackCorners else result
    }

    /**
     * Precomputed outer planar face candidates for OrcaSlicer's "Lay on Face" (Place on Face) tool,
     * sorted by planar area descending with 2D convex hull polygon patches in local 3D mesh space.
     */
    val layOnFaceCandidates: List<LayOnFaceCandidate> by lazy {
        if (triangles.isEmpty()) return@lazy emptyList()

        class NormalBucket(
            var sumNormal: Vec3 = Vec3(0f, 0f, 0f),
            val tris: ArrayList<Pair<Triangle, Float>> = ArrayList()
        )

        val buckets = HashMap<Int, NormalBucket>()
        var totalMeshArea = 0f

        for (t in triangles) {
            val cross = (t.v1 - t.v0).cross(t.v2 - t.v0)
            val area = 0.5f * cross.length()
            if (area < 1e-4f || !area.isFinite()) continue
            totalMeshArea += area
            val n = cross / (2f * area)
            val qx = (n.x * 24f).roundToInt().coerceIn(-24, 24) + 32
            val qy = (n.y * 24f).roundToInt().coerceIn(-24, 24) + 32
            val qz = (n.z * 24f).roundToInt().coerceIn(-24, 24) + 32
            val key = (qx shl 14) or (qy shl 7) or qz
            val bucket = buckets.getOrPut(key) { NormalBucket() }
            bucket.sumNormal = bucket.sumNormal + (n * area)
            bucket.tris.add(t to area)
        }

        val minAreaThreshold = max(1.5f, totalMeshArea * 0.0025f)
        val rawCandidates = ArrayList<LayOnFaceCandidate>()

        for ((_, bucket) in buckets) {
            val avgN = bucket.sumNormal.normalized()
            // Find outermost plane projection along avgN across the entire mesh support hull
            var maxMeshProj = Float.NEGATIVE_INFINITY
            for (sv in supportVertices) {
                val p = sv.dot(avgN)
                if (p > maxMeshProj) maxMeshProj = p
            }
            for ((tri, _) in bucket.tris) {
                val p = max(tri.v0.dot(avgN), max(tri.v1.dot(avgN), tri.v2.dot(avgN)))
                if (p > maxMeshProj) maxMeshProj = p
            }

            // Keep triangles in this normal bucket that lie on the outermost plane (within 0.65mm)
            val planeTol = 0.65f
            var outerArea = 0f
            var weightedCenter = Vec3(0f, 0f, 0f)
            var refinedNormalSum = Vec3(0f, 0f, 0f)
            val coplanarVerts = ArrayList<Vec3>()

            for ((tri, area) in bucket.tris) {
                val c = (tri.v0 + tri.v1 + tri.v2) * (1f / 3f)
                if (c.dot(avgN) >= maxMeshProj - planeTol) {
                    outerArea += area
                    weightedCenter = weightedCenter + (c * area)
                    refinedNormalSum = refinedNormalSum + (tri.normal * area)
                    if (coplanarVerts.size < 600) {
                        coplanarVerts.add(tri.v0)
                        coplanarVerts.add(tri.v1)
                        coplanarVerts.add(tri.v2)
                    }
                }
            }

            if (outerArea < minAreaThreshold || coplanarVerts.size < 3) continue
            val center = weightedCenter / outerArea
            val unitN = refinedNormalSum.normalized()
            val poly = buildFaceConvexHull3D(center, unitN, coplanarVerts)
            if (poly.size >= 3) {
                rawCandidates.add(
                    LayOnFaceCandidate(
                        id = 0,
                        localNormal = unitN,
                        localCenter = center,
                        localPolygon = poly,
                        areaMm2 = outerArea,
                        label = describeFaceNormal(unitN)
                    )
                )
            }
        }

        // Sort by area descending and deduplicate nearly identical normal directions
        rawCandidates.sortByDescending { it.areaMm2 }
        val merged = ArrayList<LayOnFaceCandidate>(16)
        for (cand in rawCandidates) {
            if (merged.size >= 16) break
            val isDuplicate = merged.any { existing ->
                existing.localNormal.dot(cand.localNormal) > 0.985f &&
                    abs((cand.localCenter - existing.localCenter).dot(existing.localNormal)) < 1.0f
            }
            if (!isDuplicate) {
                merged.add(cand.copy(id = merged.size))
            }
        }
        merged
    }

    companion object {
        private fun describeFaceNormal(n: Vec3): String {
            return when {
                n.z < -0.92f -> "Bottom (-Z)"
                n.z > 0.92f -> "Top (+Z)"
                n.y < -0.92f -> "Front (-Y)"
                n.y > 0.92f -> "Back (+Y)"
                n.x < -0.92f -> "Left (-X)"
                n.x > 0.92f -> "Right (+X)"
                else -> {
                    val tiltDeg = (acos(n.z.coerceIn(-1f, 1f)) * (180f / PI.toFloat())).roundToInt()
                    "Angled (${tiltDeg}°)"
                }
            }
        }

        private fun buildFaceConvexHull3D(center: Vec3, normal: Vec3, points3D: List<Vec3>): List<Vec3> {
            // Orthonormal tangent basis (uAxis, vAxis) on the face plane
            val ref = if (abs(normal.z) < 0.85f) Vec3(0f, 0f, 1f) else Vec3(1f, 0f, 0f)
            val uAxis = normal.cross(ref).normalized()
            val vAxis = normal.cross(uAxis).normalized()

            val pts2D = ArrayList<Vec2>(points3D.size)
            for (p in points3D) {
                val rel = p - center
                pts2D.add(Vec2(rel.dot(uAxis), rel.dot(vAxis)))
            }
            pts2D.sortWith(compareBy<Vec2> { it.x }.thenBy { it.y })

            fun cross2D(o: Vec2, a: Vec2, b: Vec2): Float =
                (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x)

            val lower = ArrayList<Vec2>()
            for (p in pts2D) {
                if (lower.isNotEmpty() && lower.last().distanceTo(p) < 0.05f) continue
                while (lower.size >= 2 && cross2D(lower[lower.size - 2], lower.last(), p) <= 0f) {
                    lower.removeAt(lower.size - 1)
                }
                lower.add(p)
            }
            val upper = ArrayList<Vec2>()
            for (i in pts2D.indices.reversed()) {
                val p = pts2D[i]
                if (upper.isNotEmpty() && upper.last().distanceTo(p) < 0.05f) continue
                while (upper.size >= 2 && cross2D(upper[upper.size - 2], upper.last(), p) <= 0f) {
                    upper.removeAt(upper.size - 1)
                }
                upper.add(p)
            }
            if (lower.isEmpty() || upper.isEmpty()) return emptyList()
            lower.removeAt(lower.size - 1)
            upper.removeAt(upper.size - 1)
            val hull2D = lower + upper
            if (hull2D.size < 3) return emptyList()

            // Subsample to at most 16 vertices and inset slightly (90%) toward the hull centroid for Orca-style patch look
            val maxVerts = 16
            val sampled = if (hull2D.size <= maxVerts) {
                hull2D
            } else {
                List(maxVerts) { idx -> hull2D[(idx * hull2D.size) / maxVerts] }
            }

            var cx2 = 0f
            var cy2 = 0f
            for (p in sampled) {
                cx2 += p.x
                cy2 += p.y
            }
            cx2 /= sampled.size
            cy2 /= sampled.size

            val insetFactor = 0.88f
            return sampled.map { p ->
                val ix = cx2 + (p.x - cx2) * insetFactor
                val iy = cy2 + (p.y - cy2) * insetFactor
                center + (uAxis * ix) + (vAxis * iy)
            }
        }

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

    /**
     * Transforms a local surface normal into a unit world-space normal vector.
     */
    fun transformNormal(localNormal: Vec3): Vec3 {
        var x = localNormal.x
        var y = localNormal.y
        var z = localNormal.z

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

        val invSx = if (abs(scale.x) > 1e-5f) 1f / scale.x else 1f
        val invSy = if (abs(scale.y) > 1e-5f) 1f / scale.y else 1f
        val invSz = if (abs(scale.z) > 1e-5f) 1f / scale.z else 1f
        return Vec3(x * invSx, y * invSy, z * invSz).normalized()
    }

    /**
     * Computes the exact Euler angles `(rxDeg, ryDeg, rzDeg)` (for `transformPoint`'s X -> Y -> Z rotation order)
     * that rotate `localNormal` to point straight down at the build plate `(0, 0, -1)`, preserving current Z yaw.
     */
    fun rotationToLayNormalFlat(localNormal: Vec3): Vec3 {
        val n = localNormal.normalized()
        val rYz = sqrt(n.y * n.y + n.z * n.z)
        val rxRad: Float
        val ryRad: Float
        if (rYz > 1e-5f) {
            rxRad = atan2(-n.y, -n.z)
            ryRad = atan2(n.x, rYz)
        } else {
            rxRad = 0f
            ryRad = if (n.x >= 0f) PI.toFloat() * 0.5f else -PI.toFloat() * 0.5f
        }

        fun cleanDeg(rad: Float): Float {
            var deg = rad * (180f / PI.toFloat())
            val nearestInt = deg.roundToInt().toFloat()
            if (abs(deg - nearestInt) < 0.05f) deg = nearestInt
            if (abs(deg) < 0.01f) deg = 0f
            return deg
        }

        return Vec3(
            x = cleanDeg(rxRad),
            y = cleanDeg(ryRad),
            z = rotationDeg.z
        )
    }

    fun worldBoundingBox(): BoundingBox {
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY

        for (c in mesh.supportVertices) {
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
