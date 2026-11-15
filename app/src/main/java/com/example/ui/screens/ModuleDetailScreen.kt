package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.HephaistosModule
import com.example.model.LayoutMode
import com.example.model.ModuleCommandManual
import com.example.model.ModuleTaskProgress
import com.example.model.UiElementSchema
import com.example.ui.components.ModuleManualSheet
import com.example.ui.components.ModuleUiRenderer
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun ModuleDetailScreen(
    module: HephaistosModule,
    effectiveLayout: LayoutMode,
    fieldValues: Map<String, String>,
    moduleTasks: List<ModuleTaskProgress>,
    onBack: () -> Unit,
    onToggleLayout: () -> Unit,
    onOpenTerminal: () -> Unit,
    onValueChanged: (UiElementSchema, String, Boolean) -> Unit,
    onElementTriggered: (UiElementSchema) -> Unit,
    onRunManualCommand: (ModuleCommandManual) -> Unit,
    modifier: Modifier = Modifier
) {
    var showManualSheet by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("module_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Retour à l'accueil"
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = module.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = module.packageName,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onToggleLayout,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("module_layout_toggle_button")
                ) {
                    Icon(
                        imageVector = if (effectiveLayout == LayoutMode.LIST) {
                            Icons.Outlined.GridView
                        } else {
                            Icons.Outlined.ViewAgenda
                        },
                        contentDescription = "Basculer entre liste et grille"
                    )
                }

                IconButton(
                    onClick = { showManualSheet = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("module_manual_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = "Voir le mode d'emploi du module"
                    )
                }

                IconButton(
                    onClick = onOpenTerminal,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("module_terminal_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Terminal,
                        contentDescription = "Ouvrir le terminal du module",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            val runningTasks = moduleTasks.filter { !it.isCompleted && !it.isError }
            if (runningTasks.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    runningTasks.forEach { task ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "${task.title} — ${task.status}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                val pct = task.progress
                                if (pct != null) {
                                    LinearProgressIndicator(
                                        progress = { pct / 100f },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }
            }

            ModuleUiRenderer(
                elements = module.elements,
                layoutMode = effectiveLayout,
                fieldValues = fieldValues,
                onValueChanged = onValueChanged,
                onElementTriggered = onElementTriggered,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showManualSheet) {
        ModuleManualSheet(
            module = module,
            onRunCommand = { cmd ->
                showManualSheet = false
                onRunManualCommand(cmd)
            },
            onDismiss = { showManualSheet = false }
        )
    }
}
