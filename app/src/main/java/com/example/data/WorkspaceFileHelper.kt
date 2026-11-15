package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.WorkspaceFileItem
import java.io.File

class WorkspaceFileHelper(private val context: Context) {

    fun getWorkspaceRoot(): File {
        val dir = File(context.filesDir, "hephaistos_workspace")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun listDirectory(directory: File = getWorkspaceRoot()): List<WorkspaceFileItem> {
        val target = if (directory.exists() && directory.isDirectory) directory else getWorkspaceRoot()
        val children = target.listFiles()?.toList().orEmpty()
        return children
            .sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
            .map { file ->
                WorkspaceFileItem(
                    name = file.name,
                    pathOrUri = file.absolutePath,
                    sizeBytes = if (file.isDirectory) 0L else file.length(),
                    mimeType = if (file.isDirectory) "inode/directory" else guessMime(file.name),
                    isDirectory = file.isDirectory,
                    isExternalSaf = false
                )
            }
    }

    fun createWorkspaceFolder(parent: File, folderName: String): Result<File> = runCatching {
        val sanitized = folderName.trim().replace(Regex("[/\\\\:*?\"<>|]"), "_")
        require(sanitized.isNotEmpty()) { "Nom de dossier invalide" }
        val newDir = File(parent, sanitized)
        if (!newDir.exists() && !newDir.mkdirs()) {
            error("Impossible de créer le dossier $sanitized")
        }
        newDir
    }

    fun describeSafUri(uri: Uri, isDirectory: Boolean = false): WorkspaceFileItem {
        runCatching {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        }

        var displayName = uri.lastPathSegment ?: "document"
        var size = 0L

        if (!isDirectory) {
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIdx >= 0) {
                            cursor.getString(nameIdx)?.let { displayName = it }
                        }
                        if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                            size = cursor.getLong(sizeIdx)
                        }
                    }
                }
            }
        }

        val mime = if (isDirectory) {
            "inode/directory"
        } else {
            context.contentResolver.getType(uri) ?: guessMime(displayName)
        }

        return WorkspaceFileItem(
            name = displayName,
            pathOrUri = uri.toString(),
            sizeBytes = size,
            mimeType = mime,
            isDirectory = isDirectory,
            isExternalSaf = true
        )
    }

    private fun guessMime(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "json" -> "application/json"
            "txt", "md", "log" -> "text/plain"
            "pdf" -> "application/pdf"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "mp4" -> "video/mp4"
            "zip" -> "application/zip"
            else -> "application/octet-stream"
        }
    }
}
