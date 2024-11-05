package com.example.storage.screen

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.example.storage.MainActivity
import com.example.storage.R
import com.example.storage.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var isPassVisible: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.passLayout.setEndIconOnClickListener {
            togglePass()
        }
        binding.btnSignIn.setOnClickListener {
            handleSignIn()
        }
        binding.btnForgotPass.setOnClickListener {
            handleReset()
        }
        binding.signupLink.setOnClickListener {
            handleSignUpLink()
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

    private fun handleSignIn() {
        binding.progressbar.visibility = View.VISIBLE
        val email = binding.emailET.text.toString()
        val password = binding.passET.text.toString()

        if (email.isEmpty()) {
            binding.emailET.error = "Email is required!"
            binding.emailET.requestFocus()
            binding.progressbar.visibility = View.GONE
            return
        }
        if (password.isEmpty()) {
            binding.passET.error = "Password is required!"
            binding.passET.requestFocus()
            binding.progressbar.visibility = View.GONE
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailET.error = "Invalid email address!"
            binding.emailET.requestFocus()
            binding.progressbar.visibility = View.GONE
            return
        }

        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { response ->
            binding.progressbar.visibility = View.GONE
            if(response.isSuccessful) {
                Toast.makeText(this, "Logged in successfully", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                finish()
            } else {
                Toast.makeText(this, "${response.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleReset() {
        startActivity(Intent(this, ResetActivity::class.java))
    }

    private fun handleSignUpLink() {
        startActivity(Intent(this, SignUpActivity::class.java))
    }
}