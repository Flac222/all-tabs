package com.uade.alltabs.domain.usecase

import com.uade.alltabs.domain.repository.TabRepository
import com.uade.alltabs.domain.model.Tab
import javax.inject.Inject

class SaveTabUseCase @Inject constructor(
    private val repository: TabRepository
) {
    suspend operator fun invoke(tab: Tab) {
        repository.saveTab(tab)
    }
}
