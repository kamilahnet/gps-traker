package com.example.location

import com.example.data.location.GpsData
import com.example.data.location.SignalQuality
import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Address
import android.location.Geocoder
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class LocationManagerHelper(private val context: Context) : SensorEventListener {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val _gpsData = MutableStateFlow(GpsData())
    val gpsData: StateFlow<GpsData> = _gpsData.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private var isUpdating = false
    private var simulationJob: Job? = null

    // Sensors for Compass Bearing
    private var accelerometerReading = FloatArray(3)
    private var magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var gnssStatusCallback: GnssStatus.Callback? = null
    private var locationCallback: LocationCallback? = null

    init {
        registerSensors()
        registerGnssStatus()
    }

    fun checkGpsEnabled(): Boolean {
        return try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(updateIntervalMs: Long = 2000L) {
        if (isUpdating) return
        isUpdating = true

        val isGpsActive = checkGpsEnabled()
        _gpsData.value = _gpsData.value.copy(
            isGpsEnabled = isGpsActive,
            isSearching = true
        )

        // Request Last Known Location first for quick display
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                updateGpsDataFromLocation(location)
            }
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            updateIntervalMs
        ).setMinUpdateIntervalMillis(1000L)
            .setWaitForAccurateLocation(false)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                updateGpsDataFromLocation(location)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            _gpsData.value = _gpsData.value.copy(
                isSearching = false,
                address = "Izin lokasi belum diberikan."
            )
        } catch (e: Exception) {
            // Fallback to LocationManager
            startLocationManagerUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationManagerUpdates() {
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                updateGpsDataFromLocation(location)
            }

            override fun onProviderEnabled(provider: String) {
                _gpsData.value = _gpsData.value.copy(isGpsEnabled = true)
            }

            override fun onProviderDisabled(provider: String) {
                _gpsData.value = _gpsData.value.copy(isGpsEnabled = false)
            }
        }

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L,
                    1f,
                    listener,
                    Looper.getMainLooper()
                )
            }
        } catch (e: Exception) {
            _gpsData.value = _gpsData.value.copy(isSearching = false)
        }
    }

    fun stopLocationUpdates() {
        isUpdating = false
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        _gpsData.value = _gpsData.value.copy(isSearching = false)
    }

    private fun updateGpsDataFromLocation(location: Location) {
        val now = System.currentTimeMillis()
        val fixLatency = if (location.time > 0) now - location.time else 0L

        val isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }

        val currentBearing = if (location.hasBearing()) {
            location.bearing
        } else {
            _gpsData.value.bearing
        }

        val currentAltitude = if (location.hasAltitude()) location.altitude else _gpsData.value.altitude
        val currentSpeed = if (location.hasSpeed()) location.speed else 0.0f

        val updated = _gpsData.value.copy(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = currentAltitude,
            accuracy = location.accuracy,
            speed = currentSpeed,
            bearing = currentBearing,
            provider = location.provider?.uppercase(Locale.getDefault()) ?: "GPS",
            timestamp = location.time,
            isMock = isMock,
            isSearching = false,
            isGpsEnabled = checkGpsEnabled(),
            lastFixLatencyMs = fixLatency
        )
        _gpsData.value = updated

        // Trigger Geocoding in background
        fetchAddress(location.latitude, location.longitude)
    }

    private fun fetchAddress(lat: Double, lng: Double) {
        scope.launch {
            try {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(context, Locale("id", "ID"))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(lat, lng, 1) { addresses ->
                            val addrStr = formatAddress(addresses.firstOrNull())
                            _gpsData.value = _gpsData.value.copy(address = addrStr)
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = withContext(Dispatchers.IO) {
                            geocoder.getFromLocation(lat, lng, 1)
                        }
                        val addrStr = formatAddress(addresses?.firstOrNull())
                        _gpsData.value = _gpsData.value.copy(address = addrStr)
                    }
                } else {
                    _gpsData.value = _gpsData.value.copy(address = "Lat: %.5f, Lng: %.5f".format(lat, lng))
                }
            } catch (e: Exception) {
                _gpsData.value = _gpsData.value.copy(
                    address = "Lat: %.5f, Lng: %.5f (Offline)".format(lat, lng)
                )
            }
        }
    }

    private fun formatAddress(address: Address?): String {
        if (address == null) return "Alamat tidak ditemukan"
        val sb = StringBuilder()
        address.thoroughfare?.let { sb.append(it).append(", ") }
        address.subLocality?.let { sb.append(it).append(", ") }
        address.locality?.let { sb.append(it).append(", ") }
        address.subAdminArea?.let { sb.append(it).append(", ") }
        address.adminArea?.let { sb.append(it).append(", ") }
        address.countryName?.let { sb.append(it) }

        return sb.toString().trimEnd(',', ' ')
            .ifEmpty { address.getAddressLine(0) ?: "Alamat tidak diketahui" }
    }

    // --- Compass Sensor Management ---
    private fun registerSensors() {
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        accel?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        mag?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    fun unregisterSensors() {
        sensorManager.unregisterListener(this)
        unregisterGnssStatus()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
        }

        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        // Azimuth in degrees
        val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        val normalizedAzimuth = (azimuth + 360) % 360

        // Only update bearing from sensor if speed is very low (stationary)
        if (_gpsData.value.speed < 0.5f) {
            _gpsData.value = _gpsData.value.copy(bearing = normalizedAzimuth)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // --- GNSS Satellite Listener ---
    @SuppressLint("MissingPermission")
    private fun registerGnssStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            gnssStatusCallback = object : GnssStatus.Callback() {
                override fun onSatelliteStatusChanged(status: GnssStatus) {
                    val inView = status.satelliteCount
                    var inFix = 0
                    for (i in 0 until inView) {
                        if (status.usedInFix(i)) {
                            inFix++
                        }
                    }
                    _gpsData.value = _gpsData.value.copy(
                        satellitesInView = inView,
                        satellitesInFix = inFix
                    )
                }
            }
            try {
                locationManager.registerGnssStatusCallback(
                    gnssStatusCallback!!,
                    android.os.Handler(Looper.getMainLooper())
                )
            } catch (e: Exception) {
                // Ignore if permission or hardware missing
            }
        }
    }

    private fun unregisterGnssStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            gnssStatusCallback?.let {
                try {
                    locationManager.unregisterGnssStatusCallback(it)
                } catch (e: Exception) {
                }
            }
        }
    }

    // --- Simulation Mode for testing GPS Checker UI ---
    fun toggleSimulationMode(enable: Boolean) {
        simulationJob?.cancel()
        if (enable) {
            stopLocationUpdates()
            _gpsData.value = _gpsData.value.copy(
                isSimulationMode = true,
                isGpsEnabled = true,
                isSearching = false,
                provider = "SIMULASI",
                latitude = -6.175392, // Jakarta Monas landmark default
                longitude = 106.827153,
                altitude = 18.5,
                accuracy = 3.2f,
                speed = 5.5f,
                bearing = 45f,
                satellitesInView = 18,
                satellitesInFix = 12,
                isMock = false,
                address = "Monumen Nasional, Gambir, Jakarta Pusat, DKI Jakarta"
            )

            simulationJob = scope.launch {
                var step = 0
                while (true) {
                    delay(1500L)
                    step++
                    val current = _gpsData.value
                    val dLat = (Math.sin(step * 0.1) * 0.0001)
                    val dLng = (Math.cos(step * 0.1) * 0.0001)
                    val newLat = current.latitude + dLat
                    val newLng = current.longitude + dLng
                    val newBearing = (current.bearing + 12f) % 360f
                    val newSpeed = (15f + Math.sin(step * 0.2) * 8f).toFloat()

                    _gpsData.value = current.copy(
                        latitude = newLat,
                        longitude = newLng,
                        bearing = newBearing,
                        speed = newSpeed,
                        timestamp = System.currentTimeMillis()
                    )
                }
            }
        } else {
            simulationJob?.cancel()
            _gpsData.value = _gpsData.value.copy(
                isSimulationMode = false,
                provider = "GPS"
            )
            startLocationUpdates()
        }
    }
}
