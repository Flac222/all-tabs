package com.uade.alltabs.domain.usecase

import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.repository.TabRepository
import javax.inject.Inject

class FetchTabsUseCase @Inject constructor(
    private val tabRepository: TabRepository
) {
    suspend operator fun invoke(query: String): List<Tab> {
        return tabRepository.fetchTabsFromApi(query)
    }
}
