package com.uade.alltabs.presentation.tabdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.usecase.AddFavoriteUseCase
import com.uade.alltabs.domain.usecase.GetTabUseCase
import com.uade.alltabs.domain.usecase.IsTabFavoritedUseCase
import com.uade.alltabs.domain.usecase.RemoveFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TabDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val firebaseAuth: FirebaseAuth,
    private val getTabUseCase: GetTabUseCase,
    private val addFavoriteUseCase: AddFavoriteUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase,
    private val isTabFavoritedUseCase: IsTabFavoritedUseCase
) : ViewModel() {

    private val _tab = MutableStateFlow<Tab?>(null)
    val tab: StateFlow<Tab?> = _tab.asStateFlow()

    private val _isFavorited = MutableStateFlow(false)
    val isFavorited: StateFlow<Boolean> = _isFavorited.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        savedStateHandle.get<String>("tabId")?.let { tabId ->
            loadTabDetail(tabId)
        }
    }

    private fun loadTabDetail(tabId: String) {
        viewModelScope.launch {
            try {
                val fetchedTab = getTabUseCase(tabId)
                _tab.value = fetchedTab
                val userId = firebaseAuth.currentUser?.uid
                if (userId != null && fetchedTab != null) {
                    _isFavorited.value = isTabFavoritedUseCase(userId, tabId)
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Error loading tab detail."
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val currentTab = _tab.value ?: return@launch
            val userId = firebaseAuth.currentUser?.uid ?: run {
                _error.value = "User not logged in."
                return@launch
            }

            try {
                if (_isFavorited.value) {
                    removeFavoriteUseCase(userId, currentTab.id)
                } else {
                    addFavoriteUseCase(userId, currentTab.id, currentTab.titulo, currentTab.artista)
                }
                _isFavorited.value = !_isFavorited.value
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Error toggling favorite status."
            }
        }
    }

    fun consumeError() {
        _error.value = null
    }
}
