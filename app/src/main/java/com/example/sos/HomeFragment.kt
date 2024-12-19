package com.example.sos

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.media.MediaRecorder
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
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private val recordDuration = 5000L // 5 seconds for holding the button
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationTextView: TextView

    private var currentLocation: String = "Unknown Location"

    private var mediaRecorder: MediaRecorder? = null
    private lateinit var audioFile: File
    private val firebaseStorage = FirebaseStorage.getInstance()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val CHANNEL_ID = "sos_notifications"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        locationTextView = view.findViewById(R.id.tv_location)

        val sosButton: Button = view.findViewById(R.id.button)
        val settingsButton: ImageButton = view.findViewById(R.id.button_settings)
        val notificationButton: ImageButton = view.findViewById(R.id.button_notification)

        checkAndRequestPermissions()

        // Set OnTouchListener for the SOS button to handle long press
        sosButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (checkPermissions()) {
                        startRecording()
                        Handler(Looper.getMainLooper()).postDelayed({
                            stopRecording()
                            uploadAudioToFirebase()
                            showNotification()
                            storeFirebaseToken()
                        }, recordDuration)
                    } else {
                        Toast.makeText(requireContext(), "Please grant required permissions", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    stopRecording()
                    true
                }
                else -> false
            }
        }

        notificationButton.setOnClickListener {
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            val notificationFragment = NotificationFragment()
            val bundle = Bundle().apply {
                putString("audioUri", audioFile.absolutePath)
            }
            notificationFragment.arguments = bundle
            transaction.replace(R.id.fragment_container, notificationFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        settingsButton.setOnClickListener {
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, SettingsFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }

        fetchLocation()
        createNotificationChannel()
        return view
    }

    private fun checkAndRequestPermissions() {
        if (!checkPermissions()) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ), LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun checkPermissions(): Boolean {
        val audioPermission = ActivityCompat.checkSelfPermission(
            requireContext(), Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val locationPermission = ActivityCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return audioPermission && locationPermission
    }

    private fun startRecording() {
        val cacheDir = requireContext().externalCacheDir
        if (cacheDir == null) {
            Log.e("SOS", "External cache directory is null")
            Toast.makeText(requireContext(), "Failed to access storage", Toast.LENGTH_SHORT).show()
            return
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        audioFile = File(cacheDir, "SOS_$timestamp.3gp")

        try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setOutputFile(audioFile.absolutePath)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                prepare()
                start()
            }
            Log.d("SOS", "Recording started: ${audioFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("SOS", "Error starting recording", e)
            Toast.makeText(requireContext(), "Recording failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            Log.d("SOS", "Recording stopped")
        } catch (e: IllegalStateException) {
            Log.e("SOS", "Error stopping MediaRecorder", e)
        }
    }

    private fun uploadAudioToFirebase() {
        val storageRef = firebaseStorage.reference
        val audioUri = audioFile.toUri()
        val audioRef = storageRef.child("audio/${audioFile.name}")

        audioRef.putFile(audioUri)
            .addOnSuccessListener {
                audioRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    saveAudioMetadata(downloadUri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to upload audio", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveAudioMetadata(downloadUri: String) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("users/${FirebaseAuth.getInstance().currentUser?.uid}/audios")
        val audioId = databaseRef.push().key ?: return

        val audioMetadata = mapOf(
            "id" to audioId,
            "uri" to downloadUri,
            "timestamp" to System.currentTimeMillis()
        )

        databaseRef.child(audioId).setValue(audioMetadata)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Audio saved successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to save audio metadata", Toast.LENGTH_SHORT).show()
            }
    }


    private fun fetchLocation() {
        if (!checkPermissions()) return

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
                currentLocation = areaName
                locationTextView.text = "You are in $areaName"
            } else {
                locationTextView.text = "Unable to determine location name"
            }
        } catch (e: Exception) {
            Log.e("SOS", "Error fetching address", e)
            locationTextView.text = "Error fetching address"
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
        val notificationText = "Help! I need assistance!"
        val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("SOS Alert")
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(requireContext())) {
            notify(1, builder.build())
        }
    }

    private fun storeFirebaseToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("SOS", "Firebase Token: $token")
                // Save token to your database or use it as needed
            }
        }
    }
}
