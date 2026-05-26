package com.uade.alltabs.presentation.search

data class SongSearchResult(
    val mbid: String,
    val titulo: String,
    val artista: String,
    val tabCount: Int,
    val tabCreators: List<Pair<String, String>> // Pair<UserId, Username>
)