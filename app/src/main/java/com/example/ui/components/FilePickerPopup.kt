package com.example.ui.components

import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.WorkspaceFileHelper
import com.example.model.WorkspaceFileItem
import com.example.ui.FilePickerRequest
import java.io.File

@Composable
fun FilePickerPopup(
    request: FilePickerRequest,
    fileHelper: WorkspaceFileHelper,
    recentFiles: List<WorkspaceFileItem>,
    onWorkspaceFileSelected: (WorkspaceFileItem) -> Unit,
    onSafUriSelected: (Uri, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val workspaceRoot = remember { fileHelper.getWorkspaceRoot() }
    var currentDir by remember { mutableStateOf(workspaceRoot) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var showNewFolderInput by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    val entries = remember(currentDir, refreshTrigger) {
        fileHelper.listDirectory(currentDir)
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onSafUriSelected(uri, false)
        }
    }

    val openTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            onSafUriSelected(uri, true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GESTIONNAIRE DE FICHIERS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = request.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("file_picker_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Fermer le gestionnaire de fichiers"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // System SAF zero-permission picker button
                Button(
                    onClick = {
                        if (request.allowDirectory) {
                            openTreeLauncher.launch(null)
                        } else {
                            openDocumentLauncher.launch(arrayOf(request.mimeFilter))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("file_picker_saf_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (request.allowDirectory) {
                            "Parcourir un dossier système"
                        } else {
                            "Parcourir les fichiers de l'appareil"
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                // Local Workspace bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (currentDir.absolutePath != workspaceRoot.absolutePath) {
                            IconButton(
                                onClick = {
                                    currentDir = currentDir.parentFile ?: workspaceRoot
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Dossier parent"
                                )
                            }
                        }
                        Text(
                            text = "Espace HEPHAISTOS /" +
                                currentDir.relativeTo(workspaceRoot).path.ifBlank { "" },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { showNewFolderInput = !showNewFolderInput },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CreateNewFolder,
                            contentDescription = "Nouveau dossier",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (showNewFolderInput) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newFolderName,
                            onValueChange = { newFolderName = it },
                            placeholder = { Text("Nom du dossier") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                fileHelper.createWorkspaceFolder(currentDir, newFolderName)
                                    .onSuccess {
                                        newFolderName = ""
                                        showNewFolderInput = false
                                        refreshTrigger++
                                    }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Créer")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Directory list + recent SAF picks
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (entries.isEmpty() && recentFiles.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Text(
                                    text = "Aucun fichier dans l'espace HEPHAISTOS. Utilisez le bouton ci-dessus pour choisir un fichier depuis votre appareil.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }

                    items(entries, key = { it.pathOrUri }) { item ->
                        FileRowItem(
                            item = item,
                            subtitle = if (item.isDirectory) {
                                "Dossier local"
                            } else {
                                "${Formatter.formatShortFileSize(context, item.sizeBytes)} • ${item.mimeType}"
                            },
                            onClick = {
                                if (item.isDirectory && !request.allowDirectory) {
                                    currentDir = File(item.pathOrUri)
                                } else {
                                    onWorkspaceFileSelected(item)
                                }
                            }
                        )
                    }

                    if (recentFiles.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "RÉCENTS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        items(recentFiles, key = { "recent_${it.pathOrUri}" }) { recent ->
                            FileRowItem(
                                item = recent,
                                subtitle = if (recent.isExternalSaf) {
                                    "SAF • ${recent.mimeType}"
                                } else {
                                    Formatter.formatShortFileSize(context, recent.sizeBytes)
                                },
                                onClick = { onWorkspaceFileSelected(recent) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileRowItem(
    item: WorkspaceFileItem,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (item.isDirectory) Icons.Outlined.Folder else Icons.Outlined.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
