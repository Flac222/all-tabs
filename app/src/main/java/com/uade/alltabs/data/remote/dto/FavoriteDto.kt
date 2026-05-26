package com.uade.alltabs.data.remote.dto

data class FavoriteDto(
    val userId: String = "",
    val tabId: String = "",
    val titulo: String = "", // Redundancy for offline display
    val artista: String = "" // Redundancy for offline display
)
