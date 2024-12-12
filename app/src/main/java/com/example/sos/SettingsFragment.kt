package com.example.sos

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class SettingsFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var userName: TextView
    private lateinit var userEmail: TextView
    private lateinit var userPhone: TextView
    private lateinit var profileImage: ImageView
    private lateinit var notificationSwitch: Switch

    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Initialize Firestore and Firebase Auth
        firestore = FirebaseFirestore.getInstance()

        // Find views
        userName = view.findViewById(R.id.user_name)
        userEmail = view.findViewById(R.id.user_email)
        userPhone = view.findViewById(R.id.user_phone)
        profileImage = view.findViewById(R.id.profile_image)
        notificationSwitch = view.findViewById(R.id.notification_switch)

        // Load user profile from Firestore
        loadUserProfile()

        // Set up click listener for profile image
        profileImage.setOnClickListener {
            openImageChooser()
        }

        // Handle account options button
        val accountOption: View = view.findViewById(R.id.account_option)
        accountOption.setOnClickListener {
            navigateToProfileFragment()
        }

        // Set up switch for notifications
        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(
                requireContext(),
                if (isChecked) "Notifications Enabled" else "Notifications Disabled",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Logout button click listener
        val logoutButton: Button = view.findViewById(R.id.logout_button)
        logoutButton.setOnClickListener {
            showLogoutConfirmation()
        }

        return view
    }

    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            profileImage.setImageURI(selectedImageUri)
            uploadImageToFirebase(selectedImageUri)
        }
    }

    private fun uploadImageToFirebase(imageUri: Uri?) {
        if (imageUri != null) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val userId = currentUser?.uid ?: return
            val userEmail = currentUser.email ?: return
            val storageRef = FirebaseStorage.getInstance().getReference("profile_images/$userId")

            storageRef.putFile(imageUri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        val profileImageUrl = uri.toString()

                        // Update Firestore document
                        firestore.collection("emailDB").document(userEmail)
                            .collection("profiles").document(userId)
                            .update("profileImageURL", profileImageUrl)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    requireContext(),
                                    "Profile image updated",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to update Firestore",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadUserProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val firebaseEmail = currentUser?.email

        if (firebaseEmail != null) {
            // Verifikasi email di emailDB
            firestore.collection("emailDB").document(firebaseEmail).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val userId = currentUser.uid

                        // Jika email valid, ambil data dari subkoleksi profiles
                        firestore.collection("emailDB").document(firebaseEmail)
                            .collection("profiles").document(userId)
                            .get()
                            .addOnSuccessListener { profileDocument ->
                                if (profileDocument.exists()) {
                                    userName.text = profileDocument.getString("name") ?: "N/A"
                                    userEmail.text = firebaseEmail // Gunakan firebaseEmail untuk TextView
                                    userPhone.text = profileDocument.getString("number") ?: "N/A"
                                    val profileImageUrl = profileDocument.getString("profileImageURL")

                                    if (!profileImageUrl.isNullOrEmpty()) {
                                        Glide.with(this).load(profileImageUrl).into(profileImage)
                                    } else {
                                        profileImage.setImageResource(R.drawable.ic_profile) // Placeholder
                                    }
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        "Profile data does not exist",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Email not verified in database",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to verify email", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }



    private fun navigateToProfileFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ProfileFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { dialog, _ ->
                try {
                    // Sign out from Firebase
                    FirebaseAuth.getInstance().signOut()

                    // Clear SharedPreferences (if used)
                    val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                    sharedPreferences.edit().clear().apply()

                    // Navigate to LoginActivity
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)

                    // Add transition animation
                    requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    requireActivity().finish()

                    // Show success message
                    Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error during logout: ${e.message}", Toast.LENGTH_SHORT).show()
                }

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}