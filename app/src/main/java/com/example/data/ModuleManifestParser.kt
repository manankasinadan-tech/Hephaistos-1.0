package com.example.data

import com.example.ipc.HephaistosContract
import com.example.model.HephaistosModule
import com.example.model.LayoutMode
import com.example.model.ModuleCommandManual
import com.example.model.ModuleCommandParam
import com.example.model.ModuleManual
import com.example.model.PopupSchema
import com.example.model.UiElementSchema
import org.json.JSONArray
import org.json.JSONObject

object ModuleManifestParser {

    fun parse(
        packageName: String,
        fallbackName: String,
        versionName: String,
        serviceClassName: String?,
        receiverClassName: String?,
        rawJson: String
    ): Result<HephaistosModule> = runCatching {
        val root = JSONObject(rawJson)
        val uiObj = root.optJSONObject("ui") ?: JSONObject()
        val manualObj = root.optJSONObject("manual") ?: JSONObject()

        val displayName = root.optString("name").ifBlank {
            uiObj.optString("title").ifBlank { fallbackName }
        }
        val description = root.optString("description").ifBlank {
            manualObj.optString("summary", "Module HEPHAISTOS sans interface propre")
        }
        val iconKey = root.optString("icon").ifBlank {
            uiObj.optString("icon", "extension")
        }
        val layoutStr = uiObj.optString("default_layout", "list").uppercase()
        val defaultLayout = if (layoutStr == "GRID") LayoutMode.GRID else LayoutMode.LIST

        val elements = parseElements(uiObj.optJSONArray("elements") ?: JSONArray())
        val popups = parsePopups(uiObj.optJSONArray("popups") ?: JSONArray())
        val manual = parseManual(packageName, manualObj)

        HephaistosModule(
            packageName = packageName,
            displayName = displayName,
            versionName = versionName,
            description = description,
            iconKey = iconKey,
            defaultLayout = defaultLayout,
            serviceClassName = serviceClassName,
            receiverClassName = receiverClassName,
            elements = elements,
            popups = popups,
            manual = manual,
            rawManifestJson = root.toString(2)
        )
    }

    private fun parseElements(array: JSONArray): List<UiElementSchema> {
        val result = mutableListOf<UiElementSchema>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val type = obj.optString("type", "card").lowercase()
            val id = obj.optString("id", "elem_$i")
            val title = obj.optString("title", id)
            val subtitle = obj.optString("subtitle").takeIf { it.isNotBlank() }
            val icon = obj.optString("icon", defaultIconForType(type))
            val commandId = obj.optString("command_id", id)

            val element: UiElementSchema = when (type) {
                "toggle" -> UiElementSchema.ToggleOption(
                    id = id,
                    title = title,
                    subtitle = subtitle,
                    iconKey = icon,
                    defaultValue = obj.optBoolean("default", false),
                    commandId = commandId
                )

                "dropdown" -> {
                    val optsArray = obj.optJSONArray("options") ?: JSONArray()
                    val opts = (0 until optsArray.length()).mapNotNull { idx ->
                        optsArray.optString(idx).takeIf { it.isNotBlank() }
                    }.ifEmpty { listOf("Par défaut") }
                    val defaultVal = obj.optString("default").takeIf { it in opts } ?: opts.first()
                    UiElementSchema.DropdownOption(
                        id = id,
                        title = title,
                        subtitle = subtitle,
                        iconKey = icon,
                        options = opts,
                        defaultValue = defaultVal,
                        commandId = commandId
                    )
                }

                "file_picker" -> UiElementSchema.FilePickerOption(
                    id = id,
                    title = title,
                    subtitle = subtitle,
                    iconKey = icon,
                    mimeFilter = obj.optString("mime_filter", "*/*"),
                    allowDirectory = obj.optBoolean("allow_directory", false),
                    commandId = commandId
                )

                "text_input" -> UiElementSchema.TextInputOption(
                    id = id,
                    title = title,
                    subtitle = subtitle,
                    iconKey = icon,
                    placeholder = obj.optString("placeholder", ""),
                    defaultValue = obj.optString("default", ""),
                    commandId = commandId
                )

                "action", "action_button" -> UiElementSchema.ActionButtonOption(
                    id = id,
                    title = title,
                    subtitle = subtitle,
                    iconKey = icon,
                    commandId = commandId,
                    isPrimary = obj.optBoolean("primary", true),
                    confirmPopupId = obj.optString("confirm_popup_id").takeIf { it.isNotBlank() }
                )

                else -> UiElementSchema.OptionCard(
                    id = id,
                    title = title,
                    subtitle = subtitle,
                    iconKey = icon,
                    commandId = obj.optString("command_id").takeIf { it.isNotBlank() },
                    popupId = obj.optString("popup_id").takeIf { it.isNotBlank() }
                )
            }
            result.add(element)
        }
        return result
    }

    private fun parsePopups(array: JSONArray): Map<String, PopupSchema> {
        val map = mutableMapOf<String, PopupSchema>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
            map[id] = PopupSchema(
                id = id,
                title = obj.optString("title", "Confirmation"),
                message = obj.optString("message", ""),
                confirmLabel = obj.optString("confirm_label", "Confirmer"),
                cancelLabel = obj.optString("cancel_label", "Annuler"),
                commandId = obj.optString("command_id").takeIf { it.isNotBlank() }
            )
        }
        return map
    }

    private fun parseManual(packageName: String, obj: JSONObject): ModuleManual {
        val commandsArray = obj.optJSONArray("commands") ?: JSONArray()
        val commands = mutableListOf<ModuleCommandManual>()
        for (i in 0 until commandsArray.length()) {
            val cmdObj = commandsArray.optJSONObject(i) ?: continue
            val paramsArray = cmdObj.optJSONArray("parameters") ?: JSONArray()
            val params = (0 until paramsArray.length()).mapNotNull { idx ->
                val p = paramsArray.optJSONObject(idx) ?: return@mapNotNull null
                ModuleCommandParam(
                    name = p.optString("name", "param_$idx"),
                    type = p.optString("type", "string"),
                    required = p.optBoolean("required", false),
                    description = p.optString("description", ""),
                    defaultValue = p.optString("default").takeIf { it.isNotBlank() }
                )
            }
            val cmdId = cmdObj.optString("id", "cmd_$i")
            val exampleJson = cmdObj.opt("example")?.toString()
                ?: """{"command":"$cmdId","params":{}}"""

            commands.add(
                ModuleCommandManual(
                    commandId = cmdId,
                    title = cmdObj.optString("title", cmdId),
                    description = cmdObj.optString("description", ""),
                    intentAction = cmdObj.optString("action", HephaistosContract.ACTION_EXECUTE),
                    parameters = params,
                    returns = cmdObj.optString("returns", "Broadcast ${HephaistosContract.ACTION_COMMAND_RESULT}"),
                    exampleJson = exampleJson
                )
            )
        }
        return ModuleManual(
            protocolVersion = obj.optString("protocol_version", HephaistosContract.PROTOCOL_VERSION),
            summary = obj.optString("summary", "Module pilotable via Intents HEPHAISTOS ($packageName)"),
            targetServiceAction = obj.optString("target_action", HephaistosContract.ACTION_EXECUTE),
            broadcastCallbackAction = obj.optString("callback_action", HephaistosContract.ACTION_COMMAND_RESULT),
            commands = commands
        )
    }

    private fun defaultIconForType(type: String): String = when (type) {
        "toggle" -> "toggle"
        "dropdown" -> "list"
        "file_picker" -> "folder"
        "text_input" -> "edit"
        "action", "action_button" -> "play"
        else -> "tune"
    }
}
