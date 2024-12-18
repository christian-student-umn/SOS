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

        // Request necessary permissions
        checkAndRequestPermissions()

        // Set OnTouchListener for the SOS button to handle long press
        sosButton.setOnTouchListener { v, event ->
            when (event.action) {
                // When button is pressed down, start recording
                MotionEvent.ACTION_DOWN -> {
                    if (checkPermissions()) {
                        startRecording()
                        Handler(Looper.getMainLooper()).postDelayed({
                            // Stop recording after 5 seconds
                            stopRecording()
                            uploadAudioToFirebase()
                            showNotification()
                            storeFirebaseToken()  // Store the Firebase token
                        }, recordDuration) // Delay for the required duration
                    } else {
                        Toast.makeText(requireContext(), "Please grant required permissions", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                // When button is released, stop recording
                MotionEvent.ACTION_UP -> {
                    // In case the button is released before 5 seconds, stop the recording manually
                    stopRecording()
                    return@setOnTouchListener true
                }
                else -> false
            }
        }

        notificationButton.setOnClickListener {
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, NotificationFragment())
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
        val audioRef = firebaseStorage.reference.child("audio/${audioFile.name}")
        val uploadTask = audioRef.putFile(audioFile.toUri())

        uploadTask.addOnSuccessListener {
            audioRef.downloadUrl.addOnSuccessListener { uri ->
                Log.d("SOS", "Upload successful: $uri")
                saveAudioUriToDatabase(uri.toString())
            }
        }.addOnFailureListener { e ->
            Log.e("SOS", "Failed to upload audio", e)
            Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveAudioUriToDatabase(uri: String) {
        val database = FirebaseDatabase.getInstance().getReference("audio_messages")
        val audioMessageId = database.push().key ?: return

        val audioMessage = mapOf(
            "id" to audioMessageId,
            "uri" to uri,
            "timestamp" to System.currentTimeMillis(),
            "location" to currentLocation
        )

        database.child(audioMessageId).setValue(audioMessage)
            .addOnSuccessListener {
                Log.d("SOS", "Audio URI saved successfully")
            }
            .addOnFailureListener { e ->
                Log.e("SOS", "Failed to save audio URI", e)
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
        val notificationText = "Help! I am at $currentLocation"

        val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("SOS Alert")
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationId = System.currentTimeMillis().toInt()

        with(NotificationManagerCompat.from(requireContext())) {
            notify(notificationId, builder.build())
        }

        Toast.makeText(requireContext(), "SOS Triggered at $currentLocation!", Toast.LENGTH_SHORT).show()
    }

    // New function to store Firebase token
    private fun storeFirebaseToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    FirebaseDatabase.getInstance()
                        .getReference("users/$userId/token")
                        .setValue(token)
                        .addOnSuccessListener {
                            Log.d("SOS", "Firebase token saved successfully")
                        }
                        .addOnFailureListener { e ->
                            Log.e("SOS", "Failed to save Firebase token", e)
                        }
                }
            }
        }
    }
}

