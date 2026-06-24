package com.uade.alltabs.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.uade.alltabs.domain.model.User
import com.uade.alltabs.domain.usecase.GetTabsByUserIdUseCase
import com.uade.alltabs.domain.usecase.GetUserUseCase
import com.uade.alltabs.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(
        val user: User,
        val tabCount: Int
    ) : ProfileUiState()
    object LoggedOut : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserUseCase: GetUserUseCase,
    private val getTabsByUserIdUseCase: GetTabsByUserIdUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _tabCount = MutableStateFlow(0)

    init {
        loadProfile()
    }

    fun loadProfile() {
        val uid = firebaseAuth.currentUser?.uid ?: run {
            _uiState.value = ProfileUiState.Error("No hay sesión activa")
            return
        }

        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                val user = getUserUseCase(uid)
                if (user == null) {
                    // Build a basic user from FirebaseAuth if Firestore record is missing
                    val firebaseUser = firebaseAuth.currentUser
                    val fallbackUser = com.uade.alltabs.domain.model.User(
                        uid = uid,
                        nombre = firebaseUser?.displayName ?: "Usuario",
                        email = firebaseUser?.email ?: "",
                        fotoUrl = firebaseUser?.photoUrl?.toString() ?: ""
                    )
                    collectTabCount(uid, fallbackUser)
                } else {
                    collectTabCount(uid, user)
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.localizedMessage ?: "Error al cargar perfil")
            }
        }
    }

    private fun collectTabCount(uid: String, user: User) {
        viewModelScope.launch {
            getTabsByUserIdUseCase(uid).collect { tabs ->
                _tabCount.value = tabs.size
                _uiState.value = ProfileUiState.Success(
                    user = user,
                    tabCount = tabs.size
                )
            }
        }
    }

    fun logout() {
        signOutUseCase()
        _uiState.value = ProfileUiState.LoggedOut
    }
}
