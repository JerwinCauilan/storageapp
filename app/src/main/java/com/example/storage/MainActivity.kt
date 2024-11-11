package com.example.storage

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.storage.databinding.ActivityMainBinding
import com.example.storage.screen.HomeFragment
import com.example.storage.screen.LogoutFragment
import com.example.storage.screen.ProfileFragment
import com.example.storage.screen.StorageFragment
import java.io.OutputStream
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var tts: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer

    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private val REQUEST_NOTIFICATION_PERMISSION = 201
    private val REQUEST_BLUETOOTH_PERMISSIONS = 1
    private var isListening = false

    private var bluetoothSocket: BluetoothSocket? = null
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothDevice: BluetoothDevice? = null

    companion object {
        private const val ARDUINO_DEVICE_NAME = "HC-05"
        private val ARDUINO_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NotificationService(this)

        tts = TextToSpeech(this, this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(VoiceRecognitionListener())

        navbar()
        displayFragment(HomeFragment())

        checkAudioPermissions()
        checkNotificationPermissions()
        checkBluetoothPermissions()

        binding.mic.setOnClickListener {
            activateVoiceRecognition()
        }
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

    private fun checkAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
        }
    }

    private fun checkNotificationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATION_PERMISSION)
        }
    }

    private fun checkBluetoothPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADMIN), REQUEST_BLUETOOTH_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_RECORD_AUDIO_PERMISSION -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_NOTIFICATION_PERMISSION -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Notification permission denied!", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_BLUETOOTH_PERMISSIONS -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Bluetooth permission denied!", Toast.LENGTH_SHORT).show()
                } else {
                    connectToArduino()
                }
            }
        }
    }

    private fun activateVoiceRecognition() {
        if (isListening) {
            terminateVoiceRecognition()
        } else {
            launchVoiceRecognition()
        }
    }

    private fun launchVoiceRecognition() {
        if (!isListening) {
            if (SpeechRecognizer.isRecognitionAvailable(this)) {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something")
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                }
                try {
                    speechRecognizer.startListening(intent)
                    isListening = true
                } catch (e: Exception) {
                    Toast.makeText(this, "Speech recognition is unavailable: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "This device does not support speech recognition.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun terminateVoiceRecognition() {
        if (isListening) {
            speechRecognizer.stopListening()
            isListening = false
        }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val langResult = tts.setLanguage(Locale.US)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "This language is not supported.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "An error occurred during initialization.", Toast.LENGTH_SHORT).show()
        }
    }

    private inner class VoiceRecognitionListener : android.speech.RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            isListening = false
        }

        override fun onError(error: Int) {
            terminateVoiceRecognition()
            if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                noMatchDetected()
                return
            }

            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "An error occurred while recording audio."
                SpeechRecognizer.ERROR_CLIENT -> "An issue occurred locally."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions."
                SpeechRecognizer.ERROR_NETWORK -> "Network error."
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy."
                SpeechRecognizer.ERROR_SERVER -> "Server error."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech recognition timed out."
                else -> "Unknown error: $error"
            }
            Log.e("SpeechRecognizer", "Recognition error: $errorMessage")
            Toast.makeText(this@MainActivity, "Recognition error: $errorMessage", Toast.LENGTH_SHORT).show()
        }

        override fun onResults(results: Bundle?) {
            terminateVoiceRecognition()
            val result = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val spokenText = result?.get(0) ?: ""
            manageRecognitionResult(spokenText)
        }

        override fun onPartialResults(partialResults: Bundle?) {}

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun noMatchDetected() {
        speak("Give it another try, please.")
    }


    private fun manageRecognitionResult(spokenText: String) {
        when {
            spokenText.contains("Set level one", ignoreCase = true) || spokenText.contains("Set level 1", ignoreCase = true) -> {
                sendCommandToArduino("1")
                speak("Setting to level one")
            }
            spokenText.contains("Set level two", ignoreCase = true) || spokenText.contains("Set level 2", ignoreCase = true) -> {
                sendCommandToArduino("2")
                speak("Setting to level two")
            }
            spokenText.contains("Set level three", ignoreCase = true) || spokenText.contains("Set level 3", ignoreCase = true) -> {
                sendCommandToArduino("3")
                speak("Setting to level three")
            }
            spokenText.contains("Set level four", ignoreCase = true) || spokenText.contains("Set level 4", ignoreCase = true) -> {
                sendCommandToArduino("4")
                speak("Setting to level four")
            }
            spokenText.contains("Set level five", ignoreCase = true) || spokenText.contains("Set level 5", ignoreCase = true) -> {
                sendCommandToArduino("5")
                speak("Setting to level five")
            }
            spokenText.contains("Set saving mode", ignoreCase = true) -> {
                sendCommandToArduino("3")
                speak("Setting to Saving Mode")
            }
            spokenText.contains("Set defrost mode", ignoreCase = true) -> {
                sendCommandToArduino("AD")
                speak("Setting to Defrost Mode")
            }
            else -> {
                noMatchDetected()
            }
        }
    }

    private fun connectToArduino() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: run {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            return
        }

        val device: BluetoothDevice? = bluetoothAdapter.bondedDevices.find { it.name == ARDUINO_DEVICE_NAME }
        if (device != null) {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(ARDUINO_UUID)
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothSocket?.connect()
                    Toast.makeText(this, "Connected to Arduino", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Arduino not paired", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendCommandToArduino(command: String) {
        val outputStream: OutputStream? = bluetoothSocket?.outputStream
        if (outputStream != null) {
            try {
                outputStream.write("$command\n".toByteArray())
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to send command", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
        bluetoothSocket?.close()
        super.onDestroy()
    }
}