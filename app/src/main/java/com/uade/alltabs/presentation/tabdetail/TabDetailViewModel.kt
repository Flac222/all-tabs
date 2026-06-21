package com.uade.alltabs.presentation.tabdetail

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.model.Place
import com.uade.alltabs.domain.repository.TabRepository
import com.uade.alltabs.core.utils.MusicXmlParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TabDetailUiState {
    object Idle : TabDetailUiState()
    object Loading : TabDetailUiState()
    data class Success(
        val tab: Tab,
        val chords: List<String>,
        val places: List<Place>,
        val isFavorited: Boolean
    ) : TabDetailUiState()
    data class Error(val message: String) : TabDetailUiState()
}

@HiltViewModel
class TabDetailViewModel @Inject constructor(
    private val tabRepository: TabRepository,
    private val firebaseAuth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<TabDetailUiState>(TabDetailUiState.Idle)
    val uiState: StateFlow<TabDetailUiState> = _uiState.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _tempo = MutableStateFlow(120) // BPM
    val tempo: StateFlow<Int> = _tempo.asStateFlow()

    private val _isMetronomeEnabled = MutableStateFlow(false)
    val isMetronomeEnabled: StateFlow<Boolean> = _isMetronomeEnabled.asStateFlow()

    private val _currentPlayIndex = MutableStateFlow(-1)
    val currentPlayIndex: StateFlow<Int> = _currentPlayIndex.asStateFlow()

    private val tabId: String? = savedStateHandle["tabId"]
    private var playbackJob: Job? = null
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (e: Exception) {
            // Fallback for emulator environments without audio
        }
        tabId?.let { fetchTabDetails(it) }
    }

    fun fetchTabDetails(id: String) {
        viewModelScope.launch {
            _uiState.value = TabDetailUiState.Loading
            try {
                val tab = tabRepository.getTab(id)
                if (tab != null) {
                    val chords = MusicXmlParser.parseChords(tab.acordes)
                    val places = MusicXmlParser.parsePlaces(tab.acordes)
                    val userId = firebaseAuth.currentUser?.uid ?: ""
                    val isFav = tabRepository.isTabFavorited(userId, tab.id)
                    _uiState.value = TabDetailUiState.Success(
                        tab = tab,
                        chords = chords,
                        places = places,
                        isFavorited = isFav
                    )
                } else {
                    _uiState.value = TabDetailUiState.Error("No se encontró la tablatura")
                }
            } catch (e: Exception) {
                _uiState.value = TabDetailUiState.Error(e.localizedMessage ?: "Error al cargar la tablatura")
            }
        }
    }

    fun togglePlay() {
        val currentState = _isPlaying.value
        _isPlaying.value = !currentState

        if (_isPlaying.value) {
            startPlayback()
        } else {
            stopPlayback()
        }
    }

    fun setTempo(newTempo: Int) {
        _tempo.value = newTempo.coerceIn(40, 240)
        if (_isPlaying.value) {
            // Restart playback to apply new tempo intervals immediately
            stopPlayback()
            startPlayback()
        }
    }

    fun toggleMetronome() {
        _isMetronomeEnabled.value = !_isMetronomeEnabled.value
    }

    private fun startPlayback() {
        playbackJob?.cancel()
        val state = _uiState.value as? TabDetailUiState.Success ?: return
        val maxIndex = if (state.chords.isNotEmpty()) state.chords.size else state.places.size
        if (maxIndex == 0) return

        playbackJob = viewModelScope.launch {
            while (true) {
                val index = (_currentPlayIndex.value + 1) % maxIndex
                _currentPlayIndex.value = index

                // Trigger Metronome Beep if enabled
                if (_isMetronomeEnabled.value) {
                    try {
                        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
                    } catch (e: Exception) {
                        // Suppress audio output exceptions in test/headless environments
                    }
                }

                // calculate delay in ms based on tempo (60000ms / BPM)
                val delayMs = 60000L / _tempo.value
                delay(delayMs)
            }
        }
    }

    private fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        _currentPlayIndex.value = -1
        _isPlaying.value = false
    }

    fun toggleFavorite() {
        val state = _uiState.value as? TabDetailUiState.Success ?: return
        val tab = state.tab
        val userId = firebaseAuth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                if (state.isFavorited) {
                    tabRepository.removeFavorite(userId, tab.id)
                } else {
                    tabRepository.addFavorite(userId, tab.id, tab.titulo, tab.artista)
                }
                // Refresh local status
                val isFav = tabRepository.isTabFavorited(userId, tab.id)
                _uiState.value = state.copy(isFavorited = isFav)
            } catch (e: Exception) {
                // handle favorite toggling failure silently
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        toneGenerator?.release()
    }
}
