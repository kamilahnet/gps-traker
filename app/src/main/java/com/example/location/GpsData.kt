package com.example.data.location

import kotlin.math.abs
import kotlin.math.floor

enum class SignalQuality {
    EXCELLENT, GOOD, FAIR, POOR, NO_SIGNAL
}

data class GpsData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val accuracy: Float = 0.0f,
    val speed: Float = 0.0f, // m/s
    val bearing: Float = 0.0f, // degrees
    val provider: String = "GPS",
    val timestamp: Long = System.currentTimeMillis(),
    val satellitesInView: Int = 0,
    val satellitesInFix: Int = 0,
    val isMock: Boolean = false,
    val address: String = "Mengambil alamat...",
    val isGpsEnabled: Boolean = true,
    val isSearching: Boolean = false,
    val isSimulationMode: Boolean = false,
    val lastFixLatencyMs: Long = 0L
) {
    val speedKmh: Float get() = speed * 3.6f
    val speedMph: Float get() = speed * 2.23694f
    val altitudeFeet: Double get() = altitude * 3.28084

    val signalQuality: SignalQuality
        get() = when {
            !isGpsEnabled -> SignalQuality.NO_SIGNAL
            accuracy <= 0f -> SignalQuality.NO_SIGNAL
            accuracy <= 5f -> SignalQuality.EXCELLENT
            accuracy <= 15f -> SignalQuality.GOOD
            accuracy <= 30f -> SignalQuality.FAIR
            else -> SignalQuality.POOR
        }

    val cardinalDirection: String
        get() {
            val normalized = (bearing % 360 + 360) % 360
            return when {
                normalized >= 337.5 || normalized < 22.5 -> "U" // Utara / North
                normalized >= 22.5 && normalized < 67.5 -> "TL" // Timur Laut / NE
                normalized >= 67.5 && normalized < 112.5 -> "T" // Timur / East
                normalized >= 112.5 && normalized < 157.5 -> "TG" // Tenggara / SE
                normalized >= 157.5 && normalized < 202.5 -> "S" // Selatan / South
                normalized >= 202.5 && normalized < 247.5 -> "BD" // Barat Daya / SW
                normalized >= 247.5 && normalized < 292.5 -> "B" // Barat / West
                else -> "BL" // Barat Laut / NW
            }
        }

    fun toDmsLatitude(): String {
        return convertDecimalToDms(latitude, true)
    }

    fun toDmsLongitude(): String {
        return convertDecimalToDms(longitude, false)
    }

    private fun convertDecimalToDms(decimal: Double, isLatitude: Boolean): String {
        val direction = if (isLatitude) {
            if (decimal >= 0) "N" else "S"
        } else {
            if (decimal >= 0) "E" else "W"
        }
        val absVal = abs(decimal)
        val degrees = floor(absVal).toInt()
        val minutesDouble = (absVal - degrees) * 60.0
        val minutes = floor(minutesDouble).toInt()
        val seconds = (minutesDouble - minutes) * 60.0
        return String.format("%d° %d' %.2f\" %s", degrees, minutes, seconds, direction)
    }

    fun toGoogleMapsUrl(): String {
        return "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
    }
}
