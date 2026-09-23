package com.espinosa.shinydex.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PokedexDao {

    /** Entries whose National Dex number falls inside one generation's own range. */
    @Query("SELECT * FROM pokedex WHERE id BETWEEN :first AND :last ORDER BY id ASC")
    fun observeRange(first: Int, last: Int): Flow<List<PokedexEntity>>

    @Query("SELECT COUNT(*) FROM pokedex WHERE id BETWEEN :first AND :last")
    suspend fun countInRange(first: Int, last: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<PokedexEntity>)
}
