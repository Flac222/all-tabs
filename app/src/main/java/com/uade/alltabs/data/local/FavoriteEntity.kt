package com.uade.alltabs.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorites",
    indices = [Index(value = ["userId", "tabId"], unique = true)]
)
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tabId: String,
    val userId: String,
    val titulo: String,
    val artista: String,
    val timestamp: Long
)
