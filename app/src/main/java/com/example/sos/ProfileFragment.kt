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
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        tvCurrentName = view.findViewById(R.id.tvCurrentName)
        tvCurrentNumber = view.findViewById(R.id.tvCurrentNumber)
        etName = view.findViewById(R.id.etName)
        etNumber = view.findViewById(R.id.etNumber)
        val updateButton = view.findViewById<Button>(R.id.btnUpdateProfile)

        loadProfileData()

        updateButton.setOnClickListener {
            val name = etName.text.toString().trim()
            val number = etNumber.text.toString().trim()

            if (name.isNotEmpty() && number.isNotEmpty()) {
                updateProfile(name, number)
            } else {
                Toast.makeText(context, "Please fill in both fields", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun loadProfileData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("profiles").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name") ?: ""
                        val number = document.getString("number") ?: ""

                        tvCurrentName.text = "Current Name: $name"
                        tvCurrentNumber.text = "Current Number: $number"
                        etName.setText(name)
                        etNumber.setText(number)
                    } else {
                        Toast.makeText(context, "No profile found.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateProfile(name: String, number: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val user = mapOf(
                "name" to name,
                "number" to number,
                "userId" to currentUser.uid
            )

            firestore.collection("profiles").document(currentUser.uid)
                .set(user)
                .addOnSuccessListener {
                    Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    tvCurrentName.text = "Current Name: $name"
                    tvCurrentNumber.text = "Current Number: $number"
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
