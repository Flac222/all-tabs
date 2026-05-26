package com.uade.alltabs.domain.usecase

import com.uade.alltabs.domain.repository.TabRepository
import javax.inject.Inject

class IsTabFavoritedUseCase @Inject constructor(
    private val repository: TabRepository
) {
    suspend operator fun invoke(userId: String, tabId: String): Boolean {
        return repository.isTabFavorited(userId, tabId)
    }
}
