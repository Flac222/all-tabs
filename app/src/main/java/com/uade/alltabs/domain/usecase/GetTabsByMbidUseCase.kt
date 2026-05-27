package com.uade.alltabs.domain.usecase

import com.uade.alltabs.domain.repository.TabRepository
import com.uade.alltabs.domain.model.Tab
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetTabsByMbidUseCase @Inject constructor(
    private val repository: TabRepository
) {
    operator fun invoke(mbid: String, title: String? = null, artist: String? = null): Flow<List<Tab>> {
        return repository.getAllTabs().map { allTabs ->
            allTabs.filter { tab ->
                // Match by MBID OR by exact Title + Artist match (to consolidate versions)
                val mbidMatch = tab.mbid == mbid || tab.id == mbid
                val nameMatch = if (title != null && artist != null) {
                    tab.titulo.equals(title, ignoreCase = true) && 
                    tab.artista.equals(artist, ignoreCase = true)
                } else false
                
                mbidMatch || nameMatch
            }
        }
    }
}
