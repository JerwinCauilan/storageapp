//package com.example.storage
//
//import android.Manifest
//import android.bluetooth.BluetoothAdapter
//import android.bluetooth.BluetoothDevice
//import android.bluetooth.BluetoothSocket
//import android.content.Context
//import android.content.pm.PackageManager
//import android.util.Log
//import androidx.core.content.ContextCompat
//import java.io.OutputStream
//import java.util.UUID
//import kotlin.concurrent.thread
//
//object BluetoothManager {
//    private const val TAG = "BluetoothManager"
//    private const val DEVICE_NAME = "HC-05"
//    private val DEVICE_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
//
//    private var bluetoothSocket: BluetoothSocket? = null
//    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
//
//    fun readData(onDataReceived: (String) -> Unit) {
//        val inputStream = bluetoothSocket?.inputStream ?: return
//        thread {
//            val buffer = ByteArray(1024)
//            while (bluetoothSocket?.isConnected == true) {
//                try {
//                    val bytesRead = inputStream.read(buffer)
//                    if (bytesRead > 0) {
//                        val data = String(buffer, 0, bytesRead).trim()
//                        onDataReceived(data)
//                    }
//                } catch (e: Exception) {
//                    Log.e(TAG, "Error reading data", e)
//                    break
//                }
//            }
//        }
//    }
//
//    fun isBluetoothSupported(): Boolean {
//        return bluetoothAdapter != null
//    }
//
//    fun connectToArduino(context: Context): Boolean {
//        if (bluetoothSocket?.isConnected == true) {
//            Log.d(TAG, "Already connected to Arduino")
//            return true
//        }
//
//        val device: BluetoothDevice? = bluetoothAdapter?.bondedDevices?.find { it.name == DEVICE_NAME }
//        if (device != null) {
//            try {
//                bluetoothSocket = device.createRfcommSocketToServiceRecord(DEVICE_UUID)
//                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
//                    bluetoothSocket?.connect()
//                    Log.d(TAG, "Connected to Arduino")
//                    return true
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "Error connecting to Arduino", e)
//                bluetoothSocket = null
//            }
//        } else {
//            Log.e(TAG, "Arduino device not found")
//        }
//        return false
//    }
//
//    fun sendCommand(command: String) {
//        val outputStream: OutputStream? = bluetoothSocket?.outputStream
//        if (outputStream != null) {
//            try {
//                outputStream.write("$command\n".toByteArray())
//                Log.d(TAG, "Sent command: $command")
//            } catch (e: Exception) {
//                Log.e(TAG, "Error sending command", e)
//            }
//        }
//    }
//
//    fun closeConnection() {
//        try {
//            bluetoothSocket?.close()
//            bluetoothSocket = null
//            Log.d(TAG, "Bluetooth connection closed")
//        } catch (e: Exception) {
//            Log.e(TAG, "Error closing Bluetooth connection", e)
//        }
//    }
//
//    fun isConnected(): Boolean {
//        return bluetoothSocket?.isConnected == true
//    }
//}