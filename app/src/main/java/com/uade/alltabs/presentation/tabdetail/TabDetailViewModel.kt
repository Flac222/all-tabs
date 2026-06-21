package com.uade.alltabs.presentation.tabdetail

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.repository.TabRepository
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
        val notes: List<GuitarNote>,
        val isFavorited: Boolean
    ) : TabDetailUiState()
    data class Error(val message: String) : TabDetailUiState()
}

data class GuitarNote(
    val stringNum: Int, // 1 (High E) to 6 (Low E)
    val fret: Int,      // Fret number, e.g. 3
    val step: String    // Musical note name, e.g. "C"
)

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
                    val chords = parseChords(tab.acordes)
                    val notes = parseNotes(tab.acordes)
                    val userId = firebaseAuth.currentUser?.uid ?: ""
                    val isFav = tabRepository.isTabFavorited(userId, tab.id)
                    _uiState.value = TabDetailUiState.Success(
                        tab = tab,
                        chords = chords,
                        notes = notes,
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
        val maxIndex = if (state.chords.isNotEmpty()) state.chords.size else state.notes.size
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

    private fun parseChords(xml: String): List<String> {
        val parsedList = mutableListOf<String>()

        // 1. Try comment-based harmony representation
        val commentRegex = Regex("<!-- Harmony:\\s*(.*?)\\s*-->")
        val commentMatches = commentRegex.findAll(xml).toList()
        for (m in commentMatches) {
            parsedList.add(m.groupValues[1])
        }

        // 2. Try harmony block representation
        if (parsedList.isEmpty()) {
            val harmonyRegex = Regex("<harmony>.*?<root-step>([A-G])</root-step>.*?(?:<root-alter>(-?\\d+)</root-alter>)?.*?(?:<kind>([a-zA-Z0-9]+)</kind>)?.*?</harmony>", RegexOption.DOT_MATCHES_ALL)
            val harmonyMatches = harmonyRegex.findAll(xml).toList()
            for (match in harmonyMatches) {
                val step = match.groupValues[1]
                val alter = match.groupValues[2]
                val kind = match.groupValues[3]
                val altStr = when(alter) {
                    "1" -> "#"
                    "-1" -> "b"
                    else -> ""
                }
                val kindStr = when(kind) {
                    "minor", "min" -> "m"
                    "major", "maj" -> ""
                    "dominant", "7" -> "7"
                    else -> kind
                }
                parsedList.add("$step$altStr$kindStr")
            }
        }

        return parsedList
    }

    private fun parseNotes(xml: String): List<GuitarNote> {
        val notes = mutableListOf<GuitarNote>()
        // Parse note elements containing technical attributes (string & fret)
        val noteRegex = Regex("<note>.*?<step>([A-G])</step>.*?<string>([1-6])</string>.*?<fret>(\\d+)</fret>.*?</note>", RegexOption.DOT_MATCHES_ALL)
        val matches = noteRegex.findAll(xml)
        for (m in matches) {
            val step = m.groupValues[1]
            val stringNum = m.groupValues[2].toIntOrNull() ?: 6
            val fret = m.groupValues[3].toIntOrNull() ?: 0
            notes.add(GuitarNote(stringNum = stringNum, fret = fret, step = step))
        }

        // Fallback: If no technical notes exist (e.g. from custom simple chords), map chords to some mock strings/frets
        if (notes.isEmpty()) {
            val chords = parseChords(xml)
            chords.forEachIndexed { i, chord ->
                // Generate a mock fret position for visual representation
                val fret = when {
                    chord.startsWith("C") -> 3
                    chord.startsWith("A") -> 0
                    chord.startsWith("G") -> 3
                    chord.startsWith("F") -> 1
                    chord.startsWith("D") -> 2
                    chord.startsWith("E") -> 2
                    else -> 0
                }
                val stringNum = if (chord.startsWith("E") || chord.startsWith("G")) 6 else 5
                notes.add(GuitarNote(stringNum = stringNum, fret = fret, step = chord.take(1)))
            }
        }
        return notes
    }

    override fun onCleared() {
        super.onCleared()
        toneGenerator?.release()
    }
}
