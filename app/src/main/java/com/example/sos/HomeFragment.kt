package com.example.sos

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.Locale

class HomeFragment : Fragment() {

    private val holdTime = 3000L // 3 seconds in milliseconds
    private var isHeld = false
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragmentt
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // SOS Button
        val sosButton: Button = view.findViewById(R.id.button)
        locationTextView = view.findViewById(R.id.tv_location)

        // Settings Button
        val settingsButton: ImageButton = view.findViewById(R.id.button_settings)

        // Set up the hold-down functionality for the SOS button
        sosButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isHeld = true
                    handler.postDelayed({
                        if (isHeld) {
                            Toast.makeText(requireContext(), "SOS Triggered!", Toast.LENGTH_SHORT).show()
                        }
                    }, holdTime)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isHeld = false
                    handler.removeCallbacksAndMessages(null)
                    true
                }
                else -> false
            }
        }

        // Set onClickListener for settings button
        settingsButton.setOnClickListener {
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, SettingsFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }

        // Fetch and display current location
        fetchLocation()

        return view
    }

    private fun fetchLocation() {
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

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                getLocationAddress(location)
            } else {
                locationTextView.text = "Unable to fetch location"
            }
        }.addOnFailureListener {
            locationTextView.text = "Error fetching location"
        }
    }

    private fun getLocationAddress(location: Location) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val areaName = address.locality ?: address.subAdminArea ?: "Unknown Area"
                locationTextView.text = "You are in $areaName"
            } else {
                locationTextView.text = "Unable to determine location name"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            locationTextView.text = "Error fetching address"
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}
