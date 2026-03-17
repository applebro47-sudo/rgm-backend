package com.pushkar.RGM

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.pushkar.RGM.databinding.FragmentFirstBinding
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private lateinit var appDatabase: AppDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        appDatabase = AppDatabase.getDatabase(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.signupLink.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        binding.loginButton.setOnClickListener {
            val identifier = binding.username.text.toString().trim()
            val pass = binding.password.text.toString().trim()

            if (identifier.isEmpty() || pass.isEmpty()) {
                Toast.makeText(context, "Please enter username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Admin bypass
            if (identifier == "7879532096" && pass == "1234") {
                val sharedPref = requireActivity().getSharedPreferences("PIEE_PREFS", Context.MODE_PRIVATE)
                sharedPref.edit { 
                    putString("CURRENT_USER", "Admin")
                    putBoolean("Admin_profile_created", true)
                }
                Toast.makeText(context, "Admin account successfully login", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_FirstFragment_to_AdminFragment)
                return@setOnClickListener
            }

            binding.loginButton.isEnabled = false
            
            lifecycleScope.launch {
                try {
                    Log.d("Login", "Connecting to: ${RetrofitClient.BASE_URL}")
                    val response = RetrofitClient.instance.login(User(username = identifier, password = pass))
                    binding.loginButton.isEnabled = true
                    
                    if (response.isSuccessful) {
                        val user = response.body()
                        if (user != null) {
                            val sharedPref = requireActivity().getSharedPreferences("PIEE_PREFS", Context.MODE_PRIVATE)
                            sharedPref.edit { putString("CURRENT_USER", identifier) }

                            // Sync user data to local Room database
                            appDatabase.userDao().insertUser(UserEntity(
                                username = identifier,
                                password = pass,
                                nickname = user.nickname ?: identifier,
                                profileImage = user.profileImage ?: "",
                                isProfileCreated = true
                            ))

                            Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                            
                            val isProfileCreated = sharedPref.getBoolean("${identifier}_profile_created", false)
                            if (isProfileCreated) {
                                findNavController().navigate(R.id.action_FirstFragment_to_HomeFragment)
                            } else {
                                findNavController().navigate(R.id.action_FirstFragment_to_ProfileCreateFragment)
                            }
                        }
                    } else {
                        Log.e("Login", "Auth Failed: ${response.code()}")
                        val msg = if (response.code() == 401) "Invalid username or password" else "Server Error: ${response.code()}"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    binding.loginButton.isEnabled = true
                    Log.e("Login", "Network Error", e)
                    
                    val errorMsg = when (e) {
                        is ConnectException -> "Cannot reach server. Run 'adb reverse tcp:3000 tcp:3000' in Terminal."
                        is SocketTimeoutException -> "Connection timed out. Check if your Node server is running."
                        else -> "Connection Error: ${e.localizedMessage ?: "Unknown error"}"
                    }
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
