package com.example.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ModuleDiscoveryManager
import com.example.data.RootStatus
import com.example.data.SatellitePrivilegeManager
import com.example.data.ThemePreferencesRepository
import com.example.data.WorkspaceFileHelper
import com.example.ipc.HephaistosBus
import com.example.ipc.HephaistosContract
import com.example.model.ActivePopupRequest
import com.example.model.HephaistosModule
import com.example.model.LayoutMode
import com.example.model.ModuleTaskProgress
import com.example.model.TerminalEntry
import com.example.model.TerminalLevel
import com.example.model.ThemeMode
import com.example.model.UiElementSchema
import com.example.model.WorkspaceFileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.File

data class FilePickerRequest(
    val title: String,
    val mimeFilter: String,
    val allowDirectory: Boolean,
    val onFileSelected: (WorkspaceFileItem) -> Unit
)

class HephaistosViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = ThemePreferencesRepository(application)
    private val discoveryManager = ModuleDiscoveryManager(application)
    val privilegeManager = SatellitePrivilegeManager(application)
    val fileHelper = WorkspaceFileHelper(application)

    private var packageReceiver: BroadcastReceiver? = null

    val themeMode: StateFlow<ThemeMode> = prefsRepo.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemeMode.SYSTEM
    )

    val defaultLayout: StateFlow<LayoutMode> = prefsRepo.defaultLayout.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LayoutMode.LIST
    )

    val autoGrantSatellites: StateFlow<Boolean> = prefsRepo.autoGrantSatellites.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true
    )

    val rootAccessEnabled: StateFlow<Boolean> = prefsRepo.rootAccessEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    private val _storageGranted = MutableStateFlow(privilegeManager.hasStoragePermission())
    val storageGranted: StateFlow<Boolean> = _storageGranted.asStateFlow()

    private val _rootStatus = MutableStateFlow(
        RootStatus(isGranted = false, methodLabel = "Vérification non effectuée")
    )
    val rootStatus: StateFlow<RootStatus> = _rootStatus.asStateFlow()

    private val _isGrantingPrivileges = MutableStateFlow(false)
    val isGrantingPrivileges: StateFlow<Boolean> = _isGrantingPrivileges.asStateFlow()

    private val _lastPrivilegeReport = MutableStateFlow<String?>(null)
    val lastPrivilegeReport: StateFlow<String?> = _lastPrivilegeReport.asStateFlow()

    private val _modules = MutableStateFlow<List<HephaistosModule>>(emptyList())
    val modules: StateFlow<List<HephaistosModule>> = _modules.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _moduleLayoutOverrides = MutableStateFlow<Map<String, LayoutMode>>(emptyMap())
    val moduleLayoutOverrides: StateFlow<Map<String, LayoutMode>> = _moduleLayoutOverrides.asStateFlow()

    private val _moduleFieldValues = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())
    val moduleFieldValues: StateFlow<Map<String, Map<String, String>>> = _moduleFieldValues.asStateFlow()

    private val _recentPickedFiles = MutableStateFlow<List<WorkspaceFileItem>>(emptyList())
    val recentPickedFiles: StateFlow<List<WorkspaceFileItem>> = _recentPickedFiles.asStateFlow()

    private val _activeFilePicker = MutableStateFlow<FilePickerRequest?>(null)
    val activeFilePicker: StateFlow<FilePickerRequest?> = _activeFilePicker.asStateFlow()

    val terminalEntries: StateFlow<List<TerminalEntry>> = HephaistosBus.terminalEntries
    val tasks: StateFlow<Map<String, ModuleTaskProgress>> = HephaistosBus.tasks
    val activePopup: StateFlow<ActivePopupRequest?> = HephaistosBus.popupRequest

    init {
        refreshPrivilegeStatus()
        refreshModules()
        packageReceiver = discoveryManager.registerPackageChangeReceiver {
            HephaistosBus.appendLog(
                HephaistosContract.LAUNCHER_PACKAGE,
                TerminalLevel.INFO,
                "Changement de paquet com.hephaistos.* détecté — actualisation…"
            )
            refreshModules()
        }
    }

    override fun onCleared() {
        super.onCleared()
        packageReceiver?.let {
            runCatching { getApplication<Application>().unregisterReceiver(it) }
        }
    }

    fun refreshPrivilegeStatus() {
        _storageGranted.value = privilegeManager.hasStoragePermission()
        viewModelScope.launch {
            val enabled = prefsRepo.rootAccessEnabled.first()
            if (enabled) {
                val status = privilegeManager.verifyRootAccess()
                _rootStatus.value = status
                if (!status.isGranted) {
                    prefsRepo.setRootAccessEnabled(false)
                }
            }
        }
    }

    fun requestRootAndApplyToSatellites() {
        viewModelScope.launch {
            _isGrantingPrivileges.value = true
            val status = privilegeManager.verifyRootAccess()
            _rootStatus.value = status
            prefsRepo.setRootAccessEnabled(status.isGranted)

            if (status.isGranted) {
                if (!privilegeManager.hasStoragePermission()) {
                    privilegeManager.grantSelfStorageViaRoot()
                    _storageGranted.value = privilegeManager.hasStoragePermission()
                }
                val summary = privilegeManager.grantAllSatellites(_modules.value)
                _lastPrivilegeReport.value = summary.message
            } else {
                _lastPrivilegeReport.value = status.methodLabel
            }
            _isGrantingPrivileges.value = false
        }
    }

    fun tryGrantSelfStorageViaRootIfActive(onFallbackToSystemSettings: () -> Unit) {
        viewModelScope.launch {
            if (_rootStatus.value.isGranted) {
                val ok = privilegeManager.grantSelfStorageViaRoot()
                _storageGranted.value = privilegeManager.hasStoragePermission()
                if (!ok || !_storageGranted.value) {
                    onFallbackToSystemSettings()
                }
            } else {
                onFallbackToSystemSettings()
            }
        }
    }

    fun setAutoGrantSatellites(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setAutoGrantSatellites(enabled)
            if (enabled && _rootStatus.value.isGranted) {
                applyPrivilegesToSatellitesNow()
            }
        }
    }

    fun applyPrivilegesToSatellitesNow() {
        viewModelScope.launch {
            _isGrantingPrivileges.value = true
            val summary = privilegeManager.grantAllSatellites(_modules.value)
            _lastPrivilegeReport.value = summary.message
            _isGrantingPrivileges.value = false
        }
    }

    fun refreshModules() {
        viewModelScope.launch {
            _isScanning.value = true
            val found = discoveryManager.discoverInstalledModules()
            _modules.value = found
            initializeDefaultValues(found)
            _isScanning.value = false

            val autoEnabled = prefsRepo.autoGrantSatellites.first()
            val rootEnabled = prefsRepo.rootAccessEnabled.first()
            if (autoEnabled && rootEnabled && found.isNotEmpty()) {
                val summary = privilegeManager.grantAllSatellites(found)
                _lastPrivilegeReport.value = summary.message
            }
        }
    }

    private fun initializeDefaultValues(found: List<HephaistosModule>) {
        _moduleFieldValues.update { current ->
            val updated = current.toMutableMap()
            for (module in found) {
                val existingMap = updated[module.packageName]?.toMutableMap() ?: mutableMapOf()
                for (el in module.elements) {
                    if (!existingMap.containsKey(el.id)) {
                        when (el) {
                            is UiElementSchema.ToggleOption ->
                                existingMap[el.id] = el.defaultValue.toString()
                            is UiElementSchema.DropdownOption ->
                                existingMap[el.id] = el.defaultValue
                            is UiElementSchema.TextInputOption ->
                                existingMap[el.id] = el.defaultValue
                            else -> Unit
                        }
                    }
                }
                updated[module.packageName] = existingMap
            }
            updated
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    }

    fun setDefaultLayout(layout: LayoutMode) {
        viewModelScope.launch { prefsRepo.setDefaultLayout(layout) }
    }

    fun toggleModuleLayout(packageName: String, currentEffective: LayoutMode) {
        val next = if (currentEffective == LayoutMode.LIST) LayoutMode.GRID else LayoutMode.LIST
        _moduleLayoutOverrides.update { it + (packageName to next) }
    }

    fun updateFieldAndDispatch(
        module: HephaistosModule,
        elementId: String,
        newValue: String,
        commandId: String?,
        dispatchNow: Boolean = true
    ) {
        _moduleFieldValues.update { all ->
            val pkgMap = all[module.packageName].orEmpty() + (elementId to newValue)
            all + (module.packageName to pkgMap)
        }
        if (dispatchNow && !commandId.isNullOrBlank()) {
            val allValues = _moduleFieldValues.value[module.packageName].orEmpty()
            discoveryManager.dispatchCommand(
                module = module,
                commandId = commandId,
                params = allValues + mapOf("changed_id" to elementId, "value" to newValue)
            )
        }
    }

    fun triggerElement(module: HephaistosModule, element: UiElementSchema) {
        val values = _moduleFieldValues.value[module.packageName].orEmpty()
        when (element) {
            is UiElementSchema.OptionCard -> {
                val popupId = element.popupId
                if (!popupId.isNullOrBlank()) {
                    openSchemaPopup(module, popupId)
                } else if (!element.commandId.isNullOrBlank()) {
                    discoveryManager.dispatchCommand(module, element.commandId, values)
                }
            }

            is UiElementSchema.ActionButtonOption -> {
                val confirmId = element.confirmPopupId
                if (!confirmId.isNullOrBlank()) {
                    openSchemaPopup(module, confirmId)
                } else {
                    discoveryManager.dispatchCommand(module, element.commandId, values)
                }
            }

            is UiElementSchema.FilePickerOption -> {
                openFilePicker(
                    title = element.title,
                    mimeFilter = element.mimeFilter,
                    allowDirectory = element.allowDirectory
                ) { picked ->
                    recordPickedFile(picked)
                    updateFieldAndDispatch(
                        module = module,
                        elementId = element.id,
                        newValue = picked.pathOrUri,
                        commandId = element.commandId,
                        dispatchNow = true
                    )
                }
            }

            else -> Unit
        }
    }

    private fun openSchemaPopup(module: HephaistosModule, popupId: String) {
        val popup = module.popups[popupId] ?: return
        HephaistosBus.showPopup(
            ActivePopupRequest(
                sourcePackage = module.packageName,
                title = popup.title,
                message = popup.message,
                confirmLabel = popup.confirmLabel,
                cancelLabel = popup.cancelLabel,
                commandId = popup.commandId
            )
        )
    }

    fun confirmActivePopup(request: ActivePopupRequest) {
        val cmd = request.commandId
        if (!cmd.isNullOrBlank()) {
            val module = _modules.value.firstOrNull { it.packageName == request.sourcePackage }
            if (module != null) {
                val values = _moduleFieldValues.value[module.packageName].orEmpty()
                discoveryManager.dispatchCommand(module, cmd, values)
            }
        }
        HephaistosBus.dismissPopup()
    }

    fun dismissActivePopup() {
        HephaistosBus.dismissPopup()
    }

    fun openFilePicker(
        title: String = "Gestionnaire de fichiers",
        mimeFilter: String = "*/*",
        allowDirectory: Boolean = false,
        onSelected: (WorkspaceFileItem) -> Unit = { recordPickedFile(it) }
    ) {
        _activeFilePicker.value = FilePickerRequest(
            title = title,
            mimeFilter = mimeFilter,
            allowDirectory = allowDirectory,
            onFileSelected = onSelected
        )
    }

    fun closeFilePicker() {
        _activeFilePicker.value = null
    }

    fun onSafPicked(uri: Uri, isDirectory: Boolean = false) {
        val item = fileHelper.describeSafUri(uri, isDirectory)
        recordPickedFile(item)
        _activeFilePicker.value?.onFileSelected?.invoke(item)
        _activeFilePicker.value = null
    }

    fun onWorkspaceFilePicked(item: WorkspaceFileItem) {
        recordPickedFile(item)
        _activeFilePicker.value?.onFileSelected?.invoke(item)
        _activeFilePicker.value = null
    }

    private fun recordPickedFile(item: WorkspaceFileItem) {
        _recentPickedFiles.update { list ->
            (listOf(item) + list.filterNot { it.pathOrUri == item.pathOrUri }).take(8)
        }
        HephaistosBus.appendLog(
            HephaistosContract.LAUNCHER_PACKAGE,
            TerminalLevel.INFO,
            "Fichier sélectionné: ${item.name} (${item.mimeType})"
        )
    }

    fun executeCommandForModule(packageName: String, commandId: String, params: Map<String, Any?>) {
        val module = _modules.value.firstOrNull { it.packageName == packageName } ?: return
        discoveryManager.dispatchCommand(module, commandId, params)
    }

    fun cancelTask(task: ModuleTaskProgress) {
        HephaistosBus.markTaskCancelled(task.taskId, task.modulePackage)
    }

    fun clearCompletedTasks() {
        HephaistosBus.clearCompletedTasks()
    }

    fun clearTerminal() {
        HephaistosBus.clearLogs()
    }

    fun executeTerminalCommand(rawInput: String) {
        val cmd = rawInput.trim()
        if (cmd.isEmpty()) return

        HephaistosBus.appendLog(HephaistosContract.LAUNCHER_PACKAGE, TerminalLevel.CMD, cmd)

        val parts = cmd.split(Regex("\\s+"))
        when (parts.first().lowercase()) {
            "help" -> {
                HephaistosBus.appendLog(
                    HephaistosContract.LAUNCHER_PACKAGE,
                    TerminalLevel.INFO,
                    "Commandes: help | list | scan | inspect <pkg> | exec <pkg> <cmd> [json] | clear | <commande shell>"
                )
            }

            "clear" -> HephaistosBus.clearLogs()

            "scan" -> {
                refreshModules()
                HephaistosBus.appendLog(
                    HephaistosContract.LAUNCHER_PACKAGE,
                    TerminalLevel.INFO,
                    "Analyse PackageManager relancée."
                )
            }

            "list" -> {
                val list = _modules.value
                if (list.isEmpty()) {
                    HephaistosBus.appendLog(
                        HephaistosContract.LAUNCHER_PACKAGE,
                        TerminalLevel.INFO,
                        "Aucun module com.hephaistos.* installé."
                    )
                } else {
                    list.forEach { m ->
                        HephaistosBus.appendLog(
                            HephaistosContract.LAUNCHER_PACKAGE,
                            TerminalLevel.OUT,
                            "• ${m.packageName} (${m.displayName} v${m.versionName}) — ${m.manual.commands.size} cmd(s)"
                        )
                    }
                }
            }

            "inspect" -> {
                val targetPkg = parts.getOrNull(1)
                val module = _modules.value.firstOrNull {
                    it.packageName == targetPkg || it.packageName == "${HephaistosContract.PACKAGE_PREFIX}$targetPkg"
                }
                if (module == null) {
                    HephaistosBus.appendLog(
                        HephaistosContract.LAUNCHER_PACKAGE,
                        TerminalLevel.ERR,
                        "Module introuvable: ${targetPkg ?: "(manquant)"}"
                    )
                } else {
                    HephaistosBus.appendLog(
                        module.packageName,
                        TerminalLevel.INFO,
                        "Mode d'emploi (${module.manual.commands.size} commandes):"
                    )
                    module.manual.commands.forEach { c ->
                        HephaistosBus.appendLog(
                            module.packageName,
                            TerminalLevel.OUT,
                            "  ${c.commandId}: ${c.description}"
                        )
                    }
                }
            }

            "exec" -> {
                val targetPkg = parts.getOrNull(1)
                val commandId = parts.getOrNull(2)
                if (targetPkg == null || commandId == null) {
                    HephaistosBus.appendLog(
                        HephaistosContract.LAUNCHER_PACKAGE,
                        TerminalLevel.ERR,
                        "Usage: exec <package> <commandId> [jsonParams]"
                    )
                    return
                }
                val module = _modules.value.firstOrNull {
                    it.packageName == targetPkg || it.packageName == "${HephaistosContract.PACKAGE_PREFIX}$targetPkg"
                }
                if (module == null) {
                    HephaistosBus.appendLog(
                        HephaistosContract.LAUNCHER_PACKAGE,
                        TerminalLevel.ERR,
                        "Module introuvable: $targetPkg"
                    )
                    return
                }
                val jsonRaw = parts.drop(3).joinToString(" ").trim()
                val params = mutableMapOf<String, Any?>()
                if (jsonRaw.isNotEmpty()) {
                    runCatching {
                        val obj = JSONObject(jsonRaw)
                        obj.keys().forEach { key -> params[key] = obj.get(key) }
                    }.onFailure {
                        HephaistosBus.appendLog(
                            HephaistosContract.LAUNCHER_PACKAGE,
                            TerminalLevel.ERR,
                            "JSON invalide: ${it.message}"
                        )
                        return
                    }
                }
                discoveryManager.dispatchCommand(module, commandId, params)
            }

            else -> runShellCommand(cmd, fileHelper.getWorkspaceRoot())
        }
    }

    private fun runShellCommand(command: String, workDir: File) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    val process = ProcessBuilder("sh", "-c", command)
                        .directory(workDir)
                        .redirectErrorStream(true)
                        .start()

                    val outputBuilder = StringBuilder()
                    val readerThread = Thread {
                        runCatching {
                            process.inputStream.bufferedReader().useLines { lines ->
                                lines.forEach { line ->
                                    outputBuilder.appendLine(line)
                                }
                            }
                        }
                    }.apply { start() }

                    val exitCode = withTimeoutOrNull(5_000L) {
                        val code = process.waitFor()
                        readerThread.join(1_000L)
                        code
                    }

                    if (exitCode == null) {
                        process.destroy()
                        HephaistosBus.appendLog(
                            HephaistosContract.LAUNCHER_PACKAGE,
                            TerminalLevel.ERR,
                            "Délai d'exécution dépassé (5s)."
                        )
                    } else {
                        val lines = outputBuilder.toString().lines().filter { it.isNotBlank() }
                        if (lines.isEmpty()) {
                            HephaistosBus.appendLog(
                                HephaistosContract.LAUNCHER_PACKAGE,
                                TerminalLevel.INFO,
                                "(code retour $exitCode)"
                            )
                        } else {
                            lines.forEach { line ->
                                HephaistosBus.appendLog(
                                    HephaistosContract.LAUNCHER_PACKAGE,
                                    if (exitCode == 0) TerminalLevel.OUT else TerminalLevel.ERR,
                                    line
                                )
                            }
                        }
                    }
                }.onFailure { err ->
                    HephaistosBus.appendLog(
                        HephaistosContract.LAUNCHER_PACKAGE,
                        TerminalLevel.ERR,
                        "Erreur shell: ${err.message}"
                    )
                }
            }
        }
    }
}
