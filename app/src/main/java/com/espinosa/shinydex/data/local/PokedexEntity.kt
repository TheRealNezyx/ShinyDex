package com.espinosa.shinydex.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Offline cache of the Pokedex so the app still works without a connection. */
@Entity(tableName = "pokedex")
data class PokedexEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int,

    @ColumnInfo(name = "name")
    val name: String,
)
