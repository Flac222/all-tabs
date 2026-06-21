package com.uade.alltabs.presentation.createTab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.usecase.GetUserUseCase
import com.uade.alltabs.domain.usecase.SaveTabUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class CreateTabUiState {
    object Idle : CreateTabUiState()
    object Loading : CreateTabUiState()
    data class Success(val tabId: String) : CreateTabUiState()
    data class Error(val message: String) : CreateTabUiState()
}

@HiltViewModel
class CreateTabViewModel @Inject constructor(
    private val saveTabUseCase: SaveTabUseCase,
    private val getUserUseCase: GetUserUseCase,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateTabUiState>(CreateTabUiState.Idle)
    val uiState: StateFlow<CreateTabUiState> = _uiState.asStateFlow()

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

    fun createCustomTab(titulo: String, artista: String, mbid: String? = null) {
        if (titulo.isBlank() || artista.isBlank()) {
            _uiState.value = CreateTabUiState.Error("Título y Artista son obligatorios")
            return
        }

        val uid = firebaseAuth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            _uiState.value = CreateTabUiState.Loading
            try {
                val tabId = UUID.randomUUID().toString()
                val newTab = Tab(
                    id = tabId,
                    userId = uid,
                    userName = _userName.value,
                    mbid = mbid,
                    titulo = titulo,
                    artista = artista,
                    acordes = "", // Will be edited in the next delivery
                    esIA = false,
                    esFavorito = false,
                    fechaCreacion = System.currentTimeMillis()
                )
                saveTabUseCase(newTab)
                _uiState.value = CreateTabUiState.Success(tabId)
            } catch (e: Exception) {
                _uiState.value = CreateTabUiState.Error(e.localizedMessage ?: "Error al guardar la tab")
            }
        }
    }
}
