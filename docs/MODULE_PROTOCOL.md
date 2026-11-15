# Protocole de Module HEPHAISTOS & Mode d'Emploi Inter-Modules (FR / EN)

Ce document définit **exactement** comment un module futur (`com.hephaistos.<nom_du_module>`) est détecté par HEPHAISTOS, comment il déclare son interface, comment il expose son **mode d'emploi (`manual`)** et comment deux modules peuvent se commander mutuellement.

---

## 1. Convention de nommage & Visibilité Android

1. **Application ID / Package Name obligatoire :**
   ```kotlin
   applicationId = "com.hephaistos.<nom_du_module>"
   ```
   *(Exemple : `com.hephaistos.compressor`, `com.hephaistos.ocr`, `com.hephaistos.sync`)*

2. **Aucune `Activity` de lancement (`MAIN/LAUNCHER`) :**
   Le module ne doit déclarer **aucune** activité dans son `AndroidManifest.xml` (zéro icône dans le tiroir Android système, zéro UI propre).

3. **Déclaration dans `AndroidManifest.xml` du module :**
   - Ajoutez `<queries>` pour découvrir et commander les autres modules `com.hephaistos.*` ainsi que HEPHAISTOS (`com.hephaistos.launcher`).
   - Exposez un `<service>` (ou `<receiver>`) avec `android:exported="true"` et les filtres d'intent HEPHAISTOS + `<meta-data>` pointant vers `res/raw/hephaistos_manifest.json`.

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <queries>
        <package android:name="com.hephaistos.launcher" />
        <intent>
            <action android:name="com.hephaistos.action.MODULE" />
        </intent>
        <intent>
            <action android:name="com.hephaistos.action.EXECUTE" />
        </intent>
    </queries>

    <application
        android:allowBackup="false"
        android:label="@string/app_name">

        <service
            android:name=".EngineService"
            android:exported="true">
            <intent-filter>
                <action android:name="com.hephaistos.action.MODULE" />
                <action android:name="com.hephaistos.action.EXECUTE" />
            </intent-filter>
            <meta-data
                android:name="com.hephaistos.module.manifest"
                android:resource="@raw/hephaistos_manifest" />
        </service>

        <!-- Optionnel : écoute l'annulation depuis les notifications HEPHAISTOS -->
        <receiver
            android:name=".CancelTaskReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="com.hephaistos.action.CANCEL_TASK" />
            </intent-filter>
        </receiver>

    </application>
</manifest>
```

---

## 2. Schéma complet de `res/raw/hephaistos_manifest.json`

Chaque module **DOIT** embarquer ce fichier JSON contenant :
- Les métadonnées (`name`, `description`, `icon`)
- La section `ui` (interprétée par HEPHAISTOS pour générer l'interface)
- La section `manual` (**le mode d'emploi** lisible par HEPHAISTOS et par tous les autres modules)

```json
{
  "name": "Mon Moteur",
  "description": "Description courte du moteur sans interface",
  "icon": "bolt",
  "ui": {
    "default_layout": "list",
    "elements": [
      {
        "id": "input_file",
        "type": "file_picker",
        "title": "Fichier d'entrée",
        "subtitle": "Ouvre le gestionnaire de fichiers HEPHAISTOS",
        "icon": "folder",
        "mime_filter": "*/*",
        "allow_directory": false,
        "command_id": "set_input"
      },
      {
        "id": "auto_mode",
        "type": "toggle",
        "title": "Mode automatique",
        "subtitle": "Optimise les paramètres automatiquement",
        "icon": "toggle",
        "default": true,
        "command_id": "set_auto_mode"
      },
      {
        "id": "quality_level",
        "type": "dropdown",
        "title": "Niveau de précision",
        "subtitle": "Sélectionnez un profil",
        "icon": "list",
        "options": ["Rapide", "Équilibré", "Maximum"],
        "default": "Équilibré",
        "command_id": "set_quality"
      },
      {
        "id": "custom_tag",
        "type": "text_input",
        "title": "Étiquette de sortie",
        "icon": "edit",
        "placeholder": "ex: export_v1",
        "default": "",
        "command_id": "set_tag"
      },
      {
        "id": "info_card",
        "type": "card",
        "title": "Vérifier l'état du moteur",
        "subtitle": "Exécute un diagnostic rapide",
        "icon": "tune",
        "command_id": "health_check"
      },
      {
        "id": "start_job",
        "type": "action_button",
        "title": "Lancer le traitement",
        "icon": "play",
        "primary": true,
        "confirm_popup_id": "confirm_start",
        "command_id": "run_job"
      }
    ],
    "popups": [
      {
        "id": "confirm_start",
        "title": "Démarrer le moteur ?",
        "message": "Le traitement s'exécutera en arrière-plan avec suivi dans les notifications et le terminal.",
        "confirm_label": "Lancer",
        "cancel_label": "Annuler",
        "command_id": "run_job"
      }
    ]
  },
  "manual": {
    "protocol_version": "1.0",
    "summary": "Moteur headless exposant des commandes de traitement de fichiers pilotables par HEPHAISTOS ou par un module tiers.",
    "target_action": "com.hephaistos.action.EXECUTE",
    "callback_action": "com.hephaistos.action.COMMAND_RESULT",
    "commands": [
      {
        "id": "run_job",
        "title": "Exécuter le traitement",
        "description": "Traite le fichier désigné par input_file et diffuse la progression.",
        "action": "com.hephaistos.action.EXECUTE",
        "parameters": [
          {
            "name": "input_file",
            "type": "uri_or_path",
            "required": true,
            "description": "URI SAF ou chemin local retourné par le file_picker"
          },
          {
            "name": "quality_level",
            "type": "string",
            "required": false,
            "default": "Équilibré",
            "description": "Profil parmi Rapide | Équilibré | Maximum"
          }
        ],
        "returns": "Broadcast com.hephaistos.action.COMMAND_RESULT avec hephaistos.extra.RESULT_JSON",
        "example": {
          "command": "run_job",
          "params": {
            "input_file": "content://...",
            "quality_level": "Équilibré"
          }
        }
      }
    ]
  }
}
```

---

## 3. Constantes IPC officielles (`HephaistosContract`)

### Actions
| Constante | Valeur | Direction |
| :--- | :--- | :--- |
| Découverte | `com.hephaistos.action.MODULE` | HEPHAISTOS / Module → Module |
| Exécution commande | `com.hephaistos.action.EXECUTE` | HEPHAISTOS / Module → Module |
| Annulation tâche | `com.hephaistos.action.CANCEL_TASK` | HEPHAISTOS → Module |
| Log vers Terminal | `com.hephaistos.action.LOG` | Module → HEPHAISTOS |
| Progression & Notification | `com.hephaistos.action.PROGRESS` | Module → HEPHAISTOS |
| Afficher Popup minimale | `com.hephaistos.action.SHOW_POPUP` | Module → HEPHAISTOS |
| Résultat de commande | `com.hephaistos.action.COMMAND_RESULT` | Module → HEPHAISTOS / Appelant |

### Extras d'Intent
- `hephaistos.extra.COMMAND_ID` (`String`)
- `hephaistos.extra.PARAMS_JSON` (`String` JSON objet clé/valeur)
- `hephaistos.extra.SOURCE_PACKAGE` (`String` package émetteur)
- `hephaistos.extra.TASK_ID` (`String`)
- `hephaistos.extra.TITLE` (`String`)
- `hephaistos.extra.MESSAGE` (`String`)
- `hephaistos.extra.LEVEL` (`String`: `"CMD"`, `"INFO"`, `"OUT"`, `"ERR"`)
- `hephaistos.extra.PROGRESS` (`Int`: `0..100`, ou `-1` si indéterminé)
- `hephaistos.extra.COMPLETED` (`Boolean`)
- `hephaistos.extra.ERROR` (`Boolean`)
- `hephaistos.extra.CAN_CANCEL` (`Boolean`)
- `hephaistos.extra.RESULT_JSON` (`String` JSON)

---

## 4. Comment un module commande un autre module (Inter-Module Orchestration)

Tout module `com.hephaistos.A` peut lire le mode d'emploi d'un module `com.hephaistos.B` via `PackageManager` (`com.hephaistos.module.manifest`), puis lui envoyer un `Intent(com.hephaistos.action.EXECUTE)` :

```kotlin
// Exemple Kotlin : Module A découvrant et commandant Module B
fun callRemoteHephaistosModule(
    context: Context,
    targetPackage: String,
    commandId: String,
    paramsJson: String
) {
    val intent = Intent("com.hephaistos.action.EXECUTE").apply {
        setPackage(targetPackage)
        putExtra("hephaistos.extra.COMMAND_ID", commandId)
        putExtra("hephaistos.extra.PARAMS_JSON", paramsJson)
        putExtra("hephaistos.extra.SOURCE_PACKAGE", context.packageName)
    }
    // Résoudre le service cible via PackageManager puis démarrer le service ou envoyer un broadcast
    val services = context.packageManager.queryIntentServices(intent, 0)
    val targetService = services.firstOrNull()?.serviceInfo
    if (targetService != null) {
        intent.component = ComponentName(targetService.packageName, targetService.name)
        ContextCompat.startForegroundService(context, intent)
    } else {
        context.sendBroadcast(intent)
    }
}
```

### Envoi de logs au Terminal & Progression vers Notifications depuis un module

```kotlin
fun emitLogToHephaistos(context: Context, level: String, message: String) {
    val intent = Intent("com.hephaistos.action.LOG").apply {
        setPackage("com.hephaistos.launcher")
        putExtra("hephaistos.extra.SOURCE_PACKAGE", context.packageName)
        putExtra("hephaistos.extra.LEVEL", level) // "INFO", "OUT", "ERR"
        putExtra("hephaistos.extra.MESSAGE", message)
    }
    context.sendBroadcast(intent)
}

fun emitProgressToHephaistos(
    context: Context,
    taskId: String,
    title: String,
    status: String,
    progress: Int,
    completed: Boolean = false
) {
    val intent = Intent("com.hephaistos.action.PROGRESS").apply {
        setPackage("com.hephaistos.launcher")
        putExtra("hephaistos.extra.SOURCE_PACKAGE", context.packageName)
        putExtra("hephaistos.extra.TASK_ID", taskId)
        putExtra("hephaistos.extra.TITLE", title)
        putExtra("hephaistos.extra.MESSAGE", status)
        putExtra("hephaistos.extra.PROGRESS", progress)
        putExtra("hephaistos.extra.COMPLETED", completed)
    }
    context.sendBroadcast(intent)
}
```

---

## 5. Attribution automatique Root, Stockage & Permissions aux Satellites

Lorsque l'utilisateur accorde **Root (`su`)** et active l'option **Attribution automatique aux satellites** dans **Paramètres** de HEPHAISTOS :

1. **Permissions déclarées (`pm grant`)** : HEPHAISTOS inspecte les `requestedPermissions` du satellite via `PackageManager` et accorde automatiquement toutes ses permissions runtime sans interaction manuelle.
2. **Stockage complet (`appops`)** : HEPHAISTOS accorde automatiquement `MANAGE_EXTERNAL_STORAGE` et `LEGACY_STORAGE` à chaque satellite `com.hephaistos.*`.
3. **Accès Root automatique** :
   - Sous **Magisk**, HEPHAISTOS injecte automatiquement la règle `policy=2` pour l'UID du satellite dans la base Magisk (`magisk --sqlite`).
   - Sur tout environnement root (Magisk, KernelSU, APatch, `su`), le satellite peut aussi lier le service **`com.hephaistos.action.ROOT_BRIDGE`** (`HephaistosRootService`, qui vérifie l'UID appelant `com.hephaistos.*`) via `Messenger` (`what = 1`, bundle `"command"` -> réponse `what = 2`, `"exit_code"`, `"output"`).

