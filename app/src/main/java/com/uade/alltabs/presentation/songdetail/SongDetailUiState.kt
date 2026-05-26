package com.uade.alltabs.presentation.songdetail

import com.uade.alltabs.domain.model.Tab

sealed class SongDetailUiState {
    object Idle : SongDetailUiState()
    object Loading : SongDetailUiState()
    data class Success(val songDetail: SongDetail) : SongDetailUiState()
    data class Error(val message: String) : SongDetailUiState()
}

data class SongDetail(
    val mbid: String,
    val titulo: String,
    val artista: String,
    val year: String? = null,
    val genre: String? = null,
    val tempo: String? = null,
    val key: String? = null,
    val tabs: List<Tab> = emptyList() // List of local/Firebase tabs for this song
)