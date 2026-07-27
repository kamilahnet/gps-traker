package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.CompassSpeedometerCard
import com.example.ui.components.GpsDiagnosticsCard
import com.example.ui.components.GpsHeaderCard
import com.example.ui.components.GpsHistorySheet
import com.example.ui.components.GpsMetricsGrid
import com.example.ui.components.LocationAddressCard
import com.example.ui.components.SaveSnapshotDialog
import com.example.ui.theme.GpsAmberWarning
import com.example.ui.theme.GpsEmeraldSuccess
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GpsViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                GpsCheckerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun GpsCheckerApp(
    viewModel: GpsViewModel = viewModel()
) {
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val gpsData by viewModel.gpsData.collectAsStateWithLifecycle()
    val isPaused by viewModel.isLivePaused.collectAsStateWithLifecycle()
    val coordinateFormat by viewModel.coordinateFormat.collectAsStateWithLifecycle()
    val savedSnapshots by viewModel.savedSnapshots.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val maxSpeedSession by viewModel.maxSpeedSession.collectAsStateWithLifecycle()
    val showSaveDialog by viewModel.showSaveDialog.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (locationPermissionsState.allPermissionsGranted) {
                    viewModel.startLocationService()
                }
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.stopLocationService()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            viewModel.startLocationService()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "GPS Checker",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Pemantau Signal & Koordinat",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Satellites Badge Indicator
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SatelliteAlt,
                                contentDescription = "Satelit",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (gpsData.satellitesInView > 0) "${gpsData.satellitesInFix}/${gpsData.satellitesInView}" else "Active",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    // Refresh Location Button
                    IconButton(
                        onClick = {
                            if (locationPermissionsState.allPermissionsGranted) {
                                viewModel.startLocationService()
                            } else {
                                locationPermissionsState.launchMultiplePermissionRequest()
                            }
                        },
                        modifier = Modifier.testTag("refresh_gps_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Muat Ulang GPS"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission Banner if location permission is not granted
            if (!locationPermissionsState.allPermissionsGranted) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("permission_banner_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = GpsAmberWarning.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOff,
                                contentDescription = null,
                                tint = GpsAmberWarning,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Izin Lokasi Presisi Diperlukan",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = GpsAmberWarning
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Beri izin lokasi agar GPS Checker dapat membaca data sinyal satelit, koordinat presisi, dan kecepatan secara real-time.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { locationPermissionsState.launchMultiplePermissionRequest() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("grant_permission_button")
                        ) {
                            Text("Izinkan Akses Lokasi")
                        }
                    }
                }
            }

            // 1. Primary Live Coordinate Header
            GpsHeaderCard(
                gpsData = gpsData,
                isPaused = isPaused,
                coordinateFormat = coordinateFormat,
                onTogglePause = { viewModel.togglePauseLiveUpdates() },
                onToggleFormat = {
                    val nextFormat = if (coordinateFormat == com.example.ui.viewmodel.CoordinateFormat.DECIMAL_DEGREES) {
                        com.example.ui.viewmodel.CoordinateFormat.DMS
                    } else {
                        com.example.ui.viewmodel.CoordinateFormat.DECIMAL_DEGREES
                    }
                    viewModel.setCoordinateFormat(nextFormat)
                },
                onOpenSaveDialog = { viewModel.openSaveDialog() }
            )

            // 2. Metrics Grid (Accuracy, Altitude, Speed, Satellites, Bearing, Latency)
            GpsMetricsGrid(gpsData = gpsData)

            // 3. Interactive Compass & Speedometer Visualizer
            CompassSpeedometerCard(
                gpsData = gpsData,
                maxSpeedSession = maxSpeedSession,
                onResetMaxSpeed = { viewModel.resetMaxSpeed() }
            )

            // 4. Reverse Geocoded Location Address Card
            LocationAddressCard(gpsData = gpsData)

            // 5. System & GPS Diagnostics Switcher Card
            GpsDiagnosticsCard(
                gpsData = gpsData,
                onToggleSimulation = { viewModel.toggleSimulationMode(it) }
            )

            // 6. Saved Waypoints & Snapshots History Log
            GpsHistorySheet(
                snapshots = savedSnapshots,
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onDeleteSnapshot = { viewModel.deleteSnapshot(it) },
                onClearAll = { viewModel.clearAllSnapshots() }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Save Snapshot Dialog
        if (showSaveDialog) {
            SaveSnapshotDialog(
                gpsData = gpsData,
                onDismiss = { viewModel.closeSaveDialog() },
                onConfirmSave = { title -> viewModel.saveCurrentGpsSnapshot(title) }
            )
        }
    }
}
