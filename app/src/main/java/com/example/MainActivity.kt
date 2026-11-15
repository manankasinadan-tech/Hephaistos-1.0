package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ipc.HephaistosContract
import com.example.notifications.HephaistosNotificationManager
import com.example.ui.HephaistosViewModel
import com.example.ui.components.DockTab
import com.example.ui.components.FilePickerPopup
import com.example.ui.components.HephaistosDock
import com.example.ui.components.IntegratedTerminalSheet
import com.example.ui.components.MinimalPopupDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ModuleDetailScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.HephaistosTheme

object HephaistosRoutes {
    const val ROUTE_HOME = "home"
    const val ROUTE_SETTINGS = "settings"
    const val ROUTE_MODULE = "module/{packageName}"

    fun moduleRoute(packageName: String): String = "module/$packageName"
}

class MainActivity : ComponentActivity() {

    private var pendingModulePackage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        HephaistosNotificationManager(this).ensureChannels()
        pendingModulePackage = intent?.getStringExtra(HephaistosContract.EXTRA_OPEN_MODULE_PACKAGE)

        setContent {
            val vm: HephaistosViewModel = viewModel()
            val themeMode by vm.themeMode.collectAsStateWithLifecycle()

            HephaistosTheme(themeMode = themeMode) {
                HephaistosApp(
                    viewModel = vm,
                    pendingModulePackage = pendingModulePackage,
                    onConsumedPendingModule = { pendingModulePackage = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(HephaistosContract.EXTRA_OPEN_MODULE_PACKAGE)?.let {
            pendingModulePackage = it
        }
    }
}

@Composable
fun HephaistosApp(
    viewModel: HephaistosViewModel,
    pendingModulePackage: String?,
    onConsumedPendingModule: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val defaultLayout by viewModel.defaultLayout.collectAsStateWithLifecycle()
    val modules by viewModel.modules.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val moduleLayoutOverrides by viewModel.moduleLayoutOverrides.collectAsStateWithLifecycle()
    val moduleFieldValues by viewModel.moduleFieldValues.collectAsStateWithLifecycle()
    val terminalEntries by viewModel.terminalEntries.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val activePopup by viewModel.activePopup.collectAsStateWithLifecycle()
    val activeFilePicker by viewModel.activeFilePicker.collectAsStateWithLifecycle()
    val recentPickedFiles by viewModel.recentPickedFiles.collectAsStateWithLifecycle()

    var homeLayoutOverride by remember { mutableStateOf<com.example.model.LayoutMode?>(null) }
    val effectiveHomeLayout = homeLayoutOverride ?: defaultLayout

    var showTerminal by remember { mutableStateOf(false) }
    var terminalFilterPackage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pendingModulePackage, modules) {
        if (!pendingModulePackage.isNullOrBlank() && modules.any { it.packageName == pendingModulePackage }) {
            navController.navigate(HephaistosRoutes.moduleRoute(pendingModulePackage)) {
                launchSingleTop = true
            }
            onConsumedPendingModule()
        }
    }

    val showDock = currentRoute == HephaistosRoutes.ROUTE_HOME ||
        currentRoute == HephaistosRoutes.ROUTE_SETTINGS

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showDock) {
                val selectedTab = if (currentRoute == HephaistosRoutes.ROUTE_SETTINGS) {
                    DockTab.SETTINGS
                } else {
                    DockTab.HOME
                }
                HephaistosDock(
                    selectedTab = selectedTab,
                    onSelectTab = { tab ->
                        val targetRoute = when (tab) {
                            DockTab.HOME -> HephaistosRoutes.ROUTE_HOME
                            DockTab.SETTINGS -> HephaistosRoutes.ROUTE_SETTINGS
                        }
                        if (currentRoute != targetRoute) {
                            navController.navigate(targetRoute) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            NavHost(
                navController = navController,
                startDestination = HephaistosRoutes.ROUTE_HOME
            ) {
                composable(HephaistosRoutes.ROUTE_HOME) {
                    HomeScreen(
                        modules = modules,
                        isScanning = isScanning,
                        layoutMode = effectiveHomeLayout,
                        tasks = tasks,
                        onToggleLayout = {
                            homeLayoutOverride = if (effectiveHomeLayout == com.example.model.LayoutMode.LIST) {
                                com.example.model.LayoutMode.GRID
                            } else {
                                com.example.model.LayoutMode.LIST
                            }
                        },
                        onRefresh = { viewModel.refreshModules() },
                        onOpenTerminal = {
                            terminalFilterPackage = null
                            showTerminal = true
                        },
                        onOpenFilePicker = { viewModel.openFilePicker() },
                        onOpenModule = { module ->
                            navController.navigate(HephaistosRoutes.moduleRoute(module.packageName))
                        },
                        onCancelTask = { viewModel.cancelTask(it) },
                        onClearCompletedTasks = { viewModel.clearCompletedTasks() }
                    )
                }

                composable(HephaistosRoutes.ROUTE_SETTINGS) {
                    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
                    val storageGranted by viewModel.storageGranted.collectAsStateWithLifecycle()
                    val rootStatus by viewModel.rootStatus.collectAsStateWithLifecycle()
                    val autoGrantSatellites by viewModel.autoGrantSatellites.collectAsStateWithLifecycle()
                    val isGrantingPrivileges by viewModel.isGrantingPrivileges.collectAsStateWithLifecycle()
                    val lastPrivilegeReport by viewModel.lastPrivilegeReport.collectAsStateWithLifecycle()
                    SettingsScreen(
                        themeMode = themeMode,
                        defaultLayout = defaultLayout,
                        storageGranted = storageGranted,
                        rootStatus = rootStatus,
                        autoGrantSatellites = autoGrantSatellites,
                        isGrantingPrivileges = isGrantingPrivileges,
                        lastPrivilegeReport = lastPrivilegeReport,
                        satellitesCount = modules.size,
                        onSelectTheme = { viewModel.setThemeMode(it) },
                        onSelectDefaultLayout = {
                            homeLayoutOverride = null
                            viewModel.setDefaultLayout(it)
                        },
                        onGrantStorageClicked = { fallbackLaunch ->
                            viewModel.tryGrantSelfStorageViaRootIfActive(fallbackLaunch)
                        },
                        onGrantRootClicked = { viewModel.requestRootAndApplyToSatellites() },
                        onToggleAutoGrantSatellites = { viewModel.setAutoGrantSatellites(it) },
                        onApplySatellitesNow = { viewModel.applyPrivilegesToSatellitesNow() },
                        onRefreshPrivilegeStatus = { viewModel.refreshPrivilegeStatus() },
                        onOpenTerminal = {
                            terminalFilterPackage = null
                            showTerminal = true
                        },
                        onOpenFilePicker = { viewModel.openFilePicker() }
                    )
                }

                composable(
                    route = HephaistosRoutes.ROUTE_MODULE,
                    arguments = listOf(navArgument("packageName") { type = NavType.StringType })
                ) { entry ->
                    val pkg = entry.arguments?.getString("packageName").orEmpty()
                    val module = modules.firstOrNull { it.packageName == pkg }
                    if (module != null) {
                        val effectiveLayout = moduleLayoutOverrides[pkg] ?: module.defaultLayout
                        val fieldValues = moduleFieldValues[pkg].orEmpty()
                        val moduleTasks = tasks.values.filter { it.modulePackage == pkg }
                        ModuleDetailScreen(
                            module = module,
                            effectiveLayout = effectiveLayout,
                            fieldValues = fieldValues,
                            moduleTasks = moduleTasks,
                            onBack = { navController.popBackStack() },
                            onToggleLayout = { viewModel.toggleModuleLayout(pkg, effectiveLayout) },
                            onOpenTerminal = {
                                terminalFilterPackage = pkg
                                showTerminal = true
                            },
                            onValueChanged = { element, newVal, dispatch ->
                                val cmdId = when (element) {
                                    is com.example.model.UiElementSchema.ToggleOption -> element.commandId
                                    is com.example.model.UiElementSchema.DropdownOption -> element.commandId
                                    is com.example.model.UiElementSchema.TextInputOption -> element.commandId
                                    is com.example.model.UiElementSchema.FilePickerOption -> element.commandId
                                    else -> null
                                }
                                viewModel.updateFieldAndDispatch(module, element.id, newVal, cmdId, dispatch)
                            },
                            onElementTriggered = { element ->
                                viewModel.triggerElement(module, element)
                            },
                            onRunManualCommand = { cmd ->
                                viewModel.executeCommandForModule(
                                    packageName = module.packageName,
                                    commandId = cmd.commandId,
                                    params = fieldValues
                                )
                                terminalFilterPackage = pkg
                                showTerminal = true
                            }
                        )
                    } else {
                        LaunchedEffect(pkg) {
                            navController.popBackStack()
                        }
                    }
                }
            }
        }
    }

    // Integrated Terminal Sheet
    if (showTerminal) {
        IntegratedTerminalSheet(
            entries = terminalEntries,
            filterPackage = terminalFilterPackage,
            onExecuteCommand = { viewModel.executeTerminalCommand(it) },
            onClear = { viewModel.clearTerminal() },
            onDismiss = { showTerminal = false }
        )
    }

    // Integrated File Picker Popup
    activeFilePicker?.let { request ->
        FilePickerPopup(
            request = request,
            fileHelper = viewModel.fileHelper,
            recentFiles = recentPickedFiles,
            onWorkspaceFileSelected = { viewModel.onWorkspaceFilePicked(it) },
            onSafUriSelected = { uri, isDir -> viewModel.onSafPicked(uri, isDir) },
            onDismiss = { viewModel.closeFilePicker() }
        )
    }

    // Minimal Popup Dialog
    activePopup?.let { request ->
        MinimalPopupDialog(
            request = request,
            onConfirm = { viewModel.confirmActivePopup(request) },
            onDismiss = { viewModel.dismissActivePopup() }
        )
    }
}
