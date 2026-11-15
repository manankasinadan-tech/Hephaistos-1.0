package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.ipc.HephaistosContract
import com.example.model.ModuleTaskProgress

class HephaistosNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_PROGRESS = "hephaistos_progress"
        const val CHANNEL_ACTIONS = "hephaistos_actions"
        const val ACTION_NOTIFICATION_CANCEL = "com.hephaistos.internal.NOTIFICATION_CANCEL"
    }

    init {
        ensureChannels()
    }

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val progressChannel = NotificationChannel(
                CHANNEL_PROGRESS,
                context.getString(R.string.notif_channel_progress_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_progress_desc)
                setShowBadge(false)
            }

            val actionsChannel = NotificationChannel(
                CHANNEL_ACTIONS,
                context.getString(R.string.notif_channel_actions_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_actions_desc)
            }

            manager.createNotificationChannel(progressChannel)
            manager.createNotificationChannel(actionsChannel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun notifyTaskProgress(task: ModuleTaskProgress) {
        if (!hasNotificationPermission()) return

        val notificationId = task.taskId.hashCode()
        val channel = if (task.isCompleted || task.isError) CHANNEL_ACTIONS else CHANNEL_PROGRESS

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(HephaistosContract.EXTRA_OPEN_MODULE_PACKAGE, task.modulePackage)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(task.title)
            .setContentText(task.status)
            .setSubText(task.modulePackage.removePrefix(HephaistosContract.PACKAGE_PREFIX))
            .setContentIntent(openPendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(!task.isCompleted && !task.isError)
            .setAutoCancel(task.isCompleted || task.isError)

        if (!task.isCompleted && !task.isError) {
            val indeterminate = task.progress == null || task.progress < 0
            builder.setProgress(100, task.progress?.coerceIn(0, 100) ?: 0, indeterminate)

            if (task.canCancel) {
                val cancelIntent = Intent(context, HephaistosNotificationActionReceiver::class.java).apply {
                    action = ACTION_NOTIFICATION_CANCEL
                    putExtra(HephaistosContract.EXTRA_TASK_ID, task.taskId)
                    putExtra(HephaistosContract.EXTRA_SOURCE_PACKAGE, task.modulePackage)
                }
                val cancelPending = PendingIntent.getBroadcast(
                    context,
                    notificationId + 1,
                    cancelIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Annuler",
                    cancelPending
                )
            }
        } else {
            builder.setProgress(0, 0, false)
            builder.setSmallIcon(
                if (task.isError) android.R.drawable.stat_notify_error
                else android.R.drawable.stat_sys_download_done
            )
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission revoked at runtime
        }
    }

    fun dismissTaskNotification(taskId: String) {
        NotificationManagerCompat.from(context).cancel(taskId.hashCode())
    }
}
