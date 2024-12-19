package com.example.sos

data class Notification(
    val profileImageUrl: Int,
    val message: String,
    val location: String,
    val time: String,
    val audioUri: String
)

