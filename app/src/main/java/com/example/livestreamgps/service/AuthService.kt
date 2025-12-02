// AuthService.kt
package com.example.livestreamgps.service

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AuthService(private val context: Context) {
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    
    private val _token = MutableStateFlow<String?>(prefs.getString("auth_token", null))
    val token: StateFlow<String?> = _token
    
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private fun getBaseURL(): String {
        val wsURL = ServerConfig.getWsURL(context)
        var url = wsURL.replace("/ws", "")
        
        // Convert ws → http and wss → https
        url = when {
            url.startsWith("wss://") -> url.replace("wss://", "https://")
            url.startsWith("ws://") -> url.replace("ws://", "http://")
            else -> url
        }
        
        return url
    }
    
    suspend fun login(email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseURL()}/login"
            val json = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            
            val requestBody = json.toString()
                .toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                _lastError.value = response.body?.string() ?: "Login failed"
                return@withContext false
            }
            
            val responseBody = response.body?.string()
            val responseJson = JSONObject(responseBody ?: "{}")
            val token = responseJson.optString("token", null)
            
            if (token != null) {
                saveToken(token)
                _token.value = token
                _lastError.value = null
                return@withContext true
            }
            
            false
        } catch (e: Exception) {
            _lastError.value = e.localizedMessage
            false
        }
    }
    
    suspend fun register(email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseURL()}/register"
            val json = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            
            val requestBody = json.toString()
                .toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                _lastError.value = "Registration failed"
                return@withContext false
            }
            
            true
        } catch (e: Exception) {
            _lastError.value = e.localizedMessage
            false
        }
    }
    
    fun logout() {
        clearToken()
        _token.value = null
    }
    
    private fun saveToken(token: String) {
        prefs.edit().putString("auth_token", token).apply()
    }
    
    private fun clearToken() {
        prefs.edit().remove("auth_token").apply()
    }
}

object ServerConfig {
    private const val PREFS_NAME = "server_config"
    private const val KEY_WS_URL = "ws_url"
    private const val DEFAULT_URL = "wss://4928ba6f960f.ngrok-free.app/ws"
    
    fun getWsURL(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_WS_URL, DEFAULT_URL) ?: DEFAULT_URL
    }
    
    fun setWsURL(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_WS_URL, url).apply()
    }
}