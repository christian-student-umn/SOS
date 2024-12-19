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
import androidx.fragment.app.Fragment
import com.example.sos.R
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.*

class NotificationFragment : Fragment() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var audioUri: String
    private val db = FirebaseFirestore.getInstance()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationTextView: TextView
    private var currentLocation: String = "Unknown"
    private lateinit var nameTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notification, container, false)

        // UI elements
        val notificationContainer: ConstraintLayout = view.findViewById(R.id.notification_container)
        val playButton: ImageView = view.findViewById(R.id.image_play)
        val switchToggle: Switch = view.findViewById(R.id.switch_toggle)
        nameTextView = view.findViewById(R.id.text_name_message)
        locationTextView = view.findViewById(R.id.text_location)
        val profileImageView: ImageView = view.findViewById(R.id.image_profile)

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Fetch profile data (name)
        fetchProfileData(nameTextView, profileImageView)

        // Fetch location
        fetchLocation()

        // Get audio URI from arguments
        arguments?.getString("audioUri")?.let {
            audioUri = it
        } ?: run {
            Toast.makeText(requireContext(), "No audio file to play", Toast.LENGTH_SHORT).show()
        }

        // Set up play button functionality
        playButton.setOnClickListener {
            if (::audioUri.isInitialized) {
                playAudio(audioUri)
            } else {
                Toast.makeText(requireContext(), "No audio file to play", Toast.LENGTH_SHORT).show()
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
        val email = "admin@admin.com" // Replace with the current user's email or dynamically fetch it

        db.collection("emailDB").document(email).collection("profiles").get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val name = document.getString("name") ?: "Unknown"
                    val profileImageUrl = document.getString("profileImageUrl")

                    // Set name
                    nameTextView.text = "$name needs your help"

                    // Load profile image
                    profileImageUrl?.let {
                        Picasso.get().load(it).into(profileImageView)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("NotificationFragment", "Error fetching profile data", exception)
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

    // Check if permissions are granted (you need to implement this method)
    private fun checkPermissions(): Boolean {
        // Implement permission checking logic
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}
