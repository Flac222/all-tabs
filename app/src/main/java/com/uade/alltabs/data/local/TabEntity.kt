package com.uade.alltabs.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val userName: String,
    val mbid: String?,
    val titulo: String,
    val artista: String,
    val acordes: String,
    val esIA: Boolean,
    val esFavorito: Boolean,
    val fechaCreacion: Long
)
