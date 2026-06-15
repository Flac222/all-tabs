package com.uade.alltabs.presentation.songdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.uade.alltabs.domain.model.SongDetail
import com.uade.alltabs.domain.repository.TabRepository
import com.uade.alltabs.domain.usecase.GetTabsByMbidUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SongDetailViewModel @Inject constructor(
    private val tabRepository: TabRepository,
    private val getTabsByMbidUseCase: GetTabsByMbidUseCase,
    private val firebaseAuth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<SongDetailUiState>(SongDetailUiState.Idle)
    val uiState: StateFlow<SongDetailUiState> = _uiState.asStateFlow()

    private val mbid: String? = savedStateHandle["mbid"]
    private val initialTitle: String? = savedStateHandle["title"]
    private val initialArtist: String? = savedStateHandle["artist"]

    init {
        mbid?.let { fetchSongDetail(it, initialTitle, initialArtist) }
    }

    fun fetchSongDetail(mbid: String, title: String? = null, artist: String? = null) {
        viewModelScope.launch {
            _uiState.value = SongDetailUiState.Loading
            try {
                val songTab = tabRepository.getSongDetailFromApi(mbid)
                
                // Get cover art URL using the MBID from the API (which might be a release-group ID now)
                val coverUrl = songTab?.mbid?.let { tabRepository.getCoverArtUrl(it) }

                getTabsByMbidUseCase(mbid, title ?: songTab?.titulo, artist ?: songTab?.artista).collectLatest { tabs ->
                    val songDetail = SongDetail(
                        mbid = mbid,
                        titulo = title ?: songTab?.titulo ?: tabs.firstOrNull()?.titulo ?: "Unknown Title",
                        artista = artist ?: songTab?.artista ?: tabs.firstOrNull()?.artista ?: "Unknown Artist",
                        coverUrl = coverUrl,
                        year = songTab?.acordes?.takeIf { it.isNotEmpty() }?.let { dateStr ->
                            // MusicBrainz dates can be YYYY-MM-DD, YYYY-MM, or YYYY
                            dateStr.split("-").firstOrNull()
                        },
                        tabs = tabs
                    )
                    _uiState.value = SongDetailUiState.Success(songDetail)
                }
            } catch (e: Exception) {
                _uiState.value = SongDetailUiState.Error(e.localizedMessage ?: "Error loading song details")
            }
        }
    }

    fun toggleFavorite(tabId: String, title: String, artist: String, currentStatus: Boolean) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch {
            if (currentStatus) {
                tabRepository.removeFavorite(userId, tabId)
            } else {
                tabRepository.addFavorite(userId, tabId, title, artist)
            }
            mbid?.let { fetchSongDetail(it, initialTitle, initialArtist) }
        }
    }
}
