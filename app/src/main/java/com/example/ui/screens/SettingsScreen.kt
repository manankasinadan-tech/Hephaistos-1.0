package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.RootStatus
import com.example.ipc.HephaistosContract
import com.example.model.LayoutMode
import com.example.model.ThemeMode

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    defaultLayout: LayoutMode,
    storageGranted: Boolean,
    rootStatus: RootStatus,
    autoGrantSatellites: Boolean,
    isGrantingPrivileges: Boolean,
    lastPrivilegeReport: String?,
    satellitesCount: Int,
    onSelectTheme: (ThemeMode) -> Unit,
    onSelectDefaultLayout: (LayoutMode) -> Unit,
    onGrantStorageClicked: (() -> Unit) -> Unit,
    onGrantRootClicked: () -> Unit,
    onToggleAutoGrantSatellites: (Boolean) -> Unit,
    onApplySatellitesNow: () -> Unit,
    onRefreshPrivilegeStatus: () -> Unit,
    onOpenTerminal: () -> Unit,
    onOpenFilePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var notifGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshPrivilegeStatus()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifGranted = granted
    }

    val legacyStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onRefreshPrivilegeStatus()
    }

    val launchSystemStorageGrant: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            runCatching { context.startActivity(intent) }.onFailure {
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            }
        } else {
            legacyStorageLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Paramètres",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Apparence, autorisations système & satellites",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Storage & Root permissions + Auto-grant satellites
                item {
                    SettingsSectionCard(title = "AUTORISATIONS STOCKAGE, ROOT & SATELLITES") {
                        // Storage permission row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.SdStorage,
                                    contentDescription = null,
                                    tint = if (storageGranted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Autorisation de stockage",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (storageGranted) {
                                            "Accès complet au stockage accordé"
                                        } else {
                                            "Accès tous fichiers requis pour les modules"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { onGrantStorageClicked(launchSystemStorageGrant) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("settings_grant_storage_button")
                            ) {
                                Text(if (storageGranted) "Accordé" else "Accorder")
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Root access row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = if (rootStatus.isGranted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Autorisation Root (su)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = rootStatus.methodLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            if (isGrantingPrivileges) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Button(
                                    onClick = onGrantRootClicked,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (rootStatus.isGranted) {
                                            MaterialTheme.colorScheme.secondary
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    ),
                                    modifier = Modifier.testTag("settings_grant_root_button")
                                ) {
                                    Text(if (rootStatus.isGranted) "Actif" else "Accorder Root")
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Auto-grant to all satellite modules
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clickable { onToggleAutoGrantSatellites(!autoGrantSatellites) }
                                .padding(14.dp)
                                .testTag("settings_auto_grant_toggle"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Attribution automatique aux satellites",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Accorde automatiquement root, stockage et permissions à chaque com.hephaistos.*",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = autoGrantSatellites,
                                onCheckedChange = onToggleAutoGrantSatellites
                            )
                        }

                        if (satellitesCount > 0 || !lastPrivilegeReport.isNullOrBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Column(modifier = Modifier.padding(14.dp)) {
                                if (!lastPrivilegeReport.isNullOrBlank()) {
                                    Text(
                                        text = lastPrivilegeReport,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                if (satellitesCount > 0) {
                                    OutlinedButton(
                                        onClick = onApplySatellitesNow,
                                        enabled = !isGrantingPrivileges,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("settings_apply_satellites_button")
                                    ) {
                                        Text("Appliquer maintenant aux $satellitesCount satellite(s)")
                                    }
                                }
                            }
                        }
                    }
                }

                // Theme selector (Sombre, Clair, Système)
                item {
                    SettingsSectionCard(title = "THÈME DE L'INTERFACE") {
                        ThemeOptionRow(
                            label = "Clair",
                            subtitle = "Palette douce albâtre et ardoise",
                            icon = Icons.Outlined.LightMode,
                            selected = themeMode == ThemeMode.LIGHT,
                            testTag = "theme_option_light",
                            onClick = { onSelectTheme(ThemeMode.LIGHT) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ThemeOptionRow(
                            label = "Sombre",
                            subtitle = "Palette feutrée obsidienne et brume",
                            icon = Icons.Outlined.DarkMode,
                            selected = themeMode == ThemeMode.DARK,
                            testTag = "theme_option_dark",
                            onClick = { onSelectTheme(ThemeMode.DARK) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ThemeOptionRow(
                            label = "Suivre le système",
                            subtitle = "S'adapte automatiquement à Android",
                            icon = Icons.Outlined.BrightnessAuto,
                            selected = themeMode == ThemeMode.SYSTEM,
                            testTag = "theme_option_system",
                            onClick = { onSelectTheme(ThemeMode.SYSTEM) }
                        )
                    }
                }

                // Default layout selector
                item {
                    SettingsSectionCard(title = "DISPOSITION PAR DÉFAUT") {
                        ThemeOptionRow(
                            label = "Liste verticale",
                            subtitle = "Affichage épuré ligne par ligne",
                            icon = Icons.Outlined.ViewAgenda,
                            selected = defaultLayout == LayoutMode.LIST,
                            testTag = "layout_option_list",
                            onClick = { onSelectDefaultLayout(LayoutMode.LIST) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ThemeOptionRow(
                            label = "Grille modulaire",
                            subtitle = "Cartes compactes sur deux colonnes",
                            icon = Icons.Outlined.GridView,
                            selected = defaultLayout == LayoutMode.GRID,
                            testTag = "layout_option_grid",
                            onClick = { onSelectDefaultLayout(LayoutMode.GRID) }
                        )
                    }
                }

                // Notifications permission
                item {
                    SettingsSectionCard(title = "NOTIFICATIONS & PROGRESSION") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (notifGranted) "Notifications actives" else "Autoriser les notifications",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Affiche la progression et les actions des modules",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (!notifGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("settings_request_notif_button")
                                ) {
                                    Text("Activer")
                                }
                            }
                        }
                    }
                }

                // Shared Tools & Docs
                item {
                    SettingsSectionCard(title = "OUTILS & DOCUMENTATION") {
                        SettingsActionRow(
                            label = "Terminal intégré HEPHAISTOS",
                            subtitle = "Logs IPC en direct et commandes système",
                            icon = Icons.Outlined.Terminal,
                            testTag = "settings_terminal_button",
                            onClick = onOpenTerminal
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsActionRow(
                            label = "Gestionnaire de fichiers (Popup)",
                            subtitle = "Espace HEPHAISTOS et sélecteur système SAF",
                            icon = Icons.Outlined.FolderOpen,
                            testTag = "settings_files_button",
                            onClick = onOpenFilePicker
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsActionRow(
                            label = "Documentation & Guide IA",
                            subtitle = HephaistosContract.GITHUB_DOCS_URL,
                            icon = Icons.AutoMirrored.Outlined.OpenInNew,
                            testTag = "settings_docs_button",
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(HephaistosContract.GITHUB_DOCS_URL))
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RadioButton(
            selected = selected,
            onClick = onClick
        )
    }
}

@Composable
private fun SettingsActionRow(
    label: String,
    subtitle: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
