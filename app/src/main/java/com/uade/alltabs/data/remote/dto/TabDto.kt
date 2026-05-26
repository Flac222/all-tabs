package com.uade.alltabs.data.remote.dto

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class TabDto(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val mbid: String? = null,
    val titulo: String = "",
    val artista: String = "",
    val acordes: String = "",
    val esIA: Boolean = false,
    val esFavorito: Boolean = false,
    @ServerTimestamp
    val fechaCreacion: Timestamp? = null
)
