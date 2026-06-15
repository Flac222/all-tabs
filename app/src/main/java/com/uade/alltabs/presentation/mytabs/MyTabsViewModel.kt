package com.uade.alltabs.presentation.mytabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.usecase.GetFavoriteTabsUseCase
import com.uade.alltabs.domain.usecase.GetTabsByUserIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MyTabsUiState {
    object Loading : MyTabsUiState()
    data class Success(val tabs: List<Tab>) : MyTabsUiState()
    data class Error(val message: String) : MyTabsUiState()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MyTabsViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val getTabsByUserIdUseCase: GetTabsByUserIdUseCase,
    private val getFavoriteTabsUseCase: GetFavoriteTabsUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _myTabs = MutableStateFlow<List<Tab>>(emptyList())
    private val _favoriteTabs = MutableStateFlow<List<Tab>>(emptyList())

    val uiState: StateFlow<MyTabsUiState> = combine(
        _myTabs,
        _favoriteTabs,
        _searchQuery
    ) { myTabs, favoriteTabs, query ->
        // Combine both lists and ensure no duplicate entries (in case user favorited their own tab)
        val allTabs = (myTabs + favoriteTabs).distinctBy { it.id }
        val filteredTabs = if (query.isBlank()) {
            allTabs
        } else {
            allTabs.filter { 
                it.titulo.contains(query, ignoreCase = true) || 
                it.artista.contains(query, ignoreCase = true) 
            }
        }
        MyTabsUiState.Success(filteredTabs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MyTabsUiState.Loading
    )

    init {
        loadMyTabs()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    private fun loadMyTabs() {
        val userId = firebaseAuth.currentUser?.uid

        if (userId != null) {
            viewModelScope.launch {
                getTabsByUserIdUseCase(userId).collect { userTabs ->
                    _myTabs.value = userTabs
                }
            }
            viewModelScope.launch {
                getFavoriteTabsUseCase(userId).collect { favorites ->
                    _favoriteTabs.value = favorites
                }
            }
        }
    }
}
