// WebSocketClient.kt
package com.example.livestreamgps.service

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class WebSocketClient(private val context: Context) {
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected
    
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs
    
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError
    
    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 8
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .build()
    
    fun connect() {
        // Get token from shared preferences
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)
        
        if (token.isNullOrEmpty()) {
            appendLog("🔒 No auth token found — connect skipped. Please login first.")
            return
        }
        
        val urlString = ServerConfig.getWsURL(context).trim()
        val connector = if (urlString.contains("?")) {
            "$urlString&token=$token"
        } else {
            "$urlString?token=$token"
        }
        
        appendLog("Connecting → $connector")
        
        val request = Request.Builder()
            .url(connector)
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _isConnected.value = true
                reconnectAttempts = 0
                appendLog("✓ Connected")
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                appendLog("Recv text (${text.length} chars)")
            }
            
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                appendLog("Recv data (${bytes.size} bytes)")
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                appendLog("Closing: $reason")
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _isConnected.value = false
                appendLog("Closed: $reason")
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _isConnected.value = false
                appendLog("WS failure: ${t.localizedMessage}")
                scheduleReconnect()
            }
        })
    }
    
    fun disconnect() {
        appendLog("Disconnecting")
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        _isConnected.value = false
    }
    
    private fun scheduleReconnect() {
        if (reconnectAttempts >= maxReconnectAttempts) {
            appendLog("Max reconnect attempts reached")
            return
        }
        
        reconnectAttempts++
        val delay = minOf(Math.pow(2.0, reconnectAttempts.toDouble()).toLong(), 60)
        appendLog("Reconnect in ${delay}s (attempt $reconnectAttempts)")
        
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(delay * 1000)
            appendLog("Reconnecting (attempt $reconnectAttempts)")
            connect()
        }
    }
    
    fun sendText(text: String) {
        if (!_isConnected.value) {
            appendLog("Skip text send (not connected)")
            return
        }
        
        val success = webSocket?.send(text) ?: false
        if (success) {
            appendLog("Sent text (${text.length} chars)")
        } else {
            appendLog("Text send failed")
        }
    }
    
    fun sendData(data: ByteArray) {
        if (!_isConnected.value) {
            appendLog("Send skipped (not connected)")
            return
        }
        
        val success = webSocket?.send(ByteString.of(*data)) ?: false
        if (success) {
            appendLog("Sent ${data.size} bytes")
        } else {
            appendLog("Send failed")
        }
    }
    
    private fun appendLog(text: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val entry = "[$timestamp] $text"
        
        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(0, entry)
        if (currentLogs.size > 300) {
            currentLogs.removeAt(currentLogs.size - 1)
        }
        _logs.value = currentLogs
    }
}