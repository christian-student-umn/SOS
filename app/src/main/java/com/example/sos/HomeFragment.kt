import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.sos.R
import com.example.sos.SettingsFragment

class HomeFragment : Fragment() {

    private val holdTime = 3000L // 3 seconds in milliseconds
    private var isHeld = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // SOS Button (this is a Button in your XML)
        val sosButton: Button = view.findViewById(R.id.button)

        // Settings Button (this is an ImageButton in your XML)
        val settingsButton: ImageButton = view.findViewById(R.id.button_settings)

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
                            // You can add additional SOS logic here
                        }
                    }, holdTime)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // User releases the button or cancels the action before 3 seconds
                    isHeld = false
                    handler.removeCallbacksAndMessages(null)
                    true
                }
                else -> false
            }
        }

        // Set onClickListener for settings button
        settingsButton.setOnClickListener {
            // Replace the fragment with SettingsFragment
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, SettingsFragment()) // 'fragment_container' is your container ID
            transaction.addToBackStack(null) // Optional, adds to back stack so user can navigate back
            transaction.commit()
        }

        return view
    }
}
