package com.pushkar.RGM

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pushkar.RGM.databinding.FragmentProfileCreateBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class ProfileCreateFragment : Fragment() {

    private var _binding: FragmentProfileCreateBinding? = null
    private val binding get() = _binding!!
    private var selectedImageUri: Uri? = null
    private var database: DatabaseReference? = null
    private val DB_URL = "https://pushkar2b-default-rtdb.firebaseio.com/"

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            selectedImageUri = data?.data
            if (selectedImageUri != null) {
                binding.ivProfileImage.setPadding(0, 0, 0, 0)
                binding.ivProfileImage.setImageURI(selectedImageUri)
            }
        }
    }

    private val pickPostLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                handleNewPost(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileCreateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            if (FirebaseApp.getApps(requireContext()).isNotEmpty()) {
                database = FirebaseDatabase.getInstance(DB_URL).reference
            }
        } catch (e: Exception) { e.printStackTrace() }

        val sharedPref = requireActivity().getSharedPreferences("PIEE_PREFS", Context.MODE_PRIVATE)
        val currentUser = sharedPref.getString("CURRENT_USER", "") ?: ""

        if (currentUser.isNotEmpty()) {
            val existingNickname = sharedPref.getString("${currentUser}_nickname", "") ?: ""
            binding.etNickname.setText(if (existingNickname.isEmpty()) currentUser else existingNickname)
            binding.etBirthday.setText(sharedPref.getString("${currentUser}_birthday", ""))
            binding.etComment.setText(sharedPref.getString("${currentUser}_comment", ""))
        }

        binding.profileImageContainer.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            pickImageLauncher.launch(intent)
        }

        binding.btnNewPost.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
            }
            pickPostLauncher.launch(intent)
        }

        binding.btnSaveProfile.setOnClickListener {
            val nickname = binding.etNickname.text.toString().trim()
            if (nickname.isEmpty()) {
                binding.etNickname.error = "Nickname is required"
                return@setOnClickListener
            }

            // Save details locally
            sharedPref.edit {
                putString("${currentUser}_nickname", nickname)
                putString("${currentUser}_birthday", binding.etBirthday.text.toString().trim())
                putString("${currentUser}_comment", binding.etComment.text.toString().trim())
                if (selectedImageUri != null) {
                    putString("${currentUser}_image", selectedImageUri.toString())
                }
                putBoolean("${currentUser}_profile_created", true)
            }

            // Sync to Firebase Cloud
            if (database != null) {
                val userObj = User(
                    username = currentUser,
                    nickname = nickname,
                    birthday = binding.etBirthday.text.toString().trim(),
                    comment = binding.etComment.text.toString().trim(),
                    profileImage = selectedImageUri?.toString() ?: ""
                )
                database?.child("user_profiles")?.child(currentUser)?.setValue(userObj)
            }

            Toast.makeText(context, "Profile Saved!", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_ProfileCreateFragment_to_HomeFragment)
        }

        binding.logoutLink.setOnClickListener {
            sharedPref.edit { remove("CURRENT_USER") }
            findNavController().navigate(R.id.action_ProfileCreateFragment_to_FirstFragment)
        }
    }

    private fun handleNewPost(uri: Uri) {
        val type = requireActivity().contentResolver.getType(uri) ?: ""
        val currentUser = requireActivity().getSharedPreferences("PIEE_PREFS", Context.MODE_PRIVATE)
            .getString("CURRENT_USER", "") ?: ""
        val newPost = Post(owner = currentUser, mediaUri = uri.toString(), mediaType = if (type.contains("video")) "VIDEO" else "IMAGE")
        database?.child("posts")?.push()?.setValue(newPost)
        Toast.makeText(context, "Memory Shared!", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
