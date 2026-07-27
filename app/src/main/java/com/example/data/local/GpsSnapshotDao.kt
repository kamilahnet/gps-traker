package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GpsSnapshotDao {
    @Query("SELECT * FROM gps_snapshots ORDER BY timestamp DESC")
    fun getAllSnapshots(): Flow<List<GpsSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: GpsSnapshotEntity): Long

    @Query("DELETE FROM gps_snapshots WHERE id = :id")
    suspend fun deleteSnapshotById(id: Long)

    @Query("DELETE FROM gps_snapshots")
    suspend fun clearAll()
}
