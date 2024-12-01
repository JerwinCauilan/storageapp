package com.example.storage.screen

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.storage.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val firebaseAuth = FirebaseAuth.getInstance()
    private lateinit var db: DatabaseReference


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        db = FirebaseDatabase.getInstance().reference

        displayTemp()
        setupButtonListeners()

        return binding.root
    }

    private fun displayTemp() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            val listen = object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val temp = snapshot.child("chiller/temperature").value.toString()
                    binding.tempDisplay.text = "$temp°C"
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Something went wrong..", Toast.LENGTH_SHORT).show()
                }
            }
            db.addValueEventListener(listen)
        }
    }

    private fun setupButtonListeners() {
        binding.btnOne.setOnClickListener { changeLevel("1") }
        binding.btnTwo.setOnClickListener { changeLevel("2") }
        binding.btnThree.setOnClickListener { changeLevel("3") }
        binding.btnFour.setOnClickListener { changeLevel("4") }
        binding.btnFive.setOnClickListener { changeLevel("5") }
        binding.btnDefrost.setOnClickListener { changeLevel("AD") }
        binding.btnPS.setOnClickListener { changeLevel("3") }
    }

    private fun changeLevel(newLevel: String) {
        val chillerRef = db.child("chiller/level")
        chillerRef.setValue(newLevel).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.e("db", "Successfully")
            }
        }.addOnFailureListener { exception ->
            Log.e("db", "Failed to set level $newLevel.", exception)
        }
    }
}
