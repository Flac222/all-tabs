package com.uade.alltabs.domain.usecase

import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class SignOutUseCase @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    operator fun invoke() {
        firebaseAuth.signOut()
    }
}
