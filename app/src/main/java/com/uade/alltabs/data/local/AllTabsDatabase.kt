package com.uade.alltabs.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TabEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AllTabsDatabase : RoomDatabase() {
    abstract fun tabDao(): TabDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        const val DATABASE_NAME = "all_tabs_db"
    }
}
