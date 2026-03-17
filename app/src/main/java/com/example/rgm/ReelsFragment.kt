package com.pushkar.RGM

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.pushkar.RGM.databinding.FragmentReelsBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ReelsFragment : Fragment() {

    private var _binding: FragmentReelsBinding? = null
    private val binding get() = _binding!!
    private lateinit var currentUser: String
    private var database: DatabaseReference? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReelsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase safely
        try {
            if (FirebaseApp.getApps(requireContext()).isNotEmpty()) {
                database = FirebaseDatabase.getInstance().reference
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val sharedPref = requireActivity().getSharedPreferences("PIEE_PREFS", Context.MODE_PRIVATE)
        currentUser = sharedPref.getString("CURRENT_USER", "") ?: ""

        if (database != null) {
            loadReelsFromCloud()
        } else {
            binding.tvNoReels.visibility = View.VISIBLE
        }
    }

    private fun loadReelsFromCloud() {
        // Fetch ALL video posts from ALL users (like Instagram discovery)
        database?.child("posts")?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val reelPosts = mutableListOf<Post>()
                snapshot.children.forEach { child ->
                    val post = child.getValue(Post::class.java)
                    if (post != null && post.mediaType == "VIDEO") {
                        reelPosts.add(post)
                    }
                }

                updateUI(reelPosts.reversed())
            }

            override fun onCancelled(error: DatabaseError) {
                updateUI(emptyList())
            }
        })
    }

    private fun updateUI(reelPosts: List<Post>) {
        if (!isAdded) return
        
        if (reelPosts.isEmpty()) {
            binding.tvNoReels.visibility = View.VISIBLE
            binding.rvReels.visibility = View.GONE
        } else {
            binding.tvNoReels.visibility = View.GONE
            binding.rvReels.visibility = View.VISIBLE
            
            val adapter = ReelsAdapter(reelPosts)
            binding.rvReels.layoutManager = LinearLayoutManager(context)
            binding.rvReels.adapter = adapter
            
            // To make it feel like reels (snap to center)
            binding.rvReels.onFlingListener = null
            val snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(binding.rvReels)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
