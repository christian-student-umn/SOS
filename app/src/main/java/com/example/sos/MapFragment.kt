package com.example.sos

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var userMarker: Marker? = null

    // Dummy contact data (replace with actual dynamic data)
    private val contacts = listOf(
        Contact("John Doe", "1234567890", "john@example.com", LatLng(37.7749, -122.4194)), // San Francisco
        Contact("Jane Smith", "9876543210", "jane@example.com", LatLng(34.0522, -118.2437)) // Los Angeles
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        setupMap()
    }

    private fun setupMap() {
        // Check location permissions
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        googleMap.isMyLocationEnabled = true

        // Real-time user location updates
        startLocationUpdates()

        // Add markers for contacts
        contacts.forEach { contact ->
            googleMap.addMarker(
                MarkerOptions()
                    .position(contact.location)
                    .title(contact.name)
                    .snippet(contact.phone)
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000 // 10 seconds
            fastestInterval = 5000 // 5 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // Callback for location changes
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val location = locationResult.lastLocation
                updateUserMarker(location)
            }
        }

        // Request location updates
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            requireActivity().mainLooper
        )
    }

    private fun updateUserMarker(location: Location?) {
        location?.let {
            val userLatLng = LatLng(it.latitude, it.longitude)

            // If marker already exists, update its position
            if (userMarker == null) {
                userMarker = googleMap.addMarker(
                    MarkerOptions().position(userLatLng).title("You are here")
                )
            } else {
                userMarker?.position = userLatLng
            }

            // Move camera to user's location
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 12f))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop location updates to save battery
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    data class Contact(val name: String, val phone: String, val email: String, val location: LatLng)
}
