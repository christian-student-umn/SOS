package com.example.sos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class NotificationFragment : Fragment() {

    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var notificationList: List<Notification>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notification, container, false)

        // Initialize data
        notificationList = listOf(
            Notification(
                R.drawable.ic_samplepp,
                "Voice Note from Mom",
                "Home",
                "12:15",
                "https://firebasestorage.googleapis.com/v0/b/sosapp-d4e64.appspot.com/o/audio%2FSOS_20241219_163557.3gp?alt=media&token=b3ac7dd7-b7d5-4e92-8c82-392c13af53b0"
            )
        )


        // Add notifications to Firestore if collection is empty
        addNotificationsToFirestore(notificationList)

        // Set up RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvNotificationList)
        notificationAdapter = NotificationAdapter(notificationList)
        recyclerView.adapter = notificationAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        return view
    }

    private fun addNotificationsToFirestore(notifications: List<Notification>) {
        val db = FirebaseFirestore.getInstance()
        val collectionRef = db.collection("notification")

        // Check if the collection is empty
        collectionRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    // Add notifications to Firestore
                    notifications.forEach { notification ->
                        val notificationData = mapOf(
                            "profile" to mapOf(
                                "profilePicture" to "R.drawable.ic_samplepp", // Use the actual URL or reference for profile picture
                                "name" to "Voice Note from Dad",
                                "location" to "Home",
                                "time" to "10:15"
                            ),
                            "audio" to notification.audioPath // URL of the audio file
                        )

                        collectionRef.add(notificationData)
                            .addOnSuccessListener {
                                println("Notification added to Firestore with ID: ${it.id}")
                            }
                            .addOnFailureListener { e ->
                                println("Error adding notification: ${e.message}")
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                println("Error fetching collection: ${e.message}")
            }
    }
}
