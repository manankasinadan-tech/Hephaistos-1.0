package com.example.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.model.ActivePopupRequest
import com.example.model.ModuleTaskProgress
import com.example.model.TerminalLevel
import com.example.notifications.HephaistosNotificationManager

class HephaistosBusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sourcePkg = intent.getStringExtra(HephaistosContract.EXTRA_SOURCE_PACKAGE)
            ?.takeIf { it.isNotBlank() }
            ?: "com.hephaistos.module"

        when (intent.action) {
            HephaistosContract.ACTION_LOG -> {
                val rawLevel = intent.getStringExtra(HephaistosContract.EXTRA_LEVEL) ?: "OUT"
                val level = runCatching { TerminalLevel.valueOf(rawLevel.uppercase()) }
                    .getOrDefault(TerminalLevel.OUT)
                val message = intent.getStringExtra(HephaistosContract.EXTRA_MESSAGE) ?: return
                HephaistosBus.appendLog(sourcePkg, level, message)
            }

            HephaistosContract.ACTION_PROGRESS -> {
                val taskId = intent.getStringExtra(HephaistosContract.EXTRA_TASK_ID)
                    ?: "${sourcePkg}_default"
                val title = intent.getStringExtra(HephaistosContract.EXTRA_TITLE)
                    ?: sourcePkg.removePrefix(HephaistosContract.PACKAGE_PREFIX)
                val status = intent.getStringExtra(HephaistosContract.EXTRA_MESSAGE)
                    ?: "En cours…"
                val progressRaw = intent.getIntExtra(HephaistosContract.EXTRA_PROGRESS, -1)
                val progress = if (progressRaw in 0..100) progressRaw else null
                val completed = intent.getBooleanExtra(HephaistosContract.EXTRA_COMPLETED, false)
                val isError = intent.getBooleanExtra(HephaistosContract.EXTRA_ERROR, false)
                val canCancel = intent.getBooleanExtra(HephaistosContract.EXTRA_CAN_CANCEL, true)

                val task = ModuleTaskProgress(
                    taskId = taskId,
                    modulePackage = sourcePkg,
                    title = title,
                    status = status,
                    progress = progress,
                    isCompleted = completed,
                    isError = isError,
                    canCancel = canCancel
                )
                HephaistosBus.updateTaskProgress(task)
                HephaistosNotificationManager(context).notifyTaskProgress(task)

                val level = if (isError) TerminalLevel.ERR else TerminalLevel.INFO
                val pct = progress?.let { " ($it%)" } ?: ""
                HephaistosBus.appendLog(sourcePkg, level, "$title: $status$pct")
            }

            HephaistosContract.ACTION_SHOW_POPUP -> {
                val title = intent.getStringExtra(HephaistosContract.EXTRA_TITLE) ?: sourcePkg
                val message = intent.getStringExtra(HephaistosContract.EXTRA_MESSAGE) ?: ""
                val confirmLabel = intent.getStringExtra(HephaistosContract.EXTRA_CONFIRM_LABEL)
                    ?: "OK"
                val cancelLabel = intent.getStringExtra(HephaistosContract.EXTRA_CANCEL_LABEL)
                val commandId = intent.getStringExtra(HephaistosContract.EXTRA_COMMAND_ID)

                HephaistosBus.showPopup(
                    ActivePopupRequest(
                        sourcePackage = sourcePkg,
                        title = title,
                        message = message,
                        confirmLabel = confirmLabel,
                        cancelLabel = cancelLabel,
                        commandId = commandId
                    )
                )
            }

            HephaistosContract.ACTION_COMMAND_RESULT -> {
                val commandId = intent.getStringExtra(HephaistosContract.EXTRA_COMMAND_ID) ?: "cmd"
                val resultJson = intent.getStringExtra(HephaistosContract.EXTRA_RESULT_JSON)
                    ?: intent.getStringExtra(HephaistosContract.EXTRA_MESSAGE)
                    ?: "{}"
                val isError = intent.getBooleanExtra(HephaistosContract.EXTRA_ERROR, false)
                HephaistosBus.appendLog(
                    sourcePkg,
                    if (isError) TerminalLevel.ERR else TerminalLevel.OUT,
                    "[$commandId] -> $resultJson"
                )
            }
        }
    }
}
