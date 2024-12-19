//package com.example.sos
//
//import android.content.Context
//import android.media.MediaPlayer
//import android.net.Uri
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageButton
//import android.widget.ImageView
//import android.widget.TextView
//import android.widget.Toast
//import androidx.recyclerview.widget.RecyclerView
//import com.google.firebase.storage.FirebaseStorage
//
//interface OnNotificationClickListener {
//    fun onNotificationClick(notification: Notification)
//}
//
//class NotificationAdapter(private val notificationList: List<Notification>) :
//    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {
//
//    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val profileImage: ImageView = itemView.findViewById(R.id.ivProfilePicture)
//        val message: TextView = itemView.findViewById(R.id.tvNotificationMessage)
//        val location: TextView = itemView.findViewById(R.id.tvNotificationLocation)
//        val time: TextView = itemView.findViewById(R.id.tvNotificationTime)
//        val playButton: ImageButton = itemView.findViewById(R.id.btnPlayAudio)
//
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
//        return NotificationViewHolder(view)
//    }
//
//    private fun playAudioFromUri(context: Context, uri: Uri) {
//        val mediaPlayer = MediaPlayer()
//        try {
//            mediaPlayer.setDataSource(context, uri)
//            mediaPlayer.setOnPreparedListener { it.start() }
//            mediaPlayer.prepareAsync()
//        } catch (e: Exception) {
//            Toast.makeText(context, "Error playing audio: ${e.message}", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//
//    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
//        val notification = notificationList[position]
//        holder.profileImage.setImageResource(notification.profileImageResId)
//        holder.message.text = notification.message
//        holder.location.text = notification.location
//        holder.time.text = notification.time
//
//        holder.playButton.setOnClickListener {
//            val storageRef = FirebaseStorage.getInstance().reference.child(notification.audioPath)
//            storageRef.downloadUrl.addOnSuccessListener { uri ->
//                playAudioFromUri(holder.itemView.context, uri)
//            }.addOnFailureListener { e ->
//                Toast.makeText(holder.itemView.context, "Failed to fetch audio: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    override fun getItemCount(): Int {
//        return notificationList.size
//    }
//
//
//}
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sos.R
import com.squareup.picasso.Picasso

// Data model untuk notifikasi
data class Notification(
    val name: String,
    val message: String,
    val location: String,
    val profileImageUrl: String,
    var isHelped: Boolean = false// Menggunakan var agar nilai bisa diubah
)

class NotificationAdapter(
    private val notifications: List<Notification>,
    private val onPlayClick: (Notification) -> Unit, // Callback untuk tombol Play
    private val onSwitchToggle: (Notification, Boolean) -> Unit // Callback untuk Switch Toggle
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    // ViewHolder untuk elemen notifikasi
    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.image_profile)
        val nameMessageText: TextView = itemView.findViewById(R.id.text_name_message)
        val locationText: TextView = itemView.findViewById(R.id.text_location)
        val playButton: ImageView = itemView.findViewById(R.id.image_play)
        val toggleSwitch: Switch = itemView.findViewById(R.id.switch_toggle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]

        // Set data ke elemen UI
        holder.nameMessageText.text = "${notification.name} needs your help"
        holder.locationText.text = notification.location
        Picasso.get()
            .load(notification.profileImageUrl.ifEmpty { null }) // Gunakan URL default jika kosong
            .placeholder(R.drawable.ic_profile) // Gambar placeholder
            .into(holder.profileImage)

        // Event listener untuk tombol Play
        holder.playButton.setOnClickListener {
            onPlayClick(notification) // Memanggil callback untuk Play
        }

        // Event listener untuk Switch Toggle
        holder.toggleSwitch.setOnCheckedChangeListener(null) // Hindari listener lama dipanggil ulang
        holder.toggleSwitch.isChecked = notification.isHelped
        holder.toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            notification.isHelped = isChecked // Perbarui data status switch
            onSwitchToggle(notification, isChecked)
        }
    }

    override fun getItemCount(): Int = notifications.size
}


