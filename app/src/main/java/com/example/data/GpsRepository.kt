package com.example.data

import com.example.data.local.GpsSnapshotDao
import com.example.data.local.GpsSnapshotEntity
import kotlinx.coroutines.flow.Flow

class GpsRepository(private val snapshotDao: GpsSnapshotDao) {
    val allSnapshots: Flow<List<GpsSnapshotEntity>> = snapshotDao.getAllSnapshots()

    suspend fun saveSnapshot(snapshot: GpsSnapshotEntity): Long {
        return snapshotDao.insertSnapshot(snapshot)
    }

    suspend fun deleteSnapshot(id: Long) {
        snapshotDao.deleteSnapshotById(id)
    }

    suspend fun clearAll() {
        snapshotDao.clearAll()
    }
}
