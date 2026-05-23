package com.uade.alltabs.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.usecase.FetchTabsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HomeUiState {
    object Idle : HomeUiState()
    object Loading : HomeUiState()
    data class Success(val tabs: List<Tab>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val fetchTabsUseCase: FetchTabsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun fetchTabs(query: String) {
        if (query.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val tabs = fetchTabsUseCase(query)
                _uiState.value = HomeUiState.Success(tabs)
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }
}
