package com.orcaslicer.android.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

enum class NetworkProtocol(val label: String, val defaultPort: Int) {
    MOONRAKER_KLIPPER("Moonraker / Klipper (Mainsail/Fluidd)", 7125),
    OCTOPRINT("OctoPrint REST API", 5000)
}

data class PrinterTelemetry(
    val connected: Boolean,
    val stateText: String,
    val nozzleTempC: Float,
    val nozzleTargetC: Float,
    val bedTempC: Float,
    val bedTargetC: Float,
    val progressPercent: Int,
    val statusMessage: String
)

object PrinterNetworkClient {

    suspend fun queryPrinterStatus(
        hostOrIp: String,
        port: Int,
        apiKey: String,
        protocol: NetworkProtocol
    ): Result<PrinterTelemetry> = withContext(Dispatchers.IO) {
        try {
            val cleanHost = hostOrIp.trim()
                .removePrefix("http://")
                .removePrefix("https://")
                .substringBefore("/")
                .substringBefore(":")

            if (cleanHost.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Enter a valid Printer IP or Hostname."))
            }

            val endpoint = when (protocol) {
                NetworkProtocol.MOONRAKER_KLIPPER ->
                    "http://$cleanHost:$port/printer/objects/query?heater_bed&extruder&print_stats&virtual_sdcard"
                NetworkProtocol.OCTOPRINT ->
                    "http://$cleanHost:$port/api/printer"
            }

            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4500
                readTimeout = 4500
                if (apiKey.isNotBlank()) {
                    setRequestProperty("X-Api-Key", apiKey.trim())
                }
            }

            val code = conn.responseCode
            if (code !in 200..299) {
                return@withContext Result.failure(
                    IllegalStateException("Printer responded with HTTP $code from $cleanHost:$port")
                )
            }

            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            conn.disconnect()

            val nozzleTemp = extractNumberNear(body, "extruder", "temperature")
                ?: extractNumberNear(body, "tool0", "actual")
                ?: 25.0f
            val nozzleTarget = extractNumberNear(body, "extruder", "target")
                ?: extractNumberNear(body, "tool0", "target")
                ?: 0.0f
            val bedTemp = extractNumberNear(body, "heater_bed", "temperature")
                ?: extractNumberNear(body, "bed", "actual")
                ?: 24.0f
            val bedTarget = extractNumberNear(body, "heater_bed", "target")
                ?: extractNumberNear(body, "bed", "target")
                ?: 0.0f
            val progressFloat = extractNumberNear(body, "virtual_sdcard", "progress") ?: 0f
            val stateStr = Regex("\"state\"\\s*:\\s*\"([^\"]+)\"")
                .find(body)?.groupValues?.getOrNull(1)
                ?: "Ready"

            Result.success(
                PrinterTelemetry(
                    connected = true,
                    stateText = stateStr.replaceFirstChar { it.uppercase() },
                    nozzleTempC = nozzleTemp,
                    nozzleTargetC = nozzleTarget,
                    bedTempC = bedTemp,
                    bedTargetC = bedTarget,
                    progressPercent = (progressFloat * 100f).toInt().coerceIn(0, 100),
                    statusMessage = "Connected to $cleanHost:$port (${protocol.label})"
                )
            )
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Connection failed: ${e.message ?: "Host unreachable"}", e))
        }
    }

    suspend fun uploadGCodeToPrinter(
        hostOrIp: String,
        port: Int,
        apiKey: String,
        protocol: NetworkProtocol,
        fileName: String,
        gcodeText: String,
        startPrintImmediately: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanHost = hostOrIp.trim()
                .removePrefix("http://")
                .removePrefix("https://")
                .substringBefore("/")
                .substringBefore(":")

            if (cleanHost.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Enter a valid Printer IP or Hostname."))
            }

            val uploadPath = when (protocol) {
                NetworkProtocol.MOONRAKER_KLIPPER -> "http://$cleanHost:$port/server/files/upload"
                NetworkProtocol.OCTOPRINT -> "http://$cleanHost:$port/api/files/local"
            }

            val boundary = "----OrcaSlicerAndroidBoundary${UUID.randomUUID().toString().replace("-", "")}"
            val conn = (URL(uploadPath).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                connectTimeout = 8000
                readTimeout = 20000
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                if (apiKey.isNotBlank()) {
                    setRequestProperty("X-Api-Key", apiKey.trim())
                }
            }

            val safeName = if (fileName.endsWith(".gcode", ignoreCase = true)) fileName else "$fileName.gcode"

            DataOutputStream(conn.outputStream).use { out ->
                if (startPrintImmediately) {
                    out.writeBytes("--$boundary\r\n")
                    out.writeBytes("Content-Disposition: form-data; name=\"print\"\r\n\r\n")
                    out.writeBytes("true\r\n")
                }

                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$safeName\"\r\n")
                out.writeBytes("Content-Type: application/octet-stream\r\n\r\n")
                out.write(gcodeText.toByteArray(Charsets.UTF_8))
                out.writeBytes("\r\n--$boundary--\r\n")
                out.flush()
            }

            val code = conn.responseCode
            conn.disconnect()

            if (code in 200..299) {
                val action = if (startPrintImmediately) "Uploaded & started print" else "Uploaded '$safeName'"
                Result.success("$action on $cleanHost:$port")
            } else {
                Result.failure(IllegalStateException("Upload rejected with HTTP $code from $cleanHost:$port"))
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Upload failed: ${e.message ?: "Network error"}", e))
        }
    }

    private fun extractNumberNear(json: String, sectionKey: String, fieldKey: String): Float? {
        val secIdx = json.indexOf("\"$sectionKey\"")
        if (secIdx < 0) return null
        val window = json.substring(secIdx, (secIdx + 320).coerceAtMost(json.length))
        val regex = Regex("\"$fieldKey\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
        return regex.find(window)?.groupValues?.getOrNull(1)?.toFloatOrNull()
    }
}
