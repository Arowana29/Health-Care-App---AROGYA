package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class AppointmentNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val visitId = intent.getIntExtra("visit_id", -1)
        if (visitId == -1) return

        val reminderType = intent.getStringExtra("reminder_type") ?: "day_before"
        val doctorName = intent.getStringExtra("doctor_name") ?: "Doctor"
        val speciality = intent.getStringExtra("speciality") ?: ""
        val hospital = intent.getStringExtra("hospital") ?: "Clinic"
        val date = intent.getStringExtra("date") ?: ""

        val title = if (reminderType == "day_before") {
            "Appointment Tomorrow"
        } else {
            "Appointment Today"
        }

        val content = if (reminderType == "day_before") {
            "Reminder: Tomorrow with Dr. $doctorName ($speciality) at $hospital"
        } else {
            "Today: Appointment with Dr. $doctorName ($speciality) at $hospital"
        }

        showNotification(context.applicationContext, visitId, title, content)
    }

    private fun showNotification(context: Context, visitId: Int, title: String, content: String) {
        val channelId = "local_appointment_reminders"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Doctor Visits & Appointments",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for your scheduled doctor visits"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "visits")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            visitId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(visitId + 20000, builder.build())
    }
}
