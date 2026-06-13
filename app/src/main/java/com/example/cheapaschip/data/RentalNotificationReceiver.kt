package com.example.cheapaschip.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.cheapaschip.MainActivity

class RentalNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val contractId = intent.getStringExtra("contractId") ?: return
        val customerName = intent.getStringExtra("customerName") ?: "Customer"
        val scooterModel = intent.getStringExtra("scooterModel") ?: "Scooter"
        val plateNumber = intent.getStringExtra("plateNumber") ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "scooter_rentals_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Scooter Rental Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for when scooters are due for return soon"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Action when user taps the notification - opens the main activity
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            contractId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationText = if (plateNumber.isNotEmpty()) {
            "$customerName's rental of $scooterModel ($plateNumber) is due in 1 hour!"
        } else {
            "$customerName's rental of $scooterModel is due in 1 hour!"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Scooter Due Soon - CheapAsChip")
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(contractId.hashCode(), notification)
    }
}
