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
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.sos.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import java.util.*

class NotificationFragment : Fragment() {

    private var mediaPlayer: MediaPlayer? = null
    private var audioUri: String? = null
    private val db = FirebaseFirestore.getInstance()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationTextView: TextView
    private lateinit var nameTextView: TextView
    private var currentLocation: String = "Unknown"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notification, container, false)

        // UI elementss
        val notificationContainer: ConstraintLayout = view.findViewById(R.id.notification_container)
        val playButton: ImageView = view.findViewById(R.id.image_play)
        val switchToggle: Switch = view.findViewById(R.id.switch_toggle)
        nameTextView = view.findViewById(R.id.text_name_message)
        locationTextView = view.findViewById(R.id.text_location)
        val profileImageView: ImageView = view.findViewById(R.id.image_profile)

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Fetch profile data (name and profile picture)
        fetchProfileData(nameTextView, profileImageView)

        // Fetch location
        fetchLocation()

        // Get audio URI from arguments
        arguments?.getString("audioUri")?.let {
            audioUri = it
        }

        // Set up play button functionality
        playButton.setOnClickListener {
            if (!audioUri.isNullOrEmpty()) {
                playAudio(audioUri!!)
            } else {
                Toast.makeText(requireContext(), "No audio file available", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up switch toggle listener
        switchToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(requireContext(), "I have helped them", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "I have not helped them yet", Toast.LENGTH_SHORT).show()
            }
        }

        return view
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

    private fun fetchProfileData(nameTextView: TextView, profileImageView: ImageView) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            val email = currentUser.email

            if (!email.isNullOrEmpty()) {
                // Query Firestore for the user's profile using their email
                db.collection("emailDB").document(email).collection("profiles").get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            for (document in documents) {
                                val name = document.getString("name") ?: "Unknown"
                                val profileImageUrl = document.getString("profileImageUrl") ?: ""

                                // Set name
                                nameTextView.text = "$name needs your help"

                                // Load profile image using Glide
                                if (profileImageUrl.isNotEmpty()) {
                                    Glide.with(requireContext())
                                        .load(profileImageUrl)
                                        .placeholder(R.drawable.ic_profile) // Placeholder drawable
                                        .error(R.drawable.ic_profile)       // Error fallback drawable
                                        .circleCrop()
                                        .into(profileImageView)
                                } else {
                                    profileImageView.setImageResource(R.drawable.ic_profile) // Fallback image
                                }
                            }
                        } else {
                            nameTextView.text = "Unknown Name"
                            profileImageView.setImageResource(R.drawable.ic_profile)
                            Log.w("NotificationFragment", "No profiles found for this user.")
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("NotificationFragment", "Error fetching profile data", exception)
                        nameTextView.text = "Unknown Name"
                        profileImageView.setImageResource(R.drawable.ic_profile) // Fallback image
                    }
            } else {
                Log.e("NotificationFragment", "User email is null or empty")
                nameTextView.text = "Unknown Name"
                profileImageView.setImageResource(R.drawable.ic_profile)
            }
        } else {
            Log.e("NotificationFragment", "No authenticated user found")
            nameTextView.text = "Unknown Name"
            profileImageView.setImageResource(R.drawable.ic_profile)
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
