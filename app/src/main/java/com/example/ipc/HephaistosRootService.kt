package com.example.ipc

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import com.example.data.SatellitePrivilegeManager
import com.example.data.ThemePreferencesRepository
import com.example.model.TerminalLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Root execution bridge for verified com.hephaistos.* satellite applications.
 * Verifies caller UID via PackageManager and checks that auto-grant + root are enabled.
 */
class HephaistosRootService : Service() {

    companion object {
        const val MSG_EXEC_ROOT = 1
        const val MSG_EXEC_RESULT = 2
        const val KEY_COMMAND = "command"
        const val KEY_EXIT_CODE = "exit_code"
        const val KEY_OUTPUT = "output"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var privilegeManager: SatellitePrivilegeManager
    private lateinit var prefsRepo: ThemePreferencesRepository

    private val incomingHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what != MSG_EXEC_ROOT) return

            val callerUid = msg.sendingUid
            val callerPkg = packageManager.getPackagesForUid(callerUid)
                ?.firstOrNull { it.startsWith(HephaistosContract.PACKAGE_PREFIX) }

            val replyTo = msg.replyTo
            val command = msg.data.getString(KEY_COMMAND).orEmpty()

            if (callerPkg == null) {
                replyResult(replyTo, -1, "Accès refusé: réservé aux modules com.hephaistos.*")
                return
            }

            scope.launch {
                val autoEnabled = prefsRepo.autoGrantSatellites.first()
                val rootEnabled = prefsRepo.rootAccessEnabled.first()
                if (!autoEnabled || !rootEnabled) {
                    replyResult(replyTo, -1, "Pont Root désactivé dans les paramètres HEPHAISTOS.")
                    return@launch
                }

                HephaistosBus.appendLog(callerPkg, TerminalLevel.CMD, "su -c $command")
                val res = privilegeManager.executeRootShellForVerifiedSatellite(command)
                HephaistosBus.appendLog(
                    callerPkg,
                    if (res.exitCode == 0) TerminalLevel.OUT else TerminalLevel.ERR,
                    res.output.ifBlank { "(exit ${res.exitCode})" }
                )
                replyResult(replyTo, res.exitCode, res.output)
            }
        }
    }

    private val messenger = Messenger(incomingHandler)

    override fun onCreate() {
        super.onCreate()
        privilegeManager = SatellitePrivilegeManager(applicationContext)
        prefsRepo = ThemePreferencesRepository(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder = messenger.binder

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun replyResult(replyTo: Messenger?, exitCode: Int, output: String) {
        replyTo ?: return
        val reply = Message.obtain(null, MSG_EXEC_RESULT).apply {
            data = Bundle().apply {
                putInt(KEY_EXIT_CODE, exitCode)
                putString(KEY_OUTPUT, output)
            }
        }
        runCatching { replyTo.send(reply) }
    }
}
