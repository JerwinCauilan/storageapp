package com.example.storage.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.storage.BluetoothManager
import com.example.storage.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    companion object {
        private const val TAG = "HomeFragment"
        private const val REQUEST_BLUETOOTH_PERMISSIONS = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        binding.gauge.maxSpeed = 100f
        binding.gauge.speedTo(0f)

        if (checkBluetoothPermissions()) {
            if (BluetoothManager.connectToArduino(requireContext())) {
                if (BluetoothManager.isConnected()) {
                    showToast("Connected to Arduino")
                    readFromArduino()
                } else {
                    showToast("Failed to connect to Arduino")
                }
            } else {
                showToast("Failed to connect to Arduino")
            }
        }

        setupButtonListeners()
        return binding.root
    }

    private fun setupButtonListeners() {
        binding.btnOne.setOnClickListener { BluetoothManager.sendCommand("1") }
        binding.btnTwo.setOnClickListener { BluetoothManager.sendCommand("2") }
        binding.btnThree.setOnClickListener { BluetoothManager.sendCommand("3") }
        binding.btnFour.setOnClickListener { BluetoothManager.sendCommand("4") }
        binding.btnFive.setOnClickListener { BluetoothManager.sendCommand("5") }
        binding.btnDefrost.setOnClickListener { BluetoothManager.sendCommand("AD") }
        binding.btnPS.setOnClickListener { BluetoothManager.sendCommand("3") }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("Bluetooth permission granted")
                if (BluetoothManager.connectToArduino(requireContext())) {
                    if (BluetoothManager.isConnected()) {
                        showToast("Connected to Arduino")
                        readFromArduino()
                    } else {
                        showToast("Failed to connect to Arduino")
                    }
                }
            } else {
                showToast("Bluetooth permission denied")
            }
        }
    }

    private fun readFromArduino() {
        BluetoothManager.readData { data ->
            activity?.runOnUiThread {
                val temperature = data.toFloatOrNull()
                if (temperature != null) {
                    binding.gauge.speedTo(temperature)
                } else {
                    showToast("Invalid temperature data received")
                    binding.gauge.speedTo(0f)
                }
            }
        }
    }

    private fun checkBluetoothPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
            val missingPermissions = permissions.filter {
                ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
            }
            if (missingPermissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(requireActivity(), missingPermissions.toTypedArray(), REQUEST_BLUETOOTH_PERMISSIONS)
                return false
            }
        }
        return true
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        BluetoothManager.closeConnection()
    }
}
