package com.cookandroid.challengers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.cookandroid.challengers.R
import com.cookandroid.challengers.util.UserPreference

class InactivityWorker(appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val prefs = UserPreference(applicationContext)
        val lastVisit = prefs.getLastVisitDate()
        val now = System.currentTimeMillis()
        val days = (now - lastVisit) / (1000 * 60 * 60 * 24)

        val isNotificationEnabled = prefs.isNotificationEnabled()
        if (isNotificationEnabled && days >= 3) {
            showNotification(applicationContext)
        }

        return Result.success()
    }

    private fun showNotification(context: Context) {
        val channelId = "reminder_channel"
        val notificationId = 1001

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "운동 알림",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "3일 이상 미접속" }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // 알림 권한 확인
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                // 권한 없을 경우 알림 생략
                return
            }
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_home_notification)
            .setContentTitle("FitBuddy")
            .setContentText("3일 이상 운동 기록이 없어요. 핏버디가 기다려요! 오늘 운동 어떠세요?")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }
}