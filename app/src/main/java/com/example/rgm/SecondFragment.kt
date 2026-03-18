package com.pushkar.RGM

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.pushkar.RGM.databinding.FragmentSecondBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException

class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    private lateinit var appDatabase: AppDatabase
    private var checkUsernameJob: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        appDatabase = AppDatabase.getDatabase(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Navigate back to Login
        binding.loginLink.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }

        // Real-time username availability check
        binding.signupUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkUsernameJob?.cancel()
                val username = s.toString().trim()
                if (username.length >= 3) {
                    checkUsernameJob = viewLifecycleOwner.lifecycleScope.launch {
                        delay(500) // Debounce for 500ms
                        try {
                            val response = RetrofitClient.instance.checkUsername(username)
                            if (response.isSuccessful) {
                                val available = response.body()?.get("available") ?: false
                                if (available) {
                                    binding.signupUsername.error = null
                                } else {
                                    binding.signupUsername.error = "Username already taken"
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("SignUp", "Check username failed", e)
                        }
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.signupButton.setOnClickListener {
            val username = binding.signupUsername.text.toString().trim()
            val pass = binding.signupPassword.text.toString().trim()

            // Basic Validation
            if (username.isEmpty() || pass.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass.length < 4) {
                Toast.makeText(context, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (binding.signupUsername.error != null) {
                Toast.makeText(context, "Please choose a different username", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable button to prevent multiple clicks
            binding.signupButton.isEnabled = false
            binding.signupButton.text = "Creating Account..."

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    Log.d("SignUp", "Attempting registration for: $username at ${RetrofitClient.BASE_URL}")
                    
                    val userRequest = User(username = username, password = pass)
                    val response = RetrofitClient.instance.register(userRequest)
                    
                    if (response.isSuccessful) {
                        // 1. Save to local Room Database for offline access
                        appDatabase.userDao().insertUser(UserEntity(
                            username = username, 
                            password = pass, 
                            nickname = username,
                            isProfileCreated = false
                        ))
                        
                        // 2. Save current user session in SharedPreferences
                        val sharedPref = requireActivity().getSharedPreferences("PIEE_PREFS", Context.MODE_PRIVATE)
                        sharedPref.edit {
                            putString("CURRENT_USER", username)
                            putBoolean("${username}_profile_created", false)
                        }
                        
                        Toast.makeText(context, "Successfully created account!", Toast.LENGTH_LONG).show()
                        
                        // 3. Navigate to Profile Creation
                        findNavController().navigate(R.id.action_SecondFragment_to_ProfileCreateFragment)
                    } else {
                        // Server error (e.g., 400 Conflict)
                        binding.signupButton.isEnabled = true
                        binding.signupButton.text = "Sign Up"
                        
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = try {
                            // Try to extract the "error" message from our Node.js backend JSON
                            JSONObject(errorBody ?: "").getString("error")
                        } catch (e: Exception) {
                            "Registration failed (Code: ${response.code()})"
                        }
                        
                        Log.e("SignUp", "Server Error: $errorMessage")
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    binding.signupButton.isEnabled = true
                    binding.signupButton.text = "Sign Up"
                    
                    Log.e("SignUp", "Network failure", e)
                    
                    val userFriendlyError = when (e) {
                        is ConnectException -> "Server is sleeping or unreachable. Please try again in a moment."
                        is SocketTimeoutException -> "Connection timed out. Check your internet."
                        else -> "Connection Error: ${e.localizedMessage}"
                    }
                    Toast.makeText(context, userFriendlyError, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
