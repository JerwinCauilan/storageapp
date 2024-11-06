package com.example.storage.screen

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.example.storage.R
import com.example.storage.databinding.ActivitySignUpBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isPassVisible: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.passLayout.setEndIconOnClickListener {
            togglePass()
        }
        binding.btnRegister.setOnClickListener {
            handleSignUp()
        }
        binding.signInLink.setOnClickListener {
            handleSignInLink()
        }
    }

    private fun handleSignUp() {
        binding.progressbar.visibility = View.VISIBLE
        val name = binding.nameET.text.toString().trim()
        val email = binding.emailET.text.toString().trim()
        val pass = binding.passET.text.toString().trim()

        if(name.isEmpty()) {
            binding.nameET.error = "Name is required!"
            binding.nameET.requestFocus()
            binding.progressbar.visibility = View.GONE
            return
        }
        if(email.isEmpty()) {
            binding.emailET.error = "Email is required!"
            binding.emailET.requestFocus()
            binding.progressbar.visibility = View.GONE
            return
        }
        if(pass.isEmpty()) {
            binding.passET.error = "Password is required!"
            binding.passET.requestFocus()
            binding.progressbar.visibility = View.GONE
            return
        }
        createGranted(name, email, pass)
    }

    private fun createGranted(name: String, email: String, pass: String) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid
                    val timestamp = System.currentTimeMillis()

                    val data = hashMapOf(
                        "name" to name,
                        "email" to email,
                        "timestamp" to timestamp,
                    )
                    if (uid != null) {
                        db.collection("users").document(uid).set(data)
                            .addOnSuccessListener {
                                binding.progressbar.visibility = View.GONE
                                Toast.makeText(
                                    this,
                                    "Your account has been created",
                                    Toast.LENGTH_SHORT
                                ).show()
                                val intent = Intent(this, LoginActivity::class.java)
                                startActivity(intent)
                            }
                            .addOnFailureListener { e ->
                                binding.progressbar.visibility = View.GONE
                                Toast.makeText(this, "Unexpected error", Toast.LENGTH_SHORT)
                                    .show()
                            }
                    }
                } else {
                    binding.progressbar.visibility = View.GONE
                    Toast.makeText(this, "${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun togglePass() {
        if (isPassVisible) {
            binding.passET.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.passLayout.endIconDrawable = AppCompatResources.getDrawable(this, R.drawable.visibility_off)
        } else {
            binding.passET.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.passLayout.endIconDrawable = AppCompatResources.getDrawable(this, R.drawable.visibility)
        }
        binding.passET.text?.let {
            binding.passET.setSelection(it.length)
        }
        isPassVisible = !isPassVisible
    }

    private fun handleSignInLink() {
        startActivity(Intent(this, LoginActivity::class.java))
    }
}