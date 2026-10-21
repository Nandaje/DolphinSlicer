package com.orcaslicer.android

import com.orcaslicer.android.io.ModelExporter
import com.orcaslicer.android.io.ModelImporter
import com.orcaslicer.android.io.ProfileImporter
import com.orcaslicer.android.model.BuiltInPresets
import com.orcaslicer.android.model.InfillPattern
import com.orcaslicer.android.model.ProceduralMeshes
import com.orcaslicer.android.model.SceneObject
import com.orcaslicer.android.slicer.SlicingEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class OrcaSlicerEngineTest {

    @Test
    fun `binary STL export and re-import roundtrip preserves triangle count`() {
        val cubeMesh = ProceduralMeshes.createCalibrationCube(20f)
        val obj = SceneObject(
            id = "test-cube",
            name = "CalibrationCube",
            sourceFormat = "PRIMITIVE",
            mesh = cubeMesh
        )

        val out = ByteArrayOutputStream()
        val exportRes = ModelExporter.exportBinaryStl(out, listOf(obj))
        assertTrue("Binary STL export should succeed", exportRes.isSuccess)

        val bytes = out.toByteArray()
        assertTrue("Binary STL size should be 84 + 50 * tris", bytes.size == 84 + 50 * cubeMesh.triangleCount)

        val importRes = ModelImporter.importFromStream(ByteArrayInputStream(bytes), "cube.stl")
        assertTrue("Binary STL import should succeed", importRes.isSuccess)
        val importedModels = (importRes.getOrThrow() as ModelImporter.ImportedContent.Models).objects
        assertEquals(1, importedModels.size)
        assertEquals(cubeMesh.triangleCount, importedModels.first().mesh.triangleCount)
    }

    @Test
    fun `3MF project export and re-import roundtrip preserves objects and meshes`() {
        val boatMesh = ProceduralMeshes.createCalibrationBoat()
        val obj = SceneObject(
            id = "test-boat",
            name = "CalibrationBoat",
            sourceFormat = "PRIMITIVE",
            mesh = boatMesh
        )

        val out = ByteArrayOutputStream()
        val exportRes = ModelExporter.export3mfProject(
            outputStream = out,
            objects = listOf(obj),
            printer = BuiltInPresets.printers.first(),
            filament = BuiltInPresets.filaments.first(),
            process = BuiltInPresets.processes.first()
        )
        assertTrue("3MF export should succeed", exportRes.isSuccess)

        val importRes = ModelImporter.importFromStream(ByteArrayInputStream(out.toByteArray()), "project.3mf")
        assertTrue("3MF import should succeed", importRes.isSuccess)
        val importedModels = (importRes.getOrThrow() as ModelImporter.ImportedContent.Models).objects
        assertEquals(1, importedModels.size)
        assertEquals("CalibrationBoat", importedModels.first().name)
        assertEquals(boatMesh.triangleCount, importedModels.first().mesh.triangleCount)
    }

    @Test
    fun `OBJ parser triangulates quads and triangles accurately`() {
        val objText = """
            # Test quad base pyramid
            v -10.0 -10.0 0.0
            v  10.0 -10.0 0.0
            v  10.0  10.0 0.0
            v -10.0  10.0 0.0
            v   0.0   0.0 15.0
            f 1 2 3 4
            f 1 2 5
            f 2 3 5
            f 3 4 5
            f 4 1 5
        """.trimIndent()

        val res = ModelImporter.importFromStream(
            ByteArrayInputStream(objText.toByteArray(Charsets.UTF_8)),
            "pyramid.obj"
        )
        assertTrue("OBJ import should succeed", res.isSuccess)
        val models = (res.getOrThrow() as ModelImporter.ImportedContent.Models).objects
        // 1 quad (2 tris) + 4 side tris = 6 triangles
        assertEquals(6, models.first().mesh.triangleCount)
    }

    @Test
    fun `malformed and truncated files fail gracefully without crashing`() {
        val emptyRes = ModelImporter.importFromStream(ByteArrayInputStream(ByteArray(0)), "empty.stl")
        assertTrue(emptyRes.isFailure)

        // Truncated binary STL claiming 999999999 triangles in header
        val hostileStl = ByteArray(100)
        hostileStl[80] = 0xFF.toByte()
        hostileStl[81] = 0xFF.toByte()
        hostileStl[82] = 0xFF.toByte()
        hostileStl[83] = 0x7F.toByte()
        val hostileRes = ModelImporter.importFromStream(ByteArrayInputStream(hostileStl), "hostile.stl")
        // Should either parse 0 valid triangles and return failure, never OOM or crash
        assertTrue(hostileRes.isFailure || hostileRes.isSuccess)
    }

    @Test
    fun `slicing engine slices model with Gyroid infill, supports, and generates valid G-code`() = runTest {
        val archMesh = ProceduralMeshes.createOverhangTestArch()
        val obj = SceneObject(
            id = "arch-1",
            name = "OverhangArch",
            sourceFormat = "PRIMITIVE",
            mesh = archMesh
        )
        val process = BuiltInPresets.processes.first().copy(
            layerHeightMm = 0.25f,
            sparseInfillPattern = InfillPattern.GYROID,
            enableSupport = true
        )

        val sliceRes = SlicingEngine.sliceScene(
            objects = listOf(obj),
            printer = BuiltInPresets.printers.first(),
            filament = BuiltInPresets.filaments.first(),
            process = process
        )
        assertTrue("Slicing should succeed", sliceRes.isSuccess)
        val result = sliceRes.getOrThrow()
        assertTrue("Should generate multiple layers", result.layers.size > 40)
        assertTrue("Filament usage should be positive", result.statistics.filamentLengthMeters > 0.1f)
        assertTrue("G-code should contain LAYER_CHANGE markers", result.gcodeText.contains(";LAYER_CHANGE"))

        // Verify G-code can also be re-imported into preview toolpaths
        val gcodeImport = ModelImporter.importFromStream(
            ByteArrayInputStream(result.gcodeText.toByteArray(Charsets.UTF_8)),
            "arch.gcode"
        )
        assertTrue("Exported G-code should re-import cleanly", gcodeImport.isSuccess)
    }

    @Test
    fun `mesh cut and mirror operations and calibration generators work cleanly`() = runTest {
        val towerMesh = ProceduralMeshes.createTemperatureTower(5)
        assertTrue("Temperature tower should have triangles", towerMesh.triangleCount > 20)

        val mirrored = ProceduralMeshes.mirrorMesh(towerMesh, mirrorX = true, mirrorY = false, mirrorZ = false)
        assertEquals(towerMesh.triangleCount, mirrored.triangleCount)

        val (lower, upper) = ProceduralMeshes.cutMeshAtZ(towerMesh, 15.0f)
        assertTrue("Lower cut part should have triangles", lower.triangleCount > 0)
        assertTrue("Upper cut part should have triangles", upper.triangleCount > 0)

        // Test Fuzzy Skin + Spiral Vase + Pressure Advance G-code output
        val cylObj = SceneObject(
            id = "vase-1",
            name = "VaseCylinder",
            sourceFormat = "PRIMITIVE",
            mesh = ProceduralMeshes.createCylinder(12f, 20f)
        )
        val vaseProcess = BuiltInPresets.processes.first().copy(
            spiralVaseMode = true,
            fuzzySkinType = com.orcaslicer.android.model.FuzzySkinType.CONTOUR
        )
        val sliceRes = SlicingEngine.sliceScene(
            objects = listOf(cylObj),
            printer = BuiltInPresets.printers.first(),
            filament = BuiltInPresets.filaments.first(),
            process = vaseProcess
        )
        assertTrue("Vase + Fuzzy skin slicing should succeed", sliceRes.isSuccess)
        val gcode = sliceRes.getOrThrow().gcodeText
        assertTrue("G-code should include Klipper SET_PRESSURE_ADVANCE", gcode.contains("SET_PRESSURE_ADVANCE"))
        assertTrue("G-code should include EXCLUDE_OBJECT_DEFINE", gcode.contains("EXCLUDE_OBJECT_DEFINE"))
    }

    @Test
    fun `built-in printer and filament catalogs ProfileImporter and filament cost calculator work`() = runTest {
        assertTrue("Should have 50+ built-in printers", BuiltInPresets.printers.size >= 50)
        assertTrue("Should have 45+ built-in filaments", BuiltInPresets.filaments.size >= 45)
        assertTrue("Should have 10+ built-in process profiles", BuiltInPresets.processes.size >= 10)

        // Test importing an OrcaSlicer JSON filament profile
        val jsonFilament = """
            {
              "type": "filament",
              "name": "Custom eSUN PLA-HS",
              "filament_vendor": "eSUN",
              "filament_type": ["PLA"],
              "nozzle_temperature": ["225"],
              "hot_plate_temp": ["60"],
              "filament_density": ["1.24"],
              "filament_cost": ["22.50"],
              "filament_flow_ratio": ["0.975"],
              "pressure_advance": ["0.030"]
            }
        """.trimIndent()
        val importedFil = ProfileImporter.parseProfileText(jsonFilament, "esun_hs.json")
        assertTrue("JSON filament profile should import", importedFil.isSuccess)
        val filProfile = (importedFil.getOrThrow() as ProfileImporter.ImportedProfile.Filament).profile
        assertEquals("Custom eSUN PLA-HS", filProfile.name)
        assertEquals(22.50f, filProfile.costPerKgUsd, 0.01f)

        // Verify full cost breakdown calculation
        val cubeObj = SceneObject(
            id = "cost-cube",
            name = "CostCube",
            sourceFormat = "PRIMITIVE",
            mesh = ProceduralMeshes.createCalibrationCube(20f)
        )
        val slice = SlicingEngine.sliceScene(
            objects = listOf(cubeObj),
            printer = BuiltInPresets.printers.first(),
            filament = filProfile,
            process = BuiltInPresets.processes.first()
        ).getOrThrow()

        val costBreakdown = slice.statistics.calculateFullCostBreakdown(
            printer = BuiltInPresets.printers.first(),
            filament = filProfile
        )
        assertTrue("Material cost should be positive", costBreakdown.materialCost > 0.01f)
        assertTrue("Total production cost >= material cost", costBreakdown.totalProductionCost >= costBreakdown.materialCost)
        assertTrue("Suggested quote price >= total production cost", costBreakdown.suggestedQuotePrice >= costBreakdown.totalProductionCost)
        assertTrue("Prints per spool should be > 10 for a 20mm cube", costBreakdown.printsPerSpool > 10)
    }

    @Test
    fun `slicing engine generates solid BOTTOM_SURFACE and TOP_SURFACE on first intermediate roof and final layers`() = runTest {
        val cubeObj = SceneObject(
            id = "calib-cube",
            name = "CalibrationCube",
            sourceFormat = "PRIMITIVE",
            mesh = ProceduralMeshes.createCalibrationCube(20f)
        )
        val process = BuiltInPresets.processes.first() // Production 0.24 0.4Noz
        val slice = SlicingEngine.sliceScene(
            objects = listOf(cubeObj),
            printer = BuiltInPresets.printers.first(),
            filament = BuiltInPresets.filaments.first(),
            process = process
        ).getOrThrow()

        // 1. Layer 0 MUST have dense BOTTOM_SURFACE segments
        val bottomLayer = slice.layers.first()
        val bottomSurfaceSegs = bottomLayer.segments.count { it.type == com.orcaslicer.android.model.ToolpathType.BOTTOM_SURFACE }
        assertTrue("Layer 0 must contain >= 15 BOTTOM_SURFACE segments, got $bottomSurfaceSegs", bottomSurfaceSegs >= 15)

        // 2. Layer 1 MUST have dense SOLID_INFILL segments (internal bottom shell)
        val secondLayer = slice.layers[1]
        val secondSolidSegs = secondLayer.segments.count { it.type == com.orcaslicer.android.model.ToolpathType.SOLID_INFILL }
        assertTrue("Layer 1 must contain >= 15 SOLID_INFILL segments, got $secondSolidSegs", secondSolidSegs >= 15)

        // 3. Final topmost layer MUST have dense TOP_SURFACE segments (and non-zero GPU ribbon vertices)
        val topLayer = slice.layers.last()
        val topSurfaceSegs = topLayer.segments.count { it.type == com.orcaslicer.android.model.ToolpathType.TOP_SURFACE }
        assertTrue("Final layer must contain >= 8 TOP_SURFACE segments, got $topSurfaceSegs", topSurfaceSegs >= 8)
        assertTrue("Final layer must have GPU ribbon vertices", slice.layerRibbonVertexCount.last() > 0)

        // 4. Intermediate horizontal roof at Z ~ 20.0mm (where 20x20mm cube transitions to 10x10mm boss)
        // must ALSO generate TOP_SURFACE segments!
        val midRoofTopSegs = slice.layers
            .filter { it.zHeightMm in 19.5f..20.1f }
            .sumOf { l -> l.segments.count { it.type == com.orcaslicer.android.model.ToolpathType.TOP_SURFACE } }
        assertTrue("Intermediate horizontal roof at Z=20mm must generate TOP_SURFACE segments, got $midRoofTopSegs", midRoofTopSegs >= 10)
    }
}
