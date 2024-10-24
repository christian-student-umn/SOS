package com.example.sos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SettingsFragment : Fragment() {

    private lateinit var database: DatabaseReference
    private lateinit var userName: TextView
    private lateinit var userEmail: TextView
    private lateinit var profileImage: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance().reference.child("users").child("user_id") // Replace with your user ID logic

        // Find views
        userName = view.findViewById(R.id.user_name)
        userEmail = view.findViewById(R.id.user_email)
        profileImage = view.findViewById(R.id.profile_image) // For future use if you want to load profile picture

        // Load user data from Firebase
        loadUserProfile()

        // Set onClickListeners for settings options
        val accountOption: View = view.findViewById(R.id.account_option)
        val notificationOption: View = view.findViewById(R.id.notification_option)
        accountOption.setOnClickListener {
            // Navigate to ProfileFragment
            navigateToProfileFragment()
        }

        notificationOption.setOnClickListener {
            // Handle notification settings logic
            updateNotificationSettings(true) // or false depending on the action
        }

        return view
    }

    private fun loadUserProfile() {
        database.child("profile").get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // Get values from the database
                val name = snapshot.child("name").getValue(String::class.java) ?: "Unknown"
                val email = snapshot.child("email").getValue(String::class.java) ?: "Unknown"

                // Update UI with the fetched values
                userName.text = name
                userEmail.text = email
            } else {
                Toast.makeText(requireContext(), "No user data found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Error fetching profile", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateNotificationSettings(enabled: Boolean) {
        database.child("settings").child("notifications").setValue(enabled)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Notification settings updated", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update settings", Toast.LENGTH_SHORT).show()
            }
    }
    private fun navigateToProfileFragment() {
        // Create a new instance of ProfileFragment
        val profileFragment = ProfileFragment()

        // Get the FragmentManager and start a transaction
        val transaction: FragmentTransaction = parentFragmentManager.beginTransaction()

        // Replace the current fragment with the ProfileFragment
        transaction.replace(R.id.fragment_container, profileFragment) // Use your actual container ID
        transaction.addToBackStack(null) // Optional: Add this transaction to the back stack
        transaction.commit()
    }
}

