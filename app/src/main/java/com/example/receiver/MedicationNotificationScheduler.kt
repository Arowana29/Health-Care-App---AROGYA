package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.Medication
import java.util.Calendar

object MedicationNotificationScheduler {
    fun scheduleNotification(context: Context, medication: Medication) {
        if (medication.status != "Active" || !medication.notificationEnabled) {
            cancelNotification(context, medication.id)
            return
        }

        val timeParts = parseTime(medication.reminderTime) ?: return
        val hour = timeParts.first
        val minute = timeParts.second

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MedicationNotificationReceiver::class.java).apply {
            putExtra("medication_id", medication.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medication.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1) // if time has passed, schedule for tomorrow
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // fallback trigger
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun cancelNotification(context: Context, medicationId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MedicationNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicationId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun parseTime(timeStr: String): Pair<Int, Int>? {
        return try {
            val clean = timeStr.trim().uppercase()
            if (clean.contains("AM") || clean.contains("PM")) {
                val isPm = clean.contains("PM")
                val parts = clean.replace("AM", "").replace("PM", "").trim().split(":")
                var hour = parts[0].trim().toInt()
                val min = parts[1].trim().toInt()
                if (isPm && hour < 12) hour += 12
                if (!isPm && hour == 12) hour = 0
                Pair(hour, min)
            } else {
                val parts = clean.split(":")
                Pair(parts[0].trim().toInt(), parts[1].trim().toInt())
            }
        } catch (e: Exception) {
            null
        }
    }
}
