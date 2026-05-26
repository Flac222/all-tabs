package com.uade.alltabs.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.repository.TabRepository
import com.uade.alltabs.domain.usecase.FetchTabsUseCase
import com.uade.alltabs.domain.usecase.GetUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val results: List<SongSearchResult>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val fetchTabsUseCase: FetchTabsUseCase,
    private val tabRepository: TabRepository,
    private val getUserUseCase: GetUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun search() {
        val query = _searchQuery.value
        if (query.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                val apiResults = fetchTabsUseCase(query) // These are MusicBrainz results
                
                // 1. Get ALL global tabs from Firestore to check for matches
                val allGlobalTabs = tabRepository.getAllTabs().first()

                // 2. Prepare the list of results
                val results = mutableListOf<SongSearchResult>()

                // 3. Process API results and de-duplicate by MBID
                val processedMbids = mutableSetOf<String>()
                
                apiResults.forEach { apiTab ->
                    val mbid = apiTab.mbid ?: return@forEach
                    if (processedMbids.contains(mbid)) return@forEach
                    
                    processedMbids.add(mbid)
                    
                    // Find tabs in Firestore that match this MBID
                    val matchingTabs = allGlobalTabs.filter { it.mbid == mbid }
                    
                    results.add(
                        SongSearchResult(
                            mbid = mbid,
                            titulo = apiTab.titulo,
                            artista = apiTab.artista,
                            tabCount = matchingTabs.size,
                            tabCreators = matchingTabs.map { it.userId to it.userName }
                        )
                    )
                }
                
                // 4. Also add custom tabs from Firestore that match the search but aren't in MusicBrainz
                allGlobalTabs.forEach { customTab ->
                    // If it has MBID and we already processed it, skip
                    if (customTab.mbid != null && processedMbids.contains(customTab.mbid)) return@forEach
                    
                    // Simple text match if it matches query and wasn't found in API
                    if (customTab.titulo.contains(query, ignoreCase = true) || 
                        customTab.artista.contains(query, ignoreCase = true)) {
                        
                        // Check if we already have this exact song (by title/artist) to avoid duplicates
                        val alreadyAdded = results.any { 
                            it.titulo.equals(customTab.titulo, ignoreCase = true) && 
                            it.artista.equals(customTab.artista, ignoreCase = true) 
                        }
                        
                        if (!alreadyAdded) {
                            results.add(
                                SongSearchResult(
                                    mbid = customTab.mbid ?: customTab.id, // Fallback to id if no mbid
                                    titulo = customTab.titulo,
                                    artista = customTab.artista,
                                    tabCount = allGlobalTabs.count { 
                                        it.titulo.equals(customTab.titulo, ignoreCase = true) && 
                                        it.artista.equals(customTab.artista, ignoreCase = true) 
                                    },
                                    tabCreators = allGlobalTabs.filter { 
                                        it.titulo.equals(customTab.titulo, ignoreCase = true) && 
                                        it.artista.equals(customTab.artista, ignoreCase = true) 
                                    }.map { it.userId to it.userName }
                                )
                            )
                        }
                    }
                }

                _uiState.value = SearchUiState.Success(results)
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }
}
