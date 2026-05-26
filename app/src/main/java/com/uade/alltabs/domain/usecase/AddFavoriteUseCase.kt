package com.uade.alltabs.domain.usecase

import com.uade.alltabs.domain.repository.TabRepository
import javax.inject.Inject

class AddFavoriteUseCase @Inject constructor(
    private val repository: TabRepository
) {
    suspend operator fun invoke(userId: String, tabId: String, titulo: String, artista: String) {
        repository.addFavorite(userId, tabId, titulo, artista)
    }
}
