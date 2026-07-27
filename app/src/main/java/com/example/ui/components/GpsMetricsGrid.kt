package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.location.GpsData
import com.example.ui.theme.GpsEmeraldSuccess
import com.example.ui.theme.GpsTextMuted

@Composable
fun GpsMetricsGrid(
    gpsData: GpsData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Akurasi Metric
            MetricTile(
                title = "AKURASI",
                value = if (gpsData.accuracy > 0f) "±%.1f m".format(gpsData.accuracy) else "--",
                subtitle = if (gpsData.accuracy <= 5f && gpsData.accuracy > 0f) "Presisi Tinggi" else if (gpsData.accuracy <= 15f) "Sedang" else "Rendah",
                icon = Icons.Default.PrecisionManufacturing,
                iconColor = if (gpsData.accuracy <= 10f) GpsEmeraldSuccess else MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )

            // Ketinggian Metric
            MetricTile(
                title = "KETINGGIAN",
                value = "%.1f m".format(gpsData.altitude),
                subtitle = "%.0f ft DPL".format(gpsData.altitudeFeet),
                icon = Icons.Default.Height,
                iconColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Kecepatan Metric
            MetricTile(
                title = "KECEPATAN",
                value = "%.1f km/j".format(gpsData.speedKmh),
                subtitle = "%.1f m/s (%.1f mph)".format(gpsData.speed, gpsData.speedMph),
                icon = Icons.Default.Speed,
                iconColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )

            // Satelit status
            MetricTile(
                title = "SATELIT",
                value = if (gpsData.satellitesInView > 0) "${gpsData.satellitesInFix}/${gpsData.satellitesInView}" else "Fix Active",
                subtitle = if (gpsData.satellitesInView > 0) "Digunakan / Terdeteksi" else "Navigasi Aktif",
                icon = Icons.Default.SatelliteAlt,
                iconColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Arah Bearing
            MetricTile(
                title = "ARAH BEARING",
                value = "%.1f° %s".format(gpsData.bearing, gpsData.cardinalDirection),
                subtitle = "Kompas Derajat",
                icon = Icons.Default.CompassCalibration,
                iconColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )

            // Fix Latency
            MetricTile(
                title = "LATENSI FIX",
                value = if (gpsData.lastFixLatencyMs > 0) "${gpsData.lastFixLatencyMs} ms" else "< 10 ms",
                subtitle = "Waktu Respon Sinyal",
                icon = Icons.Default.Timer,
                iconColor = GpsEmeraldSuccess,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricTile(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("metric_tile_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = GpsTextMuted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = GpsTextMuted
                )
            )
        }
    }
}
