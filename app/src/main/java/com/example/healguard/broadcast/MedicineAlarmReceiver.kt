package com.example.healguard.broadcast

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.healguard.R
import com.example.healguard.view.HomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class MedicineAlarmReceiver : BroadcastReceiver() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onReceive(context: Context, intent: Intent) {
        val medicineName = intent.getStringExtra("medicine_name") ?: "Medicine"
        val medicineDosage = intent.getStringExtra("medicine_dosage") ?: ""
        val medicineId = intent.getStringExtra("medicine_id") ?: ""

        // Show notification
        showNotification(context, medicineName, medicineDosage)

        // Save notification to Firebase
        saveMedicineReminderNotification(medicineName, medicineDosage, medicineId)

        // Play ringtone
        playRingtone(context)
    }

    private fun showNotification(context: Context, medicineName: String, medicineDosage: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel (required for Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "medicine_reminder_channel",
                "Medicine Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for medicine reminders"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent for when notification is clicked
        val contentIntent = Intent(context, HomeActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, "medicine_reminder_channel")
            .setSmallIcon(R.drawable.capsuleicon)
            .setContentTitle("Time to take your medicine!")
            .setContentText("$medicineName - $medicineDosage")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("It's time to take your $medicineName. Dosage: $medicineDosage"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000)) // Vibrate pattern
            .setTimeoutAfter(600000) // Auto cancel after 10 minutes
            .build()

        // Show notification
        notificationManager.notify(medicineName.hashCode(), notification)
    }

    private fun playRingtone(context: Context) {
        try {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, alarmSound)
            ringtone.play()

            // Stop ringtone after 30 seconds
            android.os.Handler().postDelayed({
                ringtone.stop()
            }, 30000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveMedicineReminderNotification(medicineName: String, medicineDosage: String, medicineId: String) {
        val currentUser = auth.currentUser
        currentUser?.uid?.let { userId ->
            val notification = com.example.healguard.model.NotificationItem(
                id = UUID.randomUUID().toString(),
                type = "reminder",
                message = "Time to take your $medicineName, $medicineDosage",
                medicineName = medicineName,
                dosage = medicineDosage,
                timestamp = System.currentTimeMillis()
            )

            db.collection("users").document(userId).collection("notifications")
                .document(notification.id)
                .set(notification)
        }
    }
}