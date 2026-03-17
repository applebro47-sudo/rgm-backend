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
import com.pushkar.RGM.databinding.FragmentSecondBinding
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException

class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding

    private lateinit var appDatabase: AppDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        appDatabase = AppDatabase.getDatabase(requireContext())
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.loginLink?.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }

        binding?.signupButton?.setOnClickListener {
            val username = binding?.signupUsername?.text.toString().trim()
            val pass = binding?.signupPassword?.text.toString().trim()

            if (username.isEmpty() || pass.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding?.signupButton?.isEnabled = false
            
            // Use viewLifecycleOwner to ensure the coroutine is tied to the View's lifecycle
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    Log.d("SignUp", "Attempting registration at: ${RetrofitClient.BASE_URL}")
                    val response = RetrofitClient.instance.register(User(username = username, password = pass))
                    
                    if (response.isSuccessful) {
                        // Save to local Room DB
                        appDatabase.userDao().insertUser(UserEntity(username, pass, nickname = username))
                        
                        val sharedPref = requireActivity().getSharedPreferences("PIEE_PREFS", Context.MODE_PRIVATE)
                        sharedPref.edit {
                            putString("CURRENT_USER", username)
                            putBoolean("${username}_profile_created", false)
                        }
                        
                        Toast.makeText(context, "Successfully created account!", Toast.LENGTH_LONG).show()
                        findNavController().navigate(R.id.action_SecondFragment_to_ProfileCreateFragment)
                    } else {
                        binding?.signupButton?.isEnabled = true
                        val errorBody = response.errorBody()?.string()
                        Log.e("SignUp", "Server returned error: $errorBody")
                        val msg = if (response.code() == 400) "Username already exists" else "Server Error: ${response.code()}"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    binding?.signupButton?.isEnabled = true
                    Log.e("SignUp", "Network failure", e)
                    
                    val errorMsg = when (e) {
                        is ConnectException -> "Cannot reach server. If using a real phone, run 'adb reverse tcp:3000 tcp:3000' in your Terminal."
                        is SocketTimeoutException -> "Connection timed out. Is your Node.js server running?"
                        else -> "Error: ${e.localizedMessage}"
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
