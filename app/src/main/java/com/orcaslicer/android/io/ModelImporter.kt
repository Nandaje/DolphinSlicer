package com.orcaslicer.android.io

import com.orcaslicer.android.model.MeshData
import com.orcaslicer.android.model.PrintStatistics
import com.orcaslicer.android.model.SceneObject
import com.orcaslicer.android.model.SliceLayer
import com.orcaslicer.android.model.SliceResult
import com.orcaslicer.android.model.ToolpathSegment
import com.orcaslicer.android.model.ToolpathType
import com.orcaslicer.android.model.Triangle
import com.orcaslicer.android.model.Vec3
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipInputStream
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

object ModelImporter {

    private const val MAX_FILE_BYTES = 120 * 1024 * 1024 // 120 MB safety cap
    private const val MAX_TRIANGLES = 1_500_000          // 1.5M triangles safety cap

    sealed class ImportedContent {
        data class Models(val objects: List<SceneObject>) : ImportedContent()
        data class GCodePreview(val sliceResult: SliceResult, val fileName: String) : ImportedContent()
    }

    fun importFromStream(
        inputStream: InputStream,
        fileName: String
    ): Result<ImportedContent> {
        return try {
            val bytes = readBoundedBytes(inputStream, MAX_FILE_BYTES)
                ?: return Result.failure(IllegalArgumentException("File exceeds maximum allowed size (120 MB)."))

            if (bytes.isEmpty()) {
                return Result.failure(IllegalArgumentException("File '$fileName' is empty."))
            }

            val lower = fileName.lowercase(Locale.ROOT)
            val baseName = fileName.substringBeforeLast('.').ifBlank { "Imported_Model" }

            when {
                lower.endsWith(".gcode") || lower.endsWith(".gco") -> {
                    parseGCode(bytes, fileName).map { ImportedContent.GCodePreview(it, fileName) }
                }
                lower.endsWith(".3mf") || isZipHeader(bytes) -> {
                    parse3mf(bytes, baseName).map { ImportedContent.Models(it) }
                }
                lower.endsWith(".obj") -> {
                    parseObj(bytes, baseName).map { mesh ->
                        ImportedContent.Models(
                            listOf(
                                SceneObject(
                                    id = UUID.randomUUID().toString(),
                                    name = baseName,
                                    sourceFormat = "OBJ",
                                    mesh = mesh
                                )
                            )
                        )
                    }
                }
                else -> {
                    // Default / .stl (auto-detect binary vs ASCII STL)
                    parseStl(bytes, baseName).map { mesh ->
                        ImportedContent.Models(
                            listOf(
                                SceneObject(
                                    id = UUID.randomUUID().toString(),
                                    name = baseName,
                                    sourceFormat = "STL",
                                    mesh = mesh
                                )
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Failed to import '$fileName': ${e.message ?: "malformed file"}", e))
        }
    }

    private fun readBoundedBytes(input: InputStream, maxBytes: Int): ByteArray? {
        val buffer = ByteArray(64 * 1024)
        val out = java.io.ByteArrayOutputStream()
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            total += read
            if (total > maxBytes) return null
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private fun isZipHeader(bytes: ByteArray): Boolean =
        bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() &&
            bytes[1] == 0x4B.toByte() &&
            bytes[2] == 0x03.toByte() &&
            bytes[3] == 0x04.toByte()

    fun parseStl(bytes: ByteArray, modelName: String): Result<MeshData> {
        if (bytes.size >= 84) {
            val triCountLong = ByteBuffer.wrap(bytes, 80, 4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .int.toLong() and 0xFFFFFFFFL
            val expectedBinarySize = 84L + triCountLong * 50L
            // Check if exact binary size matches or if header does not start with ASCII "solid" + "facet"
            val headerSample = String(bytes, 0, min(bytes.size, 256), Charsets.US_ASCII)
            val looksAscii = headerSample.trimStart().startsWith("solid", ignoreCase = true) &&
                headerSample.contains("facet", ignoreCase = true)

            if (!looksAscii || expectedBinarySize == bytes.size.toLong()) {
                val binRes = parseBinaryStl(bytes)
                if (binRes.isSuccess) return binRes
            }
        }
        return parseAsciiStl(bytes, modelName)
    }

    private fun parseBinaryStl(bytes: ByteArray): Result<MeshData> {
        if (bytes.size < 84) {
            return Result.failure(IllegalArgumentException("Binary STL is smaller than 84-byte header."))
        }
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val rawCount = buf.getInt(80).toLong() and 0xFFFFFFFFL
        val availableTriangles = ((bytes.size - 84) / 50).toLong()
        val triCount = min(rawCount, availableTriangles).coerceAtMost(MAX_TRIANGLES.toLong()).toInt()
        if (triCount <= 0) {
            return Result.failure(IllegalArgumentException("Binary STL contains 0 valid triangles."))
        }

        val triangles = ArrayList<Triangle>(triCount)
        var offset = 84
        for (i in 0 until triCount) {
            if (offset + 50 > bytes.size) break
            val nx = buf.getFloat(offset)
            val ny = buf.getFloat(offset + 4)
            val nz = buf.getFloat(offset + 8)

            val v0 = Vec3(buf.getFloat(offset + 12), buf.getFloat(offset + 16), buf.getFloat(offset + 20))
            val v1 = Vec3(buf.getFloat(offset + 24), buf.getFloat(offset + 28), buf.getFloat(offset + 32))
            val v2 = Vec3(buf.getFloat(offset + 36), buf.getFloat(offset + 40), buf.getFloat(offset + 44))
            offset += 50

            if (v0.isFinite() && v1.isFinite() && v2.isFinite()) {
                val n = Vec3(nx, ny, nz)
                val validNormal = if (n.isFinite() && n.length() > 0.1f) n.normalized() else Triangle.computeNormal(v0, v1, v2)
                triangles.add(Triangle(v0, v1, v2, validNormal))
            }
        }

        if (triangles.isEmpty()) {
            return Result.failure(IllegalArgumentException("No finite triangles found in binary STL."))
        }
        return Result.success(MeshData.fromRawTriangles(triangles))
    }

    private fun parseAsciiStl(bytes: ByteArray, modelName: String): Result<MeshData> {
        val reader = BufferedReader(InputStreamReader(ByteArrayInputStream(bytes), Charsets.UTF_8))
        val triangles = ArrayList<Triangle>()
        val verts = ArrayList<Vec3>(3)

        var line: String? = reader.readLine()
        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.startsWith("vertex", ignoreCase = true)) {
                val parts = trimmed.split(Regex("\\s+"))
                if (parts.size >= 4) {
                    val x = parts[1].toFloatOrNull()
                    val y = parts[2].toFloatOrNull()
                    val z = parts[3].toFloatOrNull()
                    if (x != null && y != null && z != null) {
                        val v = Vec3(x, y, z)
                        if (v.isFinite()) verts.add(v)
                    }
                }
                if (verts.size == 3) {
                    triangles.add(Triangle(verts[0], verts[1], verts[2]))
                    verts.clear()
                    if (triangles.size >= MAX_TRIANGLES) break
                }
            }
            line = reader.readLine()
        }

        if (triangles.isEmpty()) {
            return Result.failure(IllegalArgumentException("No valid triangles found in STL '$modelName'."))
        }
        return Result.success(MeshData.fromRawTriangles(triangles))
    }

    fun parseObj(bytes: ByteArray, modelName: String): Result<MeshData> {
        val reader = BufferedReader(InputStreamReader(ByteArrayInputStream(bytes), Charsets.UTF_8))
        val vertices = ArrayList<Vec3>()
        val triangles = ArrayList<Triangle>()

        var line: String? = reader.readLine()
        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                line = reader.readLine()
                continue
            }
            if (trimmed.startsWith("v ")) {
                val parts = trimmed.split(Regex("\\s+"))
                if (parts.size >= 4) {
                    val x = parts[1].toFloatOrNull()
                    val y = parts[2].toFloatOrNull()
                    val z = parts[3].toFloatOrNull()
                    if (x != null && y != null && z != null) {
                        val v = Vec3(x, y, z)
                        if (v.isFinite()) vertices.add(v)
                    }
                }
            } else if (trimmed.startsWith("f ")) {
                val parts = trimmed.split(Regex("\\s+"))
                if (parts.size >= 4) {
                    val faceIndices = ArrayList<Int>(parts.size - 1)
                    for (i in 1 until parts.size) {
                        val rawIdxStr = parts[i].substringBefore('/')
                        val rawIdx = rawIdxStr.toIntOrNull() ?: continue
                        val resolved = if (rawIdx > 0) rawIdx - 1 else vertices.size + rawIdx
                        if (resolved in 0 until vertices.size) {
                            faceIndices.add(resolved)
                        }
                    }
                    // Fan triangulation for triangles, quads, and n-gons
                    for (i in 1 until faceIndices.size - 1) {
                        val v0 = vertices[faceIndices[0]]
                        val v1 = vertices[faceIndices[i]]
                        val v2 = vertices[faceIndices[i + 1]]
                        triangles.add(Triangle(v0, v1, v2))
                        if (triangles.size >= MAX_TRIANGLES) break
                    }
                }
            }
            if (triangles.size >= MAX_TRIANGLES) break
            line = reader.readLine()
        }

        if (triangles.isEmpty()) {
            return Result.failure(IllegalArgumentException("No valid faces found in OBJ '$modelName'."))
        }
        return Result.success(MeshData.fromRawTriangles(triangles))
    }

    /**
     * Parses a `.3mf` archive (ZIP containing `3D/3dmodel.model`).
     */
    fun parse3mf(bytes: ByteArray, defaultName: String): Result<List<SceneObject>> {
        val objects = ArrayList<SceneObject>()
        val zipIn = ZipInputStream(ByteArrayInputStream(bytes))
        var entry = zipIn.nextEntry
        while (entry != null) {
            val entryName = entry.name
            if (!entry.isDirectory && entryName.endsWith(".model", ignoreCase = true)) {
                val modelBytes = readBoundedBytes(zipIn, MAX_FILE_BYTES)
                if (modelBytes != null && modelBytes.isNotEmpty()) {
                    val parsedMeshes = parse3mfModelXml(String(modelBytes, Charsets.UTF_8), defaultName)
                    objects.addAll(parsedMeshes)
                }
            }
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }

        if (objects.isEmpty()) {
            return Result.failure(IllegalArgumentException("No 3D mesh objects found inside .3mf archive."))
        }
        return Result.success(objects)
    }

    private fun parse3mfModelXml(xml: String, defaultName: String): List<SceneObject> {
        val results = ArrayList<SceneObject>()
        val objectRegex = Regex("<object\\b([^>]*)>([\\s\\S]*?)</object>", RegexOption.IGNORE_CASE)
        val vertexRegex = Regex("<vertex\\b[^>]*?x\\s*=\\s*[\"']([^\"']+)[\"'][^>]*?y\\s*=\\s*[\"']([^\"']+)[\"'][^>]*?z\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
        val triRegex = Regex("<triangle\\b[^>]*?v1\\s*=\\s*[\"'](\\d+)[\"'][^>]*?v2\\s*=\\s*[\"'](\\d+)[\"'][^>]*?v3\\s*=\\s*[\"'](\\d+)[\"']", RegexOption.IGNORE_CASE)
        val nameAttrRegex = Regex("name\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)

        var objIndex = 1
        for (objMatch in objectRegex.findAll(xml)) {
            val attrs = objMatch.groupValues[1]
            val body = objMatch.groupValues[2]
            val objName = nameAttrRegex.find(attrs)?.groupValues?.getOrNull(1)
                ?.takeIf { it.isNotBlank() }
                ?: if (objIndex == 1) defaultName else "${defaultName}_$objIndex"

            val vertices = ArrayList<Vec3>()
            for (vMatch in vertexRegex.findAll(body)) {
                val x = vMatch.groupValues[1].toFloatOrNull()
                val y = vMatch.groupValues[2].toFloatOrNull()
                val z = vMatch.groupValues[3].toFloatOrNull()
                if (x != null && y != null && z != null) {
                    vertices.add(Vec3(x, y, z))
                }
            }

            val triangles = ArrayList<Triangle>()
            for (tMatch in triRegex.findAll(body)) {
                val i1 = tMatch.groupValues[1].toIntOrNull()
                val i2 = tMatch.groupValues[2].toIntOrNull()
                val i3 = tMatch.groupValues[3].toIntOrNull()
                if (i1 != null && i2 != null && i3 != null &&
                    i1 in 0 until vertices.size &&
                    i2 in 0 until vertices.size &&
                    i3 in 0 until vertices.size
                ) {
                    val v0 = vertices[i1]
                    val v1 = vertices[i2]
                    val v2 = vertices[i3]
                    if (v0.isFinite() && v1.isFinite() && v2.isFinite()) {
                        triangles.add(Triangle(v0, v1, v2))
                        if (triangles.size >= MAX_TRIANGLES) break
                    }
                }
            }

            if (triangles.isNotEmpty()) {
                val mesh = MeshData.fromRawTriangles(triangles)
                results.add(
                    SceneObject(
                        id = UUID.randomUUID().toString(),
                        name = objName,
                        sourceFormat = "3MF",
                        mesh = mesh,
                        position = Vec3((objIndex - 1) * 32f, 0f, 0f)
                    )
                )
                objIndex++
            }
        }
        return results
    }

    /**
     * Parses an existing `.gcode` file into a 3D `SliceResult` so users can preview any G-code toolpath.
     */
    fun parseGCode(bytes: ByteArray, fileName: String): Result<SliceResult> {
        val gcodeText = String(bytes, Charsets.UTF_8)
        val reader = BufferedReader(InputStreamReader(ByteArrayInputStream(bytes), Charsets.UTF_8))

        val layers = ArrayList<SliceLayer>()
        var currentLayerSegments = ArrayList<ToolpathSegment>()
        var currentLayerZ = 0.2f
        var currentType = ToolpathType.OUTER_WALL

        var curX = 0f
        var curY = 0f
        var curZ = 0.2f
        var curF = 3000f
        var totalExtrusionMm = 0f
        var totalTimeSeconds = 0f
        val featureTimes = HashMap<ToolpathType, Float>()

        var line: String? = reader.readLine()
        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.startsWith(";TYPE:", ignoreCase = true)) {
                val typeStr = trimmed.substringAfter(":").trim().lowercase(Locale.ROOT)
                currentType = when {
                    typeStr.contains("outer") || typeStr.contains("external") -> ToolpathType.OUTER_WALL
                    typeStr.contains("inner") || typeStr.contains("perimeter") -> ToolpathType.INNER_WALL
                    typeStr.contains("top") -> ToolpathType.TOP_SURFACE
                    typeStr.contains("bottom") -> ToolpathType.BOTTOM_SURFACE
                    typeStr.contains("solid") -> ToolpathType.SOLID_INFILL
                    typeStr.contains("sparse") || typeStr.contains("infill") -> ToolpathType.SPARSE_INFILL
                    typeStr.contains("support") -> ToolpathType.SUPPORT
                    typeStr.contains("skirt") || typeStr.contains("brim") -> ToolpathType.SKIRT_BRIM
                    else -> ToolpathType.OUTER_WALL
                }
            } else if (trimmed.startsWith(";LAYER_CHANGE", ignoreCase = true)) {
                if (currentLayerSegments.isNotEmpty()) {
                    layers.add(
                        SliceLayer(
                            index = layers.size,
                            zHeightMm = currentLayerZ,
                            layerHeightMm = 0.2f,
                            segments = currentLayerSegments
                        )
                    )
                    currentLayerSegments = ArrayList()
                }
            } else if (trimmed.startsWith("G0 ") || trimmed.startsWith("G1 ")) {
                val codePart = trimmed.substringBefore(';').trim()
                val tokens = codePart.split(Regex("\\s+"))
                var nextX = curX
                var nextY = curY
                var nextZ = curZ
                var deltaE = 0f

                for (i in 1 until tokens.size) {
                    val tok = tokens[i]
                    if (tok.length < 2) continue
                    val value = tok.substring(1).toFloatOrNull() ?: continue
                    when (tok[0].uppercaseChar()) {
                        'X' -> nextX = value
                        'Y' -> nextY = value
                        'Z' -> nextZ = value
                        'E' -> deltaE = value
                        'F' -> if (value > 1f) curF = value
                    }
                }

                if (kotlin.math.abs(nextZ - curZ) > 0.01f && deltaE <= 0f) {
                    if (currentLayerSegments.isNotEmpty() && nextZ > currentLayerZ + 0.04f) {
                        layers.add(
                            SliceLayer(
                                index = layers.size,
                                zHeightMm = currentLayerZ,
                                layerHeightMm = (nextZ - currentLayerZ).coerceAtLeast(0.08f),
                                segments = currentLayerSegments
                            )
                        )
                        currentLayerSegments = ArrayList()
                        currentLayerZ = nextZ
                    } else {
                        currentLayerZ = nextZ
                    }
                }

                // Shift from printer bed coordinates (0..bedSize) to centered build plate coordinates if needed
                val start = Vec3(curX - 110f, curY - 110f, curZ)
                val end = Vec3(nextX - 110f, nextY - 110f, nextZ)
                val dist = start.distanceTo(end)

                if (dist > 0.01f) {
                    val segType = if (deltaE > 0.0001f) currentType else ToolpathType.TRAVEL
                    val speedMmS = (curF / 60f).coerceIn(5f, 600f)
                    val dt = dist / speedMmS
                    totalTimeSeconds += dt
                    featureTimes[segType] = (featureTimes[segType] ?: 0f) + dt
                    if (deltaE > 0f) totalExtrusionMm += deltaE

                    currentLayerSegments.add(
                        ToolpathSegment(
                            start = start,
                            end = end,
                            type = segType,
                            extrusionMm = max(0f, deltaE),
                            speedMmS = speedMmS,
                            layerIndex = layers.size
                        )
                    )
                }

                curX = nextX
                curY = nextY
                curZ = nextZ
            }
            line = reader.readLine()
        }

        if (currentLayerSegments.isNotEmpty()) {
            layers.add(
                SliceLayer(
                    index = layers.size,
                    zHeightMm = currentLayerZ,
                    layerHeightMm = 0.2f,
                    segments = currentLayerSegments
                )
            )
        }

        if (layers.isEmpty()) {
            return Result.failure(IllegalArgumentException("No G0/G1 toolpath moves found in '$fileName'."))
        }

        val filamentMeters = totalExtrusionMm / 1000f
        val areaMm2 = PI.toFloat() * (1.75f * 0.5f) * (1.75f * 0.5f)
        val volumeCm3 = (totalExtrusionMm * areaMm2) / 1000f
        val weightGrams = volumeCm3 * 1.24f

        val stats = PrintStatistics(
            layerCount = layers.size,
            totalSegmentCount = layers.sumOf { it.segments.size },
            filamentLengthMeters = filamentMeters,
            filamentWeightGrams = weightGrams,
            estimatedCostUsd = (weightGrams / 1000f) * 19.99f,
            totalTimeSeconds = totalTimeSeconds,
            featureTimeSeconds = featureTimes
        )

        return Result.success(SliceResult(layers, stats, gcodeText))
    }
}
