package com.example.sos

import android.app.Activity
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

        // Initialize Firestore
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

        // Add account button click listener
        val accountOption: View = view.findViewById(R.id.account_option)
        accountOption.setOnClickListener {
            navigateToProfileFragment()
        }

        // Set up switch for notification
        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(requireContext(), "Notifications Enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Notifications Disabled", Toast.LENGTH_SHORT).show()
            }
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

            // Optional: Upload image to Firebase Storage
            uploadImageToFirebase(selectedImageUri)
        }
    }

    private fun uploadImageToFirebase(imageUri: Uri?) {
        if (imageUri != null) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val storageRef = FirebaseStorage.getInstance().getReference("profile_images/$userId")

            storageRef.putFile(imageUri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        // Save the download URL to Firestore
                        firestore.collection("profiles").document(userId)
                            .update("profileImageUrl", uri.toString())
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Profile image updated", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadUserProfile() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val docRef = firestore.collection("profiles").document(userId)
            docRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name")
                    val email = document.getString("email")
                    val phone = document.getString("number")
                    val profileImageUrl = document.getString("profileImageUrl")

                    userName.text = name
                    userEmail.text = email
                    userPhone.text = phone

                    // Load profile image if available
                    if (!profileImageUrl.isNullOrEmpty()) {
                        Glide.with(this).load(profileImageUrl).into(profileImage)
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToProfileFragment() {
        val profileFragment = ProfileFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, profileFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { dialog, _ ->
                FirebaseAuth.getInstance().signOut()
                Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
                dialog.dismiss()

                // Navigate back to login screen
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finish()
            }
            .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, _ ->
                dialog.dismiss()
            })
            .show()
    }
}
