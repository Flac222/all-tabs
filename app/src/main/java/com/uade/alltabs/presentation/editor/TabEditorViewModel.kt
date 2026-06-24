package com.uade.alltabs.presentation.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.model.Place
import com.uade.alltabs.domain.repository.TabRepository
import com.uade.alltabs.core.utils.MusicXmlParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TabEditorUiState {
    object Idle : TabEditorUiState()
    object Loading : TabEditorUiState()
    data class Success(val tab: Tab) : TabEditorUiState()
    object Saved : TabEditorUiState()
    data class Error(val message: String) : TabEditorUiState()
}

@HiltViewModel
class TabEditorViewModel @Inject constructor(
    private val tabRepository: TabRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<TabEditorUiState>(TabEditorUiState.Idle)
    val uiState: StateFlow<TabEditorUiState> = _uiState.asStateFlow()

    private val _tab = MutableStateFlow<Tab?>(null)
    val tab: StateFlow<Tab?> = _tab.asStateFlow()

    private val _chords = MutableStateFlow<List<String>>(emptyList())
    val chords: StateFlow<List<String>> = _chords.asStateFlow()

    private val _places = MutableStateFlow<List<Place>>(emptyList())
    val places: StateFlow<List<Place>> = _places.asStateFlow()

    private val _activePlaceIndex = MutableStateFlow(0)
    val activePlaceIndex: StateFlow<Int> = _activePlaceIndex.asStateFlow()

    private val _activeStringIndex = MutableStateFlow(-1) // -1 means none selected
    val activeStringIndex: StateFlow<Int> = _activeStringIndex.asStateFlow()

    private val tabId: String? = savedStateHandle["tabId"]

    init {
        tabId?.let { loadTab(it) }
    }

    fun loadTab(id: String) {
        viewModelScope.launch {
            _uiState.value = TabEditorUiState.Loading
            try {
                val tabVal = tabRepository.getTab(id)
                if (tabVal != null) {
                    _tab.value = tabVal
                    _chords.value = MusicXmlParser.parseChords(tabVal.acordes)
                    _places.value = MusicXmlParser.parsePlaces(tabVal.acordes)
                    _activePlaceIndex.value = 0
                    _activeStringIndex.value = -1
                    _uiState.value = TabEditorUiState.Success(tabVal)
                } else {
                    _uiState.value = TabEditorUiState.Error("No se encontró la tablatura en la base de datos")
                }
            } catch (e: Exception) {
                _uiState.value = TabEditorUiState.Error(e.localizedMessage ?: "Error al cargar la tablatura")
            }
        }
    }

    fun selectPlace(index: Int) {
        if (index in _places.value.indices) {
            _activePlaceIndex.value = index
            _activeStringIndex.value = -1 // Reset active string focus
        }
    }

    fun selectStringSlot(stringIdx: Int) {
        if (stringIdx in 0..5) {
            _activeStringIndex.value = stringIdx
        }
    }

    fun addEmptyPlace() {
        val currentPlaces = _places.value.toMutableList()
        val currentChords = _chords.value.toMutableList()

        currentPlaces.add(Place())
        currentChords.add("N/C")

        _places.value = currentPlaces
        _chords.value = currentChords
        _activePlaceIndex.value = currentPlaces.lastIndex
        _activeStringIndex.value = -1
    }

    fun removeActivePlace() {
        val index = _activePlaceIndex.value
        val currentPlaces = _places.value.toMutableList()
        val currentChords = _chords.value.toMutableList()

        if (index in currentPlaces.indices) {
            currentPlaces.removeAt(index)
            if (index in currentChords.indices) {
                currentChords.removeAt(index)
            }
            _places.value = currentPlaces
            _chords.value = currentChords
            _activePlaceIndex.value = (index - 1).coerceAtLeast(0)
            _activeStringIndex.value = -1
        }
    }

    fun addQuickChord(chordName: String) {
        val slots: MutableList<String?> = when (chordName) {
            "C" -> mutableListOf("0", "1", "0", "2", "3", null)
            "G" -> mutableListOf("3", "0", "0", "0", "2", "3")
            "D" -> mutableListOf("2", "3", "2", "0", null, null)
            "Am" -> mutableListOf("0", "1", "2", "2", "0", null)
            "Em" -> mutableListOf("0", "0", "0", "2", "2", "0")
            else -> mutableListOf(null, null, null, null, null, null)
        }
        
        val currentPlaces = _places.value.toMutableList()
        val currentChords = _chords.value.toMutableList()

        currentPlaces.add(Place(slots = slots))
        currentChords.add(chordName)

        _places.value = currentPlaces
        _chords.value = currentChords
        _activePlaceIndex.value = currentPlaces.lastIndex
        _activeStringIndex.value = -1
    }

    fun addNoteToActivePlace() {
        val idx = _activePlaceIndex.value
        val sIdx = _activeStringIndex.value
        val currentPlaces = _places.value.toMutableList()
        if (idx in currentPlaces.indices) {
            val oldPlace = currentPlaces[idx]
            val newPlace = oldPlace.copy(
                slots = oldPlace.slots.toMutableList(),
                tapping = oldPlace.tapping.toMutableList()
            )
            
            // If a specific slot is selected and it is empty, use it. Otherwise, use the first empty slot.
            var targetSlot = -1
            if (sIdx in 0..5 && newPlace.slots[sIdx] == null) {
                targetSlot = sIdx
            } else {
                for (i in 0..5) {
                    if (newPlace.slots[i] == null) {
                        targetSlot = i
                        break
                    }
                }
            }
            
            if (targetSlot != -1) {
                newPlace.slots[targetSlot] = "0" // Default to open string
                currentPlaces[idx] = newPlace
                _places.value = currentPlaces
                _activeStringIndex.value = targetSlot
            }
        }
    }

    fun changeActiveNoteString() {
        val pIdx = _activePlaceIndex.value
        val sIdx = _activeStringIndex.value
        val currentPlaces = _places.value.toMutableList()
        
        if (pIdx in currentPlaces.indices && sIdx in 0..5) {
            val oldPlace = currentPlaces[pIdx]
            val fretVal = oldPlace.slots[sIdx] ?: return
            
            // Find next free string index
            var nextFreeIdx = -1
            for (i in 1..5) {
                val checkIdx = (sIdx + i) % 6
                if (oldPlace.slots[checkIdx] == null) {
                    nextFreeIdx = checkIdx
                    break
                }
            }
            
            if (nextFreeIdx != -1) {
                val newPlace = oldPlace.copy(
                    slots = oldPlace.slots.toMutableList(),
                    tapping = oldPlace.tapping.toMutableList()
                )
                newPlace.slots[nextFreeIdx] = fretVal
                newPlace.slots[sIdx] = null
                
                val wasTapped = newPlace.tapping[sIdx]
                newPlace.tapping[nextFreeIdx] = wasTapped
                newPlace.tapping[sIdx] = false
                
                currentPlaces[pIdx] = newPlace
                _places.value = currentPlaces
                _activeStringIndex.value = nextFreeIdx
            }
        }
    }

    fun updateActiveNoteFret(fretVal: String) {
        val pIdx = _activePlaceIndex.value
        val sIdx = _activeStringIndex.value
        val currentPlaces = _places.value.toMutableList()
        
        if (pIdx in currentPlaces.indices && sIdx in 0..5) {
            val oldPlace = currentPlaces[pIdx]
            val newPlace = oldPlace.copy(slots = oldPlace.slots.toMutableList())
            newPlace.slots[sIdx] = fretVal
            currentPlaces[pIdx] = newPlace
            _places.value = currentPlaces
        }
    }

    fun deleteNoteFromActivePlace() {
        val pIdx = _activePlaceIndex.value
        val sIdx = _activeStringIndex.value
        val currentPlaces = _places.value.toMutableList()
        
        if (pIdx in currentPlaces.indices && sIdx in 0..5) {
            val oldPlace = currentPlaces[pIdx]
            val newPlace = oldPlace.copy(
                slots = oldPlace.slots.toMutableList(),
                tapping = oldPlace.tapping.toMutableList()
            )
            newPlace.slots[sIdx] = null
            newPlace.tapping[sIdx] = false
            currentPlaces[pIdx] = newPlace
            _places.value = currentPlaces
            _activeStringIndex.value = -1
        }
    }

    fun toggleActivePlacePalmMute() {
        val pIdx = _activePlaceIndex.value
        val currentPlaces = _places.value.toMutableList()
        if (pIdx in currentPlaces.indices) {
            val oldPlace = currentPlaces[pIdx]
            val newPlace = oldPlace.copy(isPalmMute = !oldPlace.isPalmMute)
            currentPlaces[pIdx] = newPlace
            _places.value = currentPlaces
        }
    }

    fun toggleActiveNoteTapping() {
        val pIdx = _activePlaceIndex.value
        val sIdx = _activeStringIndex.value
        val currentPlaces = _places.value.toMutableList()
        if (pIdx in currentPlaces.indices && sIdx in 0..5) {
            val oldPlace = currentPlaces[pIdx]
            val newPlace = oldPlace.copy(tapping = oldPlace.tapping.toMutableList())
            newPlace.tapping[sIdx] = !newPlace.tapping[sIdx]
            currentPlaces[pIdx] = newPlace
            _places.value = currentPlaces
        }
    }

    fun toggleActivePlaceBarLine() {
        val pIdx = _activePlaceIndex.value
        val currentPlaces = _places.value.toMutableList()
        if (pIdx in currentPlaces.indices) {
            val oldPlace = currentPlaces[pIdx]
            val newPlace = oldPlace.copy(isBarLine = !oldPlace.isBarLine)
            if (newPlace.isBarLine) {
                // Clear all string slots if it becomes a barline
                newPlace.slots.fill(null)
                newPlace.tapping.fill(false)
            }
            currentPlaces[pIdx] = newPlace
            _places.value = currentPlaces
        }
    }

    fun saveTab() {
        val currentTab = _tab.value ?: return
        viewModelScope.launch {
            _uiState.value = TabEditorUiState.Loading
            try {
                val compiledXml = MusicXmlParser.compileToMusicXml(_chords.value, _places.value)
                val updatedTab = currentTab.copy(
                    acordes = compiledXml,
                    fechaCreacion = System.currentTimeMillis()
                )
                tabRepository.saveTab(updatedTab)
                _tab.value = updatedTab
                _uiState.value = TabEditorUiState.Saved
            } catch (e: Exception) {
                _uiState.value = TabEditorUiState.Error(e.localizedMessage ?: "Error al guardar los cambios")
            }
        }
    }
}
