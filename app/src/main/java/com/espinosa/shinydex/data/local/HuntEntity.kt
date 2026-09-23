package com.espinosa.shinydex.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** One tracked shiny hunt. Stored locally only -- nothing about a hunt leaves the device. */
@Entity(tableName = "hunts")
data class HuntEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "pokemon_id")
    val pokemonId: Int,

    @ColumnInfo(name = "pokemon_name")
    val pokemonName: String,

    @ColumnInfo(name = "generation")
    val generation: String,

    @ColumnInfo(name = "game")
    val game: String,

    @ColumnInfo(name = "method")
    val method: String,

    @ColumnInfo(name = "encounters")
    val encounters: Int = 0,

    @ColumnInfo(name = "is_found")
    val isFound: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
)
