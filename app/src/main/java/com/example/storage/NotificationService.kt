package com.example.storage

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class NotificationService(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val CHANNEL_ID = "expired_product_channel"

    private val notifiedProducts = HashSet<String>()

    init {
        createNotificationChannel()
        launchMonitoring()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Product Expiry Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for expired products"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(product: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Product Expiry Alert")
            .setContentText("$product has expired!")
            .setSmallIcon(R.drawable.document)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(product.hashCode(), notification)
    }

    private fun launchMonitoring() {
        db.collection("storage")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null || snapshot == null) return@addSnapshotListener

                for (document in snapshot.documents) {
                    val product = document.getString("product") ?: ""
                    val expiryDate = document.getString("expiryDate") ?: ""

                    if (isExpired(expiryDate) && !notifiedProducts.contains(product)) {
                        sendNotification(product)
                        notifiedProducts.add(product)
                    }
                }
            }
    }

    private fun isExpired(expiryDate: String): Boolean {
        val dateFormat = SimpleDateFormat("MM-dd-yyyy", Locale.getDefault())
        val expiry = dateFormat.parse(expiryDate)

        val currentDate = dateFormat.format(Date())

        return dateFormat.format(expiry) == currentDate
    }
}