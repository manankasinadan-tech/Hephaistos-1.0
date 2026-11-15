# Architecture HEPHAISTOS — Séparation UI / Logique (FR / EN)

## 1. Vue d'ensemble (FR)

HEPHAISTOS (`com.hephaistos.launcher`) est le **point d'entrée graphique unique** de tout l'écosystème. Les autres applications installées sur l'appareil dont le package suit la convention `com.hephaistos.<nom_du_module>` sont des **moteurs métier purs** (headless) :

- **Zéro UI dans les modules** : aucune `Activity` de lancement, aucun layout XML, aucun thème Compose propre.
- **Toute l'UI vit dans HEPHAISTOS** : thème clair/sombre/système, composants uniformes, gestionnaire de fichiers en popup, terminal intégré, popups minimales et notifications de progression.
- **Inter-opérabilité directe entre modules** : grâce au **mode d'emploi (`manual`)** embarqué dans chaque APK, un module `com.hephaistos.alpha` peut découvrir ou invoquer les commandes d'un module `com.hephaistos.beta` sans couplage binaire.

```
┌────────────────────────────────────────────────────────────────────┐
│               HEPHAISTOS (com.hephaistos.launcher)                 │
│  • Lanceur & Découverte PackageManager (com.hephaistos.*)          │
│  • Moteur de rendu UI JSON (Liste verticale / Grille)              │
│  • Services partagés : File Picker Popup, Terminal, Notifications  │
└───────────────┬──────────────────────────────────▲─────────────────┘
                │ ACTION_EXECUTE (Intent + JSON)   │ LOG / PROGRESS /
                │                                  │ POPUP / RESULT
┌───────────────▼──────────────────────────────────┴─────────────────┐
│  Module A: com.hephaistos.<moteur_a>   (Service + Manifest JSON)   │
│  • Zéro Activity • Logique pure • Mode d'emploi "manual" embarqué  │
└───────────────┬──────────────────────────────────▲─────────────────┘
                │ Commande inter-modules           │ Résultat Intent
┌───────────────▼──────────────────────────────────┴─────────────────┐
│  Module B: com.hephaistos.<moteur_b>   (Service + Manifest JSON)   │
└────────────────────────────────────────────────────────────────────┘
```

---

## 2. Architecture Overview (EN)

HEPHAISTOS (`com.hephaistos.launcher`) acts as the **single UI shell and launcher** for all companion modules under `com.hephaistos.<module_name>`:

1. **Zero UI inside feature APKs**: Modules do not declare a `MAIN/LAUNCHER` Activity and do not ship Jetpack Compose or XML screens.
2. **Declarative UI Contract**: Modules declare their interface using a lightweight JSON manifest (`ui` section) attached via `<meta-data>` on their exported `<service>` or `<receiver>`.
3. **Self-Documenting Capabilities (`manual`)**: Every module bundles a machine-readable `manual` section listing every command, parameter type, Intent action, and return format so other modules (and AI code generators) can orchestrate it reliably.
4. **Shared System Infrastructure**:
   - **File Picker Popup**: Modules declare a `file_picker` field; HEPHAISTOS handles the UI popup + Android SAF (`OpenDocument` / `OpenDocumentTree`) and sends the resulting URI/path to the module.
   - **Integrated Terminal**: Modules emit `com.hephaistos.action.LOG` broadcasts; HEPHAISTOS renders all stdout/stderr/IPC logs in a unified monospace terminal.
   - **Notifications**: Modules emit `com.hephaistos.action.PROGRESS`; HEPHAISTOS maintains Android notification channels with progress bars and Cancel/Open actions.
