// AuthViewModel.kt
package com.example.livestreamgps.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.livestreamgps.service.AuthService
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val authService = AuthService(application.applicationContext)
    
    val token: StateFlow<String?> = authService.token
    val lastError: StateFlow<String?> = authService.lastError
    
    suspend fun login(email: String, password: String): Boolean {
        return authService.login(email, password)
    }
    
    suspend fun register(email: String, password: String): Boolean {
        return authService.register(email, password)
    }
    
    fun logout() {
        viewModelScope.launch {
            authService.logout()
        }
    }
}