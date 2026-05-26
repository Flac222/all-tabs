package com.uade.alltabs.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.uade.alltabs.domain.repository.TabRepository
import com.uade.alltabs.domain.model.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val repository: TabRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(credential).await()
                _authState.value = AuthState.Success

                // Save user to Firestore if new
                authResult.user?.let { firebaseUser ->
                    if (firebaseUser.metadata?.creationTimestamp == firebaseUser.metadata?.lastSignInTimestamp) {
                        val user = User(firebaseUser.uid, firebaseUser.displayName, firebaseUser.email, firebaseUser.photoUrl.toString())
                        repository.saveUser(user)
                    }
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "An unknown error occurred")
            }
        }
    }

    fun setError(message: String) {
        _authState.value = AuthState.Error(message)
    }
}
