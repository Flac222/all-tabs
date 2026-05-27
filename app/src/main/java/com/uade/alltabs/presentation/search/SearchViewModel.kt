package com.uade.alltabs.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.repository.TabRepository
import com.uade.alltabs.domain.usecase.FetchTabsUseCase
import com.uade.alltabs.domain.usecase.GetUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val results: List<SongSearchResult>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val fetchTabsUseCase: FetchTabsUseCase,
    tabRepository: TabRepository,
    private val getUserUseCase: GetUserUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _apiResults = MutableStateFlow<List<Tab>>(emptyList())
    private val _isSearching = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SearchUiState> = combine(
        _apiResults,
        tabRepository.getAllTabs(),
        _isSearching,
        _error,
        _searchQuery
    ) { apiResults, allGlobalTabs, isSearching, error, query ->
        when {
            error != null -> SearchUiState.Error(error)
            isSearching -> SearchUiState.Loading
            query.isEmpty() && apiResults.isEmpty() -> SearchUiState.Idle
            else -> {
                val results = mutableListOf<SongSearchResult>()
                val resultsMap = mutableMapOf<String, SongSearchResult>()

                // 1. Process API results and group by Title + Artist
                apiResults.forEach { apiTab ->
                    val key = "${apiTab.titulo.lowercase().trim()}|${apiTab.artista.lowercase().trim()}"
                    val mbid = apiTab.mbid ?: return@forEach
                    
                    if (!resultsMap.containsKey(key)) {
                        // Aggregate tabs from Firestore for this specific song name/artist
                        val matchingTabs = allGlobalTabs.filter { 
                            it.titulo.equals(apiTab.titulo, ignoreCase = true) && 
                            it.artista.equals(apiTab.artista, ignoreCase = true) 
                        }
                        
                        resultsMap[key] = SongSearchResult(
                            mbid = mbid,
                            titulo = apiTab.titulo,
                            artista = apiTab.artista,
                            tabCount = matchingTabs.size,
                            tabCreators = matchingTabs.map { it.userId to it.userName }
                        )
                    }
                }
                
                results.addAll(resultsMap.values)

                // 2. Add custom tabs from Firestore that don't match any API result
                allGlobalTabs.forEach { customTab ->
                    val key = "${customTab.titulo.lowercase().trim()}|${customTab.artista.lowercase().trim()}"
                    
                    if (!resultsMap.containsKey(key)) {
                        if (customTab.titulo.contains(query, ignoreCase = true) ||
                            customTab.artista.contains(query, ignoreCase = true)) {
                            
                            val sameSongTabs = allGlobalTabs.filter {
                                it.titulo.equals(customTab.titulo, ignoreCase = true) &&
                                it.artista.equals(customTab.artista, ignoreCase = true)
                            }
                            
                            val searchResult = SongSearchResult(
                                mbid = customTab.mbid ?: customTab.id,
                                titulo = customTab.titulo,
                                artista = customTab.artista,
                                tabCount = sameSongTabs.size,
                                tabCreators = sameSongTabs.map { it.userId to it.userName }
                            )
                            resultsMap[key] = searchResult
                            results.add(searchResult)
                        }
                    }
                }
                SearchUiState.Success(results)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState.Idle
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isEmpty()) {
            _apiResults.value = emptyList()
            _error.value = null
        }
    }

    fun search() {
        val query = _searchQuery.value
        if (query.isBlank()) return

        viewModelScope.launch {
            _isSearching.value = true
            _error.value = null
            try {
                val results = fetchTabsUseCase(query)
                _apiResults.value = results
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Unknown error occurred"
            } finally {
                _isSearching.value = false
            }
        }
    }
}
