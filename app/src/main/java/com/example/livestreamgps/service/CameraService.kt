// CameraService.kt
package com.example.livestreamgps.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Base64
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CameraService(private val context: Context) {
    
    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap
    
    private val _fps = MutableStateFlow(0)
    val fps: StateFlow<Int> = _fps
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var lastFrameTime = System.currentTimeMillis()
    private var isStreaming = false
    
    var webSocketClient: WebSocketClient? = null
    var locationService: LocationService? = null
    
    @SuppressLint("UnsafeOptInUsageError")
    fun start(lifecycleOwner: LifecycleOwner) {
        isStreaming = true
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            
            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                processFrame(imageProxy)
            }
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    fun stop() {
        isStreaming = false
        cameraProvider?.unbindAll()
    }
    
    @SuppressLint("UnsafeOptInUsageError")
    private fun processFrame(imageProxy: ImageProxy) {
        if (!isStreaming) {
            imageProxy.close()
            return
        }
        
        val now = System.currentTimeMillis()
        val minInterval = 200L // ~5 FPS
        
        if (now - lastFrameTime < minInterval) {
            imageProxy.close()
            return
        }
        
        lastFrameTime = now
        
        // Convert ImageProxy to Bitmap
        val bitmap = imageProxyToBitmap(imageProxy)
        imageProxy.close()
        
        if (bitmap == null) return
        
        // Rotate for landscape
        val rotated = rotateBitmap(bitmap, 90f)
        _previewBitmap.value = rotated
        
        // Calculate FPS
        val fpsValue = (1000 / minInterval).toInt()
        _fps.value = fpsValue
        
        // Send to WebSocket
        sendFrame(rotated, now)
    }
    
    @SuppressLint("UnsafeOptInUsageError")
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val image = imageProxy.image ?: return null
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer
        
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        
        val nv21 = ByteArray(ySize + uSize + vSize)
        
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 80, out)
        val imageBytes = out.toByteArray()
        
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
    
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    private fun sendFrame(bitmap: Bitmap, timestamp: Long) {
        val ws = webSocketClient ?: return
        val loc = locationService ?: return
        
        if (!ws.isConnected.value) return
        
        // Convert to JPEG
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
        val jpegBytes = outputStream.toByteArray()
        val base64Image = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
        
        // Create JSON payload
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        
        val payload = JSONObject().apply {
            put("timestamp", dateFormat.format(Date(timestamp)))
            put("lat", loc.latitude.value)
            put("lon", loc.longitude.value)
            put("accuracy", loc.accuracy.value)
            put("image", base64Image)
        }
        
        ws.sendText(payload.toString())
    }