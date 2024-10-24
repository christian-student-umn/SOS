package com.example.sos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var tvCreateAccount: TextView
    private lateinit var tvForgotPassword: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Check if user is already logged in
        val sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        if (sharedPreferences.getBoolean("isLoggedIn", false)) {
            // Redirect to MainActivity if already logged in
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Initialize UI components
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvCreateAccount = findViewById(R.id.tvCreateAccount)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)

        // Login button click action
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (TextUtils.isEmpty(email)) {
                etEmail.error = "Email harus diisi"
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(password)) {
                etPassword.error = "Password harus diisi"
                return@setOnClickListener
            }

            // Firebase authentication
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Login sukses", Toast.LENGTH_SHORT).show()
                        // Save login state in SharedPreferences
                        sharedPreferences.edit().putBoolean("isLoggedIn", true).apply()
                        // Redirect to MainActivity after successful login
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish() // Close the LoginActivity so user can't go back
                    } else {
                        Toast.makeText(this, "Email atau password salah", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Create new account action
        tvCreateAccount.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        // Forgot password action
        tvForgotPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (TextUtils.isEmpty(email)) {
                etEmail.error = "Masukkan email untuk reset password"
            } else {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Link reset password telah dikirim", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Gagal mengirim link reset password", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }
}