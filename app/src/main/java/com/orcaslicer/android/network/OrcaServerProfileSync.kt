package com.orcaslicer.android.network

import com.orcaslicer.android.model.FilamentProfile
import com.orcaslicer.android.model.GCodeFlavor
import com.orcaslicer.android.model.PrinterProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class ServerSyncSummary(
    val vendorsSynced: Int,
    val printersImported: Int,
    val filamentsImported: Int,
    val printers: List<PrinterProfile>,
    val filaments: List<FilamentProfile>
)

/**
 * Connects directly to the official OrcaSlicer GitHub repository
 * (`https://raw.githubusercontent.com/OrcaSlicer/OrcaSlicer/main/resources/profiles/`)
 * to fetch and import all vendor machine models and filament profiles.
 */
object OrcaServerProfileSync {

    private const val RAW_BASE = "https://raw.githubusercontent.com/OrcaSlicer/OrcaSlicer/main/resources/profiles"

    val officialServerVendors = listOf(
        "BBL" to "Bambu Lab",
        "Creality" to "Creality",
        "Prusa" to "Prusa",
        "Voron" to "Voron",
        "Qidi" to "Qidi",
        "Sovol" to "Sovol",
        "Elegoo" to "Elegoo",
        "Anycubic" to "Anycubic",
        "Flashforge" to "FlashForge",
        "RatRig" to "RatRig",
        "Flsun" to "FLSUN",
        "Artillery" to "Artillery",
        "Kingroon" to "Kingroon",
        "TwoTrees" to "TwoTrees",
        "Anker" to "AnkerMake",
        "BIQU" to "BIQU",
        "Snapmaker" to "Snapmaker",
        "Troodon" to "Troodon",
        "Vzbot" to "VzBoT",
        "Peopoly" to "Peopoly",
        "LDO" to "LDO",
        "Geeetech" to "Geeetech",
        "Tronxy" to "Tronxy"
    )

    suspend fun syncAllVendorsFromServer(
        selectedVendors: List<Pair<String, String>> = officialServerVendors,
        onProgress: (String) -> Unit = {}
    ): Result<ServerSyncSummary> = withContext(Dispatchers.IO) {
        try {
            onProgress("Connecting to OrcaSlicer GitHub profile server...")
            val allPrinters = ArrayList<PrinterProfile>()
            val allFilaments = ArrayList<FilamentProfile>()
            var successVendors = 0

            coroutineScope {
                val jobs = selectedVendors.map { (vendorCode, vendorDisplay) ->
                    async(Dispatchers.IO) {
                        fetchVendorCatalog(vendorCode, vendorDisplay)
                    }
                }
                val results = jobs.awaitAll()
                for ((printers, filaments, ok) in results) {
                    if (ok) successVendors++
                    allPrinters.addAll(printers)
                    allFilaments.addAll(filaments)
                }
            }

            if (successVendors == 0 || (allPrinters.isEmpty() && allFilaments.isEmpty())) {
                return@withContext Result.failure(
                    IllegalStateException("Could not reach raw.githubusercontent.com. Check your internet connection.")
                )
            }

            Result.success(
                ServerSyncSummary(
                    vendorsSynced = successVendors,
                    printersImported = allPrinters.size,
                    filamentsImported = allFilaments.size,
                    printers = allPrinters,
                    filaments = allFilaments
                )
            )
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Server sync failed: ${e.message}", e))
        }
    }

    private fun fetchVendorCatalog(
        vendorCode: String,
        vendorDisplay: String
    ): Triple<List<PrinterProfile>, List<FilamentProfile>, Boolean> {
        return try {
            val url = "$RAW_BASE/$vendorCode.json"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 10000
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode !in 200..299) {
                conn.disconnect()
                return Triple(emptyList(), emptyList(), false)
            }

            val json = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            conn.disconnect()

            val printers = parseServerMachinesFromVendorJson(json, vendorCode, vendorDisplay)
            val filaments = parseServerFilamentsFromVendorJson(json, vendorCode, vendorDisplay)
            Triple(printers, filaments, true)
        } catch (_: Exception) {
            Triple(emptyList(), emptyList(), false)
        }
    }

    private fun parseServerMachinesFromVendorJson(
        json: String,
        vendorCode: String,
        vendorDisplay: String
    ): List<PrinterProfile> {
        val out = ArrayList<PrinterProfile>()
        val seenNames = HashSet<String>()

        // Extract "machine_model_list" or "machine_list" entries
        val modelListIdx = json.indexOf("\"machine_model_list\"")
        val searchArea = if (modelListIdx >= 0) {
            val endIdx = json.indexOf("]", modelListIdx).takeIf { it > modelListIdx } ?: json.length
            json.substring(modelListIdx, endIdx)
        } else {
            json
        }

        val entryRegex = Regex("\\{([^{}]*)\\}")
        val nameRegex = Regex("\"name\"\\s*:\\s*\"([^\"]+)\"")

        for (match in entryRegex.findAll(searchArea)) {
            val block = match.groupValues[1]
            val name = nameRegex.find(block)?.groupValues?.getOrNull(1)?.trim() ?: continue
            if (name.contains("common", ignoreCase = true) ||
                name.startsWith("fdm_", ignoreCase = true) ||
                name.length < 3
            ) continue

            if (!seenNames.add(name.lowercase(Locale.ROOT))) continue

            val (bx, by, bz) = inferBedDimensionsFromModelName(name, vendorDisplay)
            val flavor = when (vendorCode.uppercase(Locale.ROOT)) {
                "BBL" -> GCodeFlavor.BAMBU
                "PRUSA" -> GCodeFlavor.PRUSA
                "VORON", "QIDI", "SOVOL", "RATRIG", "FLSUN", "KLIPPER", "TROODON", "VZBOT" -> GCodeFlavor.KLIPPER
                else -> if (name.contains("K1", true) || name.contains("K2", true) || name.contains("V3", true) || name.contains("Neptune 4", true)) {
                    GCodeFlavor.KLIPPER
                } else {
                    GCodeFlavor.MARLIN
                }
            }

            val id = "srv_${vendorCode.lowercase(Locale.ROOT)}_${name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "_")}"
            out.add(
                PrinterProfile(
                    id = id,
                    vendor = vendorDisplay,
                    name = if (name.contains("nozzle", ignoreCase = true)) name else "$name 0.4 nozzle",
                    bedWidthX = bx,
                    bedDepthY = by,
                    maxPrintHeightZ = bz,
                    nozzleDiameterMm = 0.4f,
                    gcodeFlavor = flavor
                )
            )
        }
        return out
    }

    private fun parseServerFilamentsFromVendorJson(
        json: String,
        vendorCode: String,
        vendorDisplay: String
    ): List<FilamentProfile> {
        val out = ArrayList<FilamentProfile>()
        val filIdx = json.indexOf("\"filament_list\"")
        if (filIdx < 0) return out

        val endIdx = json.indexOf("]", filIdx).takeIf { it > filIdx } ?: json.length
        val filArea = json.substring(filIdx, endIdx)

        val nameRegex = Regex("\"name\"\\s*:\\s*\"([^\"]+)\"")
        val seen = HashSet<String>()

        for (m in nameRegex.findAll(filArea)) {
            val rawName = m.groupValues[1].trim()
            if (rawName.contains("common", ignoreCase = true) || rawName.startsWith("fdm_", ignoreCase = true)) continue
            // Strip @Machine suffix if present so the filament list stays clean and reusable across machines
            val cleanName = rawName.substringBefore("@").trim()
            if (cleanName.length < 3 || !seen.add(cleanName.lowercase(Locale.ROOT))) continue

            val upper = cleanName.uppercase(Locale.ROOT)
            val (mat, nozzleT, bedT, color, density, price) = when {
                upper.contains("PAHT") || upper.contains("PA-CF") || upper.contains("PA6") ->
                    FilamentDefaults("PA-CF", 285, 100, 0xFF37474F, 1.22f, 54.99f)
                upper.contains("PETG-CF") ->
                    FilamentDefaults("PETG-CF", 255, 75, 0xFF546E7A, 1.25f, 32.99f)
                upper.contains("PLA-CF") ->
                    FilamentDefaults("PLA-CF", 225, 55, 0xFF455A64, 1.22f, 29.99f)
                upper.contains("PETG") ->
                    FilamentDefaults("PETG", 245, 75, 0xFF2898FF, 1.27f, 19.99f)
                upper.contains("ASA") ->
                    FilamentDefaults("ASA", 260, 95, 0xFFEF5350, 1.07f, 24.99f)
                upper.contains("ABS") ->
                    FilamentDefaults("ABS", 255, 95, 0xFFFF6E40, 1.05f, 19.99f)
                upper.contains("TPU") || upper.contains("TPE") ->
                    FilamentDefaults("TPU", 225, 50, 0xFFE040FB, 1.21f, 26.99f)
                upper.contains("PC") ->
                    FilamentDefaults("PC", 275, 100, 0xFFB0BEC5, 1.20f, 36.99f)
                upper.contains("PVA") || upper.contains("BVOH") ->
                    FilamentDefaults("PVA", 210, 55, 0xFFFFF59D, 1.23f, 44.99f)
                upper.contains("SILK") ->
                    FilamentDefaults("PLA-Silk", 225, 60, 0xFFFFB300, 1.25f, 23.99f)
                upper.contains("MATTE") ->
                    FilamentDefaults("PLA-Matte", 215, 55, 0xFF78909C, 1.28f, 19.99f)
                else ->
                    FilamentDefaults("PLA", 220, 60, 0xFF00A896, 1.24f, 19.99f)
            }

            val id = "srv_fil_${vendorCode.lowercase(Locale.ROOT)}_${cleanName.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "_")}"
            out.add(
                FilamentProfile(
                    id = id,
                    vendor = vendorDisplay,
                    name = cleanName,
                    materialType = mat,
                    colorHex = color,
                    densityGPerCm3 = density,
                    costPerKgUsd = price,
                    nozzleTempInitialC = nozzleT,
                    nozzleTempOtherC = nozzleT,
                    bedTempInitialC = bedT,
                    bedTempOtherC = bedT
                )
            )
        }
        return out
    }

    private data class FilamentDefaults(
        val mat: String,
        val nozzleT: Int,
        val bedT: Int,
        val color: Long,
        val density: Float,
        val price: Float
    )

    private fun inferBedDimensionsFromModelName(name: String, vendor: String): Triple<Float, Float, Float> {
        val n = name.lowercase(Locale.ROOT)
        return when {
            n.contains("500") -> Triple(500f, 500f, 500f)
            n.contains("420") || n.contains("neptune 4 max") || n.contains("kobra 2 max") -> Triple(420f, 420f, 480f)
            n.contains("400") -> Triple(400f, 400f, 400f)
            n.contains("360") || n.contains("prusa xl") -> Triple(360f, 360f, 360f)
            n.contains("ender-5 plus") || n.contains("ender 5 plus") || n.contains("ender5p") -> Triple(350f, 350f, 400f)
            n.contains("350") || n.contains("sv08") || n.contains("k2 plus") -> Triple(350f, 350f, 350f)
            n.contains("325") || n.contains("x-max 3") -> Triple(325f, 325f, 315f)
            n.contains("320") || n.contains("neptune 4 plus") -> Triple(320f, 320f, 385f)
            n.contains("305") || n.contains("plus 4") -> Triple(305f, 305f, 280f)
            n.contains("300") || n.contains("k1 max") || n.contains("cr-10") || n.contains("sv06 plus") || n.contains("sv07 plus") -> Triple(300f, 300f, 300f)
            n.contains("280") || n.contains("x-plus") -> Triple(280f, 280f, 270f)
            n.contains("256") || vendor.equals("Bambu Lab", true) && !n.contains("mini") -> Triple(256f, 256f, 256f)
            n.contains("250") && vendor.equals("Prusa", true) -> Triple(250f, 210f, 220f)
            n.contains("250") -> Triple(250f, 250f, 250f)
            n.contains("180") || n.contains("mini") || n.contains("v-minion") -> Triple(180f, 180f, 180f)
            n.contains("120") || n.contains("v0") -> Triple(120f, 120f, 120f)
            else -> Triple(220f, 220f, 250f)
        }
    }
}
