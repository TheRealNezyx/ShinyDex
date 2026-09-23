package com.espinosa.shinydex.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HuntDao {

    @Query("SELECT * FROM hunts ORDER BY is_found ASC, updated_at DESC")
    fun observeAll(): Flow<List<HuntEntity>>

    @Query("SELECT * FROM hunts WHERE id = :id")
    fun observeById(id: Long): Flow<HuntEntity?>

    @Query("SELECT * FROM hunts WHERE id = :id")
    suspend fun findById(id: Long): HuntEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(hunt: HuntEntity): Long

    @Update
    suspend fun update(hunt: HuntEntity)

    @Delete
    suspend fun delete(hunt: HuntEntity)

    @Query("UPDATE hunts SET encounters = :encounters, updated_at = :updatedAt WHERE id = :id")
    suspend fun setEncounters(id: Long, encounters: Int, updatedAt: Long)
}
