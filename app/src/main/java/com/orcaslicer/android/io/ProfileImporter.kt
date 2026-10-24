package com.orcaslicer.android.io

import com.orcaslicer.android.model.FilamentProfile
import com.orcaslicer.android.model.GCodeFlavor
import com.orcaslicer.android.model.InfillPattern
import com.orcaslicer.android.model.PrinterProfile
import com.orcaslicer.android.model.ProcessProfile
import java.io.InputStream
import java.util.Locale
import java.util.UUID

/**
 * ============================================================================
 * OrcaSlicer / PrusaSlicer Profile Importer (`ProfileImporter.kt`)
 * ============================================================================
 * Imports external `.json`, `.orca_printer`, `.orca_filament`, or `.ini`
 * configuration files and maps their keys into [PrinterProfile], [FilamentProfile],
 * or [ProcessProfile].
 */
object ProfileImporter {

    sealed class ImportedProfile {
        data class Printer(val profile: PrinterProfile) : ImportedProfile()
        data class Filament(val profile: FilamentProfile) : ImportedProfile()
        data class Process(val profile: ProcessProfile) : ImportedProfile()
    }

    fun importProfileFromStream(
        inputStream: InputStream,
        fileName: String
    ): Result<ImportedProfile> {
        return try {
            val text = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (text.isBlank()) {
                return Result.failure(IllegalArgumentException("Profile file '$fileName' is empty."))
            }
            parseProfileText(text, fileName)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Failed to import profile '$fileName': ${e.message}", e))
        }
    }

    fun parseProfileText(text: String, fileName: String): Result<ImportedProfile> {
        val baseName = fileName.substringBeforeLast('.').ifBlank { "Custom_Profile" }
        val profileName = extractStringField(text, "name") ?: baseName
        val typeField = extractStringField(text, "type")?.lowercase(Locale.ROOT) ?: ""

        val isFilament = typeField.contains("filament") ||
            fileName.endsWith(".orca_filament", ignoreCase = true) ||
            text.contains("\"filament_type\"") ||
            text.contains("\"filament_density\"") ||
            text.contains("\"filament_cost\"")

        val isPrinter = typeField.contains("machine") ||
            typeField.contains("printer") ||
            fileName.endsWith(".orca_printer", ignoreCase = true) ||
            text.contains("\"printable_height\"") ||
            text.contains("\"machine_start_gcode\"") ||
            text.contains("\"bed_shape\"")

        return when {
            isFilament -> {
                val matType = extractStringField(text, "filament_type") ?: "PLA"
                val nozzleTemp = extractFloatField(text, "nozzle_temperature")?.toInt()
                    ?: extractFloatField(text, "temperature")?.toInt()
                    ?: 220
                val nozzleInit = extractFloatField(text, "nozzle_temperature_initial_layer")?.toInt()
                    ?: nozzleTemp
                val bedTemp = extractFloatField(text, "hot_plate_temp")?.toInt()
                    ?: extractFloatField(text, "textured_plate_temp")?.toInt()
                    ?: extractFloatField(text, "bed_temperature")?.toInt()
                    ?: 60
                val density = extractFloatField(text, "filament_density") ?: 1.24f
                val cost = extractFloatField(text, "filament_cost") ?: 19.99f
                val flow = extractFloatField(text, "filament_flow_ratio")
                    ?: extractFloatField(text, "extrusion_multiplier")
                    ?: 0.98f
                val mvs = extractFloatField(text, "filament_max_volumetric_speed") ?: 20.0f
                val pa = extractFloatField(text, "pressure_advance") ?: 0.032f

                Result.success(
                    ImportedProfile.Filament(
                        FilamentProfile(
                            id = "custom_fil_${UUID.randomUUID()}",
                            vendor = extractStringField(text, "filament_vendor") ?: "Imported",
                            name = profileName,
                            materialType = matType,
                            colorHex = 0xFF00C896,
                            densityGPerCm3 = density.coerceIn(0.5f, 3.0f),
                            costPerKgUsd = cost.coerceIn(1.0f, 500.0f),
                            nozzleTempInitialC = nozzleInit.coerceIn(160, 350),
                            nozzleTempOtherC = nozzleTemp.coerceIn(160, 350),
                            bedTempInitialC = bedTemp.coerceIn(0, 140),
                            bedTempOtherC = bedTemp.coerceIn(0, 140),
                            flowRatio = flow.coerceIn(0.4f, 1.5f),
                            maxVolumetricSpeedMm3S = mvs.coerceIn(1f, 60f),
                            pressureAdvance = pa.coerceIn(0f, 0.5f)
                        )
                    )
                )
            }

            isPrinter -> {
                val widthX = extractFloatField(text, "printable_width") ?: 256f
                val depthY = extractFloatField(text, "printable_depth") ?: widthX
                val heightZ = extractFloatField(text, "printable_height") ?: 256f
                val nozzle = extractFloatField(text, "nozzle_diameter") ?: 0.4f
                val flavorStr = (extractStringField(text, "gcode_flavor") ?: "klipper").lowercase(Locale.ROOT)
                val flavor = when {
                    flavorStr.contains("bambu") -> GCodeFlavor.BAMBU
                    flavorStr.contains("marlin") -> GCodeFlavor.MARLIN
                    flavorStr.contains("prusa") -> GCodeFlavor.PRUSA
                    flavorStr.contains("reprap") -> GCodeFlavor.REPRAP
                    else -> GCodeFlavor.KLIPPER
                }

                Result.success(
                    ImportedProfile.Printer(
                        PrinterProfile(
                            id = "custom_prt_${UUID.randomUUID()}",
                            vendor = extractStringField(text, "printer_vendor") ?: "Imported",
                            name = profileName,
                            bedWidthX = widthX.coerceIn(80f, 1000f),
                            bedDepthY = depthY.coerceIn(80f, 1000f),
                            maxPrintHeightZ = heightZ.coerceIn(80f, 1000f),
                            nozzleDiameterMm = nozzle.coerceIn(0.15f, 1.2f),
                            gcodeFlavor = flavor
                        )
                    )
                )
            }

            else -> {
                val lh = extractFloatField(text, "layer_height") ?: 0.20f
                val initLh = extractFloatField(text, "initial_layer_print_height") ?: 0.24f
                val walls = extractFloatField(text, "wall_loops")?.toInt()
                    ?: extractFloatField(text, "perimeters")?.toInt()
                    ?: 3
                val infill = extractFloatField(text, "sparse_infill_density")?.toInt()
                    ?: extractFloatField(text, "fill_density")?.toInt()
                    ?: 15
                val outerSpeed = extractFloatField(text, "outer_wall_speed") ?: 120f
                val innerSpeed = extractFloatField(text, "inner_wall_speed") ?: 200f

                Result.success(
                    ImportedProfile.Process(
                        ProcessProfile(
                            id = "custom_proc_${UUID.randomUUID()}",
                            name = profileName,
                            layerHeightMm = lh.coerceIn(0.05f, 0.60f),
                            initialLayerHeightMm = initLh.coerceIn(0.08f, 0.60f),
                            wallLoops = walls.coerceIn(1, 12),
                            sparseInfillDensityPercent = infill.coerceIn(0, 100),
                             sparseInfillPattern = InfillPattern.GYROID,
                            outerWallSpeedMmS = outerSpeed.coerceIn(20f, 500f),
                            innerWallSpeedMmS = innerSpeed.coerceIn(20f, 600f)
                        )
                    )
                )
            }
        }
    }

    private fun extractStringField(text: String, key: String): String? {
        // Matches JSON "key": "val" or "key": ["val"] or INI key = val
        val jsonRegex = Regex("\"$key\"\\s*:\\s*(?:\\[\\s*)?\"([^\"]+)\"", RegexOption.IGNORE_CASE)
        jsonRegex.find(text)?.groupValues?.getOrNull(1)?.let { return it.trim() }

        val iniRegex = Regex("^\\s*$key\\s*=\\s*([^\\r\\n;]+)", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
        return iniRegex.find(text)?.groupValues?.getOrNull(1)?.trim()?.removeSurrounding("\"")
    }

    private fun extractFloatField(text: String, key: String): Float? {
        val raw = extractStringField(text, key)
            ?: Regex("\"$key\"\\s*:\\s*(?:\\[\\s*)?(-?\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE)
                .find(text)?.groupValues?.getOrNull(1)
            ?: return null
        return raw.removeSuffix("%").trim().toFloatOrNull()
    }
}
