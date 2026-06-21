package com.uade.alltabs.presentation.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.usecase.GenerateTabWithAiUseCase
import com.uade.alltabs.domain.usecase.GetUserUseCase
import com.uade.alltabs.domain.usecase.SaveTabUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class AiJamUiState {
    object Idle : AiJamUiState()
    object Loading : AiJamUiState()
    data class Success(val chords: List<String>, val musicXml: String, val generatedPrompt: String) : AiJamUiState()
    data class Error(val message: String) : AiJamUiState()
    data class Saved(val tabId: String) : AiJamUiState()
}

@HiltViewModel
class AiJamViewModel @Inject constructor(
    private val generateTabWithAiUseCase: GenerateTabWithAiUseCase,
    private val saveTabUseCase: SaveTabUseCase,
    private val getUserUseCase: GetUserUseCase,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<AiJamUiState>(AiJamUiState.Idle)
    val uiState: StateFlow<AiJamUiState> = _uiState.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    init {
        fetchUserName()
    }

    private fun fetchUserName() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch {
            val user = getUserUseCase(uid)
            _userName.value = user?.nombre ?: "Usuario Desconocido"
        }
    }

    fun generateChords(prompt: String) {
        if (prompt.isBlank()) {
            _uiState.value = AiJamUiState.Error("El prompt no puede estar vacío")
            return
        }

        viewModelScope.launch {
            _uiState.value = AiJamUiState.Loading
            try {
                val rawXml = generateTabWithAiUseCase(prompt)
                val chordList = parseMusicXmlToChordList(rawXml)
                if (chordList.isEmpty()) {
                    _uiState.value = AiJamUiState.Error("No se pudieron generar acordes válidos")
                } else {
                    val uid = firebaseAuth.currentUser?.uid ?: ""
                    val tabId = UUID.randomUUID().toString()
                    val tab = Tab(
                        id = tabId,
                        userId = uid,
                        userName = _userName.value,
                        mbid = null,
                        titulo = prompt,
                        artista = "Compositor IA",
                        acordes = rawXml,
                        esIA = true,
                        esFavorito = false,
                        fechaCreacion = System.currentTimeMillis()
                    )
                    saveTabUseCase(tab)
                    _uiState.value = AiJamUiState.Saved(tabId)
                }
            } catch (e: Exception) {
                _uiState.value = AiJamUiState.Error(e.localizedMessage ?: "Error al generar acordes")
            }
        }
    }

    private fun parseMusicXmlToChordList(xml: String): List<String> {
        val parsedList = mutableListOf<String>()

        // 1. Try comment-based harmony representation (fallback helper)
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

        // 3. Try note technical step extraction
        if (parsedList.isEmpty()) {
            val noteRegex = Regex("<note>.*?<step>([A-G])</step>.*?(?:<alter>(-?\\d+)</alter>)?.*?(?:<octave>(\\d+)</octave>)?.*?</note>", RegexOption.DOT_MATCHES_ALL)
            val noteMatches = noteRegex.findAll(xml).toList()
            for (m in noteMatches) {
                val step = m.groupValues[1]
                val alter = m.groupValues[2]
                val altStr = when(alter) {
                    "1" -> "#"
                    "-1" -> "b"
                    else -> ""
                }
                val octave = m.groupValues[3]
                parsedList.add("$step$altStr$octave")
            }
        }

        return parsedList
    }

    fun resetState() {
        _uiState.value = AiJamUiState.Idle
    }
}
