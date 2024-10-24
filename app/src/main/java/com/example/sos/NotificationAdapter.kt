package com.example.sos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotificationAdapter(private val notificationList: List<Notification>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.ivProfilePicture)
        val message: TextView = itemView.findViewById(R.id.tvNotificationMessage)
        val location: TextView = itemView.findViewById(R.id.tvNotificationLocation)
        val time: TextView = itemView.findViewById(R.id.tvNotificationTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notificationList[position]
        holder.profileImage.setImageResource(notification.profileImageResId)
        holder.message.text = notification.message
        holder.location.text = notification.location
        holder.time.text = notification.time
    }

    override fun getItemCount(): Int {
        return notificationList.size
    }
}
