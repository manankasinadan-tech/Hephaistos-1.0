# HEPHAISTOS (`com.hephaistos.launcher`)

> **Guide complet de création des modules & spécification IA :**
> 👉 **[https://rakotonirinatdaniel.github.io/hephaistos/](https://rakotonirinatdaniel.github.io/hephaistos/)**

---

## Description suggérée pour le dépôt GitHub (*About*)

Copiez ces informations dans la section **About** de votre dépôt GitHub (`RakotonirinaTDaniel/hephaistos`) :

- **Description :** `Lanceur minimaliste et moteur d'interface unifié pour les modules Android sans interface com.hephaistos.* — Documentation & Guide IA : https://rakotonirinatdaniel.github.io/hephaistos/`
- **Website :** `https://rakotonirinatdaniel.github.io/hephaistos/`

---

## Français — Présentation

**HEPHAISTOS** (`com.hephaistos.launcher`) est un lanceur Android minimaliste et une **couche d'interface graphique unifiée** conçue pour piloter un écosystème d'applications modulaires (`com.hephaistos.<nom_du_module>`) **totalement dépourvues d'interface propre**.

### Principes fondamentaux
1. **Séparation stricte UI / Logique** : Les modules futurs (`com.hephaistos.*`) sont des moteurs purs (Services / BroadcastReceivers) sans aucune Activity ni écran.
2. **Interface centralisée & uniforme** : HEPHAISTOS embarque le thème (*Soft Alabaster* Clair / *Velvet Obsidian* Sombre / Système), les composants visuels, la popup gestionnaire de fichiers, le terminal intégré et les notifications de progression.
3. **Mode d'emploi embarqué (*Inter-Module Manual*)** : Chaque module embarque son propre mode d'emploi JSON décrivant ses commandes, paramètres et retours pour que HEPHAISTOS et **d'autres modules** puissent le commander automatiquement.
4. **Prêt pour GitHub Actions** : Compilation automatique des tests et de l'APK Debug à chaque push via `.github/workflows/android-ci.yml`.

### Documentation complète
- [Architecture & Séparation UI / Logique](docs/ARCHITECTURE.md)
- [Règles d'Interface & Charte Graphique](docs/UI_RULES.md)
- [Protocole de Détection, Schéma UI & Mode d'Emploi](docs/MODULE_PROTOCOL.md)
- [Guide & Prompt Maître pour toute IA (Créer un module)](docs/AI_PROMPT_GUIDE.md)

---

## English — Overview

**HEPHAISTOS** (`com.hephaistos.launcher`) is a minimalist Android launcher and **unified UI engine** for an ecosystem of headless modular Android apps (`com.hephaistos.<module_name>`) that ship **zero UI of their own**.

### Core Pillars
1. **Strict UI / Logic Separation**: Future modules (`com.hephaistos.*`) contain pure background engines (Services / BroadcastReceivers) with no Activities or XML/Compose screens.
2. **Unified Design & Shared System Tools**: HEPHAISTOS renders every module's interface dynamically and provides a unified soft theme (Light / Dark / System), built-in File Picker popup (SAF + workspace), integrated Terminal, and system Progress Notifications.
3. **Embedded Machine-Readable Manual**: Every module bundles a JSON manual (`manual`) declaring its commands, parameter schemas, and Intent contracts so HEPHAISTOS and **other modules** can discover and orchestrate it.
4. **GitHub Actions Ready**: Automated unit tests + APK build (`.github/workflows/android-ci.yml`) and automated GitHub Pages deployment (`.github/workflows/pages.yml`).

### Compilation locale / Local Build
```bash
gradle :app:testDebugUnitTest :app:assembleDebug
```
