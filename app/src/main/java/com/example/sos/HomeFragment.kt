package com.example.sos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.sos.R

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout manually
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Find views using findViewById
        val sosButton: Button = view.findViewById(R.id.button)
        sosButton.setOnClickListener {
            // Handle button click
        }

        return view
    }
}
