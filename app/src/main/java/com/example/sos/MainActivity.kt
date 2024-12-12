package com.example.sos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Periksa apakah user sudah login
        if (FirebaseAuth.getInstance().currentUser == null) {
            // Jika belum login, arahkan ke LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        // Jika user sudah login, tampilkan main layout
        setContentView(R.layout.activity_main)

        // Mendapatkan token FCM dan menyimpannya ke Firestore
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Mendapatkan token FCM
                val token = task.result
                Log.d("FCM", "FCM Token: $token")

                // Menyimpan token ke Firestore
                saveTokenToFirebase(token)
            } else {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
            }
        }

        // Default fragment: HomeFragment
        loadFragment(HomeFragment())

        // Setup bottom navigation
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigation)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_map -> loadFragment(MapFragment())
                R.id.nav_contact -> loadFragment(ContactFragment())
            }
            true
        }
    }

    // Fungsi untuk menyimpan token ke Firestore
    private fun saveTokenToFirebase(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val userRef = FirebaseFirestore.getInstance().collection("users").document(userId!!)

        userRef.update("fcmToken", token)
            .addOnSuccessListener {
                Log.d("FCM", "Token berhasil disimpan")
            }
            .addOnFailureListener { e ->
                Log.w("FCM", "Error menyimpan token", e)
            }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
