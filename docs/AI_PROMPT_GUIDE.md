# Guide & Prompt Maître pour toute IA — Créer un Module HEPHAISTOS (FR / EN)

Ce document est conçu pour être **copié-collé directement dans le prompt système ou utilisateur de n'importe quelle IA** afin qu'elle génère un nouveau module Android (`com.hephaistos.<nom_du_module>`) **100 % fonctionnel, puissant, sans interface graphique, conforme aux règles d'interface HEPHAISTOS et capable de commander / être commandé par d'autres modules**.

---

## Prompt Prêt à l'Emploi / Turnkey AI Prompt

```markdown
Tu dois créer une application Android (Kotlin, Gradle Kotlin DSL) appartenant à l'écosystème HEPHAISTOS.

### RÈGLES STRICTES OBLIGATOIRES
1. **Package / Application ID** : `com.hephaistos.<nom_du_module>` (ex: `com.hephaistos.pdfengine`).
2. **ZÉRO INTERFACE GRAPHIQUE** :
   - Ne crée AUCUNE `Activity`, AUCUN XML de layout, AUCUN composable Jetpack Compose.
   - N'ajoute AUCUN intent-filter `MAIN` / `LAUNCHER` dans `AndroidManifest.xml`.
   - HEPHAISTOS (`com.hephaistos.launcher`) fournit 100 % de l'interface visuelle, du thème (Clair/Sombre/Système), du sélecteur de fichiers en popup, du terminal intégré et des notifications.
3. **Découverte par HEPHAISTOS (`AndroidManifest.xml`)** :
   - Déclare `<queries>` pour `com.hephaistos.launcher` et les actions `com.hephaistos.action.MODULE` et `com.hephaistos.action.EXECUTE`.
   - Déclare un `<service android:name=".ModuleEngineService" android:exported="true">` portant :
     - `<intent-filter>` avec `com.hephaistos.action.MODULE` et `com.hephaistos.action.EXECUTE`
     - `<meta-data android:name="com.hephaistos.module.manifest" android:resource="@raw/hephaistos_manifest" />`
   - Déclare un `<receiver>` écoutant `com.hephaistos.action.CANCEL_TASK` si des tâches longues peuvent être annulées.
4. **Manifeste JSON & Mode d'emploi (`res/raw/hephaistos_manifest.json`)** :
   - Définis la section `"ui"` uniquement avec les composants officiels HEPHAISTOS :
     - `default_layout`: `"list"` ou `"grid"`
     - `elements`: tableau d'objets parmi `card`, `toggle`, `dropdown`, `file_picker`, `text_input`, `action_button`
     - `popups`: tableau de popups minimales (`id`, `title`, `message`, `confirm_label`, `cancel_label`, `command_id`)
     - `icon`: parmi `terminal`, `folder`, `tune`, `toggle`, `list`, `play`, `sync`, `shield`, `bolt`, `settings`, `code`, `description`, `storage`, `notifications`, `layers`, `edit`, `search`, `download`, `upload`, `build`, `extension`.
   - Définis obligatoirement la section `"manual"` (mode d'emploi machine-readable) listant :
     - `protocol_version`: `"1.0"`
     - `summary`: rôle du module et capacité inter-modules
     - `target_action`: `"com.hephaistos.action.EXECUTE"`
     - `callback_action`: `"com.hephaistos.action.COMMAND_RESULT"`
     - `commands`: chaque commande avec `id`, `title`, `description`, `action`, `parameters` (`name`, `type`, `required`, `default`, `description`), `returns`, et `example`.
5. **Protocole d'exécution & Communication HEPHAISTOS** :
   - À la réception de `com.hephaistos.action.EXECUTE`, lis les extras :
     - `hephaistos.extra.COMMAND_ID` (String)
     - `hephaistos.extra.PARAMS_JSON` (String JSON)
     - `hephaistos.extra.SOURCE_PACKAGE` (String — peut être `com.hephaistos.launcher` OU un autre module `com.hephaistos.*` !)
   - Émets systématiquement les logs dans le Terminal intégré de HEPHAISTOS via broadcast vers `com.hephaistos.launcher` (`action = "com.hephaistos.action.LOG"`, extras `SOURCE_PACKAGE`, `LEVEL` = `INFO|OUT|ERR`, `MESSAGE`).
   - Émets l'avancement des tâches longues vers les notifications et l'UI via `action = "com.hephaistos.action.PROGRESS"` (extras `SOURCE_PACKAGE`, `TASK_ID`, `TITLE`, `MESSAGE`, `PROGRESS` 0..100 ou -1, `COMPLETED`, `ERROR`, `CAN_CANCEL`).
   - Retourne le résultat structuré JSON à la fois à `SOURCE_PACKAGE` (si différent de `com.hephaistos.launcher`) ET à `com.hephaistos.launcher` via `action = "com.hephaistos.action.COMMAND_RESULT"` (`COMMAND_ID`, `SOURCE_PACKAGE`, `RESULT_JSON`, `ERROR`).
6. **Capacité d'appeler d'autres modules (`HephaistosModuleClient`)** :
   - Implémente un utilitaire qui interroge `PackageManager` sur `com.hephaistos.action.MODULE`, lit le `manual` JSON d'un module cible `com.hephaistos.*` via `getResourcesForApplication(...)`, et lui envoie une commande via `com.hephaistos.action.EXECUTE`.
7. **Qualité & Sécurité Android** :
   - Kotlin + Coroutines (`Dispatchers.IO`).
   - Si le module utilise un `ForegroundService`, déclare `foregroundServiceType` explicite dans le manifeste et promeut en foreground uniquement le temps du traitement.
   - Zéro permission de stockage large (`READ_EXTERNAL_STORAGE` interdit) : consomme directement les `content://` URIs transmises par le File Picker popup de HEPHAISTOS via `ContentResolver`.

### ARCHITECTURE ATTENDUE DU MODULE À GÉNÉRER
- `AndroidManifest.xml` (headless, `<queries>`, `<service>`, `<meta-data>`)
- `res/raw/hephaistos_manifest.json` (`ui` + `manual` complets)
- `HephaistosBridge.kt` (émission `LOG`, `PROGRESS`, `SHOW_POPUP`, `COMMAND_RESULT` + client d'appel inter-modules lisant les `manual` distants)
- `ModuleEngineService.kt` (dispatcher des commandes + coroutines)
- Couche métier pure (classes Kotlin testables unitairement sans dépendance UI).
```

---

## Liste de contrôle avant soumission / AI Verification Checklist

Avant de finaliser un nouveau module `com.hephaistos.<nom>`, l'IA doit vérifier :
- [ ] `applicationId` commence bien par `com.hephaistos.` (et n'est pas `com.hephaistos.launcher`).
- [ ] Aucune `Activity` ni dépendance Compose UI inutile n'alourdit le module.
- [ ] `res/raw/hephaistos_manifest.json` est un JSON valide contenant `ui.elements` ET `manual.commands`.
- [ ] Chaque `command_id` utilisé dans `ui.elements` correspond à une commande documentée dans `manual.commands`.
- [ ] Tous les logs passent par `com.hephaistos.action.LOG` et toutes les tâches longues utilisent `com.hephaistos.action.PROGRESS`.
- [ ] Le module gère `hephaistos.extra.SOURCE_PACKAGE` pour renvoyer son résultat aussi bien à HEPHAISTOS qu'à tout autre module appelant.
