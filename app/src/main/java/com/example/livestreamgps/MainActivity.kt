// MainActivity.kt
package com.example.livestreamgps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.livestreamgps.ui.theme.LiveStreamGPSTheme
import com.example.livestreamgps.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request landscape orientation
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        setContent {
            LiveStreamGPSTheme {
                val authViewModel: AuthViewModel = viewModel()
                val token by authViewModel.token.collectAsState()
                
                if (token == null) {
                    LoginScreen(authViewModel = authViewModel)
                } else {
                    MainScreen(authViewModel = authViewModel)
                }
            }
        }
    }
}