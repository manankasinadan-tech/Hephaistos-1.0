# Règles d'Interface HEPHAISTOS / Unified UI Rules (FR / EN)

Toutes les interfaces générées par HEPHAISTOS pour les modules `com.hephaistos.*` obéissent strictement à ce système de design uniforme, doux et épuré. **Aucun module ne doit contourner ces règles.**

---

## 1. Palette Uniforme « Soft, Douce & Élégante »

### Thème Clair (*Soft Alabaster*)
| Rôle | Token | Hex |
| :--- | :--- | :--- |
| Fond principal | `background` | `#F7F6F2` |
| Surface carte | `surface` | `#FFFFFF` |
| Surface secondaire | `surfaceContainerLow` | `#F2F0EB` |
| Accent principal | `primary` | `#4A6572` |
| Conteneur accent | `primaryContainer` | `#DCE5EA` |
| Accent secondaire | `secondary` | `#7C8B7A` |
| Texte principal | `onSurface` | `#1E2022` |
| Texte secondaire | `onSurfaceVariant` | `#63686E` |
| Bordure hairline | `outlineVariant` | `#E9E5DD` |

### Thème Sombre (*Velvet Obsidian*)
| Rôle | Token | Hex |
| :--- | :--- | :--- |
| Fond principal | `background` | `#141619` |
| Surface carte | `surface` | `#1C1F24` |
| Surface secondaire | `surfaceContainerLow` | `#181B20` |
| Accent principal | `primary` | `#8FAEC0` |
| Conteneur accent | `primaryContainer` | `#293D48` |
| Accent secondaire | `secondary` | `#9CB09A` |
| Texte principal | `onSurface` | `#ECEEF1` |
| Texte secondaire | `onSurfaceVariant` | `#9BA1A8` |
| Bordure hairline | `outlineVariant` | `#272C34` |

### Règle de thème
- HEPHAISTOS gère **Clair**, **Sombre** et **Suivre le système** depuis l'onglet **Paramètres** (persisté via DataStore).
- Les modules héritent automatiquement du thème actif puisqu'ils ne possèdent aucune UI propre.

---

## 2. Typographie & Géométrie
- **Police UI** : `Plus Jakarta Sans` (titres et libellés doux, lisibles).
- **Police Technique / Terminal** : `JetBrains Mono` (identifiants de paquets, flux terminal, JSON du mode d'emploi).
- **Rayons d'arrondi** : Cartes `16.dp`, Champs/Boutons `12.dp`, Popups `20.dp`.
- **Bordures & Élévation** : Privilégier une bordure fine `1.dp` (`outlineVariant`) et des écarts de luminance subtils plutôt que des ombres lourdes.
- **Cibles tactiles** : Minimum `48.dp x 48.dp` sur chaque élément interactif.

---

## 3. Catalogue des Composants Autorisés (UI Building Blocks)

Chaque module déclare ses composants dans la section `ui.elements` de son manifeste JSON :

1. **Accueil minimaliste (`HomeScreen`)**
   - Affiche uniquement les applications installées au préfixe `com.hephaistos.*`.
   - Dock inférieur minimal à 2 boutons : **Accueil** et **Paramètres**.
2. **Disposition Liste verticale ou Grille (`default_layout`: `"list"` | `"grid"`)**
   - Chaque module définit sa disposition par défaut et l'utilisateur peut basculer entre Liste et Grille à tout moment depuis l'en-tête.
3. **Carte d'option simple (`type: "card"`)**
   - Icône Material contenue dans un badge arrondi + Titre + Sous-titre optionnel. Peut déclencher une commande ou ouvrir une popup minimale.
4. **Toggle (`type: "toggle"`)**
   - Interrupteur d'activation/désactivation d'une option avec synchronisation immédiate vers le module.
5. **Liste déroulante simple (`type: "dropdown"`)**
   - Champ épuré ouvrant un menu de sélection à choix unique (`options[]`).
6. **File Picker en popup (`type: "file_picker"`)**
   - Champ affichant le fichier/dossier sélectionné. Au clic, ouvre **le gestionnaire de fichiers intégré de HEPHAISTOS en popup** (navigation dans l'espace local HEPHAISTOS + bouton zéro-permission Android SAF `OpenDocument` / `OpenDocumentTree`).
7. **Champ texte minimal (`type: "text_input"`)**
   - Saisie simple sur une ligne avec bouton de validation.
8. **Bouton d'action (`type: "action_button"`)**
   - Bouton principal (`primary: true`) ou secondaire (`primary: false`), avec confirmation optionnelle par popup minimale.
9. **Popup minimale (`ui.popups[]` ou broadcast `ACTION_SHOW_POPUP`)**
   - Boîte de dialogue épurée : badge du module, titre, message, bouton Annuler, bouton Confirmer.
10. **Notifications intégrées (`ACTION_PROGRESS`)**
    - Toute progression longue et toute action utilisateur (Annuler / Ouvrir) s'intègrent dans les canaux de notification HEPHAISTOS (`hephaistos_progress` et `hephaistos_actions`).
11. **Terminal intégré (`ACTION_LOG`)**
    - Tous les modules envoient leurs sorties et erreurs vers le terminal intégré de HEPHAISTOS, filtrable par module et doté d'une ligne de commande (`help`, `list`, `inspect`, `exec`, shell).

---

## 4. Icônes standards supportées (`icon`)

Valeurs reconnues dans le manifeste JSON :
`terminal`, `folder`, `tune`, `toggle`, `list`, `play`, `sync`, `shield`, `bolt`, `settings`, `code`, `description`, `storage`, `notifications`, `layers`, `edit`, `search`, `download`, `upload`, `build`, `extension`.
