package com.example.storage.screen

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.storage.R
import com.example.storage.databinding.FragmentChangePassBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth


class ChangePassFragment : Fragment() {
    private lateinit var binding: FragmentChangePassBinding
    private val firebaseAuth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChangePassBinding.inflate(inflater, container, false)

        binding.btnSubmit.setOnClickListener { handleChangePassword() }

        binding.arrowBack.setOnClickListener { handleBackLink() }

        return binding.root
    }

    private fun handleChangePassword() {
        binding.progressbar.visibility = View.VISIBLE
        val oldPass = binding.oldPassET.text.toString()
        val newPass = binding.newPassET.text.toString()
        val retypePass = binding.retypePassET.text.toString()

        if (newPass != retypePass) {
            binding.progressbar.visibility = View.GONE
            Toast.makeText(requireContext(), "Password does not match.", Toast.LENGTH_SHORT).show()
        } else {
            val user = firebaseAuth.currentUser
            if (user != null && user.email != null) {
                val credential = EmailAuthProvider
                    .getCredential(user.email!!, oldPass)
                user.reauthenticate(credential)
                    .addOnCompleteListener { response ->
                        if (response.isSuccessful) {
                            user.updatePassword(newPass)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        binding.progressbar.visibility = View.GONE
                                        Toast.makeText(requireContext(), "Password has been successfully reset", Toast.LENGTH_SHORT).show()
                                        parentFragmentManager.popBackStack()
                                    } else {
                                        binding.progressbar.visibility = View.GONE
                                        Toast.makeText(requireContext(), "Unexpected error", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        } else {
                            binding.progressbar.visibility = View.GONE
                            Toast.makeText(requireContext(), "Old password is incorrect", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                binding.progressbar.visibility = View.GONE
            }
        }
    }

    private fun handleBackLink() {
        val profile = ProfileFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, profile)
            .addToBackStack(null)
            .commit()
    }

}