// MainScreen.kt
package com.example.livestreamgps

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.livestreamgps.service.CameraService
import com.example.livestreamgps.service.LocationService
import com.example.livestreamgps.service.ServerConfig
import com.example.livestreamgps.service.WebSocketClient
import com.example.livestreamgps.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(authViewModel: AuthViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    val cameraService = remember { CameraService(context) }
    val locationService = remember { LocationService(context) }
    val wsClient = remember { WebSocketClient(context) }
    
    val previewBitmap by cameraService.previewBitmap.collectAsState()
    val fps by cameraService.fps.collectAsState()
    val latitude by locationService.latitude.collectAsState()
    val longitude by locationService.longitude.collectAsState()
    val accuracy by locationService.accuracy.collectAsState()
    val isConnected by wsClient.isConnected.collectAsState()
    val logs by wsClient.logs.collectAsState()
    
    var isRecording by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }
    var showEditURL by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            locationService.start()
        }
    }
    
    // Check and request permissions
    LaunchedEffect(Unit) {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        
        if (allGranted) {
            locationService.start()
        } else {
            permissionLauncher.launch(permissions)
        }
    }
    
    // Setup camera and websocket references
    LaunchedEffect(Unit) {
        cameraService.webSocketClient = wsClient
        cameraService.locationService = locationService
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        if (previewBitmap != null) {
            Image(
                bitmap = previewBitmap!!.asImageBitmap(),
                contentDescription = "Camera Preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text("Waiting for camera…", color = Color.White)
            }
        }
        
        // Top status bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, start = 14.dp, end = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Status info
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GPS ±%.1f m".format(accuracy),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
                Divider(
                    modifier = Modifier
                        .width(1.dp)
                        .height(16.dp),
                    color = Color.White.copy(alpha = 0.5f)
                )
                Text(
                    text = "FPS $fps",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
                Divider(
                    modifier = Modifier
                        .width(1.dp)
                        .height(16.dp),
                    color = Color.White.copy(alpha = 0.5f)
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            if (isConnected) Color.Green else Color.Red,
                            CircleShape
                        )
                )
            }
            
            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { showEditURL = true },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Edit URL",
                        tint = Color.White
                    )
                }
                
                IconButton(
                    onClick = { showLogs = true },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Logs",
                        tint = Color.White
                    )
                }
            }
        }
        
        // Right side buttons
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            // Menu button
            FloatingActionButton(
                onClick = { showMenu = true },
                containerColor = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
            
            // Record button
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        if (isRecording) {
                            cameraService.stop()
                            wsClient.disconnect()
                        } else {
                            wsClient.connect()
                            delay(400)
                            cameraService.start(lifecycleOwner)
                        }
                        isRecording = !isRecording
                    }
                },
                containerColor = if (isRecording) Color.Red else Color.Green,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                    contentDescription = if (isRecording) "Stop" else "Record",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        
        // Logs drawer
        if (showLogs) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.4f)
                    .background(Color.Black.copy(alpha = 0.9f))
            ) {
                Column {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Logs",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(onClick = { showLogs = false }) {
                            Text("Close", color = Color.White)
                        }
                    }
                    
                    // Logs list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(logs) { log ->
                            Text(
                                text = log,
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Menu dialog
    if (showMenu) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            title = { Text("Menu") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showMenu = false
                            showEditURL = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Server URL")
                        }
                    }
                    
                    TextButton(
                        onClick = {
                            showMenu = false
                            showLogs = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View Logs")
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    TextButton(
                        onClick = {
                            authViewModel.logout()
                            showMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ExitToApp,
                                contentDescription = null,
                                tint = Color.Red
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Out", color = Color.Red)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMenu = false }) {
                    Text("Close")
                }
            }
        )
    }
    
    // Edit URL dialog
    if (showEditURL) {
        EditURLDialog(onDismiss = { showEditURL = false })
    }
}
