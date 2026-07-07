package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.DoctorVisit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object AppointmentNotificationScheduler {
    private const val TAG = "AppointmentScheduler"

    fun scheduleAppointmentReminders(context: Context, visit: DoctorVisit) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val visitDate = try {
            sdf.parse(visit.date)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date: ${visit.date}", e)
            null
        } ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 1. One day before at 9:00 AM
        val oneDayBeforeCal = Calendar.getInstance().apply {
            time = visitDate
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (oneDayBeforeCal.timeInMillis > System.currentTimeMillis()) {
            val intent = Intent(context, AppointmentNotificationReceiver::class.java).apply {
                putExtra("visit_id", visit.id)
                putExtra("reminder_type", "day_before")
                putExtra("doctor_name", visit.doctorName)
                putExtra("speciality", visit.speciality)
                putExtra("hospital", visit.hospital)
                putExtra("date", visit.date)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                visit.id * 2,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            scheduleExact(alarmManager, oneDayBeforeCal.timeInMillis, pendingIntent)
            Log.d(TAG, "Scheduled 1-day-before reminder for ${visit.doctorName} at ${oneDayBeforeCal.time}")
        }

        // 2. Same day at 8:00 AM
        val sameDayCal = Calendar.getInstance().apply {
            time = visitDate
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (sameDayCal.timeInMillis > System.currentTimeMillis()) {
            val intent = Intent(context, AppointmentNotificationReceiver::class.java).apply {
                putExtra("visit_id", visit.id)
                putExtra("reminder_type", "same_day")
                putExtra("doctor_name", visit.doctorName)
                putExtra("speciality", visit.speciality)
                putExtra("hospital", visit.hospital)
                putExtra("date", visit.date)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                visit.id * 2 + 1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            scheduleExact(alarmManager, sameDayCal.timeInMillis, pendingIntent)
            Log.d(TAG, "Scheduled same-day reminder for ${visit.doctorName} at ${sameDayCal.time}")
        }
    }

    private fun scheduleExact(alarmManager: AlarmManager, triggerTime: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun cancelAppointmentReminders(context: Context, visitId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AppointmentNotificationReceiver::class.java)

        val pendingIntent1 = PendingIntent.getBroadcast(
            context,
            visitId * 2,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent1 != null) {
            alarmManager.cancel(pendingIntent1)
            pendingIntent1.cancel()
        }

        val pendingIntent2 = PendingIntent.getBroadcast(
            context,
            visitId * 2 + 1,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent2 != null) {
            alarmManager.cancel(pendingIntent2)
            pendingIntent2.cancel()
        }
    }
}
