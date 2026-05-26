package com.uade.alltabs.domain.model

data class Tab(
    val id: String,
    val userId: String,
    val mbid: String?,
    val titulo: String,
    val artista: String,
    val acordes: String,
    val esIA: Boolean,
    val esFavorito: Boolean,
    val fechaCreacion: Long
)
