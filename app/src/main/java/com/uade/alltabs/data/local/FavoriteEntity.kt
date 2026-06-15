package com.uade.alltabs.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tabId: String,
    val userId: String,
    val titulo: String,
    val artista: String,
    val timestamp: Long
)
