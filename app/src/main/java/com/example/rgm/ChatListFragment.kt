package com.pushkar.RGM

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.pushkar.RGM.databinding.FragmentChatListBinding
import kotlinx.coroutines.launch

class ChatListFragment : Fragment() {

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!
    private lateinit var currentUser: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences("PIEE_PREFS", Context.MODE_PRIVATE)
        currentUser = sharedPref.getString("CURRENT_USER", "") ?: ""

        fetchChattedUsers()
        fetchSuggestions()
    }

    private fun fetchChattedUsers() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getChattedUsers(currentUser)
                if (response.isSuccessful) {
                    updateChatListUI(response.body() ?: emptyList())
                } else {
                    updateChatListUI(emptyList())
                }
            } catch (e: Exception) {
                updateChatListUI(emptyList())
            }
        }
    }

    private fun fetchSuggestions() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getAllUsers()
                if (response.isSuccessful) {
                    val allUsers = response.body() ?: emptyList()
                    val suggestions = allUsers.filter { it.username != currentUser }
                    updateSuggestionsUI(suggestions)
                } else {
                    updateSuggestionsUI(emptyList())
                }
            } catch (e: Exception) {
                updateSuggestionsUI(emptyList())
            }
        }
    }

    private fun updateSuggestionsUI(suggestions: List<User>) {
        if (!isAdded) return
        
        // Always add the AI Assistant as the first suggestion
        val finalSuggestions = mutableListOf<User>()
        finalSuggestions.add(User(username = "AI Assistant", nickname = "Smart AI Assistant", profileImage = ""))
        finalSuggestions.addAll(suggestions)

        binding.tvSuggestionsTitle.visibility = View.VISIBLE
        binding.rvSuggestions.visibility = View.VISIBLE
        binding.divider.visibility = View.VISIBLE
        
        val adapter = SuggestionAdapter(finalSuggestions) { otherUser ->
            openChat(otherUser)
        }
        binding.rvSuggestions.adapter = adapter
    }

    private fun updateChatListUI(chattedUsers: List<String>) {
        if (!isAdded) return
        
        if (chattedUsers.isEmpty()) {
            binding.tvNoChats.visibility = View.VISIBLE
            binding.rvChatList.visibility = View.GONE
        } else {
            binding.tvNoChats.visibility = View.GONE
            binding.rvChatList.visibility = View.VISIBLE
            
            val adapter = ChatListAdapter(chattedUsers, currentUser) { otherUser ->
                openChat(otherUser)
            }
            binding.rvChatList.layoutManager = LinearLayoutManager(context)
            binding.rvChatList.adapter = adapter
        }
    }

    private fun openChat(otherUser: String) {
        val bundle = Bundle().apply {
            putString("otherUser", otherUser)
        }
        findNavController().navigate(R.id.action_ChatListFragment_to_ChatFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
