package com.uade.alltabs.presentation.songdetail

import com.uade.alltabs.domain.model.SongDetail

sealed class SongDetailUiState {
    object Idle : SongDetailUiState()
    object Loading : SongDetailUiState()
    data class Success(val songDetail: SongDetail) : SongDetailUiState()
    data class Error(val message: String) : SongDetailUiState()
}
