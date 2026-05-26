package com.uade.alltabs.domain.usecase

import com.uade.alltabs.domain.repository.TabRepository
import com.uade.alltabs.domain.model.User
import javax.inject.Inject

class SaveUserUseCase @Inject constructor(
    private val repository: TabRepository
) {
    suspend operator fun invoke(user: User) {
        repository.saveUser(user)
    }
}
