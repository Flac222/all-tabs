package com.uade.alltabs.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.uade.alltabs.core.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _startDestination = MutableStateFlow("")
    val startDestination: StateFlow<String> = _startDestination.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            // Check if there is a currently signed-in user
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                _startDestination.value = Screen.Home.route
            } else {
                _startDestination.value = Screen.Login.route
            }
            // Once the check is complete, we are no longer loading
            _isLoading.value = false
        }
    }
}
