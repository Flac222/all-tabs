package com.uade.alltabs.domain.usecase

import com.uade.alltabs.domain.repository.TabRepository
import com.uade.alltabs.domain.model.Tab
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFavoriteTabsUseCase @Inject constructor(
    private val repository: TabRepository
) {
    operator fun invoke(userId: String): Flow<List<Tab>> {
        return repository.getFavoriteTabs(userId)
    }
}
