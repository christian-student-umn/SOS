package com.example.sos

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

data class Contact(val name: String, val phone: String, val email: String)

class ContactFragment : Fragment() {
    private lateinit var recyclerViewContacts: RecyclerView
    private lateinit var contactAdapter: ContactAdapter
    private var contacts: MutableList<Contact> = mutableListOf()
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_contact, container, false)

        recyclerViewContacts = view.findViewById(R.id.rv_contacts)

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Handle add contact button click
        val btnAddContact = view.findViewById<View>(R.id.btn_add_contact)
        btnAddContact.setOnClickListener {
            showAddContactDialog()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Sample data
        contacts = mutableListOf(
            Contact("John Doe", "123-456-7890", "john@example.com"),
            Contact("Jane Doe", "987-654-3210", "jane@example.com")
        )

        // Set up adapter with contact list and click listener
        contactAdapter = ContactAdapter(contacts) { contact ->
            Toast.makeText(requireContext(), "Contact clicked: ${contact.name}", Toast.LENGTH_SHORT)
                .show()
        }

        // Set up RecyclerView
        recyclerViewContacts.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewContacts.adapter = contactAdapter

        // Set up search functionality
        val searchView = view.findViewById<SearchView>(R.id.search_view)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                contactAdapter.filterList(query.orEmpty())
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                contactAdapter.filterList(newText.orEmpty())
                return false
            }
        })
    }

    private fun showAddContactDialog() {
        // Inflate custom dialog layout
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_contact, null, false)

        val editName = dialogView.findViewById<EditText>(R.id.edit_name)
        val editEmail = dialogView.findViewById<EditText>(R.id.edit_email)

        // Show AlertDialog for adding a contact
        AlertDialog.Builder(requireContext())
            .setTitle("Add Contact")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = editName.text.toString().trim()
                val email = editEmail.text.toString().trim()

                if (name.isNotEmpty() && email.isNotEmpty()) {
                    verifyEmailAndAddContact(name, email)
                } else {
                    Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun verifyEmailAndAddContact(name: String, email: String) {
        firebaseAuth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods ?: emptyList()
                    if (signInMethods.isNotEmpty()) {
                        // Email is registered
                        contacts.add(Contact(name, "N/A", email))
                        contactAdapter.notifyDataSetChanged()
                        Toast.makeText(requireContext(), "Contact added!", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        // Email is not registered
                        Toast.makeText(
                            requireContext(),
                            "Email is not registered in Firebase",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    // Handle error
                    Log.e("Firebase", "Error verifying email", task.exception)
                    Toast.makeText(
                        requireContext(),
                        "Error verifying email. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    // ContactAdapter remains inside ContactFragment
    inner class ContactAdapter(
        private var contacts: List<Contact>,
        private val listener: (Contact) -> Unit
    ) : RecyclerView.Adapter<ContactViewHolder>() {
        private var filteredContacts: List<Contact> = contacts

        fun filterList(searchQuery: String) {
            filteredContacts = contacts.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.fragment_contact_item, parent, false)
            return ContactViewHolder(view)
        }

        override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
            holder.bind(filteredContacts[position], listener)
        }

        override fun getItemCount(): Int = filteredContacts.size
    }

    // ViewHolder to manage individual contact items
    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName = itemView.findViewById<TextView>(R.id.tv_name)
        private val tvEmail = itemView.findViewById<TextView>(R.id.tv_email)
        private val ivProfile = itemView.findViewById<ImageView>(R.id.iv_profile)

        fun bind(contact: Contact, listener: (Contact) -> Unit) {
            tvName.text = contact.name
            tvEmail.text = contact.email

            // Toggle visibility of email on click
            itemView.setOnClickListener {
                if (tvEmail.visibility == View.GONE) {
                    tvEmail.visibility = View.VISIBLE
                } else {
                    tvEmail.visibility = View.GONE
                }
                listener(contact)
            }
        }
    }
}
