package com.pushkar.RGM

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.pushkar.RGM.R
import com.pushkar.RGM.databinding.FragmentAdminBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class AdminFragment : Fragment() {

    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        fetchUserListFromBackend()

        binding.btnGoHome.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_AdminFragment_to_HomeFragment)
            } catch (e: Exception) {
                Toast.makeText(context, "Navigation to Home failed", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnGoReels.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_AdminFragment_to_ReelsFragment)
            } catch (e: Exception) {
                Toast.makeText(context, "Navigation to Reels failed", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnGoSearch.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_AdminFragment_to_SearchFragment)
            } catch (e: Exception) {
                Toast.makeText(context, "Navigation to Search failed", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnGoChat.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_AdminFragment_to_ChatListFragment)
            } catch (e: Exception) {
                Toast.makeText(context, "Navigation to Chat failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchUserListLocally() {
        val sharedPref = requireActivity().getSharedPreferences("PIEE_PREFS", Context.MODE_PRIVATE)
        val userListJson = sharedPref.getString("LOCAL_USER_LIST", "[]")
        val type = object : TypeToken<List<String>>() {}.type
        val userList: List<String> = Gson().fromJson(userListJson, type)
        
        updateUI(userList)
    }

    private fun fetchUserListFromBackend() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getAllUsers()
                if (response.isSuccessful) {
                    val users = response.body() ?: emptyList()
                    val usernames = users.map { it.username }
                    updateUI(usernames)
                    
                    val sharedPref = requireActivity().getSharedPreferences("PIEE_PREFS", Context.MODE_PRIVATE)
                    sharedPref.edit().putString("LOCAL_USER_LIST", Gson().toJson(usernames)).apply()
                } else {
                    fetchUserListLocally()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Connection Error: ${e.message}", Toast.LENGTH_SHORT).show()
                fetchUserListLocally()
            }
        }
    }

    private fun updateUI(userList: List<String>) {
        if (!isAdded) return
        binding.tvTotalUsers.text = "Total Users: ${userList.size}"
        val adapter = AdminUserAdapter(userList)
        binding.rvAdminUserList.layoutManager = LinearLayoutManager(context)
        binding.rvAdminUserList.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
