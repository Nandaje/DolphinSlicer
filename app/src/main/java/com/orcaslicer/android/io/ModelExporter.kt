package com.orcaslicer.android.io

import com.orcaslicer.android.model.FilamentProfile
import com.orcaslicer.android.model.PrinterProfile
import com.orcaslicer.android.model.ProcessProfile
import com.orcaslicer.android.model.SceneObject
import com.orcaslicer.android.model.Triangle
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * ============================================================================
 * 3D Model, 3MF Project & G-Code Exporter (`ModelExporter.kt`)
 * ============================================================================
 * Handles exporting build-plate scenes and sliced toolpaths to Android storage:
 * - [exportGCode]: Writes generated `.gcode` text to a user-selected document URI.
 * - [exportBinaryStl]: Combines all visible build-plate objects into a binary `.stl`.
 * - [export3mfProject]: Packages build-plate meshes and active Printer/Filament/Process
 *   settings into a standard `.3mf` ZIP archive (`3D/3dmodel.model`).
 */
object ModelExporter {

    fun exportGCode(outputStream: OutputStream, gcodeText: String): Result<Unit> {
        return try {
            outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(gcodeText)
                writer.flush()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Failed to export G-code: ${e.message}", e))
        }
    }

    /**
     * Exports all visible objects on the build plate as a binary `.stl` file.
     */
    fun exportBinaryStl(
        outputStream: OutputStream,
        objects: List<SceneObject>
    ): Result<Unit> {
        return try {
            val allTriangles = ArrayList<Triangle>()
            for (obj in objects) {
                if (obj.visible) {
                    allTriangles.addAll(obj.worldTriangles())
                }
            }
            if (allTriangles.isEmpty()) {
                return Result.failure(IllegalArgumentException("No visible 3D objects on the build plate to export."))
            }

            val header = ByteArray(80)
            val titleBytes = "Exported by OrcaSlicer Android".toByteArray(Charsets.US_ASCII)
            System.arraycopy(titleBytes, 0, header, 0, titleBytes.size.coerceAtMost(80))
            outputStream.write(header)

            val countBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
            countBuf.putInt(allTriangles.size)
            outputStream.write(countBuf.array())

            val triBuf = ByteBuffer.allocate(50).order(ByteOrder.LITTLE_ENDIAN)
            for (tri in allTriangles) {
                triBuf.clear()
                triBuf.putFloat(tri.normal.x)
                triBuf.putFloat(tri.normal.y)
                triBuf.putFloat(tri.normal.z)
                triBuf.putFloat(tri.v0.x)
                triBuf.putFloat(tri.v0.y)
                triBuf.putFloat(tri.v0.z)
                triBuf.putFloat(tri.v1.x)
                triBuf.putFloat(tri.v1.y)
                triBuf.putFloat(tri.v1.z)
                triBuf.putFloat(tri.v2.x)
                triBuf.putFloat(tri.v2.y)
                triBuf.putFloat(tri.v2.z)
                triBuf.putShort(0)
                outputStream.write(triBuf.array())
            }
            outputStream.flush()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Failed to export binary STL: ${e.message}", e))
        }
    }

    /**
     * Exports a complete `.3mf` project archive compatible with OrcaSlicer / Bambu Studio / PrusaSlicer.
     */
    fun export3mfProject(
        outputStream: OutputStream,
        objects: List<SceneObject>,
        printer: PrinterProfile,
        filament: FilamentProfile,
        process: ProcessProfile
    ): Result<Unit> {
        return try {
            val visibleObjects = objects.filter { it.visible }
            if (visibleObjects.isEmpty()) {
                return Result.failure(IllegalArgumentException("No visible 3D objects on the build plate to export."))
            }

            ZipOutputStream(outputStream).use { zip ->
                // 1. [Content_Types].xml
                zip.putNextEntry(ZipEntry("[Content_Types].xml"))
                val contentTypes = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="model" ContentType="application/vnd.ms-package.3dmanufacturing-3dmodel+xml"/>
                      <Default Extension="config" ContentType="text/xml"/>
                    </Types>
                """.trimIndent()
                zip.write(contentTypes.toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                // 2. _rels/.rels
                zip.putNextEntry(ZipEntry("_rels/.rels"))
                val rels = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Target="/3D/3dmodel.model" Id="rel-1" Type="http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel"/>
                    </Relationships>
                """.trimIndent()
                zip.write(rels.toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                // 3. 3D/3dmodel.model
                zip.putNextEntry(ZipEntry("3D/3dmodel.model"))
                val modelXml = build3mfModelXml(visibleObjects)
                zip.write(modelXml.toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                // 4. Metadata/project_settings.config (OrcaSlicer profile metadata)
                zip.putNextEntry(ZipEntry("Metadata/project_settings.config"))
                val configJson = """
                    {
                      "printer_model": "${escapeXml(printer.name)}",
                      "printable_width": ${printer.bedWidthX},
                      "printable_depth": ${printer.bedDepthY},
                      "printable_height": ${printer.maxPrintHeightZ},
                      "nozzle_diameter": [${printer.nozzleDiameterMm}],
                      "filament_type": ["${escapeXml(filament.materialType)}"],
                      "nozzle_temperature": [${filament.nozzleTempOtherC}],
                      "bed_temperature": [${filament.bedTempOtherC}],
                      "layer_height": ${process.layerHeightMm},
                      "initial_layer_print_height": ${process.initialLayerHeightMm},
                      "wall_loops": ${process.wallLoops},
                      "sparse_infill_density": "${process.sparseInfillDensityPercent}%",
                      "sparse_infill_pattern": "${process.sparseInfillPattern.name.lowercase(Locale.ROOT)}",
                      "enable_support": ${if (process.enableSupport) 1 else 0}
                    }
                """.trimIndent()
                zip.write(configJson.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Failed to export 3MF project: ${e.message}", e))
        }
    }

    private fun build3mfModelXml(objects: List<SceneObject>): String {
        val sb = StringBuilder(65536)
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<model unit=\"millimeter\" xml:lang=\"en-US\" xmlns=\"http://schemas.microsoft.com/3dmanufacturing/core/2015/02\">\n")
        sb.append("  <metadata name=\"Application\">OrcaSlicer-Android-2.2.0</metadata>\n")
        sb.append("  <resources>\n")

        for ((idx, obj) in objects.withIndex()) {
            val objId = idx + 1
            val tris = obj.worldTriangles()
            sb.append("    <object id=\"$objId\" name=\"${escapeXml(obj.name)}\" type=\"model\">\n")
            sb.append("      <mesh>\n")
            sb.append("        <vertices>\n")
            for (t in tris) {
                sb.append(String.format(Locale.US, "          <vertex x=\"%.4f\" y=\"%.4f\" z=\"%.4f\"/>\n", t.v0.x, t.v0.y, t.v0.z))
                sb.append(String.format(Locale.US, "          <vertex x=\"%.4f\" y=\"%.4f\" z=\"%.4f\"/>\n", t.v1.x, t.v1.y, t.v1.z))
                sb.append(String.format(Locale.US, "          <vertex x=\"%.4f\" y=\"%.4f\" z=\"%.4f\"/>\n", t.v2.x, t.v2.y, t.v2.z))
            }
            sb.append("        </vertices>\n")
            sb.append("        <triangles>\n")
            for (tIdx in tris.indices) {
                val vBase = tIdx * 3
                sb.append("          <triangle v1=\"$vBase\" v2=\"${vBase + 1}\" v3=\"${vBase + 2}\"/>\n")
            }
            sb.append("        </triangles>\n")
            sb.append("      </mesh>\n")
            sb.append("    </object>\n")
        }

        sb.append("  </resources>\n")
        sb.append("  <build>\n")
        for (idx in objects.indices) {
            val objId = idx + 1
            sb.append("    <item objectid=\"$objId\"/>\n")
        }
        sb.append("  </build>\n")
        sb.append("</model>\n")
        return sb.toString()
    }

    private fun escapeXml(input: String): String =
        input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
