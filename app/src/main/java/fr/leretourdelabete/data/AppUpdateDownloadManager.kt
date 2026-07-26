package fr.leretourdelabete.data

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import fr.leretourdelabete.BuildConfig
import java.io.File

data class PendingAppUpdateDownload(
    val id: Long,
    val version: String,
    val fileName: String,
)

data class AppUpdateDownloadSnapshot(
    val status: AppUpdateDownloadStatus,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val reason: Int = 0,
)

enum class AppUpdateDownloadStatus {
    RUNNING,
    SUCCESSFUL,
    FAILED,
    MISSING,
}

sealed interface AppUpdateInstallResult {
    data object Started : AppUpdateInstallResult
    data object PermissionRequired : AppUpdateInstallResult
    data object FileMissing : AppUpdateInstallResult
    data object NoInstaller : AppUpdateInstallResult
}

class AppUpdateDownloadManager(context: Context) {
    private val appContext = context.applicationContext
    private val downloadManager =
        appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val preferences =
        appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun enqueue(update: AppUpdate): PendingAppUpdateDownload {
        val safeVersion = update.latestVersion.replace(UNSAFE_FILE_CHARS, "_")
        val fileName = "le-retour-de-la-bete-$safeVersion.apk"
        val destination = destinationFile(fileName)
        if (destination.exists() && !destination.delete()) {
            error("Impossible de remplacer l’ancien fichier de mise à jour.")
        }

        val request = DownloadManager.Request(Uri.parse(update.launchUrl))
            .setTitle("Le Retour de la Bête ${update.latestVersion}")
            .setDescription("Téléchargement de la mise à jour")
            .setMimeType(APK_MIME_TYPE)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED,
            )
            .setDestinationInExternalFilesDir(
                appContext,
                Environment.DIRECTORY_DOWNLOADS,
                fileName,
            )

        val pending = PendingAppUpdateDownload(
            id = downloadManager.enqueue(request),
            version = update.latestVersion,
            fileName = fileName,
        )
        preferences.edit()
            .putLong(KEY_DOWNLOAD_ID, pending.id)
            .putString(KEY_VERSION, pending.version)
            .putString(KEY_FILE_NAME, pending.fileName)
            .apply()
        return pending
    }

    fun pendingDownload(): PendingAppUpdateDownload? {
        val id = preferences.getLong(KEY_DOWNLOAD_ID, NO_DOWNLOAD_ID)
        val version = preferences.getString(KEY_VERSION, null)
        val fileName = preferences.getString(KEY_FILE_NAME, null)
        if (id == NO_DOWNLOAD_ID || version.isNullOrBlank() || fileName.isNullOrBlank()) {
            return null
        }
        return PendingAppUpdateDownload(id, version, fileName)
    }

    fun snapshot(pending: PendingAppUpdateDownload): AppUpdateDownloadSnapshot {
        val query = DownloadManager.Query().setFilterById(pending.id)
        downloadManager.query(query)?.use { cursor ->
            if (!cursor.moveToFirst()) {
                return AppUpdateDownloadSnapshot(AppUpdateDownloadStatus.MISSING)
            }

            val status = cursor.getInt(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS),
            )
            val downloadedBytes = cursor.getLong(
                cursor.getColumnIndexOrThrow(
                    DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR,
                ),
            )
            val totalBytes = cursor.getLong(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES),
            )
            val reason = cursor.getInt(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON),
            )
            val resolvedStatus = when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    if (destinationFile(pending.fileName).length() > 0L) {
                        AppUpdateDownloadStatus.SUCCESSFUL
                    } else {
                        AppUpdateDownloadStatus.FAILED
                    }
                }
                DownloadManager.STATUS_FAILED -> AppUpdateDownloadStatus.FAILED
                else -> AppUpdateDownloadStatus.RUNNING
            }
            return AppUpdateDownloadSnapshot(
                status = resolvedStatus,
                downloadedBytes = downloadedBytes.coerceAtLeast(0L),
                totalBytes = totalBytes.coerceAtLeast(0L),
                reason = reason,
            )
        }
        return AppUpdateDownloadSnapshot(AppUpdateDownloadStatus.MISSING)
    }

    fun install(pending: PendingAppUpdateDownload): AppUpdateInstallResult {
        val apk = destinationFile(pending.fileName)
        if (!apk.isFile || apk.length() == 0L) {
            return AppUpdateInstallResult.FileMissing
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !appContext.packageManager.canRequestPackageInstalls()
        ) {
            return AppUpdateInstallResult.PermissionRequired
        }

        val apkUri = FileProvider.getUriForFile(
            appContext,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            apk,
        )
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(apkUri, APK_MIME_TYPE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return runCatching {
            appContext.startActivity(intent)
            AppUpdateInstallResult.Started
        }.getOrElse {
            AppUpdateInstallResult.NoInstaller
        }
    }

    fun openInstallPermissionSettings(): Boolean {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${appContext.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            appContext.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun clear(pending: PendingAppUpdateDownload) {
        if (preferences.getLong(KEY_DOWNLOAD_ID, NO_DOWNLOAD_ID) == pending.id) {
            preferences.edit().clear().apply()
        }
    }

    fun remove(pending: PendingAppUpdateDownload) {
        downloadManager.remove(pending.id)
        destinationFile(pending.fileName).delete()
        clear(pending)
    }

    fun clearIfAlreadyInstalled(currentVersion: String) {
        val pending = pendingDownload() ?: return
        if (!isVersionNewer(pending.version, currentVersion)) {
            remove(pending)
        }
    }

    private fun destinationFile(fileName: String): File {
        val downloadsDirectory =
            appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: error("Le stockage de téléchargement est indisponible.")
        return File(downloadsDirectory, fileName)
    }

    private companion object {
        const val PREFERENCES_NAME = "app_update_download"
        const val KEY_DOWNLOAD_ID = "download_id"
        const val KEY_VERSION = "version"
        const val KEY_FILE_NAME = "file_name"
        const val NO_DOWNLOAD_ID = -1L
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        val UNSAFE_FILE_CHARS = Regex("[^A-Za-z0-9._-]")
    }
}
