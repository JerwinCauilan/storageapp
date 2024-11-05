package com.example.storage.screen

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.storage.R
import com.example.storage.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.squareup.picasso.Picasso


class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var userListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)

        displayData()

        binding.myAccount.setOnClickListener { handleMyAccountLink() }

        binding.security.setOnClickListener { handleSecurityLink() }

        binding.help.setOnClickListener { handleHelpLink() }

        return binding.root
    }

    private fun displayData() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            userListener = firestore.collection("users").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("ProfileFragment", "Error fetching data", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val name = snapshot.getString("name") ?: "N/A"
                        val contact = snapshot.getString("contact") ?: "N/A"
                        val profileImageUrl = snapshot.getString("photo")
                        binding.displayName.text = name
                        binding.displayNo.text = contact

                        if (profileImageUrl != null) {
                            Picasso.get().load(profileImageUrl).into(binding.profileImageView)
                        } else {
                            binding.profileImageView.setImageResource(R.drawable.default_avatar)
                        }
                    } else {
                        binding.displayName.text = "N/A"
                        binding.displayNo.text = "N/A"
                        binding.profileImageView.setImageResource(R.drawable.default_avatar)
                    }
                }
        }
    }

    private fun handleMyAccountLink() {
        val accountFragment = AccountFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, accountFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun handleSecurityLink() {
        val securityFragment = ChangePassFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, securityFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun handleHelpLink() {
        val helpFragment = HelpFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, helpFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userListener?.remove()
    }
}