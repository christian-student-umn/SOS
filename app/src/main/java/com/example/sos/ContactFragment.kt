package com.example.sos

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class Contact(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val profileImageURL: String = ""
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
        contactAdapter = ContactAdapter(contacts) {}

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
            val editEmail = view.findViewById<EditText>(R.id.edit_email)
            val email = editEmail.text.toString().trim()

            if (email.isNotEmpty()) {
                addContactToFirestore(email)
                editEmail.text.clear()
                toggleAddContactForm(false)
            } else {
                Toast.makeText(requireContext(), "Please fill in the email field", Toast.LENGTH_SHORT).show()
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

    private fun addContactToFirestore(email: String) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val userEmail = currentUser.email
            if (email == userEmail) {
                Toast.makeText(
                    requireContext(),
                    "You cannot add your own email as a contact!",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            // Cek jika email sudah ada di daftar kontak
            if (contacts.any { it.email == email }) {
                Toast.makeText(
                    requireContext(),
                    "This contact is already added!",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val userUid = currentUser.uid

            // Periksa keberadaan email di Firestore
            firestore.collection("emailDB").document(email)
                .collection("profiles").get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val document = querySnapshot.documents.first()
                        val name = document.getString("name") ?: "Unknown"
                        val phone = document.getString("number") ?: "N/A"
                        val profileImageURL = document.getString("profileImageURL") ?: ""

                        val contact = Contact(name, phone, email, profileImageURL)

                        // Tambahkan ke subkoleksi contacts di Firestore
                        firestore.collection("users").document(userUid)
                            .collection("contacts")
                            .add(contact)
                            .addOnSuccessListener {
                                // Perbarui daftar kontak lokal dan adapter
                                contacts.add(contact)
                                contactAdapter.updateContacts(contacts)
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
                        Toast.makeText(
                            requireContext(),
                            "Email not registered in emailDB!",
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
    }




    private fun loadContactsFromFirestore() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val userUid = currentUser.uid

            firestore.collection("users").document(userUid)
                .collection("contacts")
                .get()
                .addOnSuccessListener { result ->
                    contacts.clear()
                    for (document in result) {
                        val contact = document.toObject(Contact::class.java)
                        contacts.add(contact)
                    }
                    contactAdapter.updateContacts(contacts) // Perbarui data di adapter
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
    }


    inner class ContactAdapter(
        private var contacts: MutableList<Contact>,
        private val listener: (Contact) -> Unit
    ) : RecyclerView.Adapter<ContactViewHolder>() {
        private var filteredContacts: MutableList<Contact> = contacts.toMutableList()

        fun updateContacts(newContacts: List<Contact>) {
            contacts = newContacts.toMutableList()
            filteredContacts = contacts.toMutableList()
            notifyDataSetChanged()
        }

        fun filterList(searchQuery: String) {
            filteredContacts = contacts.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }.toMutableList()
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_contact_item, parent, false)
            return ContactViewHolder(view)
        }

        override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
            holder.bind(filteredContacts[position], listener) { contact ->
                deleteContact(contact, position)
            }
        }

        override fun getItemCount(): Int = filteredContacts.size

        private fun deleteContact(contact: Contact, position: Int) {
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                val userUid = currentUser.uid

                firestore.collection("users").document(userUid)
                    .collection("contacts")
                    .whereEqualTo("email", contact.email)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            val document = querySnapshot.documents.first()
                            firestore.collection("users").document(userUid)
                                .collection("contacts")
                                .document(document.id)
                                .delete()
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        requireContext(),
                                        "Contact deleted!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    contacts.removeAt(position)
                                    filteredContacts.removeAt(position)
                                    notifyItemRemoved(position)

                                    // Muat ulang kontak dari Firestore
                                    loadContactsFromFirestore()
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firestore", "Error deleting contact", e)
                                    Toast.makeText(
                                        requireContext(),
                                        "Error deleting contact. Try again.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Error finding contact to delete", e)
                        Toast.makeText(
                            requireContext(),
                            "Error deleting contact. Try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
    }


    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName = itemView.findViewById<TextView>(R.id.tv_name)
        private val tvEmail = itemView.findViewById<TextView>(R.id.tv_email)
        private val tvPhone = itemView.findViewById<TextView>(R.id.tv_phone)
        private val ivProfilePicture = itemView.findViewById<ImageView>(R.id.iv_profile_picture)
        private val btnDeleteContact = itemView.findViewById<ImageView>(R.id.btn_delete_contact)

        fun bind(contact: Contact, listener: (Contact) -> Unit, deleteListener: (Contact) -> Unit) {
            tvName.text = contact.name
            tvEmail.text = contact.email
            tvPhone.text = contact.phone

            // Load profile picture using Glide
            if (contact.profileImageURL.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(contact.profileImageURL)
                    .placeholder(R.drawable.ic_account)
                    .error(R.drawable.ic_account)
                    .circleCrop()
                    .into(ivProfilePicture)
            }

            itemView.setOnClickListener {
                if (tvEmail.visibility == View.GONE) {
                    tvEmail.visibility = View.VISIBLE
                    tvPhone.visibility = View.VISIBLE
                } else {
                    tvEmail.visibility = View.GONE
                    tvPhone.visibility = View.GONE
                }
                listener(contact)
            }

            btnDeleteContact.setOnClickListener {
                deleteListener(contact)
            }
        }
    }

}