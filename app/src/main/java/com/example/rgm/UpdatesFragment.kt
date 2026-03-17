package com.pushkar.RGM

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.pushkar.RGM.databinding.FragmentUpdatesBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*

class UpdatesFragment : Fragment() {

    private var _binding: FragmentUpdatesBinding? = null
    private val binding get() = _binding!!
    private var database: DatabaseReference? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdatesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            if (FirebaseApp.getApps(requireContext()).isNotEmpty()) {
                database = FirebaseDatabase.getInstance().reference
            }
        } catch (e: Exception) { e.printStackTrace() }

        val sharedPref = requireActivity().getSharedPreferences("PIEE_PREFS", Context.MODE_PRIVATE)
        val currentUser = sharedPref.getString("CURRENT_USER", "") ?: ""

        if (database != null && currentUser.isNotEmpty()) {
            loadFriendRequests(currentUser)
        } else {
            binding.tvNoUpdates.visibility = View.VISIBLE
        }
    }

    private fun loadFriendRequests(currentUser: String) {
        database?.child("friend_requests")?.child(currentUser)
            ?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val requestList = mutableListOf<String>()
                    snapshot.children.forEach { requestList.add(it.key ?: "") }

                    if (requestList.isEmpty()) {
                        binding.tvNoUpdates.visibility = View.VISIBLE
                        binding.rvUpdates.visibility = View.GONE
                    } else {
                        binding.tvNoUpdates.visibility = View.GONE
                        binding.rvUpdates.visibility = View.VISIBLE
                        
                        // Using a simple adapter to show requests
                        val adapter = UpdatesAdapter(requestList) { requester ->
                            acceptFriendRequest(currentUser, requester)
                        }
                        binding.rvUpdates.layoutManager = LinearLayoutManager(context)
                        binding.rvUpdates.adapter = adapter
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun acceptFriendRequest(currentUser: String, requester: String) {
        // Add to each other's friend list
        database?.child("friends")?.child(currentUser)?.child(requester)?.setValue(true)
        database?.child("friends")?.child(requester)?.child(currentUser)?.setValue(true)
        
        // Remove the request
        database?.child("friend_requests")?.child(currentUser)?.child(requester)?.removeValue()
            ?.addOnSuccessListener {
                Toast.makeText(context, "Friend Request Accepted!", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
