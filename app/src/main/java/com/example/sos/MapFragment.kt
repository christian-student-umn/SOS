package com.example.sos

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import com.bumptech.glide.request.transition.Transition
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore

class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var firestore: FirebaseFirestore

    private var userMarker: Marker? = null

    // Dummy contact data
    private val contacts = listOf(
        Contact("John Doe", "1234567890", "john@example.com", LatLng(37.7749, -122.4194), ""),
        Contact("Jane Smith", "9876543210", "jane@example.com", LatLng(34.0522, -118.2437), "")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        firestore = FirebaseFirestore.getInstance()
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
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        googleMap.isMyLocationEnabled = true
        startLocationUpdates()

        // Tambahkan marker untuk kontak
        contacts.forEach { contact ->
            Glide.with(this)
                .asBitmap()
                .load(contact.profilePictureUrl)
                .circleCrop()
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        val marker = googleMap.addMarker(
                            MarkerOptions()
                                .position(contact.location)
                                .title(contact.name)
                                .snippet(contact.phone)
                                .icon(BitmapDescriptorFactory.fromBitmap(resource))
                        )
                        marker?.tag = contact
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val location = locationResult.lastLocation
                updateUserMarker(location)
                location?.let {
                    saveLocationToFirestore(it)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, requireActivity().mainLooper)
    }

    private fun updateUserMarker(location: Location?) {
        location?.let {
            val userLatLng = LatLng(it.latitude, it.longitude)

            if (userMarker == null) {
                userMarker = googleMap.addMarker(
                    MarkerOptions().position(userLatLng).title("You are here")
                )
            } else {
                userMarker?.position = userLatLng
            }

            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 12f))
        }
    }

    private fun saveLocationToFirestore(location: Location) {
        val locationData = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("user_locations")
            .add(locationData)
            .addOnSuccessListener {
                println("Location saved successfully")
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    data class Contact(
        val name: String,
        val phone: String,
        val email: String,
        val location: LatLng,
        val profilePictureUrl: String
    )
}
