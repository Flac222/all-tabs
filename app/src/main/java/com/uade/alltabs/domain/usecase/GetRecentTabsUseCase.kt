package com.uade.alltabs.domain.usecase

import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.repository.TabRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetRecentTabsUseCase @Inject constructor(
    private val tabRepository: TabRepository
) {
    operator fun invoke(): Flow<List<Tab>> {
        return tabRepository.getAllTabs().map { tabs ->
            tabs.sortedByDescending { it.fechaCreacion }
        }
    }
}
