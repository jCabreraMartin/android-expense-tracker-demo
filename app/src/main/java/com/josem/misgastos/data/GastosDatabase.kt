package com.josem.misgastos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [MovementEntity::class, CategoryEntity::class, AccountEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class GastosDatabase : RoomDatabase() {
    abstract fun gastosDao(): GastosDao

    companion object {
        @Volatile private var instance: GastosDatabase? = null

        fun get(context: Context): GastosDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                GastosDatabase::class.java,
                "mis-gastos.db",
            ).build().also { instance = it }
        }
    }
}
