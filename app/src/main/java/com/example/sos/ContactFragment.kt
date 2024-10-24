package com.example.sos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class Contact(val name: String, val phone: String, val email: String)

class ContactFragment : Fragment() {
    private lateinit var recyclerViewContacts: RecyclerView
    private lateinit var contactAdapter: ContactAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_contact, container, false)

        recyclerViewContacts = view.findViewById(R.id.rv_contacts)

        // Handle add contact button click
        val btnAddContact = view.findViewById<View>(R.id.btn_add_contact)
        btnAddContact.setOnClickListener {
            // Logic for adding a new contact (e.g., show a dialog or navigate to another screen)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Sample data
        val contacts = listOf(
            Contact("John Doe", "123-456-7890", "john@example.com"),
            Contact("Jane Doe", "987-654-3210", "jane@example.com")
        )

        // Set up adapter with contact list and click listener
        contactAdapter = ContactAdapter(contacts) { contact ->
            // Handle contact click event (e.g., show contact details)
            println("Contact clicked: ${contact.name}")
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

    // ContactAdapter is placed inside ContactFragment
    inner class ContactAdapter(private var contacts: List<Contact>, private val listener: (Contact) -> Unit) : RecyclerView.Adapter<ContactViewHolder>() {
        private var filteredContacts: List<Contact> = contacts

        fun filterList(searchQuery: String) {
            filteredContacts = contacts.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_contact_item, parent, false)
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
