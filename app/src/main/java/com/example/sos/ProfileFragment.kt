package com.example.sos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var tvCurrentName: TextView
    private lateinit var tvCurrentNumber: TextView
    private lateinit var etName: EditText
    private lateinit var etNumber: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Initialize Firebase services
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initialize UI elements
        tvCurrentName = view.findViewById(R.id.tvCurrentName)
        tvCurrentNumber = view.findViewById(R.id.tvCurrentNumber)
        etName = view.findViewById(R.id.etName)
        etNumber = view.findViewById(R.id.etNumber)
        val updateButton = view.findViewById<Button>(R.id.btnUpdateProfile)

        // Load current profile data when the fragment loads
        loadProfileData()

        // Set up the update button click listener
        updateButton.setOnClickListener {
            val name = etName.text.toString()
            val number = etNumber.text.toString() // Change 'nim' to 'number' for consistency

            if (name.isNotEmpty() && number.isNotEmpty()) {
                updateProfile(name, number) // Pass the correct variable
            } else {
                Toast.makeText(context, "Please fill in both fields", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    // Function to load the user's current profile data
    private fun loadProfileData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val docRef = firestore.collection("profiles").document(userId)
            docRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name")
                    val number = document.getString("number") // Ensure this matches your Firestore field
                    // Set the current name and number to the TextViews
                    tvCurrentName.text = "Current Name: $name"
                    tvCurrentNumber.text = "Current Number: $number"
                    // Pre-fill the EditText fields with the current data (optional)
                    etName.setText(name)
                    etNumber.setText(number)
                } else {
                    Toast.makeText(context, "No profile found. Please update your info.", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(context, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Handle the case where the user is not logged in
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to update the profile in Firestore using the .set() method
    private fun updateProfile(name: String, number: String) { // Use 'number' instead of 'nim'
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val user = hashMapOf(
                "name" to name,
                "number" to number, // Ensure this matches your Firestore field
                "userId" to currentUser.uid
            )

            // Save the profile in Firestore using the .set() method
            firestore.collection("profiles").document(currentUser.uid)
                .set(user)
                .addOnSuccessListener {
                    Toast.makeText(context, "Profile successfully updated", Toast.LENGTH_SHORT).show()
                    // Update the TextViews after saving the profile
                    tvCurrentName.text = "Current Name: $name"
                    tvCurrentNumber.text = "Current Number: $number" // Change 'nim' to 'number' for consistency
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }
}