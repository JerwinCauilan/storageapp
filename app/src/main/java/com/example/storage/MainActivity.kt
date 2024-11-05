package com.example.storage

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.storage.databinding.ActivityMainBinding
import com.example.storage.screen.HomeFragment
import com.example.storage.screen.LogoutFragment
import com.example.storage.screen.ProfileFragment
import com.example.storage.screen.StorageFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navbar()
        displayFragment(HomeFragment())
    }

    private fun navbar() {
        binding.bottomNavigationView.background =null
        binding.bottomNavigationView.selectedItemId = R.id.navigation_home

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    displayFragment(HomeFragment())
                    true
                }
                R.id.navigation_storage -> {
                    displayFragment(StorageFragment())
                    true
                }
                R.id.navigation_profile -> {
                    displayFragment(ProfileFragment())
                    true
                }
                R.id.navigation_logout -> {
                    displayFragment(LogoutFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun displayFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}