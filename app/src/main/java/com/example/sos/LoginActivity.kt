package com.example.sos

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var tvCreateAccount: TextView
    private lateinit var tvForgotPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inisialisasi komponen UI
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvCreateAccount = findViewById(R.id.tvCreateAccount)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)

        // Aksi saat tombol login ditekan
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

            // Validasi login (logika sederhana, bisa diganti dengan autentikasi Firebase)
            if (email == "admin@example.com" && password == "password") {
                Toast.makeText(this, "Login sukses", Toast.LENGTH_SHORT).show()
                // Pindah ke halaman Home atau Main setelah login sukses
                val intent = Intent(this, HomeFragment::class.java)
                startActivity(intent)
                finish() // Untuk menghapus activity ini dari back stack
            } else {
                Toast.makeText(this, "Email atau password salah", Toast.LENGTH_SHORT).show()
            }
        }

        // Aksi saat pengguna ingin membuat akun baru
        tvCreateAccount.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        // Aksi saat pengguna lupa password (bisa ditambahkan logika reset password)
        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Fitur lupa password belum tersedia", Toast.LENGTH_SHORT).show()
        }
    }
}
