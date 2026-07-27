package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.location.GpsData
import com.example.ui.theme.GpsCyanPrimary
import com.example.ui.theme.GpsEmeraldSuccess
import com.example.ui.theme.GpsRedError
import com.example.ui.theme.GpsTextMuted
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CompassSpeedometerCard(
    gpsData: GpsData,
    maxSpeedSession: Float,
    onResetMaxSpeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Compass, 1 = Speedometer

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("compass_speedometer_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CompassCalibration,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                            Text("Kompas Bearing", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                            Text("Spedometer", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                CompassView(bearing = gpsData.bearing, cardinal = gpsData.cardinalDirection)
            } else {
                SpeedometerView(
                    speedKmh = gpsData.speedKmh,
                    maxSpeedSession = maxSpeedSession,
                    onResetMaxSpeed = onResetMaxSpeed
                )
            }
        }
    }
}

@Composable
fun CompassView(
    bearing: Float,
    cardinal: String
) {
    val animatedBearing by animateFloatAsState(
        targetValue = bearing,
        animationSpec = tween(300),
        label = "compass_bearing"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val textMeasurer = rememberTextMeasurer()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(220.dp)
                .testTag("compass_canvas_box")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = (size.minDimension / 2) - 16.dp.toPx()

                // Draw Outer Circular Dial Rim
                drawCircle(
                    color = primaryColor.copy(alpha = 0.2f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 4.dp.toPx())
                )

                // Rotate Canvas based on Bearing so North moves to correct heading
                rotate(-animatedBearing, pivot = center) {
                    // Draw Degree Tick Marks
                    for (i in 0 until 360 step 15) {
                        val angleRad = Math.toRadians(i.toDouble())
                        val isMajor = i % 90 == 0
                        val isMedium = i % 30 == 0
                        val lineLength = if (isMajor) 16.dp.toPx() else if (isMedium) 10.dp.toPx() else 6.dp.toPx()

                        val startX = center.x + (radius - lineLength) * sin(angleRad).toFloat()
                        val startY = center.y - (radius - lineLength) * cos(angleRad).toFloat()
                        val endX = center.x + radius * sin(angleRad).toFloat()
                        val endY = center.y - radius * cos(angleRad).toFloat()

                        drawLine(
                            color = if (isMajor) GpsRedError else onSurfaceColor.copy(alpha = 0.5f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = if (isMajor) 3.dp.toPx() else 1.5.dp.toPx()
                        )
                    }

                    // Draw Cardinal Direction Labels (U, T, S, B)
                    val directions = listOf("U" to 0f, "T" to 90f, "S" to 180f, "B" to 270f)
                    directions.forEach { (dir, angle) ->
                        val rad = Math.toRadians(angle.toDouble())
                        val textPos = Offset(
                            center.x + (radius - 32.dp.toPx()) * sin(rad).toFloat(),
                            center.y - (radius - 32.dp.toPx()) * cos(rad).toFloat()
                        )
                        val textResult = textMeasurer.measure(
                            text = dir,
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dir == "U") GpsRedError else onSurfaceColor
                            )
                        )
                        drawText(
                            textLayoutResult = textResult,
                            topLeft = Offset(
                                textPos.x - textResult.size.width / 2,
                                textPos.y - textResult.size.height / 2
                            )
                        )
                    }

                    // Draw Compass Pointer Arrow (Red top for North)
                    val arrowPath = Path().apply {
                        moveTo(center.x, center.y - radius + 10.dp.toPx())
                        lineTo(center.x - 12.dp.toPx(), center.y)
                        lineTo(center.x + 12.dp.toPx(), center.y)
                        close()
                    }
                    drawPath(path = arrowPath, color = GpsRedError)

                    val southArrowPath = Path().apply {
                        moveTo(center.x, center.y + radius - 10.dp.toPx())
                        lineTo(center.x - 12.dp.toPx(), center.y)
                        lineTo(center.x + 12.dp.toPx(), center.y)
                        close()
                    }
                    drawPath(path = southArrowPath, color = primaryColor)
                }

                // Center Pin Circle
                drawCircle(color = GpsRedError, radius = 8.dp.toPx(), center = center)
                drawCircle(color = Color.White, radius = 3.dp.toPx(), center = center)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Angle & Cardinal Info Readout
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "%.1f°".format(bearing),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.padding(horizontal = 6.dp))
            Text(
                text = cardinal,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = GpsRedError
                )
            )
        }
    }
}

@Composable
fun SpeedometerView(
    speedKmh: Float,
    maxSpeedSession: Float,
    onResetMaxSpeed: () -> Unit
) {
    val animatedSpeed by animateFloatAsState(
        targetValue = speedKmh,
        animationSpec = tween(300),
        label = "speedometer_speed"
    )

    val primaryColor = MaterialTheme.colorScheme.secondary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(200.dp)
                .testTag("speedometer_canvas_box")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = (size.minDimension / 2) - 16.dp.toPx()

                // Draw 240 degree Arc Gauge Background
                drawArc(
                    color = trackColor,
                    startAngle = 150f,
                    sweepAngle = 240f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                )

                // Calculate Sweep Angle for current speed (0 - 160 km/h scale)
                val speedProgress = (animatedSpeed / 160f).coerceIn(0f, 1f)
                val activeSweep = speedProgress * 240f

                drawArc(
                    color = primaryColor,
                    startAngle = 150f,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Digital Speed Readout in Center
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%.0f".format(speedKmh),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = "KM/JAM",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = GpsTextMuted
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Maksimum Sesi: %.1f km/j".format(maxSpeedSession),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            TextButton(
                onClick = onResetMaxSpeed,
                modifier = Modifier.testTag("reset_max_speed_button")
            ) {
                Text("Reset Max", fontSize = 12.sp)
            }
        }
    }
}
