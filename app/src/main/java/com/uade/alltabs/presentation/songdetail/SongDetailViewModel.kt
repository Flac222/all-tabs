package com.uade.alltabs.presentation.songdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uade.alltabs.domain.repository.TabRepository
import com.uade.alltabs.domain.usecase.GetTabUseCase
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<SongDetailUiState>(SongDetailUiState.Idle)
    val uiState: StateFlow<SongDetailUiState> = _uiState.asStateFlow()

    // mbid from navigation arguments
    private val mbid: String? = savedStateHandle["mbid"]

    init {
        mbid?.let { fetchSongDetail(it) }
    }

    fun fetchSongDetail(mbid: String) {
        viewModelScope.launch {
            _uiState.value = SongDetailUiState.Loading
            try {
                // Fetch song details from MusicBrainz (assuming a new API call is added to MusicBrainzApi)
                // For now, let's mock some data or use the existing TabRepository to fetch a single tab as a placeholder
                // You would typically have a separate API call for song details

                val songDetailFromApi = tabRepository.getSongDetailFromApi(mbid)
                val relatedTabs = getTabsByMbidUseCase(mbid)

                relatedTabs.collectLatest { tabs ->
                    val firstTab = tabs.firstOrNull()
                    val songDetail = SongDetail(
                        mbid = mbid,
                        titulo = songDetailFromApi?.titulo ?: firstTab?.titulo ?: "Unknown Title",
                        artista = songDetailFromApi?.artista ?: firstTab?.artista ?: "Unknown Artist",
                        year = songDetailFromApi?.fechaCreacion?.let { 
                            try {
                                java.text.SimpleDateFormat("yyyy").format(java.util.Date(it))
                            } catch (e: Exception) {
                                null
                            }
                        },
                        genre = songDetailFromApi?.acordes, // Reusing 'acordes' for genre temporarily
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
