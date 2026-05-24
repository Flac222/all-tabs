package com.uade.alltabs.presentation.mytabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.usecase.GetRecentTabsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MyTabsUiState {
    object Loading : MyTabsUiState()
    data class Success(val tabs: List<Tab>) : MyTabsUiState()
    data class Error(val message: String) : MyTabsUiState()
}

@HiltViewModel
class MyTabsViewModel @Inject constructor(
    private val getRecentTabsUseCase: GetRecentTabsUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<MyTabsUiState>(MyTabsUiState.Loading)
    val uiState: StateFlow<MyTabsUiState> = combine(
        getRecentTabsUseCase(),
        _searchQuery
    ) { tabs, query ->
        if (query.isBlank()) {
            MyTabsUiState.Success(tabs)
        } else {
            val filteredTabs = tabs.filter {
                it.title.contains(query, ignoreCase = true) || 
                it.artist.contains(query, ignoreCase = true)
            }
            MyTabsUiState.Success(filteredTabs)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MyTabsUiState.Loading
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
}
