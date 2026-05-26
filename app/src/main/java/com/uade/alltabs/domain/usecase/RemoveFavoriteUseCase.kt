package com.uade.alltabs.domain.usecase

import com.uade.alltabs.domain.repository.TabRepository
import javax.inject.Inject

class RemoveFavoriteUseCase @Inject constructor(
    private val repository: TabRepository
) {
    suspend operator fun invoke(userId: String, tabId: String) {
        repository.removeFavorite(userId, tabId)
    }
}
