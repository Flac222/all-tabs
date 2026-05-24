package com.uade.alltabs.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.usecase.GetRecentTabsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val recentTabs: List<Tab>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    getRecentTabsUseCase: GetRecentTabsUseCase
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = getRecentTabsUseCase()
        .map<List<Tab>, HomeUiState> { tabs -> 
            HomeUiState.Success(tabs.take(5))
        }
        .catch { e -> 
            emit(HomeUiState.Error(e.localizedMessage ?: "Unknown error"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState.Loading
        )
}
