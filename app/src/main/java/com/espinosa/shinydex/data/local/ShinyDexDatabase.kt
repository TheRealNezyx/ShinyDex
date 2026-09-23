package com.espinosa.shinydex.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [HuntEntity::class, PokedexEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ShinyDexDatabase : RoomDatabase() {

    abstract fun huntDao(): HuntDao

    abstract fun pokedexDao(): PokedexDao

    companion object {
        private const val NAME = "shinydex.db"

        @Volatile
        private var instance: ShinyDexDatabase? = null

        fun get(context: Context): ShinyDexDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ShinyDexDatabase::class.java,
                    NAME,
                ).build().also { instance = it }
            }
    }
}
