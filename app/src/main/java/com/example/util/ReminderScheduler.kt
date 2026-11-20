package com.example.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.preferences.ShopSettings
import java.util.Calendar

object ReminderScheduler {
    const val CHANNEL_ID = "snehal_milk_reminders"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_BODY = "extra_body"
    const val EXTRA_NOTIFICATION_ID = "extra_notif_id"

    private const val REQ_DAILY = 1001
    private const val REQ_MONTHLY = 1002
    private const val REQ_PENDING = 1003

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Milk Billing Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily milk entry, monthly billing, and pending payment reminders"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun syncReminders(context: Context, settings: ShopSettings) {
        createNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        if (settings.dailyEntryReminderEnabled) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, settings.dailyReminderHour)
                set(Calendar.MINUTE, settings.dailyReminderMinute)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
            }
            scheduleRepeating(
                context = context,
                alarmManager = alarmManager,
                requestCode = REQ_DAILY,
                triggerAtMillis = cal.timeInMillis,
                intervalMillis = AlarmManager.INTERVAL_DAY,
                title = "${settings.shopName} — Daily Milk Entry",
                body = "Remember to record today's morning milk packet deliveries."
            )
        } else {
            cancelReminder(context, alarmManager, REQ_DAILY)
        }

        if (settings.monthlyBillReminderEnabled) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 10)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.MONTH, 1)
            }
            scheduleRepeating(
                context = context,
                alarmManager = alarmManager,
                requestCode = REQ_MONTHLY,
                triggerAtMillis = cal.timeInMillis,
                intervalMillis = AlarmManager.INTERVAL_DAY * 30,
                title = "${settings.shopName} — Monthly Bills Ready",
                body = "Generate and review last month's customer milk statements."
            )
        } else {
            cancelReminder(context, alarmManager, REQ_MONTHLY)
        }

        if (settings.pendingPaymentReminderEnabled) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 18)
                set(Calendar.MINUTE, 30)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
            }
            scheduleRepeating(
                context = context,
                alarmManager = alarmManager,
                requestCode = REQ_PENDING,
                triggerAtMillis = cal.timeInMillis,
                intervalMillis = AlarmManager.INTERVAL_DAY * 7,
                title = "${settings.shopName} — Pending Dues Check",
                body = "Review outstanding customer balances and share statements via WhatsApp."
            )
        } else {
            cancelReminder(context, alarmManager, REQ_PENDING)
        }
    }

    fun sendTestReminderNow(context: Context, shopName: String): Boolean {
        createNotificationChannel(context)
        return postNotification(
            context = context,
            notificationId = 9001,
            title = "$shopName — Reminder Active",
            body = "Daily milk entry & monthly billing reminders are working on this device."
        )
    }

    private fun scheduleRepeating(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        triggerAtMillis: Long,
        intervalMillis: Long,
        title: String,
        body: String
    ) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_NOTIFICATION_ID, requestCode)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            intervalMillis,
            pi
        )
    }

    private fun cancelReminder(context: Context, alarmManager: AlarmManager, requestCode: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
    }

    internal fun postNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPi = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPi)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
        return true
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "Snehal Super Market"
        val body = intent.getStringExtra(ReminderScheduler.EXTRA_BODY) ?: "Open Milk Manager to check today's entries."
        val id = intent.getIntExtra(ReminderScheduler.EXTRA_NOTIFICATION_ID, 1001)
        ReminderScheduler.createNotificationChannel(context)
        ReminderScheduler.postNotification(context, id, title, body)
    }
}
