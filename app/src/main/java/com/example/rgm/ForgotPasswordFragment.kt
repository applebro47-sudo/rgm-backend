package com.example.rgm

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pushkar.RGM.databinding.FragmentForgotPasswordBinding

class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!
    private var generatedOtp: String? = null
    private var targetUser: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSendOtp.setOnClickListener {
            val identifier = binding.etPhoneOrUsername.text.toString().trim()
            if (identifier.isEmpty()) {
                Toast.makeText(context, "Please enter username or phone", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sharedPref = requireActivity().getSharedPreferences("PIEE_PREFS", Context.MODE_PRIVATE)
            if (!sharedPref.contains(identifier)) {
                Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            targetUser = identifier
            // Simulate sending OTP (since we don't have a real SMS gateway)
            generatedOtp = (1000..9999).random().toString()
            
            Toast.makeText(context, "OTP Sent to your mobile number!", Toast.LENGTH_LONG).show()
            
            // Show OTP for demo purposes since we can't send real SMS
            Handler(Looper.getMainLooper()).postDelayed({
                Toast.makeText(context, "DEMO OTP: $generatedOtp", Toast.LENGTH_LONG).show()
            }, 1000)

            binding.otpContainer.visibility = View.VISIBLE
            binding.btnSendOtp.visibility = View.GONE
            binding.etPhoneOrUsername.isEnabled = false
        }

        binding.btnResetPassword.setOnClickListener {
            val enteredOtp = binding.etOtp.text.toString().trim()
            val newPass = binding.etNewPassword.text.toString().trim()

            if (enteredOtp != generatedOtp) {
                Toast.makeText(context, "Invalid OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass.length < 4) {
                Toast.makeText(context, "Password too short", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sharedPref = requireActivity().getSharedPreferences("PIEE_PREFS", Context.MODE_PRIVATE)
            sharedPref.edit {
                putString(targetUser, newPass)
            }

            Toast.makeText(context, "Password reset successful!", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}