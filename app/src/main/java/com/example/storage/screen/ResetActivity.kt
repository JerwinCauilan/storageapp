package com.example.storage.screen

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.storage.databinding.ActivityResetBinding
import com.google.firebase.auth.FirebaseAuth

class ResetActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResetBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()

        binding.btnReset.setOnClickListener {
            handleReset()
        }
        binding.arrowBack.setOnClickListener {
            handleSignInLink()
        }
    }

    private fun handleReset() {
        binding.progressbar.visibility = View.VISIBLE
        val email = binding.emailET.text.toString()

        if(email.isEmpty()) {
            binding.emailET.error = "Email is required!"
            binding.emailET.requestFocus()
            binding.progressbar.visibility = View.GONE
            return
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailET.error = "Invalid email address!"
            binding.emailET.requestFocus()
            binding.progressbar.visibility = View.GONE
            return
        }

        firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener { response ->
            binding.progressbar.visibility = View.GONE
            if(response.isSuccessful) {
                startActivity(Intent(this, LoginActivity::class.java))
                Toast.makeText(this, "You’ve reset your password! Look for an email to proceed.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, response.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleSignInLink() {
        startActivity(Intent(this, LoginActivity::class.java))
    }
}