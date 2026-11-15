package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.ui.graphics.vector.ImageVector

object HephaistosIcons {
    fun resolve(key: String): ImageVector = when (key.lowercase().trim()) {
        "terminal", "console", "shell" -> Icons.Outlined.Terminal
        "folder", "file", "files", "picker" -> Icons.Outlined.Folder
        "tune", "options", "config" -> Icons.Outlined.Tune
        "toggle", "switch" -> Icons.Outlined.ToggleOn
        "list", "dropdown", "menu" -> Icons.Outlined.FormatListBulleted
        "play", "run", "action", "start" -> Icons.Outlined.PlayArrow
        "sync", "refresh" -> Icons.Outlined.Sync
        "shield", "security", "lock" -> Icons.Outlined.Shield
        "bolt", "fast", "power" -> Icons.Outlined.Bolt
        "settings", "gear" -> Icons.Outlined.Settings
        "code", "script", "json" -> Icons.Outlined.Code
        "description", "doc", "manual", "note" -> Icons.Outlined.Description
        "storage", "disk", "db" -> Icons.Outlined.Storage
        "notifications", "bell", "alert" -> Icons.Outlined.Notifications
        "layers", "stack" -> Icons.Outlined.Layers
        "edit", "input", "text" -> Icons.Outlined.Edit
        "search", "find" -> Icons.Outlined.Search
        "download", "export" -> Icons.Outlined.Download
        "upload", "import" -> Icons.Outlined.Upload
        "build", "tool", "engine" -> Icons.Outlined.Build
        else -> Icons.Outlined.Extension
    }
}
