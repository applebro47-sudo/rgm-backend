package com.pushkar.RGM

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.pushkar.RGM.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences("PIEE_PREFS", Context.MODE_PRIVATE)
        val currentUser = sharedPref.getString("CURRENT_USER", "") ?: ""

        if (currentUser.isNotEmpty()) {
            val imageUriString = sharedPref.getString("${currentUser}_image", null)
            if (imageUriString != null) {
                try {
                    val uri = Uri.parse(imageUriString)
                    binding.ivHomeProfileImage.setImageURI(uri)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            loadGlobalFeed(currentUser)
        } else {
            findNavController().navigate(R.id.action_HomeFragment_to_FirstFragment)
        }

        binding.cvHomeProfile.setOnClickListener {
            findNavController().navigate(R.id.action_HomeFragment_to_ProfileCreateFragment)
        }

        binding.navUpdates.setOnClickListener {
            findNavController().navigate(R.id.action_HomeFragment_to_UpdatesFragment)
        }
        binding.navSearch.setOnClickListener {
            findNavController().navigate(R.id.action_HomeFragment_to_SearchFragment)
        }
        binding.navReels.setOnClickListener {
            findNavController().navigate(R.id.action_HomeFragment_to_ReelsFragment)
        }
        binding.navChat.setOnClickListener {
            findNavController().navigate(R.id.action_HomeFragment_to_ChatListFragment)
        }
        
        binding.cvHomeProfile.setOnLongClickListener {
            sharedPref.edit { remove("CURRENT_USER") }
            findNavController().navigate(R.id.action_HomeFragment_to_FirstFragment)
            true
        }
    }

    private fun loadGlobalFeed(currentUser: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getPosts()
                if (response.isSuccessful) {
                    val allPosts = response.body() ?: emptyList()
                    updateUI(allPosts.reversed(), currentUser)
                } else {
                    updateUI(emptyList(), currentUser)
                }
            } catch (e: Exception) {
                updateUI(emptyList(), currentUser)
            }
        }
    }

    private fun updateUI(homePosts: List<Post>, currentUser: String) {
        if (!isAdded) return
        
        if (homePosts.isEmpty()) {
            binding.tvNoPosts.visibility = View.VISIBLE
            binding.tvNoPosts.text = "Be the first to share a memory!"
            binding.rvHomeFeed.visibility = View.GONE
        } else {
            binding.tvNoPosts.visibility = View.GONE
            binding.rvHomeFeed.visibility = View.VISIBLE
            
            val adapter = PostAdapter(homePosts, currentUser) {
                loadGlobalFeed(currentUser)
            }
            binding.rvHomeFeed.layoutManager = LinearLayoutManager(context)
            binding.rvHomeFeed.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
