package com.uade.alltabs.domain.model

data class Tab(
    val id: String,
    val title: String,
    val artist: String,
    val content: String,
    val createdAt: Long,
    val imageUrl: String? = null
)
