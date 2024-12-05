package com.example.sos

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class SignupActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnSignup: Button
    private lateinit var tvAlreadyHaveAccount: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Inisialisasi Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Inisialisasi komponen UI
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnSignup = findViewById(R.id.btnSignup)
        tvAlreadyHaveAccount = findViewById(R.id.tvAlreadyHaveAccount)

        // Aksi saat tombol signup ditekan
        btnSignup.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (TextUtils.isEmpty(email)) {
                etEmail.error = "Email harus diisi"
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(password)) {
                etPassword.error = "Password harus diisi"
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(confirmPassword)) {
                etConfirmPassword.error = "Konfirmasi password harus diisi"
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                etConfirmPassword.error = "Password tidak cocok"
                return@setOnClickListener
            }

            // Firebase signup with email and password
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Pendaftaran berhasil
                        Toast.makeText(this, "Pendaftaran berhasil", Toast.LENGTH_SHORT).show()

                        // Pindah ke halaman login setelah signup sukses
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish() // Untuk menghapus activity ini dari back stack
                    } else {
                        // Jika pendaftaran gagal
                        Toast.makeText(this, "Pendaftaran gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Aksi saat pengguna sudah memiliki akun
        tvAlreadyHaveAccount.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Kembali ke halaman login
        }
    }
}

//testing aja