package com.example.sos

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.storage.FirebaseStorage

interface OnNotificationClickListener {
    fun onNotificationClick(notification: Notification)
}

class NotificationAdapter(private val notificationList: List<Notification>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.ivProfilePicture)
        val message: TextView = itemView.findViewById(R.id.tvNotificationMessage)
        val location: TextView = itemView.findViewById(R.id.tvNotificationLocation)
        val time: TextView = itemView.findViewById(R.id.tvNotificationTime)
        val playButton: ImageButton = itemView.findViewById(R.id.btnPlayAudio)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    private fun playAudioFromUri(context: Context, uri: Uri) {
        val mediaPlayer = MediaPlayer()
        try {
            mediaPlayer.setDataSource(context, uri)
            mediaPlayer.setOnPreparedListener { it.start() }
            mediaPlayer.prepareAsync()
        } catch (e: Exception) {
            Toast.makeText(context, "Error playing audio: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notificationList[position]
        holder.profileImage.setImageResource(notification.profileImageResId)
        holder.message.text = notification.message
        holder.location.text = notification.location
        holder.time.text = notification.time

        holder.playButton.setOnClickListener {
            val storageRef = FirebaseStorage.getInstance().reference.child(notification.audioPath)
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                playAudioFromUri(holder.itemView.context, uri)
            }.addOnFailureListener { e ->
                Toast.makeText(holder.itemView.context, "Failed to fetch audio: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount(): Int {
        return notificationList.size
    }


}
