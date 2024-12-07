package com.example.sos

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


data class Contact(
    val name: String = "",
    val phone: String = "",
    val email: String = ""
)


class ContactFragment : Fragment() {
    private lateinit var recyclerViewContacts: RecyclerView
    private lateinit var contactAdapter: ContactAdapter
    private var contacts: MutableList<Contact> = mutableListOf()
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_contact, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerViewContacts = view.findViewById(R.id.rv_contacts)

        // Initialize Firebase Auth and Firestore
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Load contacts from Firestore
        loadContactsFromFirestore()

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

        // Handle add contact button click
        val btnAddContact = view.findViewById<View>(R.id.btn_add_contact)
        val btnSaveContact = view.findViewById<View>(R.id.btn_save_contact)
        val btnCancelContact = view.findViewById<View>(R.id.btn_cancel_contact)

        // Show form when add contact button is clicked
        btnAddContact.setOnClickListener {
            toggleAddContactForm(true)
        }

        // Save contact
        btnSaveContact.setOnClickListener {
            val editName = view.findViewById<EditText>(R.id.edit_name)
            val editEmail = view.findViewById<EditText>(R.id.edit_email)
            val editPhone = view.findViewById<EditText>(R.id.edit_phone)

            val name = editName.text.toString().trim()
            val email = editEmail.text.toString().trim()
            val phone = editPhone.text.toString().trim()

            if (name.isNotEmpty() && email.isNotEmpty() && phone.isNotEmpty()) {
                addContactToFirestore(name, email, phone)
                editName.text.clear()
                editEmail.text.clear()
                editPhone.text.clear()
                toggleAddContactForm(false)
            } else {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        // Cancel adding contact
        btnCancelContact.setOnClickListener {
            toggleAddContactForm(false)
        }
    }

    private fun toggleAddContactForm(show: Boolean) {
        val formAddContact = requireView().findViewById<LinearLayout>(R.id.form_add_contact)
        formAddContact.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun addContactToFirestore(name: String, email: String, phone: String) {
        // Cek apakah email ada di koleksi emailDB
        firestore.collection("emailDB")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // Email ditemukan di emailDB, lanjutkan untuk menambahkan kontak
                    val contact = Contact(name, phone, email)

                    firestore.collection("contacts")
                        .add(contact)
                        .addOnSuccessListener {
                            contacts.add(contact)
                            contactAdapter.notifyDataSetChanged()
                            Toast.makeText(requireContext(), "Contact added!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Error adding contact", e)
                            Toast.makeText(
                                requireContext(),
                                "Error adding contact. Please try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    // Email tidak ditemukan di emailDB
                    Toast.makeText(
                        requireContext(),
                        "Email not registered in the database!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error checking email in emailDB", e)
                Toast.makeText(
                    requireContext(),
                    "Error checking email. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    private fun loadContactsFromFirestore() {
        firestore.collection("contacts")
            .get()
            .addOnSuccessListener { result ->
                contacts.clear()
                for (document in result) {
                    val contact = document.toObject(Contact::class.java)
                    contacts.add(contact)
                }
                contactAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error loading contacts", e)
                Toast.makeText(
                    requireContext(),
                    "Error loading contacts. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

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

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName = itemView.findViewById<TextView>(R.id.tv_name)
        private val tvEmail = itemView.findViewById<TextView>(R.id.tv_email)

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
