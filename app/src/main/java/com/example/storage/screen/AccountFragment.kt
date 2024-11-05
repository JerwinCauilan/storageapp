package com.example.storage.screen

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.storage.R
import com.example.storage.databinding.FragmentAccountBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import java.util.UUID


class AccountFragment : Fragment() {
    private lateinit var binding: FragmentAccountBinding
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val storageReference: StorageReference = storage.reference
    private val PICK_IMAGE_REQUEST = 1
    private var imageUri: Uri? = null
    private var userListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAccountBinding.inflate(inflater, container, false)

        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            val docRef = firestore.collection("users").document(userId)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        binding.nameET.setText(document.getString("name") ?: "N/A")
                        binding.emailET.setText(document.getString("email") ?: "N/A")
                        binding.contactET.setText(document.getString("contact") ?: "N/A")
                        val photoUrl = document.getString("photo")
                        if (!photoUrl.isNullOrEmpty()) {
                            Picasso.get().load(photoUrl).into(binding.profileImageView)
                        } else {
                            binding.profileImageView.setImageResource(R.drawable.default_avatar)
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Unexpected error", Toast.LENGTH_SHORT).show()
                }
        }

        binding.btnSave.setOnClickListener {
            binding.progressbar.visibility = View.VISIBLE
            val name = binding.nameET.text.toString()
            val email = binding.emailET.text.toString()
            val contact = binding.contactET.text.toString()

            if(validation(name, email, contact)) {
                if (userId != null) {
                    val updateMap = mapOf(
                        "name" to name,
                        "email" to email,
                        "contact" to contact,
                    )

                    firestore.collection("users").document(userId).update(updateMap)
                        .addOnSuccessListener {
                            binding.progressbar.visibility = View.GONE
                            Toast.makeText(requireContext(), "Successfully Updated!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            binding.progressbar.visibility = View.GONE
                            Toast.makeText(requireContext(), "Update Failed.", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    binding.progressbar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Unexpected error", Toast.LENGTH_SHORT).show()
                }
            } else {
                binding.progressbar.visibility = View.GONE
            }
        }

        binding.profileImageView.setOnClickListener { openGallery() }

        binding.arrowBack.setOnClickListener { handleBackLink() }

        return binding.root
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            if (imageUri != null) {
                binding.progressbar.visibility = View.VISIBLE
                deleteOldPhoto()
            }
        }
    }

    private fun deleteOldPhoto() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { snapshot ->
                val oldPhoto = snapshot.getString("photo")
                if (oldPhoto != null) {
                    val oldPhotoRef = storage.getReferenceFromUrl(oldPhoto)
                    oldPhotoRef.delete().addOnSuccessListener {
                        Log.d("AccountFragment", "Old photo deleted")
                        newPhoto()
                    }.addOnFailureListener { error ->
                        Log.e("AccountFragment", "Failed to delete old photo", error)
                        newPhoto()
                    }
                } else {
                    newPhoto()
                }
            }
            .addOnFailureListener { error ->
                Log.e("AccountFragment", "Failed to fetch old photo url", error)
                newPhoto()
            }
    }

    private fun newPhoto() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        val fileReference = storageReference.child("profile/$userId/${UUID.randomUUID()}.jpg")

        fileReference.putFile(imageUri!!)
            .addOnSuccessListener {
                fileReference.downloadUrl.addOnSuccessListener { uri ->
                    firestore.collection("users").document(userId)
                        .update("photo", uri.toString())
                        .addOnSuccessListener {
                            Log.d("AccountFragment", "Photo URL updated")
                            Picasso.get().load(uri).into(binding.profileImageView)
                            binding.progressbar.visibility = View.GONE
                            Toast.makeText(requireContext(), "Photo updated!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { error ->
                            Log.e("AccountFragment", "Error updating photo URL", error)
                            binding.progressbar.visibility = View.GONE
                            Toast.makeText(requireContext(), "Unexpected error", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { error ->
                Log.e("AccountFragment", "Upload failed: ${error.message}", error)
                binding.progressbar.visibility = View.GONE
                Toast.makeText(requireContext(), "$error", Toast.LENGTH_SHORT).show()
            }
    }

    private fun validation(name: String, email: String, contact: String): Boolean {
        if(name.isEmpty()) {
            binding.nameET.error = "Name is required"
            binding.nameET.requestFocus()
            return false
        }

        if(email.isEmpty()) {
            binding.emailET.error = "Email is required"
            binding.emailET.requestFocus()
            return false
        }

        if(contact.isEmpty()) {
            binding.contactET.error = "Contact is required"
            binding.contactET.requestFocus()
            return false
        }

        return true
    }

    private fun handleBackLink() {
        val profile = ProfileFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, profile)
            .addToBackStack(null)
            .commit()
    }

}