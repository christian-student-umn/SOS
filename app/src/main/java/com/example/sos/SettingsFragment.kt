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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var userName: TextView
    private lateinit var userEmail: TextView
    private lateinit var userPhone: TextView // Add a TextView for phone number

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Find views
        userName = view.findViewById(R.id.user_name)
        userEmail = view.findViewById(R.id.user_email)
        userPhone = view.findViewById(R.id.user_phone) // Assuming you have a TextView for phone number in your layout

        // Load user profile from Firestore
        loadUserProfile()

        // Add account button click listener to navigate to ProfileFragment
        val accountOption: View = view.findViewById(R.id.account_option)
        accountOption.setOnClickListener {
            navigateToProfileFragment()
        }

        return view
    }

    private fun loadUserProfile() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val docRef = firestore.collection("profiles").document(userId)
            docRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name")
                    val email = document.getString("email")
                    val phone = document.getString("number") // Fetch phone number from Firestore

                    userName.text = name
                    userEmail.text = email
                    userPhone.text = phone // Set phone number to TextView
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToProfileFragment() {
        // Create a new instance of ProfileFragment
        val profileFragment = ProfileFragment()

        // Get the FragmentManager and start a transaction
        val transaction: FragmentTransaction = parentFragmentManager.beginTransaction()

        // Replace the current fragment with the ProfileFragment
        transaction.replace(R.id.fragment_container, profileFragment)
        transaction.addToBackStack(null) // Optional: Add this transaction to the back stack
        transaction.commit()
    }
}
