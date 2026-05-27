package com.uade.alltabs.domain.model

data class SongDetail(
    val mbid: String,
    val titulo: String,
    val artista: String,
    val coverUrl: String? = null,
    val year: String? = null,
    val genre: String? = null,
    val tabs: List<Tab> = emptyList()
)
