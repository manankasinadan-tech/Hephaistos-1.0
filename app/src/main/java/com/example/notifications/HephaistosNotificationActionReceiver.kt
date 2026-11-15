package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ipc.HephaistosBus
import com.example.ipc.HephaistosContract

class HephaistosNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == HephaistosNotificationManager.ACTION_NOTIFICATION_CANCEL) {
            val taskId = intent.getStringExtra(HephaistosContract.EXTRA_TASK_ID) ?: return
            val targetPackage = intent.getStringExtra(HephaistosContract.EXTRA_SOURCE_PACKAGE) ?: return

            val cancelIntent = Intent(HephaistosContract.ACTION_CANCEL_TASK).apply {
                setPackage(targetPackage)
                putExtra(HephaistosContract.EXTRA_TASK_ID, taskId)
                putExtra(HephaistosContract.EXTRA_SOURCE_PACKAGE, context.packageName)
            }
            runCatching { context.sendBroadcast(cancelIntent) }

            HephaistosBus.markTaskCancelled(taskId, targetPackage)
            HephaistosNotificationManager(context).dismissTaskNotification(taskId)
        }
    }
}
