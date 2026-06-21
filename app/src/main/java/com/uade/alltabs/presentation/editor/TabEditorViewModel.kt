package com.uade.alltabs.presentation.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.repository.TabRepository
import com.uade.alltabs.presentation.tabdetail.GuitarNote
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

    private val _notes = MutableStateFlow<List<GuitarNote>>(emptyList())
    val notes: StateFlow<List<GuitarNote>> = _notes.asStateFlow()

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
                    _chords.value = parseChords(tabVal.acordes)
                    _notes.value = parseNotes(tabVal.acordes)
                    _uiState.value = TabEditorUiState.Success(tabVal)
                } else {
                    _uiState.value = TabEditorUiState.Error("No se encontró la tablatura en la base de datos")
                }
            } catch (e: Exception) {
                _uiState.value = TabEditorUiState.Error(e.localizedMessage ?: "Error al cargar la tablatura")
            }
        }
    }

    fun addChord(chordName: String) {
        val currentList = _chords.value.toMutableList()
        currentList.add(chordName)
        _chords.value = currentList

        // Generate a matching default note for this chord step
        val currentNotes = _notes.value.toMutableList()
        val fret = when {
            chordName.startsWith("C") -> 3
            chordName.startsWith("A") -> 0
            chordName.startsWith("G") -> 3
            chordName.startsWith("F") -> 1
            chordName.startsWith("D") -> 2
            chordName.startsWith("E") -> 2
            else -> 0
        }
        val stringNum = if (chordName.startsWith("E") || chordName.startsWith("G")) 6 else 5
        currentNotes.add(GuitarNote(stringNum = stringNum, fret = fret, step = chordName.take(1)))
        _notes.value = currentNotes
    }

    fun removeChord(index: Int) {
        val currentChords = _chords.value.toMutableList()
        val currentNotes = _notes.value.toMutableList()

        if (index in currentChords.indices) {
            currentChords.removeAt(index)
            _chords.value = currentChords
        }
        if (index in currentNotes.indices) {
            currentNotes.removeAt(index)
            _notes.value = currentNotes
        }
    }

    fun updateNote(index: Int, stringNum: Int, fret: Int) {
        val currentNotes = _notes.value.toMutableList()
        if (index in currentNotes.indices) {
            val step = when(stringNum) {
                1 -> "E"
                2 -> "B"
                3 -> "G"
                4 -> "D"
                5 -> "A"
                else -> "E"
            }
            currentNotes[index] = GuitarNote(stringNum = stringNum, fret = fret, step = step)
            _notes.value = currentNotes
        }
    }

    fun saveTab() {
        val currentTab = _tab.value ?: return
        viewModelScope.launch {
            _uiState.value = TabEditorUiState.Loading
            try {
                val compiledXml = compileToMusicXml(_chords.value, _notes.value)
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

    private fun compileToMusicXml(chords: List<String>, notes: List<GuitarNote>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<score-partwise version=\"3.1\">\n")
        sb.append("  <part-list>\n")
        sb.append("    <score-part id=\"P1\">\n")
        sb.append("      <part-name>Guitar</part-name>\n")
        sb.append("    </score-part>\n")
        sb.append("  </part-list>\n")
        sb.append("  <part id=\"P1\">\n")
        sb.append("    <measure number=\"1\">\n")
        sb.append("      <attributes>\n")
        sb.append("        <divisions>1</divisions>\n")
        sb.append("        <key><fifths>0</fifths></key>\n")
        sb.append("        <time><beats>4</beats><beat-type>4</beat-type></time>\n")
        sb.append("        <clef><sign>TAB</sign><line>5</line></clef>\n")
        sb.append("        <staff-details>\n")
        sb.append("          <staff-lines>6</staff-lines>\n")
        sb.append("          <staff-tuning line=\"1\"><tuning-step>E</tuning-step><tuning-octave>4</tuning-octave></staff-tuning>\n")
        sb.append("          <staff-tuning line=\"2\"><tuning-step>B</tuning-step><tuning-octave>3</tuning-octave></staff-tuning>\n")
        sb.append("          <staff-tuning line=\"3\"><tuning-step>G</tuning-step><tuning-octave>3</tuning-octave></staff-tuning>\n")
        sb.append("          <staff-tuning line=\"4\"><tuning-step>D</tuning-step><tuning-octave>3</tuning-octave></staff-tuning>\n")
        sb.append("          <staff-tuning line=\"5\"><tuning-step>A</tuning-step><tuning-octave>2</tuning-octave></staff-tuning>\n")
        sb.append("          <staff-tuning line=\"6\"><tuning-step>E</tuning-step><tuning-octave>2</tuning-octave></staff-tuning>\n")
        sb.append("        </staff-details>\n")
        sb.append("      </attributes>\n")

        notes.forEachIndexed { index, note ->
            val chord = chords.getOrNull(index) ?: "N/C"
            sb.append("      <!-- Harmony: $chord -->\n")
            sb.append("      <harmony>\n")
            sb.append("        <root><root-step>${chord.take(1)}</root-step></root>\n")
            sb.append("      </harmony>\n")
            sb.append("      <note>\n")
            sb.append("        <pitch><step>${note.step}</step><octave>3</octave></pitch>\n")
            sb.append("        <duration>1</duration>\n")
            sb.append("        <type>quarter</type>\n")
            sb.append("        <technical>\n")
            sb.append("          <string>${note.stringNum}</string>\n")
            sb.append("          <fret>${note.fret}</fret>\n")
            sb.append("        </technical>\n")
            sb.append("      </note>\n")
        }

        sb.append("    </measure>\n")
        sb.append("  </part>\n")
        sb.append("</score-partwise>")
        return sb.toString()
    }

    private fun parseChords(xml: String): List<String> {
        val parsedList = mutableListOf<String>()
        val commentRegex = Regex("<!-- Harmony:\\s*(.*?)\\s*-->")
        val commentMatches = commentRegex.findAll(xml).toList()
        for (m in commentMatches) {
            parsedList.add(m.groupValues[1])
        }
        return parsedList
    }

    private fun parseNotes(xml: String): List<GuitarNote> {
        val notes = mutableListOf<GuitarNote>()
        val noteRegex = Regex("<note>.*?<step>([A-G])</step>.*?<string>([1-6])</string>.*?<fret>(\\d+)</fret>.*?</note>", RegexOption.DOT_MATCHES_ALL)
        val matches = noteRegex.findAll(xml)
        for (m in matches) {
            val step = m.groupValues[1]
            val stringNum = m.groupValues[2].toIntOrNull() ?: 6
            val fret = m.groupValues[3].toIntOrNull() ?: 0
            notes.add(GuitarNote(stringNum = stringNum, fret = fret, step = step))
        }
        return notes
    }
}
