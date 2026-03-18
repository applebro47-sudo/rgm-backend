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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.pushkar.RGM.databinding.FragmentProfileCreateBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import java.util.*

class ProfileCreateFragment : Fragment() {

    private var _binding: FragmentProfileCreateBinding? = null
    private val binding get() = _binding!!
    private var selectedImageUri: Uri? = null
    private val database: DatabaseReference = FirebaseUtils.database
    private val storage = FirebaseStorage.getInstance().reference

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
                uploadPostMedia(uri)
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

        val sharedPref = requireActivity().getSharedPreferences("SYNAPSE_PREFS", Context.MODE_PRIVATE)
        val currentUser = sharedPref.getString("CURRENT_USER", "") ?: ""

        if (currentUser.isNotEmpty()) {
            val existingNickname = sharedPref.getString("${currentUser}_nickname", "") ?: ""
            binding.etNickname.setText(if (existingNickname.isEmpty()) currentUser else existingNickname)
            binding.etBirthday.setText(sharedPref.getString("${currentUser}_birthday", ""))
            binding.etComment.setText(sharedPref.getString("${currentUser}_comment", ""))
            
            val imageUriString = sharedPref.getString("${currentUser}_image", null)
            if (imageUriString != null) {
                binding.ivProfileImage.setImageURI(Uri.parse(imageUriString))
                binding.ivProfileImage.setPadding(0, 0, 0, 0)
            }
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

            binding.btnSaveProfile.isEnabled = false
            binding.btnSaveProfile.text = "Saving..."

            if (selectedImageUri != null) {
                uploadProfileImage(selectedImageUri!!, currentUser)
            } else {
                val currentImage = sharedPref.getString("${currentUser}_image", "") ?: ""
                saveProfileData(currentUser, nickname, currentImage)
            }
        }

        binding.logoutLink.setOnClickListener {
            sharedPref.edit { remove("CURRENT_USER") }
            findNavController().navigate(R.id.action_ProfileCreateFragment_to_FirstFragment)
        }
    }

    private fun uploadProfileImage(uri: Uri, username: String) {
        val ref = storage.child("profile_images/$username.jpg")
        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    saveProfileData(username, binding.etNickname.text.toString().trim(), downloadUri.toString())
                }
            }
            .addOnFailureListener {
                binding.btnSaveProfile.isEnabled = true
                binding.btnSaveProfile.text = "Save Profile"
                Toast.makeText(context, "Image upload failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveProfileData(currentUser: String, nickname: String, profileImageUrl: String) {
        val sharedPref = requireActivity().getSharedPreferences("SYNAPSE_PREFS", Context.MODE_PRIVATE)
        val birthday = binding.etBirthday.text.toString().trim()
        val comment = binding.etComment.text.toString().trim()

        lifecycleScope.launch {
            try {
                val userObj = User(
                    username = currentUser,
                    nickname = nickname,
                    birthday = birthday,
                    comment = comment,
                    profileImage = profileImageUrl
                )
                
                val response = RetrofitClient.instance.updateProfile(currentUser, userObj)
                if (response.isSuccessful) {
                    sharedPref.edit {
                        putString("${currentUser}_nickname", nickname)
                        putString("${currentUser}_birthday", birthday)
                        putString("${currentUser}_comment", comment)
                        putString("${currentUser}_image", profileImageUrl)
                        putBoolean("${currentUser}_profile_created", true)
                    }

                    database.child("user_profiles").child(currentUser).setValue(userObj)

                    Toast.makeText(context, "Profile Saved!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_ProfileCreateFragment_to_HomeFragment)
                } else {
                    binding.btnSaveProfile.isEnabled = true
                    binding.btnSaveProfile.text = "Save Profile"
                    Toast.makeText(context, "Server Error: Failed to save profile", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.btnSaveProfile.isEnabled = true
                binding.btnSaveProfile.text = "Save Profile"
                Toast.makeText(context, "Network Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadPostMedia(uri: Uri) {
        val currentUser = requireActivity().getSharedPreferences("SYNAPSE_PREFS", Context.MODE_PRIVATE)
            .getString("CURRENT_USER", "") ?: ""
        val type = requireActivity().contentResolver.getType(uri) ?: ""
        val isVideo = type.contains("video")
        val extension = if (isVideo) "mp4" else "jpg"
        val fileName = UUID.randomUUID().toString() + "." + extension
        
        val ref = storage.child("posts/$currentUser/$fileName")
        
        Toast.makeText(context, "Uploading media...", Toast.LENGTH_SHORT).show()
        
        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    val newPost = Post(
                        owner = currentUser, 
                        mediaUri = downloadUri.toString(), 
                        mediaType = if (isVideo) "VIDEO" else "IMAGE"
                    )
                    database.child("posts").push().setValue(newPost)
                    
                    // Also sync to MongoDB if you have a posts endpoint
                    lifecycleScope.launch {
                        try {
                            RetrofitClient.instance.createPost(newPost)
                        } catch (e: Exception) {}
                    }
                    
                    Toast.makeText(context, "Memory Shared!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to share memory", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
