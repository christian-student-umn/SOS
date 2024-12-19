import android.annotation.SuppressLint
import android.location.Geocoder
import android.location.Location
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sos.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.util.*

class NotificationFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter
    private val notifications = mutableListOf<Notification>()
    private var mediaPlayer: MediaPlayer? = null
    private val db = FirebaseFirestore.getInstance()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationTextView: TextView
    private lateinit var nameTextView: TextView
    private var currentLocation: String = "Unknown"

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notification, container, false)

        // UI elements
        locationTextView = view.findViewById(R.id.text_location)
        nameTextView = view.findViewById(R.id.text_name_message)
        val profileImageView: ImageView = view.findViewById(R.id.image_profile)

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Fetch profile data
        fetchProfileData(nameTextView, profileImageView)

        // Fetch location
        fetchLocation()

        // Set up RecyclerView
        recyclerView = view.findViewById(R.id.recycler_notifications)
        notificationAdapter = NotificationAdapter(
            notifications,
            onPlayClick = { notification ->
                notification.audioUri?.let {
                    playAudio(it.toString())
                } ?: Toast.makeText(
                    requireContext(),
                    "No audio file available for ${notification.name}",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onSwitchToggle = { notification, isChecked ->
                val message = if (isChecked) "${notification.name} has been helped" else "${notification.name} is still waiting for help"
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                updateNotificationStatus(notification, isChecked)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = notificationAdapter

        // Fetch data for RecyclerView
        fetchNotifications()

        return view
    }

    private fun fetchNotifications() {
        db.collection("notifications").get()
            .addOnSuccessListener { documents ->
                notifications.clear()
                for (document in documents) {
                    val notification = document.toObject(Notification::class.java).apply {
                        id = document.id // Assign the Firestore document ID to the notification object
                    }
                    notifications.add(notification)
                }
                notificationAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("NotificationFragment", "Error fetching notifications", e)
            }
    }

    private fun playAudio(uri: String) {
        mediaPlayer?.release()
        mediaPlayer = null

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(requireContext(), Uri.parse(uri))
                prepare()
                start()
            }
            Toast.makeText(requireContext(), "Playing audio...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("NotificationFragment", "Error playing audio", e)
            Toast.makeText(requireContext(), "Failed to play audio", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateNotificationStatus(notification: Notification, isChecked: Boolean) {
        db.collection("notifications").document(notification.id)
            .update("isHelped", isChecked)
            .addOnSuccessListener {
                Log.d("NotificationFragment", "Notification status updated for ${notification.name}")
            }
            .addOnFailureListener { e ->
                Log.e("NotificationFragment", "Error updating notification status", e)
            }
    }

    private fun fetchProfileData(nameTextView: TextView, profileImageView: ImageView) {
        val email = "admin@admin.com" // Replace with the current user's email or dynamically fetch it

        db.collection("emailDB").document(email).collection("profiles").get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    nameTextView.text = "Profile data not available"
                } else {
                    for (document in documents) {
                        val name = document.getString("name") ?: "Unknown"
                        val profileImageUrl = document.getString("profileImageUrl")

                        nameTextView.text = "$name needs your help"
                        profileImageUrl?.let {
                            Picasso.get().load(it).placeholder(R.drawable.ic_profile).into(profileImageView)
                        } ?: profileImageView.setImageResource(R.drawable.ic_profile)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("NotificationFragment", "Error fetching profile data", exception)
                nameTextView.text = "Error fetching profile"
            }
    }

    private fun fetchLocation() {
        if (!checkPermissions()) {
            locationTextView.text = "Location permissions are not granted"
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
                currentLocation = areaName
                locationTextView.text = "I am at $areaName"
            } else {
                locationTextView.text = "Unable to determine location name"
            }
        } catch (e: Exception) {
            Log.e("NotificationFragment", "Error fetching address", e)
            locationTextView.text = "Error fetching address"
        }
    }

    private fun checkPermissions(): Boolean {
        val locationPermission = ActivityCompat.checkSelfPermission(
            requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION
        )
        return locationPermission == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}
