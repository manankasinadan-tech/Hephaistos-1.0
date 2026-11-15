package com.example.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import com.example.ipc.HephaistosBus
import com.example.ipc.HephaistosContract
import com.example.model.HephaistosModule
import com.example.model.TerminalLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class RootStatus(
    val isGranted: Boolean,
    val methodLabel: String
)

data class SatelliteGrantSummary(
    val satellitesCount: Int,
    val grantedSatellites: Int,
    val permissionsGranted: Int,
    val rootPoliciesGranted: Int,
    val message: String
)

class SatellitePrivilegeManager(private val context: Context) {

    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val read = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            val write = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            read && write
        }
    }

    suspend fun verifyRootAccess(): RootStatus = withContext(Dispatchers.IO) {
        val result = runSuCommand("id && (command -v magisk || command -v ksud || command -v apd || echo su)")
        if (result.exitCode == 0 && result.output.contains("uid=0")) {
            val label = when {
                result.output.contains("magisk") -> "Root actif (Magisk)"
                result.output.contains("ksud") -> "Root actif (KernelSU)"
                result.output.contains("apd") -> "Root actif (APatch)"
                else -> "Root actif (su)"
            }
            RootStatus(isGranted = true, methodLabel = label)
        } else {
            RootStatus(isGranted = false, methodLabel = "Root non accordé ou appareil non rooté")
        }
    }

    suspend fun grantSelfStorageViaRoot(): Boolean = withContext(Dispatchers.IO) {
        val pkg = context.packageName
        val cmd = buildString {
            append("appops set $pkg MANAGE_EXTERNAL_STORAGE allow 2>/dev/null || true; ")
            append("pm grant $pkg android.permission.READ_EXTERNAL_STORAGE 2>/dev/null || true; ")
            append("pm grant $pkg android.permission.WRITE_EXTERNAL_STORAGE 2>/dev/null || true; ")
            append("pm grant $pkg android.permission.POST_NOTIFICATIONS 2>/dev/null || true")
        }
        val res = runSuCommand(cmd)
        if (res.exitCode == 0) {
            HephaistosBus.appendLog(
                HephaistosContract.LAUNCHER_PACKAGE,
                TerminalLevel.INFO,
                "Autorisations de stockage attribuées automatiquement à HEPHAISTOS via Root."
            )
            true
        } else {
            false
        }
    }

    suspend fun grantAllSatellites(modules: List<HephaistosModule>): SatelliteGrantSummary =
        withContext(Dispatchers.IO) {
            if (modules.isEmpty()) {
                return@withContext SatelliteGrantSummary(
                    satellitesCount = 0,
                    grantedSatellites = 0,
                    permissionsGranted = 0,
                    rootPoliciesGranted = 0,
                    message = "Aucun satellite com.hephaistos.* installé pour le moment."
                )
            }

            val rootStatus = verifyRootAccess()
            if (!rootStatus.isGranted) {
                return@withContext SatelliteGrantSummary(
                    satellitesCount = modules.size,
                    grantedSatellites = 0,
                    permissionsGranted = 0,
                    rootPoliciesGranted = 0,
                    message = "Accès Root requis pour attribuer automatiquement les permissions aux satellites."
                )
            }

            val pm = context.packageManager
            var totalPermissionsGranted = 0
            var totalRootPolicies = 0
            var succeededSatellites = 0

            for (module in modules) {
                val pkg = module.packageName
                val pkgInfo = runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getPackageInfo(
                            pkg,
                            PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS)
                    }
                }.getOrNull() ?: continue

                val uid = pkgInfo.applicationInfo?.uid ?: -1
                val requestedPerms = pkgInfo.requestedPermissions?.toList().orEmpty()
                val runtimePerms = requestedPerms.filter { permName ->
                    isGrantableRuntimePermission(pm, permName)
                }

                val commands = mutableListOf<String>()
                for (perm in runtimePerms) {
                    commands.add("pm grant $pkg $perm 2>/dev/null || true")
                }

                // Always grant full storage + notifications + legacy storage appops to satellites
                commands.add("appops set $pkg MANAGE_EXTERNAL_STORAGE allow 2>/dev/null || true")
                commands.add("appops set $pkg LEGACY_STORAGE allow 2>/dev/null || true")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    commands.add("pm grant $pkg android.permission.POST_NOTIFICATIONS 2>/dev/null || true")
                }

                // Grant Magisk root policy directly to satellite UID if Magisk is present
                var magiskAttempted = false
                if (uid > 0) {
                    magiskAttempted = true
                    commands.add(
                        "if command -v magisk >/dev/null 2>&1; then " +
                            "magisk --sqlite \"REPLACE INTO policies (uid, policy, until, logging, notification) VALUES ($uid, 2, 0, 1, 1);\" && echo MAGISK_OK; " +
                            "fi"
                    )
                }

                val script = commands.joinToString("; ")
                val res = runSuCommand(script)
                if (res.exitCode == 0) {
                    succeededSatellites++
                    totalPermissionsGranted += runtimePerms.size + 1
                    if (magiskAttempted && res.output.contains("MAGISK_OK")) {
                        totalRootPolicies++
                    }
                    HephaistosBus.appendLog(
                        pkg,
                        TerminalLevel.INFO,
                        "Auto-attribution effectuée: ${runtimePerms.size} permission(s), stockage complet & pont Root activés."
                    )
                } else {
                    HephaistosBus.appendLog(
                        pkg,
                        TerminalLevel.ERR,
                        "Échec attribution Root/Stockage (${res.output.ifBlank { "code ${res.exitCode}" }})."
                    )
                }
            }

            SatelliteGrantSummary(
                satellitesCount = modules.size,
                grantedSatellites = succeededSatellites,
                permissionsGranted = totalPermissionsGranted,
                rootPoliciesGranted = totalRootPolicies,
                message = "$succeededSatellites/${modules.size} satellite(s) autorisé(s) (stockage, permissions & root)."
            )
        }

    private fun isGrantableRuntimePermission(pm: PackageManager, permission: String): Boolean {
        return runCatching {
            val info = pm.getPermissionInfo(permission, 0)
            val protection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.protection
            } else {
                @Suppress("DEPRECATION")
                info.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE
            }
            protection == PermissionInfo.PROTECTION_DANGEROUS
        }.getOrDefault(false)
    }

    suspend fun executeRootShellForVerifiedSatellite(command: String): SuResult =
        withContext(Dispatchers.IO) {
            runSuCommand(command)
        }

    data class SuResult(val exitCode: Int, val output: String)

    private suspend fun runSuCommand(command: String): SuResult {
        return runCatching {
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()

            val outputBuilder = StringBuilder()
            val readerThread = Thread {
                runCatching {
                    process.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { line -> outputBuilder.appendLine(line) }
                    }
                }
            }.apply { start() }

            val exitCode = withTimeoutOrNull(8_000L) {
                val code = process.waitFor()
                readerThread.join(1_000L)
                code
            }

            if (exitCode == null) {
                process.destroy()
                SuResult(-1, "Délai su dépassé")
            } else {
                SuResult(exitCode, outputBuilder.toString().trim())
            }
        }.getOrElse { err ->
            SuResult(-1, err.message ?: "su indisponible")
        }
    }
}
