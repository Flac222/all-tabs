package com.uade.alltabs.presentation.tabdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uade.alltabs.domain.model.SongDetail
import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.repository.TabRepository
import com.uade.alltabs.domain.usecase.GetTabsByMbidUseCase
import com.uade.alltabs.presentation.songdetail.SongDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TabDetailViewModel @Inject constructor(
    private val tabRepository: TabRepository,
    private val getTabsByMbidUseCase: GetTabsByMbidUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<SongDetailUiState>(SongDetailUiState.Idle)
    val uiState: StateFlow<SongDetailUiState> = _uiState.asStateFlow()

    private val mbid: String? = savedStateHandle["mbid"]

    init {
        mbid?.let { fetchSongDetail(it) }
    }

    fun fetchSongDetail(mbid: String) {
        viewModelScope.launch {
            _uiState.value = SongDetailUiState.Loading
            try {
                val relatedTabs = getTabsByMbidUseCase(mbid)
                // TODO: Fetch song details from MusicBrainz API (new API call needed)
                // For now, mock data based on available tab info
                relatedTabs.collectLatest { tabs ->
                    val songDetail = SongDetail(
                        mbid = mbid,
                        titulo = tabs.firstOrNull()?.titulo ?: "Unknown Title",
                        artista = tabs.firstOrNull()?.artista ?: "Unknown Artist",
                        tabs = tabs
                    )
                    _uiState.value = SongDetailUiState.Success(songDetail)
                }
            } catch (e: Exception) {
                _uiState.value = SongDetailUiState.Error(e.localizedMessage ?: "Error loading song details")
            }
        }
    }
}

