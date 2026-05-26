package com.uade.alltabs.domain.usecase

import com.uade.alltabs.domain.repository.TabRepository
import com.uade.alltabs.domain.model.Tab
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetTabsByMbidUseCase @Inject constructor(
    private val repository: TabRepository
) {
    operator fun invoke(mbid: String): Flow<List<Tab>> {
        return repository.getAllTabs().map { allTabs ->
            allTabs.filter { it.mbid == mbid || it.id == mbid }
        }
    }
}