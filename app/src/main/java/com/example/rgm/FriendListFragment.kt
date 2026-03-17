package com.pushkar.RGM

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.pushkar.RGM.databinding.FragmentFriendListBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FriendListFragment : Fragment() {

    private var _binding: FragmentFriendListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences("PIEE_PREFS", Context.MODE_PRIVATE)
        val currentUser = sharedPref.getString("CURRENT_USER", "") ?: ""

        val friendsJson = sharedPref.getString("friends_$currentUser", "[]")
        val type = object : TypeToken<List<String>>() {}.type
        val friends: List<String> = Gson().fromJson(friendsJson, type)

        if (friends.isEmpty()) {
            binding.tvNoFriends.visibility = View.VISIBLE
            binding.rvFriendList.visibility = View.GONE
        } else {
            binding.tvNoFriends.visibility = View.GONE
            binding.rvFriendList.visibility = View.VISIBLE
            
            val adapter = FriendAdapter(friends)
            binding.rvFriendList.layoutManager = LinearLayoutManager(context)
            binding.rvFriendList.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
