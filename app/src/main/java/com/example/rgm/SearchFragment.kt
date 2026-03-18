package com.pushkar.RGM

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.pushkar.RGM.databinding.FragmentSearchBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val database: DatabaseReference = FirebaseUtils.database
    private lateinit var appDatabase: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        appDatabase = AppDatabase.getDatabase(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSearch.setOnClickListener {
            val searchUsername = binding.etSearchUsername.text.toString().trim()
            
            if (searchUsername.isEmpty()) {
                Toast.makeText(context, "Please enter a username", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Hide keyboard
            val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)

            searchUserInCloud(searchUsername)
        }
        
        binding.cvUserProfile.setOnClickListener {
            val username = binding.tvSearchUsernameLabel.text.toString().removePrefix("@")
            val bundle = Bundle().apply {
                putString("otherUser", username)
            }
            findNavController().navigate(R.id.action_SearchFragment_to_ChatFragment, bundle)
        }

        binding.btnAddFriend.setOnClickListener {
            val targetUser = binding.tvSearchUsernameLabel.text.toString().removePrefix("@")
            sendFriendRequestCloud(targetUser)
        }
    }

    private fun searchUserLocally(username: String) {
        binding.cvUserProfile.visibility = View.GONE
        binding.tvNoUserFound.visibility = View.GONE
        
        lifecycleScope.launch {
            val user = appDatabase.userDao().getUserByUsername(username)
            if (user != null) {
                binding.cvUserProfile.visibility = View.VISIBLE
                binding.tvSearchNickname.text = if (!user.nickname.isNullOrEmpty()) user.nickname else user.username
                binding.tvSearchUsernameLabel.text = getString(R.string.username_format, username)
                
                if (!user.profileImage.isNullOrEmpty()) {
                    try {
                        binding.ivSearchProfileImage.setImageURI(user.profileImage.toUri())
                    } catch (e: Exception) {
                        binding.ivSearchProfileImage.setImageResource(android.R.drawable.ic_menu_camera)
                    }
                } else {
                    binding.ivSearchProfileImage.setImageResource(android.R.drawable.ic_menu_camera)
                }
                
                checkFriendshipStatusLocally(username)
            } else {
                // Check legacy SharedPreferences
                val sharedPref = requireActivity().getSharedPreferences("PIEE_PREFS", Context.MODE_PRIVATE)
                if (sharedPref.contains(username)) {
                    binding.cvUserProfile.visibility = View.VISIBLE
                    val nickname = sharedPref.getString("${username}_nickname", username)
                    binding.tvSearchNickname.text = nickname
                    binding.tvSearchUsernameLabel.text = getString(R.string.username_format, username)
                    
                    val imageUriString = sharedPref.getString("${username}_image", null)
                    if (imageUriString != null) {
                        binding.ivSearchProfileImage.setImageURI(imageUriString.toUri())
                    }
                    checkFriendshipStatusLocally(username)
                } else {
                    binding.tvNoUserFound.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun searchUserInCloud(username: String) {
        binding.cvUserProfile.visibility = View.GONE
        binding.tvNoUserFound.visibility = View.GONE
        
        database.child("user_profiles").child(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    binding.cvUserProfile.visibility = View.VISIBLE
                    binding.tvSearchNickname.text = if (user.nickname?.isNotEmpty() == true) user.nickname else user.username
                    binding.tvSearchUsernameLabel.text = getString(R.string.username_format, user.username)
                    
                    if (user.profileImage?.isNotEmpty() == true) {
                        try {
                            binding.ivSearchProfileImage.setImageURI(user.profileImage.toUri())
                        } catch (e: Exception) {
                            binding.ivSearchProfileImage.setImageResource(android.R.drawable.ic_menu_camera)
                        }
                    } else {
                        binding.ivSearchProfileImage.setImageResource(android.R.drawable.ic_menu_camera)
                    }
                    
                    checkFriendshipStatus(username)
                } else {
                    // Try to search in root users just in case
                    database.child("users").child(username).get().addOnSuccessListener { coreSnapshot ->
                        if (coreSnapshot.exists()) {
                            binding.cvUserProfile.visibility = View.VISIBLE
                            binding.tvSearchNickname.text = username
                            binding.tvSearchUsernameLabel.text = getString(R.string.username_format, username)
                            binding.ivSearchProfileImage.setImageResource(android.R.drawable.ic_menu_camera)
                            checkFriendshipStatus(username)
                        } else {
                            searchUserLocally(username)
                        }
                    }.addOnFailureListener {
                        searchUserLocally(username)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                searchUserLocally(username)
            }
        })
    }

    private fun checkFriendshipStatusLocally(targetUser: String) {
        val sharedPref = requireActivity().getSharedPreferences("PIEE_PREFS", Context.MODE_PRIVATE)
        val currentUser = sharedPref.getString("CURRENT_USER", "") ?: ""

        if (targetUser == currentUser) {
            binding.btnAddFriend.visibility = View.GONE
            return
        }

        binding.btnAddFriend.visibility = View.VISIBLE
        binding.btnAddFriend.text = getString(R.string.add_friend)
        binding.btnAddFriend.isEnabled = true
    }

    private fun checkFriendshipStatus(targetUser: String) {
        val sharedPref = requireActivity().getSharedPreferences("PIEE_PREFS", Context.MODE_PRIVATE)
        val currentUser = sharedPref.getString("CURRENT_USER", "") ?: ""

        if (targetUser == currentUser) {
            binding.btnAddFriend.visibility = View.GONE
            return
        }

        binding.btnAddFriend.visibility = View.VISIBLE
        database.child("friend_requests").child(targetUser).child(currentUser)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        binding.btnAddFriend.text = getString(R.string.requested)
                        binding.btnAddFriend.isEnabled = false
                    } else {
                        database.child("friends").child(currentUser).child(targetUser)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(friendSnapshot: DataSnapshot) {
                                    if (friendSnapshot.exists()) {
                                        binding.btnAddFriend.text = getString(R.string.friends)
                                        binding.btnAddFriend.isEnabled = false
                                    } else {
                                        binding.btnAddFriend.text = getString(R.string.add_friend)
                                        binding.btnAddFriend.isEnabled = true
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    checkFriendshipStatusLocally(targetUser)
                }
            })
    }

    private fun sendFriendRequestCloud(targetUser: String) {
        val sharedPref = requireActivity().getSharedPreferences("PIEE_PREFS", Context.MODE_PRIVATE)
        val currentUser = sharedPref.getString("CURRENT_USER", "") ?: ""

        database.child("friend_requests").child(targetUser).child(currentUser).setValue(true)
            .addOnSuccessListener {
                Toast.makeText(context, "Friend Request Sent!", Toast.LENGTH_SHORT).show()
                binding.btnAddFriend.text = getString(R.string.requested)
                binding.btnAddFriend.isEnabled = false
            }.addOnFailureListener {
                Toast.makeText(context, "Failed to send request", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
