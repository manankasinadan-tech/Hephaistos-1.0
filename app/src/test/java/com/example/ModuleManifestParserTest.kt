package com.example

import com.example.data.ModuleManifestParser
import com.example.model.LayoutMode
import com.example.model.UiElementSchema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ModuleManifestParserTest {

    @Test
    fun `parse complete hephaistos manifest with ui and manual`() {
        val rawJson = """
            {
              "name": "Media Engine",
              "description": "Traitement audio/vidéo headless",
              "icon": "bolt",
              "ui": {
                "default_layout": "grid",
                "elements": [
                  {
                    "id": "src_file",
                    "type": "file_picker",
                    "title": "Fichier source",
                    "mime_filter": "*/*",
                    "command_id": "set_source"
                  },
                  {
                    "id": "hw_accel",
                    "type": "toggle",
                    "title": "Accélération matérielle",
                    "default": true,
                    "command_id": "set_hw_accel"
                  },
                  {
                    "id": "preset",
                    "type": "dropdown",
                    "title": "Profil",
                    "options": ["Rapide", "Équilibré", "Qualité"],
                    "default": "Équilibré",
                    "command_id": "set_preset"
                  },
                  {
                    "id": "run_btn",
                    "type": "action_button",
                    "title": "Exécuter",
                    "primary": true,
                    "command_id": "run_pipeline"
                  }
                ],
                "popups": [
                  {
                    "id": "confirm_run",
                    "title": "Confirmer",
                    "message": "Lancer le traitement ?",
                    "command_id": "run_pipeline"
                  }
                ]
              },
              "manual": {
                "protocol_version": "1.0",
                "summary": "Moteur de conversion contrôlable par Intents.",
                "commands": [
                  {
                    "id": "run_pipeline",
                    "title": "Lancer le pipeline",
                    "description": "Convertit le fichier fourni",
                    "parameters": [
                      { "name": "src_file", "type": "uri", "required": true, "description": "URI du fichier" }
                    ],
                    "returns": "Broadcast com.hephaistos.action.COMMAND_RESULT"
                  }
                ]
              }
            }
        """.trimIndent()

        val result = ModuleManifestParser.parse(
            packageName = "com.hephaistos.media",
            fallbackName = "Media",
            versionName = "1.2.0",
            serviceClassName = "com.hephaistos.media.EngineService",
            receiverClassName = null,
            rawJson = rawJson
        )

        assertTrue(result.isSuccess)
        val module = result.getOrThrow()
        assertEquals("Media Engine", module.displayName)
        assertEquals(LayoutMode.GRID, module.defaultLayout)
        assertEquals(4, module.elements.size)
        assertTrue(module.elements[0] is UiElementSchema.FilePickerOption)
        assertTrue(module.elements[1] is UiElementSchema.ToggleOption)
        assertTrue(module.elements[2] is UiElementSchema.DropdownOption)
        assertTrue(module.elements[3] is UiElementSchema.ActionButtonOption)
        assertEquals(1, module.popups.size)
        assertEquals(1, module.manual.commands.size)
        assertEquals("run_pipeline", module.manual.commands.first().commandId)
    }
}
