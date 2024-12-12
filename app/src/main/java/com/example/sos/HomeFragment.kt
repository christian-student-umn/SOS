package com.example.sos

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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

    private var currentLocation: String = "Unknown Location"  // Variabel untuk menyimpan lokasi

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val CHANNEL_ID = "sos_notifications"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // SOS Button
        val sosButton: Button = view.findViewById(R.id.button)
        locationTextView = view.findViewById(R.id.tv_location)

        // Settings Button
        val settingsButton: ImageButton = view.findViewById(R.id.button_settings)

        // Notification Button
        val notificationButton: ImageButton = view.findViewById(R.id.button_notification)

        // Set up the hold-down functionality for the SOS button
        sosButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isHeld = true
                    handler.postDelayed({
                        if (isHeld) {
                            Log.d("SOS", "Button held for 3 seconds") // Verifikasi bahwa tahanan berlangsung
                            showNotification() // Trigger notification
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

        // Navigate to NotificationFragment when the notification button is clicked
        notificationButton.setOnClickListener {
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, NotificationFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }

        // Navigate to SettingsFragment when the settings button is clicked
        settingsButton.setOnClickListener {
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, SettingsFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }

        // Fetch and display current location
        fetchLocation()

        // Create notification channel
        createNotificationChannel()

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
                getLocationAddress(location)  // Update alamat berdasarkan lokasi
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
                currentLocation = areaName  // Simpan lokasi untuk digunakan dalam notifikasi
                locationTextView.text = "You are in $areaName"
            } else {
                locationTextView.text = "Unable to determine location name"
                currentLocation = "Unknown Location"  // Set default jika alamat tidak ditemukan
            }
        } catch (e: Exception) {
            e.printStackTrace()
            locationTextView.text = "Error fetching address"
            currentLocation = "Error fetching location"
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "SOS Notifications"
            val descriptionText = "Notifications triggered by SOS button"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification() {
        val notificationText = "Help! I am at $currentLocation"  // Gabungkan "Help" dengan lokasi pengguna

        val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Ikon untuk notifikasi
            .setContentTitle("SOS Alert")
            .setContentText(notificationText)  // Tampilkan teks dengan lokasi
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Gunakan timestamp atau ID acak untuk memastikan ID unik
        val notificationId = System.currentTimeMillis().toInt()

        with(NotificationManagerCompat.from(requireContext())) {
            notify(notificationId, builder.build())
        }

        Toast.makeText(requireContext(), "SOS Triggered at $currentLocation!", Toast.LENGTH_SHORT).show()
    }
}