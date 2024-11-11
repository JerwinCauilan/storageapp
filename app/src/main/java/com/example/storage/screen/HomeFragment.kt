package com.example.storage.screen

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.storage.databinding.FragmentHomeBinding
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.concurrent.thread

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private var bluetoothSocket: BluetoothSocket? = null
    private var readThread: Thread? = null

    companion object {
        private const val TAG = "HomeFragment"
        private const val ARDUINO_DEVICE_NAME = "HC-05"
        private val ARDUINO_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val REQUEST_BLUETOOTH_PERMISSIONS = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        binding.gauge.maxSpeed = 100f
        binding.gauge.speedTo(0f)

        if (checkBluetoothPermissions()) {
            connectToArduino()
        }

        launchButton()

        return binding.root
    }

    private fun launchButton() {
        binding.btnOne.setOnClickListener {
            sendCommandToArduino("1")
        }
        binding.btnTwo.setOnClickListener {
            sendCommandToArduino("2")
        }
        binding.btnThree.setOnClickListener {
            sendCommandToArduino("3")
        }
        binding.btnFour.setOnClickListener {
            sendCommandToArduino("4")
        }
        binding.btnFive.setOnClickListener {
            sendCommandToArduino("5")
        }
        binding.btnDefrost.setOnClickListener {
            sendCommandToArduino("AD")
        }
        binding.btnPS.setOnClickListener {
            sendCommandToArduino("3")
        }
    }

    private fun sendCommandToArduino(command: String) {
        val outputStream: OutputStream? = bluetoothSocket?.outputStream
        if (outputStream != null) {
            try {
                outputStream.write("$command\n".toByteArray())
                Log.d(TAG, "Sent command: $command")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending command", e)
                showToast("Failed to send command")
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
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_BLUETOOTH_PERMISSIONS
                )
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                connectToArduino()
            } else {
                showToast("Bluetooth permissions are required to connect to Arduino")
            }
        }
    }

    private fun connectToArduino() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: run {
            showToast("Bluetooth not supported")
            return
        }

        val device: BluetoothDevice? = bluetoothAdapter.bondedDevices.find { it.name == ARDUINO_DEVICE_NAME }
        if (device != null) {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(ARDUINO_UUID)
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    bluetoothSocket?.connect()
                    showToast("Connected to Arduino")
                    readFromArduino()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to Arduino", e)
                showToast("Connection failed")
            }
        } else {
            showToast("Arduino not paired")
        }
    }

    private fun readFromArduino() {
        val inputStream: InputStream? = bluetoothSocket?.inputStream
        val outputStream: OutputStream? = bluetoothSocket?.outputStream

        if (inputStream == null || outputStream == null) {
            showToast("Failed to get streams")
            return
        }

        readThread = thread {
            val buffer = ByteArray(1024)
            while (bluetoothSocket?.isConnected == true) {
                try {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        val data = String(buffer, 0, bytesRead).trim()
                        Log.d(TAG, "Received data: $data")

                        if (data.isNotEmpty()) {
                            outputStream.write("ACK\n".toByteArray())
                            Log.d(TAG, "Sent ACK to Arduino")
                        }

                        val temperature = data.toFloatOrNull()
                        if (temperature != null) {
                            activity?.runOnUiThread {
                                binding.gauge.speedTo(temperature)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading data", e)
                    break
                }
            }
        }
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        readThread?.interrupt()
        bluetoothSocket?.close()
    }
}