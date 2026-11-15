package com.example.model

enum class ThemeMode(val label: String) {
    LIGHT("Clair"),
    DARK("Sombre"),
    SYSTEM("Système")
}

enum class LayoutMode(val label: String) {
    LIST("Liste"),
    GRID("Grille")
}

sealed class UiElementSchema {
    abstract val id: String
    abstract val title: String
    abstract val subtitle: String?
    abstract val iconKey: String

    data class OptionCard(
        override val id: String,
        override val title: String,
        override val subtitle: String? = null,
        override val iconKey: String = "tune",
        val commandId: String? = null,
        val popupId: String? = null
    ) : UiElementSchema()

    data class ToggleOption(
        override val id: String,
        override val title: String,
        override val subtitle: String? = null,
        override val iconKey: String = "toggle",
        val defaultValue: Boolean = false,
        val commandId: String
    ) : UiElementSchema()

    data class DropdownOption(
        override val id: String,
        override val title: String,
        override val subtitle: String? = null,
        override val iconKey: String = "list",
        val options: List<String>,
        val defaultValue: String,
        val commandId: String
    ) : UiElementSchema()

    data class FilePickerOption(
        override val id: String,
        override val title: String,
        override val subtitle: String? = null,
        override val iconKey: String = "folder",
        val mimeFilter: String = "*/*",
        val allowDirectory: Boolean = false,
        val commandId: String
    ) : UiElementSchema()

    data class TextInputOption(
        override val id: String,
        override val title: String,
        override val subtitle: String? = null,
        override val iconKey: String = "edit",
        val placeholder: String = "",
        val defaultValue: String = "",
        val commandId: String
    ) : UiElementSchema()

    data class ActionButtonOption(
        override val id: String,
        override val title: String,
        override val subtitle: String? = null,
        override val iconKey: String = "play",
        val commandId: String,
        val isPrimary: Boolean = true,
        val confirmPopupId: String? = null
    ) : UiElementSchema()
}

data class PopupSchema(
    val id: String,
    val title: String,
    val message: String,
    val confirmLabel: String = "Confirmer",
    val cancelLabel: String = "Annuler",
    val commandId: String? = null
)

data class ModuleCommandParam(
    val name: String,
    val type: String,
    val required: Boolean,
    val description: String,
    val defaultValue: String? = null
)

data class ModuleCommandManual(
    val commandId: String,
    val title: String,
    val description: String,
    val intentAction: String,
    val parameters: List<ModuleCommandParam>,
    val returns: String,
    val exampleJson: String
)

data class ModuleManual(
    val protocolVersion: String,
    val summary: String,
    val targetServiceAction: String,
    val broadcastCallbackAction: String,
    val commands: List<ModuleCommandManual>
)

data class HephaistosModule(
    val packageName: String,
    val displayName: String,
    val versionName: String,
    val description: String,
    val iconKey: String,
    val defaultLayout: LayoutMode,
    val serviceClassName: String?,
    val receiverClassName: String?,
    val elements: List<UiElementSchema>,
    val popups: Map<String, PopupSchema>,
    val manual: ModuleManual,
    val rawManifestJson: String
)

enum class TerminalLevel {
    CMD,
    INFO,
    OUT,
    ERR
}

data class TerminalEntry(
    val id: Long,
    val timestamp: String,
    val sourcePackage: String,
    val level: TerminalLevel,
    val message: String
)

data class ModuleTaskProgress(
    val taskId: String,
    val modulePackage: String,
    val title: String,
    val status: String,
    val progress: Int? = null,
    val isCompleted: Boolean = false,
    val isError: Boolean = false,
    val canCancel: Boolean = true
)

data class ActivePopupRequest(
    val sourcePackage: String,
    val title: String,
    val message: String,
    val confirmLabel: String = "Confirmer",
    val cancelLabel: String? = "Fermer",
    val commandId: String? = null
)

data class WorkspaceFileItem(
    val name: String,
    val pathOrUri: String,
    val sizeBytes: Long,
    val mimeType: String,
    val isDirectory: Boolean,
    val isExternalSaf: Boolean = false
)
