package com.example.ipc

import com.example.model.ActivePopupRequest
import com.example.model.ModuleTaskProgress
import com.example.model.TerminalEntry
import com.example.model.TerminalLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

object HephaistosBus {
    private const val MAX_TERMINAL_ENTRIES = 500
    private val entryCounter = AtomicLong(1L)
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val _terminalEntries = MutableStateFlow<List<TerminalEntry>>(
        listOf(
            TerminalEntry(
                id = 0L,
                timestamp = timeFormatter.format(Date()),
                sourcePackage = HephaistosContract.LAUNCHER_PACKAGE,
                level = TerminalLevel.INFO,
                message = "HEPHAISTOS Terminal prêt — tapez 'help' pour les commandes."
            )
        )
    )
    val terminalEntries: StateFlow<List<TerminalEntry>> = _terminalEntries.asStateFlow()

    private val _tasks = MutableStateFlow<Map<String, ModuleTaskProgress>>(emptyMap())
    val tasks: StateFlow<Map<String, ModuleTaskProgress>> = _tasks.asStateFlow()

    private val _popupRequest = MutableStateFlow<ActivePopupRequest?>(null)
    val popupRequest: StateFlow<ActivePopupRequest?> = _popupRequest.asStateFlow()

    fun appendLog(
        sourcePackage: String,
        level: TerminalLevel,
        message: String
    ) {
        val entry = TerminalEntry(
            id = entryCounter.getAndIncrement(),
            timestamp = timeFormatter.format(Date()),
            sourcePackage = sourcePackage,
            level = level,
            message = message
        )
        _terminalEntries.update { current ->
            (current + entry).takeLast(MAX_TERMINAL_ENTRIES)
        }
    }

    fun clearLogs() {
        _terminalEntries.value = listOf(
            TerminalEntry(
                id = entryCounter.getAndIncrement(),
                timestamp = timeFormatter.format(Date()),
                sourcePackage = HephaistosContract.LAUNCHER_PACKAGE,
                level = TerminalLevel.INFO,
                message = "Journal du terminal effacé."
            )
        )
    }

    fun updateTaskProgress(task: ModuleTaskProgress) {
        _tasks.update { current ->
            current + (task.taskId to task)
        }
    }

    fun markTaskCancelled(taskId: String, modulePackage: String) {
        _tasks.update { current ->
            val existing = current[taskId]
            if (existing != null) {
                current + (taskId to existing.copy(
                    status = "Annulé par l'utilisateur",
                    isCompleted = true,
                    canCancel = false
                ))
            } else {
                current
            }
        }
        appendLog(modulePackage, TerminalLevel.INFO, "Tâche '$taskId' annulée.")
    }

    fun clearCompletedTasks() {
        _tasks.update { current ->
            current.filterValues { !it.isCompleted && !it.isError }
        }
    }

    fun showPopup(request: ActivePopupRequest) {
        _popupRequest.value = request
    }

    fun dismissPopup() {
        _popupRequest.value = null
    }
}
