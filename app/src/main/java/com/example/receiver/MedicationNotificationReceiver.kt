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
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar

class MedicationNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context.applicationContext)
                    val activeMeds = db.healthWalletDao().getAllMedications().firstOrNull()?.filter { it.status == "Active" } ?: emptyList()
                    for (med in activeMeds) {
                        MedicationNotificationScheduler.scheduleNotification(context.applicationContext, med)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        val pendingResult = goAsync()
        val medicationId = intent.getIntExtra("medication_id", -1)
        if (medicationId == -1) {
            pendingResult.finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context.applicationContext)
                val med = db.healthWalletDao().getAllMedications().firstOrNull()?.find { it.id == medicationId }
                
                if (med != null && med.status == "Active" && med.notificationEnabled) {
                    // Check if today matches the scheduled day for weekly meds
                    val shouldRemind = if (med.frequencyType == "Weekly" && med.frequencyDaysOfWeek.isNotEmpty()) {
                        val currentDayStr = getCurrentDayOfWeekStr() // e.g. "Mon", "Tue" etc.
                        val allowedDays = med.frequencyDaysOfWeek.split(",").map { it.trim() }
                        allowedDays.contains(currentDayStr)
                    } else {
                        true // Daily is always true
                    }

                    if (shouldRemind) {
                        showNotification(
                            context.applicationContext, 
                            med.id, 
                            "Time to take: ${med.name}", 
                            "${med.dose} • ${med.frequency} at ${med.reminderTime}"
                        )
                    }

                    // Reschedule daily alarm trigger for the next day
                    MedicationNotificationScheduler.scheduleNotification(context.applicationContext, med)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun getCurrentDayOfWeekStr(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "Sun"
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            else -> "Mon"
        }
    }

    private fun showNotification(context: Context, medId: Int, title: String, content: String) {
        val channelId = "local_med_reminders"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Medication Daily/Weekly Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Custom notifications scheduled for your medications"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "medications")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            medId,
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

        notificationManager.notify(medId, builder.build())
    }
}
