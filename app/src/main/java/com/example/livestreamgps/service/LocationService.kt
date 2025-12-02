// LocationService.kt
package com.example.livestreamgps.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocationService(private val context: Context) {
    
    private val _latitude = MutableStateFlow(0.0)
    val latitude: StateFlow<Double> = _latitude
    
    private val _longitude = MutableStateFlow(0.0)
    val longitude: StateFlow<Double> = _longitude
    
    private val _accuracy = MutableStateFlow(0.0)
    val accuracy: StateFlow<Double> = _accuracy
    
    private val _authorized = MutableStateFlow(false)
    val authorized: StateFlow<Boolean> = _authorized
    
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        1000L // Update every second
    ).apply {
        setMinUpdateIntervalMillis(500L)
        setMaxUpdateDelayMillis(2000L)
    }.build()
    
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                updateLocation(location)
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    fun start() {
        _authorized.value = true
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }
    
    fun stop() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    
    private fun updateLocation(location: Location) {
        _latitude.value = location.latitude
        _longitude.value = location.longitude
        _accuracy.value = if (location.hasAccuracy()) {
            location.accuracy.toDouble()
        } else {
            0.0
        }
    }
}