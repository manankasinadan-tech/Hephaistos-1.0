package com.example.data

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.example.ipc.HephaistosBus
import com.example.ipc.HephaistosContract
import com.example.model.HephaistosModule
import com.example.model.LayoutMode
import com.example.model.ModuleManual
import com.example.model.TerminalLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ModuleDiscoveryManager(private val context: Context) {

    suspend fun discoverInstalledModules(): List<HephaistosModule> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val foundByPackage = linkedMapOf<String, HephaistosModule>()

        val serviceIntent = Intent(HephaistosContract.ACTION_MODULE)
        val services = queryServicesCompat(pm, serviceIntent)
        for (resolveInfo in services) {
            val serviceInfo = resolveInfo.serviceInfo ?: continue
            val pkg = serviceInfo.packageName
            if (!isTargetModulePackage(pkg)) continue
            loadModuleFromAppInfo(
                pm = pm,
                appInfo = serviceInfo.applicationInfo,
                componentMetaData = serviceInfo.metaData,
                serviceClass = serviceInfo.name,
                receiverClass = null
            )?.let { foundByPackage[pkg] = it }
        }

        val receivers = queryReceiversCompat(pm, serviceIntent)
        for (resolveInfo in receivers) {
            val activityInfo = resolveInfo.activityInfo ?: continue
            val pkg = activityInfo.packageName
            if (!isTargetModulePackage(pkg) || foundByPackage.containsKey(pkg)) continue
            loadModuleFromAppInfo(
                pm = pm,
                appInfo = activityInfo.applicationInfo,
                componentMetaData = activityInfo.metaData,
                serviceClass = null,
                receiverClass = activityInfo.name
            )?.let { foundByPackage[pkg] = it }
        }

        foundByPackage.values.sortedBy { it.displayName.lowercase() }
    }

    private fun isTargetModulePackage(pkg: String): Boolean {
        return pkg.startsWith(HephaistosContract.PACKAGE_PREFIX) && pkg != context.packageName
    }

    private fun loadModuleFromAppInfo(
        pm: PackageManager,
        appInfo: ApplicationInfo?,
        componentMetaData: Bundle?,
        serviceClass: String?,
        receiverClass: String?
    ): HephaistosModule? {
        val info = appInfo ?: return null
        val pkg = info.packageName
        val pkgInfo = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, PackageManager.GET_META_DATA)
            }
        }.getOrNull()

        val fallbackName = pm.getApplicationLabel(info).toString().ifBlank {
            pkg.removePrefix(HephaistosContract.PACKAGE_PREFIX)
        }
        val versionName = pkgInfo?.versionName ?: "1.0"
        val appMetaData = pkgInfo?.applicationInfo?.metaData

        val rawJson = readManifestJson(pm, info, componentMetaData)
            ?: readManifestJson(pm, info, appMetaData)

        if (rawJson == null) {
            HephaistosBus.appendLog(
                pkg,
                TerminalLevel.INFO,
                "Module détecté sans manifeste JSON (${HephaistosContract.META_MANIFEST}). Mode minimal activé."
            )
            return HephaistosModule(
                packageName = pkg,
                displayName = fallbackName,
                versionName = versionName,
                description = "Module HEPHAISTOS détecté sans manifeste JSON détaillé.",
                iconKey = "extension",
                defaultLayout = LayoutMode.LIST,
                serviceClassName = serviceClass,
                receiverClassName = receiverClass,
                elements = emptyList(),
                popups = emptyMap(),
                manual = ModuleManual(
                    protocolVersion = HephaistosContract.PROTOCOL_VERSION,
                    summary = "Aucune commande déclarée dans ${HephaistosContract.META_MANIFEST}.",
                    targetServiceAction = HephaistosContract.ACTION_EXECUTE,
                    broadcastCallbackAction = HephaistosContract.ACTION_COMMAND_RESULT,
                    commands = emptyList()
                ),
                rawManifestJson = "{}"
            )
        }

        return ModuleManifestParser.parse(
            packageName = pkg,
            fallbackName = fallbackName,
            versionName = versionName,
            serviceClassName = serviceClass,
            receiverClassName = receiverClass,
            rawJson = rawJson
        ).onFailure { err ->
            HephaistosBus.appendLog(
                pkg,
                TerminalLevel.ERR,
                "Erreur de lecture du manifeste JSON: ${err.message}"
            )
        }.getOrNull()
    }

    private fun readManifestJson(
        pm: PackageManager,
        appInfo: ApplicationInfo,
        metaData: Bundle?
    ): String? {
        if (metaData == null || !metaData.containsKey(HephaistosContract.META_MANIFEST)) return null

        val resId = metaData.getInt(HephaistosContract.META_MANIFEST, 0)
        if (resId != 0) {
            return runCatching {
                val resources = pm.getResourcesForApplication(appInfo)
                resources.openRawResource(resId).bufferedReader().use { it.readText() }
            }.getOrNull()
        }

        val inlineString = metaData.getString(HephaistosContract.META_MANIFEST)
        return inlineString?.takeIf { it.trim().startsWith("{") }
    }

    fun dispatchCommand(
        module: HephaistosModule,
        commandId: String,
        params: Map<String, Any?>
    ): Boolean {
        val paramsJson = JSONObject().apply {
            params.forEach { (k, v) -> put(k, v ?: JSONObject.NULL) }
        }.toString()

        val intent = Intent(HephaistosContract.ACTION_EXECUTE).apply {
            setPackage(module.packageName)
            putExtra(HephaistosContract.EXTRA_COMMAND_ID, commandId)
            putExtra(HephaistosContract.EXTRA_PARAMS_JSON, paramsJson)
            putExtra(HephaistosContract.EXTRA_SOURCE_PACKAGE, context.packageName)
        }

        HephaistosBus.appendLog(
            module.packageName,
            TerminalLevel.CMD,
            "exec $commandId $paramsJson"
        )

        if (module.serviceClassName != null) {
            intent.component = ComponentName(module.packageName, module.serviceClassName)
            val started = runCatching {
                ContextCompat.startForegroundService(context, intent)
                true
            }.recoverCatching {
                context.startService(intent) != null
            }.getOrDefault(false)

            if (started) return true
        }

        if (module.receiverClassName != null) {
            intent.component = ComponentName(module.packageName, module.receiverClassName)
        }
        return runCatching {
            context.sendBroadcast(intent)
            true
        }.onFailure { err ->
            HephaistosBus.appendLog(
                module.packageName,
                TerminalLevel.ERR,
                "Échec d'envoi vers ${module.packageName}: ${err.message}"
            )
        }.getOrDefault(false)
    }

    fun registerPackageChangeReceiver(onChanged: () -> Unit): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val pkg = intent?.data?.schemeSpecificPart ?: return
                if (pkg.startsWith(HephaistosContract.PACKAGE_PREFIX)) {
                    onChanged()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        return receiver
    }

    private fun queryServicesCompat(pm: PackageManager, intent: Intent): List<ResolveInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentServices(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.GET_META_DATA.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentServices(intent, PackageManager.GET_META_DATA)
        }
    }

    private fun queryReceiversCompat(pm: PackageManager, intent: Intent): List<ResolveInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryBroadcastReceivers(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.GET_META_DATA.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryBroadcastReceivers(intent, PackageManager.GET_META_DATA)
        }
    }
}
