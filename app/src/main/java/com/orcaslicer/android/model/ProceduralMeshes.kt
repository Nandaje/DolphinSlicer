package com.orcaslicer.android.model

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object ProceduralMeshes {

    fun createCalibrationCube(sizeMm: Float = 20f): MeshData {
        val h = sizeMm * 0.5f
        val triangles = mutableListOf<Triangle>()
        addBox(triangles, -h, -h, 0f, h, h, sizeMm)
        val b = sizeMm * 0.25f
        addBox(triangles, -b, -b, sizeMm, b, b, sizeMm + 2.4f)
        return MeshData.fromRawTriangles(triangles)
    }

    fun createCylinder(radiusMm: Float = 12f, heightMm: Float = 25f, segments: Int = 36): MeshData {
        val triangles = mutableListOf<Triangle>()
        addCylinderAt(triangles, 0f, 0f, 0f, radiusMm, heightMm, segments)
        return MeshData.fromRawTriangles(triangles)
    }

    fun createSphere(radiusMm: Float = 14f, rings: Int = 18, sectors: Int = 28): MeshData {
        val triangles = mutableListOf<Triangle>()
        for (r in 0 until rings) {
            val phi0 = PI.toFloat() * (r.toFloat() / rings - 0.5f)
            val phi1 = PI.toFloat() * ((r + 1).toFloat() / rings - 0.5f)
            val z0 = sin(phi0) * radiusMm + radiusMm
            val zr0 = cos(phi0) * radiusMm
            val z1 = sin(phi1) * radiusMm + radiusMm
            val zr1 = cos(phi1) * radiusMm

            for (s in 0 until sectors) {
                val theta0 = 2f * PI.toFloat() * (s.toFloat() / sectors)
                val theta1 = 2f * PI.toFloat() * ((s + 1).toFloat() / sectors)

                val p00 = Vec3(cos(theta0) * zr0, sin(theta0) * zr0, z0)
                val p10 = Vec3(cos(theta1) * zr0, sin(theta1) * zr0, z0)
                val p11 = Vec3(cos(theta1) * zr1, sin(theta1) * zr1, z1)
                val p01 = Vec3(cos(theta0) * zr1, sin(theta0) * zr1, z1)

                if (r > 0) triangles.add(Triangle(p00, p10, p11))
                if (r < rings - 1) triangles.add(Triangle(p00, p11, p01))
            }
        }
        return MeshData.fromRawTriangles(triangles)
    }

    fun createCone(radiusMm: Float = 15f, heightMm: Float = 28f, segments: Int = 36): MeshData {
        val triangles = mutableListOf<Triangle>()
        val baseCenter = Vec3(0f, 0f, 0f)
        val apex = Vec3(0f, 0f, heightMm)
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / segments) * 2f * PI.toFloat()
            val b0 = Vec3(cos(a0) * radiusMm, sin(a0) * radiusMm, 0f)
            val b1 = Vec3(cos(a1) * radiusMm, sin(a1) * radiusMm, 0f)
            triangles.add(Triangle(baseCenter, b1, b0))
            triangles.add(Triangle(b0, b1, apex))
        }
        return MeshData.fromRawTriangles(triangles)
    }

    fun createTorus(majorRadius: Float = 16f, minorRadius: Float = 5f, majorSegs: Int = 32, minorSegs: Int = 16): MeshData {
        val triangles = mutableListOf<Triangle>()
        for (i in 0 until majorSegs) {
            val u0 = (i.toFloat() / majorSegs) * 2f * PI.toFloat()
            val u1 = ((i + 1).toFloat() / majorSegs) * 2f * PI.toFloat()
            for (j in 0 until minorSegs) {
                val v0 = (j.toFloat() / minorSegs) * 2f * PI.toFloat()
                val v1 = ((j + 1).toFloat() / minorSegs) * 2f * PI.toFloat()

                fun pt(u: Float, v: Float): Vec3 {
                    val r = majorRadius + minorRadius * cos(v)
                    return Vec3(r * cos(u), r * sin(u), minorRadius * sin(v) + minorRadius)
                }

                val p00 = pt(u0, v0)
                val p10 = pt(u1, v0)
                val p11 = pt(u1, v1)
                val p01 = pt(u0, v1)
                triangles.add(Triangle(p00, p10, p11))
                triangles.add(Triangle(p00, p11, p01))
            }
        }
        return MeshData.fromRawTriangles(triangles)
    }

    fun createOverhangTestArch(): MeshData {
        val triangles = mutableListOf<Triangle>()
        addBox(triangles, -22f, -10f, 0f, 22f, 10f, 2.4f)
        addBox(triangles, -20f, -8f, 2.4f, -12f, 8f, 18f)
        val steps = 8
        for (i in 0 until steps) {
            val z0 = 2.4f + i * 2.0f
            val z1 = z0 + 2.0f
            val xShift0 = i * 2.2f
            val xShift1 = (i + 1) * 2.2f
            addExtrudedQuadX(
                triangles,
                x0Bottom = 6f + xShift0,
                x1Bottom = 14f + xShift0,
                x0Top = 6f + xShift1,
                x1Top = 14f + xShift1,
                yMin = -8f,
                yMax = 8f,
                zBottom = z0,
                zTop = z1
            )
        }
        addBox(triangles, -20f, -8f, 18f, 18f, 8f, 22f)
        return MeshData.fromRawTriangles(triangles)
    }

    fun createCalibrationBoat(): MeshData {
        val triangles = mutableListOf<Triangle>()
        addExtrudedFrustum(
            triangles,
            bxMin = -18f, byMin = -10f, bxMax = 18f, byMax = 10f,
            txMin = -24f, tyMin = -13f, txMax = 26f, tyMax = 13f,
            z0 = 0f, z1 = 10f
        )
        addBox(triangles, -22f, -12f, 10f, 24f, 12f, 13f)
        addBox(triangles, -10f, -9f, 13f, -6f, -5f, 25f)
        addBox(triangles, -10f, 5f, 13f, -6f, 9f, 25f)
        addBox(triangles, 6f, -9f, 13f, 10f, -5f, 25f)
        addBox(triangles, 6f, 5f, 13f, 10f, 9f, 25f)
        addExtrudedFrustum(
            triangles,
            bxMin = -12f, byMin = -10f, bxMax = 12f, byMax = 10f,
            txMin = -9f, tyMin = -8f, txMax = 9f, tyMax = 8f,
            z0 = 25f, z1 = 29f
        )
        addExtrudedFrustum(
            triangles,
            bxMin = 2f, byMin = -2.5f, bxMax = 7f, byMax = 2.5f,
            txMin = 3f, tyMin = -2.5f, txMax = 8f, tyMax = 2.5f,
            z0 = 29f, z1 = 35f
        )
        return MeshData.fromRawTriangles(triangles)
    }

    // =========================================================================
    // OrcaSlicer Built-In Calibration Generators
    // =========================================================================

    /**
     * OrcaSlicer Temperature Tower: 5 stacked bridge & overhang tiers.
     */
    fun createTemperatureTower(tiers: Int = 5): MeshData {
        val triangles = mutableListOf<Triangle>()
        // Base
        addBox(triangles, -20f, -8f, 0f, 20f, 8f, 1.6f)
        val tierH = 8.0f
        for (t in 0 until tiers) {
            val z0 = 1.6f + t * tierH
            val zBridge = z0 + tierH - 1.6f
            val zTop = z0 + tierH
            // Left tower block
            addBox(triangles, -18f, -6f, z0, -10f, 6f, zBridge)
            // Right tower block with 45 deg overhang notch
            addExtrudedFrustum(
                triangles,
                bxMin = 10f, byMin = -6f, bxMax = 16f, byMax = 6f,
                txMin = 10f, tyMin = -6f, txMax = 19.5f, tyMax = 6f,
                z0 = z0, z1 = zBridge
            )
            // Bridge spanning across
            addBox(triangles, -18f, -6f, zBridge, 18f, 6f, zTop)
        }
        return MeshData.fromRawTriangles(triangles)
    }

    /**
     * OrcaSlicer Flow Rate Calibration Tiles (9 patches on build plate).
     */
    fun createFlowRateCalibrationSet(): MeshData {
        val triangles = mutableListOf<Triangle>()
        val tileW = 18f
        val gap = 24f
        for (row in -1..1) {
            for (col in -1..1) {
                val cx = col * gap
                val cy = row * gap
                addBox(triangles, cx - tileW * 0.5f, cy - tileW * 0.5f, 0f, cx + tileW * 0.5f, cy + tileW * 0.5f, 2.4f)
                // Raised boss indicator
                addBox(triangles, cx - 4f, cy - 4f, 2.4f, cx + 4f, cy + 4f, 3.2f)
            }
        }
        return MeshData.fromRawTriangles(triangles)
    }

    /**
     * OrcaSlicer Retraction Test (Two conical spires on a base plate).
     */
    fun createRetractionTowers(): MeshData {
        val triangles = mutableListOf<Triangle>()
        addBox(triangles, -22f, -10f, 0f, 22f, 10f, 2.0f)
        addCylinderAt(triangles, -13f, 0f, 2.0f, 4.5f, 28f, 24)
        addCylinderAt(triangles, 13f, 0f, 2.0f, 4.5f, 28f, 24)
        return MeshData.fromRawTriangles(triangles)
    }

    /**
     * OrcaSlicer Pressure Advance (PA) Calibration Chevron / Tower.
     */
    fun createPressureAdvanceTower(): MeshData {
        val triangles = mutableListOf<Triangle>()
        // Hollow thin-wall rectangular tower with sharp 90-degree corners for PA evaluation
        val outer = 22f
        val inner = 18f
        val h = 30f
        // 4 walls
        addBox(triangles, -outer, -outer, 0f, outer, -inner, h)
        addBox(triangles, -outer, inner, 0f, outer, outer, h)
        addBox(triangles, -outer, -inner, 0f, -inner, inner, h)
        addBox(triangles, inner, -inner, 0f, outer, inner, h)
        return MeshData.fromRawTriangles(triangles)
    }

    /**
     * OrcaSlicer Max Volumetric Speed Race-Track Test.
     */
    fun createMaxVolumetricSpeedTest(): MeshData {
        val triangles = mutableListOf<Triangle>()
        addCylinderAt(triangles, 0f, 0f, 0f, 24f, 32f, 40)
        return MeshData.fromRawTriangles(triangles)
    }

    // =========================================================================
    // Mesh Editing Operations: Mirror & Cut at Z Plane
    // =========================================================================

    fun mirrorMesh(mesh: MeshData, mirrorX: Boolean, mirrorY: Boolean, mirrorZ: Boolean): MeshData {
        val sx = if (mirrorX) -1f else 1f
        val sy = if (mirrorY) -1f else 1f
        val sz = if (mirrorZ) -1f else 1f
        val flipWinding = (sx * sy * sz) < 0f

        val mirrored = mesh.triangles.map { t ->
            val v0 = Vec3(t.v0.x * sx, t.v0.y * sy, t.v0.z * sz)
            val v1 = Vec3(t.v1.x * sx, t.v1.y * sy, t.v1.z * sz)
            val v2 = Vec3(t.v2.x * sx, t.v2.y * sy, t.v2.z * sz)
            if (flipWinding) Triangle(v0, v2, v1) else Triangle(v0, v1, v2)
        }
        return MeshData.fromRawTriangles(mirrored)
    }

    /**
     * Splits a mesh horizontally at `cutHeightZ` into (lowerPart, upperPart).
     */
    fun cutMeshAtZ(mesh: MeshData, cutHeightZ: Float): Pair<MeshData, MeshData> {
        val lowerTris = ArrayList<Triangle>()
        val upperTris = ArrayList<Triangle>()

        for (t in mesh.triangles) {
            val avgZ = (t.v0.z + t.v1.z + t.v2.z) / 3f
            if (avgZ <= cutHeightZ) {
                lowerTris.add(
                    Triangle(
                        Vec3(t.v0.x, t.v0.y, min(t.v0.z, cutHeightZ)),
                        Vec3(t.v1.x, t.v1.y, min(t.v1.z, cutHeightZ)),
                        Vec3(t.v2.x, t.v2.y, min(t.v2.z, cutHeightZ))
                    )
                )
            } else {
                upperTris.add(
                    Triangle(
                        Vec3(t.v0.x, t.v0.y, max(t.v0.z, cutHeightZ) - cutHeightZ),
                        Vec3(t.v1.x, t.v1.y, max(t.v1.z, cutHeightZ) - cutHeightZ),
                        Vec3(t.v2.x, t.v2.y, max(t.v2.z, cutHeightZ) - cutHeightZ)
                    )
                )
            }
        }
        return MeshData.fromRawTriangles(lowerTris) to MeshData.fromRawTriangles(upperTris)
    }

    private fun addCylinderAt(
        out: MutableList<Triangle>,
        cx: Float,
        cy: Float,
        z0: Float,
        radiusMm: Float,
        heightMm: Float,
        segments: Int
    ) {
        val z1 = z0 + heightMm
        val centerBottom = Vec3(cx, cy, z0)
        val centerTop = Vec3(cx, cy, z1)

        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * (2f * PI.toFloat())
            val a1 = ((i + 1).toFloat() / segments) * (2f * PI.toFloat())
            val x0 = cx + cos(a0) * radiusMm
            val y0 = cy + sin(a0) * radiusMm
            val x1 = cx + cos(a1) * radiusMm
            val y1 = cy + sin(a1) * radiusMm

            val b0 = Vec3(x0, y0, z0)
            val b1 = Vec3(x1, y1, z0)
            val t0 = Vec3(x0, y0, z1)
            val t1 = Vec3(x1, y1, z1)

            out.add(Triangle(centerBottom, b1, b0))
            out.add(Triangle(centerTop, t0, t1))
            out.add(Triangle(b0, b1, t1))
            out.add(Triangle(b0, t1, t0))
        }
    }

    private fun addBox(
        out: MutableList<Triangle>,
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float
    ) {
        addExtrudedFrustum(out, x0, y0, x1, y1, x0, y0, x1, y1, z0, z1)
    }

    private fun addExtrudedQuadX(
        out: MutableList<Triangle>,
        x0Bottom: Float, x1Bottom: Float,
        x0Top: Float, x1Top: Float,
        yMin: Float, yMax: Float,
        zBottom: Float, zTop: Float
    ) {
        addExtrudedFrustum(
            out,
            x0Bottom, yMin, x1Bottom, yMax,
            x0Top, yMin, x1Top, yMax,
            zBottom, zTop
        )
    }

    private fun addExtrudedFrustum(
        out: MutableList<Triangle>,
        bxMin: Float, byMin: Float, bxMax: Float, byMax: Float,
        txMin: Float, tyMin: Float, txMax: Float, tyMax: Float,
        z0: Float, z1: Float
    ) {
        val b00 = Vec3(bxMin, byMin, z0)
        val b10 = Vec3(bxMax, byMin, z0)
        val b11 = Vec3(bxMax, byMax, z0)
        val b01 = Vec3(bxMin, byMax, z0)

        val t00 = Vec3(txMin, tyMin, z1)
        val t10 = Vec3(txMax, tyMin, z1)
        val t11 = Vec3(txMax, tyMax, z1)
        val t01 = Vec3(txMin, tyMax, z1)

        out.add(Triangle(b00, b01, b11))
        out.add(Triangle(b00, b11, b10))
        out.add(Triangle(t00, t10, t11))
        out.add(Triangle(t00, t11, t01))
        out.add(Triangle(b00, b10, t10))
        out.add(Triangle(b00, t10, t00))
        out.add(Triangle(b11, b01, t01))
        out.add(Triangle(b11, t01, t11))
        out.add(Triangle(b01, b00, t00))
        out.add(Triangle(b01, t00, t01))
        out.add(Triangle(b10, b11, t11))
        out.add(Triangle(b10, t11, t10))
    }
}
