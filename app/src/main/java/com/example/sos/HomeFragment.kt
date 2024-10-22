package com.example.sos

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {

    private val holdTime = 3000L // 3 seconds in milliseconds
    private var isHeld = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout manually
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Find views using findViewById
        val sosButton: Button = view.findViewById(R.id.button)

        // Set up the hold-down functionality for the SOS button
        sosButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // User starts holding the button
                    isHeld = true
                    handler.postDelayed({
                        if (isHeld) {
                            // Action triggered after holding for 3 seconds
                            Toast.makeText(requireContext(), "SOS Triggered!", Toast.LENGTH_SHORT).show()
                        }
                    }, holdTime)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // User releases the button or cancels the action
                    isHeld = false
                    handler.removeCallbacksAndMessages(null)
                    true
                }
                else -> false
            }
        }

        return view
    }
}
