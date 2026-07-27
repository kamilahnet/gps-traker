package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GpsRepository
import com.example.data.local.AppDatabase
import com.example.data.local.GpsSnapshotEntity
import com.example.data.location.GpsData
import com.example.location.LocationManagerHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CoordinateFormat {
    DECIMAL_DEGREES, // DD e.g. -6.17539, 106.82715
    DMS // Degrees Minutes Seconds e.g. 6° 10' 31.41" S
}

class GpsViewModel(application: Application) : AndroidViewModel(application) {

    val locationManagerHelper = LocationManagerHelper(application)
    private val repository: GpsRepository

    val gpsData: StateFlow<GpsData> = locationManagerHelper.gpsData

    private val _isLivePaused = MutableStateFlow(false)
    val isLivePaused: StateFlow<Boolean> = _isLivePaused.asStateFlow()

    private val _coordinateFormat = MutableStateFlow(CoordinateFormat.DECIMAL_DEGREES)
    val coordinateFormat: StateFlow<CoordinateFormat> = _coordinateFormat.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _maxSpeedSession = MutableStateFlow(0f)
    val maxSpeedSession: StateFlow<Float> = _maxSpeedSession.asStateFlow()

    private val _showSaveDialog = MutableStateFlow(false)
    val showSaveDialog: StateFlow<Boolean> = _showSaveDialog.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = GpsRepository(db.gpsSnapshotDao())

        // Monitor speed to keep track of max speed in current session
        viewModelScope.launch {
            gpsData.collect { data ->
                if (data.speedKmh > _maxSpeedSession.value) {
                    _maxSpeedSession.value = data.speedKmh
                }
            }
        }
    }

    val savedSnapshots: StateFlow<List<GpsSnapshotEntity>> = repository.allSnapshots
        .combine(_searchQuery) { snapshots, query ->
            if (query.isBlank()) {
                snapshots
            } else {
                snapshots.filter {
                    it.title.contains(query, ignoreCase = true) ||
                            it.address.contains(query, ignoreCase = true) ||
                            it.provider.contains(query, ignoreCase = true)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun startLocationService() {
        if (!_isLivePaused.value) {
            locationManagerHelper.startLocationUpdates()
        }
    }

    fun stopLocationService() {
        locationManagerHelper.stopLocationUpdates()
    }

    fun togglePauseLiveUpdates() {
        _isLivePaused.value = !_isLivePaused.value
        if (_isLivePaused.value) {
            locationManagerHelper.stopLocationUpdates()
        } else {
            if (!gpsData.value.isSimulationMode) {
                locationManagerHelper.startLocationUpdates()
            }
        }
    }

    fun toggleSimulationMode(enable: Boolean) {
        _isLivePaused.value = false
        locationManagerHelper.toggleSimulationMode(enable)
    }

    fun setCoordinateFormat(format: CoordinateFormat) {
        _coordinateFormat.value = format
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openSaveDialog() {
        _showSaveDialog.value = true
    }

    fun closeSaveDialog() {
        _showSaveDialog.value = false
    }

    fun saveCurrentGpsSnapshot(title: String) {
        val current = gpsData.value
        viewModelScope.launch {
            repository.saveSnapshot(
                GpsSnapshotEntity(
                    title = title.ifBlank { "Snapshot GPS ${System.currentTimeMillis() % 10000}" },
                    latitude = current.latitude,
                    longitude = current.longitude,
                    altitude = current.altitude,
                    accuracy = current.accuracy,
                    speed = current.speed,
                    bearing = current.bearing,
                    provider = current.provider,
                    address = current.address,
                    timestamp = current.timestamp,
                    isMock = current.isMock
                )
            )
            _showSaveDialog.value = false
        }
    }

    fun deleteSnapshot(id: Long) {
        viewModelScope.launch {
            repository.deleteSnapshot(id)
        }
    }

    fun clearAllSnapshots() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun resetMaxSpeed() {
        _maxSpeedSession.value = 0f
    }

    override fun onCleared() {
        super.onCleared()
        locationManagerHelper.unregisterSensors()
        locationManagerHelper.stopLocationUpdates()
    }
}
