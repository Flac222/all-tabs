package com.uade.alltabs.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val content: String,
    val createdAt: Long
)
