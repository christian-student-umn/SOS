package com.example.sos

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_signup.*

class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Handle Sign-Up button click
        signupButton.setOnClickListener {
            val email = emailSignupInput.text.toString().trim()
            val password = passwordSignupInput.text.toString().trim()
            val confirmPassword = confirmPasswordSignupInput.text.toString().trim()

            // Basic validation
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            } else if (password.isEmpty()) {
                Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
            } else if (confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please confirm your password", Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else {
                // Simulate sign-up success or failure
                // Replace this with actual sign-up logic (e.g., Firebase, server, etc.)
                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()

                // Navigate back to login screen after successful sign-up
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()  // Close the sign-up activity so the user can't go back here
            }
        }
    }
}
