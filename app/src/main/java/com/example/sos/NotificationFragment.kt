package com.example.sos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class NotificationFragment : Fragment() {

    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var notificationList: List<Notification>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notification, container, false)

        // Initialize dataa
        notificationList = listOf(
            Notification(R.drawable.ic_samplepp, "Dad needs your help", "4th Mound road, California", "9:41"),
            // Add more notifications here
        )

        // Set up RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvNotificationList)
        notificationAdapter = NotificationAdapter(notificationList)
        recyclerView.adapter = notificationAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        return view
    }
}
