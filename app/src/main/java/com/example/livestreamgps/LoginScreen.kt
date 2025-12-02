// LoginScreen.kt
// Path: app/src/main/java/com/example/livestreamgps/LoginScreen.kt

package com.example.livestreamgps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.livestreamgps.service.ServerConfig
import com.example.livestreamgps.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(authViewModel: AuthViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var showRegister by remember { mutableStateOf(false) }
    var showEditURL by remember { mutableStateOf(false) }
    
    val lastError by authViewModel.lastError.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = "LiveStreamGPS Login",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                modifier = Modifier.padding(bottom = 40.dp)
            )
            
            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.Gray
                ),
                singleLine = true
            )
            
            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.Gray
                ),
                singleLine = true
            )
            
            // Error message
            if (lastError != null) {
                Text(
                    text = lastError ?: "",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Login button
            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        authViewModel.login(email, password)
                        loading = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                enabled = email.isNotEmpty() && password.isNotEmpty() && !loading
            ) {
                Text(if (loading) "Logging in..." else "Login")
            }
            
            // Register button
            TextButton(
                onClick = { showRegister = true },
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text("Register Instead", color = Color.White)
            }
            
            // Edit URL button
            TextButton(
                onClick = { showEditURL = true },
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text("Edit Server URL", color = Color.Yellow)
            }
        }
    }
    
    // Register dialog
    if (showRegister) {
        RegisterDialog(
            authViewModel = authViewModel,
            onDismiss = { showRegister = false }
        )
    }
    
    // Edit URL dialog
    if (showEditURL) {
        EditURLDialog(
            onDismiss = { showEditURL = false }
        )
    }
}

@Composable
fun RegisterDialog(
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    
    val lastError by authViewModel.lastError.collectAsState()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Account") },
        text = {
            Column {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    singleLine = true
                )
                
                if (lastError != null) {
                    Text(
                        text = lastError ?: "",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        val success = authViewModel.register(email, password)
                        if (success) {
                            authViewModel.login(email, password)
                            onDismiss()
                        }
                        loading = false
                    }
                },
                enabled = !loading && email.isNotEmpty() && password.isNotEmpty()
            ) {
                Text(if (loading) "Registering..." else "Register")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditURLDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var url by remember { mutableStateOf(ServerConfig.getWsURL(context)) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("WebSocket Server URL") },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("wss://your-ngrok-url/ws") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    ServerConfig.setWsURL(context, url.trim())
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}