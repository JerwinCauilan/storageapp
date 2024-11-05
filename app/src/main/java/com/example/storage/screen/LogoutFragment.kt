package com.example.storage.screen

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class LogoutFragment : Fragment() {
    private val firebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth.signOut()
        Toast.makeText(activity, "You Are Now Signed Out", Toast.LENGTH_SHORT).show()
        startActivity(Intent(activity, LoginActivity::class.java))
        activity?.finish()
    }
}