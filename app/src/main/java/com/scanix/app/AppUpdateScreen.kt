package com.scanix.app

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val SCANIX_UPDATE_INFO_URL =
    "https://raw.githubusercontent.com/ymflap/Scanix-QR/main/updates/latest.json"

private const val SCANIX_GITHUB_LATEST_RELEASE_URL =
    "https://api.github.com/repos/ymflap/Scanix-QR/releases/latest"

private const val SCANIX_UPDATE_PREFS = "scanix_update_state"
private const val PREF_DOWNLOAD_ID = "download_id"
private const val PREF_DOWNLOAD_URL = "download_url"
private const val PREF_DOWNLOAD_VERSION = "download_version"
private const val PREF_DOWNLOAD_VERSION_CODE = "download_version_code"
private const val PREF_DOWNLOAD_SIZE = "download_expected_size"

private data class ScanixUpdateInfo(
    val versionCode: Long?,
    val versionName: String,
    val updateUrl: String,
    val releaseNotes: String,
    val expectedSizeBytes: Long? = null
)

private data class ScanixDownloadStatus(
    val status: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val reason: Int
)

@Composable
fun AppUpdateScreen(
    visible: Boolean = true,
    downloadRequestId: Int = 0,
    onUpdateAvailable: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val updateChannel = BuildConfig.UPDATE_CHANNEL

    val currentVersion = remember(context) {
        getInstalledVersion(
            context.packageManager.getPackageInfo(
                context.packageName,
                0
            )
        )
    }

    val preferences = remember(context) {
        context.getSharedPreferences(
            SCANIX_UPDATE_PREFS,
            Context.MODE_PRIVATE
        )
    }

    val downloadManager = remember(context) {
        context.getSystemService(
            Context.DOWNLOAD_SERVICE
        ) as DownloadManager
    }

    var isChecking by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(-1) }
    var completedDownloadId by remember { mutableStateOf<Long?>(null) }
    var activeDownloadId by remember { mutableStateOf<Long?>(null) }
    var updateInfo by remember {
        mutableStateOf<ScanixUpdateInfo?>(null)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastHandledDownloadRequest by remember { mutableStateOf(0) }
    var lastNotifiedVersion by remember { mutableStateOf<String?>(null) }

    fun savedDownloadId(): Long {
        return preferences.getLong(PREF_DOWNLOAD_ID, -1L)
    }

    fun clearSavedDownload() {
        preferences.edit()
            .remove(PREF_DOWNLOAD_ID)
            .remove(PREF_DOWNLOAD_URL)
            .remove(PREF_DOWNLOAD_VERSION)
            .remove(PREF_DOWNLOAD_VERSION_CODE)
            .remove(PREF_DOWNLOAD_SIZE)
            .apply()

        activeDownloadId = null
        completedDownloadId = null
        isDownloading = false
        downloadProgress = -1
    }

    fun readDownloadStatus(id: Long): ScanixDownloadStatus? {
        return try {
            val query = DownloadManager.Query().setFilterById(id)

            downloadManager.query(query)?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    return null
                }

                val statusIndex = cursor.getColumnIndex(
                    DownloadManager.COLUMN_STATUS
                )
                val downloadedIndex = cursor.getColumnIndex(
                    DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
                )
                val totalIndex = cursor.getColumnIndex(
                    DownloadManager.COLUMN_TOTAL_SIZE_BYTES
                )
                val reasonIndex = cursor.getColumnIndex(
                    DownloadManager.COLUMN_REASON
                )

                if (
                    statusIndex < 0 ||
                    downloadedIndex < 0 ||
                    totalIndex < 0 ||
                    reasonIndex < 0
                ) {
                    return null
                }

                ScanixDownloadStatus(
                    status = cursor.getInt(statusIndex),
                    downloadedBytes = cursor.getLong(downloadedIndex),
                    totalBytes = cursor.getLong(totalIndex),
                    reason = cursor.getInt(reasonIndex)
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    fun checkForUpdates() {
        if (isChecking) return

        isChecking = true
        errorMessage = null

        Thread {
            var connection: HttpURLConnection? = null

            try {
                val result: ScanixUpdateInfo

                if (updateChannel == "github") {
                    connection = URL(
                        SCANIX_GITHUB_LATEST_RELEASE_URL
                    ).openConnection() as HttpURLConnection

                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.requestMethod = "GET"
                    connection.setRequestProperty(
                        "Accept",
                        "application/vnd.github+json"
                    )
                    connection.setRequestProperty(
                        "User-Agent",
                        "Scanix-QR-Android"
                    )
                    connection.setRequestProperty(
                        "Cache-Control",
                        "no-cache"
                    )

                    if (connection.responseCode !in 200..299) {
                        throw IllegalStateException(
                            "Published GitHub release unavailable"
                        )
                    }

                    val response = connection.inputStream
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }

                    val release = JSONObject(response)

                    if (
                        release.optBoolean("draft", true) ||
                        release.optBoolean("prerelease", true) ||
                        release.optString("published_at").isBlank()
                    ) {
                        throw IllegalStateException(
                            "No published stable release"
                        )
                    }

                    val versionName = release
                        .optString("tag_name", "")
                        .trim()

                    if (!isSupportedReleaseVersion(versionName)) {
                        throw IllegalStateException(
                            "Invalid published release version"
                        )
                    }

                    val assets = release.optJSONArray("assets")
                        ?: throw IllegalStateException(
                            "Release APK unavailable"
                        )

                    var apkUrl = ""
                    var apkSize: Long? = null

                    for (index in 0 until assets.length()) {
                        val asset = assets.optJSONObject(index)
                            ?: continue

                        if (
                            asset.optString("name") == "app-release.apk" &&
                            asset.optString("state") == "uploaded"
                        ) {
                            apkUrl = asset.optString(
                                "browser_download_url",
                                ""
                            )

                            apkSize = asset.optLong(
                                "size",
                                0L
                            ).takeIf { it > 0L }

                            break
                        }
                    }

                    val parsedUrl = Uri.parse(apkUrl)

                    if (
                        apkUrl.isBlank() ||
                        parsedUrl.scheme != "https" ||
                        parsedUrl.host != "github.com"
                    ) {
                        throw IllegalStateException(
                            "Published release APK unavailable"
                        )
                    }

                    result = ScanixUpdateInfo(
                        versionCode = null,
                        versionName = versionName,
                        updateUrl = apkUrl,
                        releaseNotes = "",
                        expectedSizeBytes = apkSize
                    )
                } else {
                    connection = URL(
                        SCANIX_UPDATE_INFO_URL
                    ).openConnection() as HttpURLConnection

                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.requestMethod = "GET"
                    connection.setRequestProperty(
                        "Accept",
                        "application/json"
                    )
                    connection.setRequestProperty(
                        "Cache-Control",
                        "no-cache"
                    )

                    if (connection.responseCode !in 200..299) {
                        throw IllegalStateException(
                            "Update information unavailable"
                        )
                    }

                    val response = connection.inputStream
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }

                    val root = JSONObject(response)
                    val channels = root.optJSONObject("channels")
                        ?: throw IllegalStateException(
                            "Missing update information"
                        )

                    val channel = channels.optJSONObject(updateChannel)
                        ?: throw IllegalStateException(
                            "Update channel unavailable"
                        )

                    val versionCode = channel.optLong(
                        "versionCode",
                        0L
                    )
                    val versionName = channel.optString(
                        "versionName",
                        ""
                    )
                    val updateUrl = channel.optString(
                        "updateUrl",
                        ""
                    )
                    val releaseNotes = channel.optString(
                        "releaseNotes",
                        ""
                    )
                    val expectedSize = channel.optLong(
                        "sizeBytes",
                        0L
                    )

                    if (
                        versionCode <= 0L ||
                        versionName.isBlank() ||
                        updateUrl.isBlank()
                    ) {
                        throw IllegalStateException(
                            "Invalid update information"
                        )
                    }

                    val parsedUrl = Uri.parse(updateUrl)

                    if (
                        parsedUrl.scheme != "https" ||
                        parsedUrl.host.isNullOrBlank()
                    ) {
                        throw IllegalStateException(
                            "Invalid update URL"
                        )
                    }

                    result = ScanixUpdateInfo(
                        versionCode = versionCode,
                        versionName = versionName,
                        updateUrl = updateUrl,
                        releaseNotes = releaseNotes,
                        expectedSizeBytes = expectedSize.takeIf {
                            it > 0L
                        }
                    )
                }

                android.os.Handler(
                    android.os.Looper.getMainLooper()
                ).post {
                    updateInfo = result
                    isChecking = false
                }
            } catch (_: Exception) {
                android.os.Handler(
                    android.os.Looper.getMainLooper()
                ).post {
                    errorMessage = localizedUpdateMessage(
                        context,
                        "تعذر التحقق من التحديث. تحقق من اتصال الإنترنت وحاول مرة أخرى.",
                        "Unable to check for updates. Check your internet connection and try again."
                    )
                    isChecking = false
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }
        fun startUpdateDownload(info: ScanixUpdateInfo) {
        if (info.updateUrl.isBlank()) return

        val parsedUrl = Uri.parse(info.updateUrl)

        if (
            parsedUrl.scheme != "https" ||
            parsedUrl.host.isNullOrBlank()
        ) {
            errorMessage = localizedUpdateMessage(
                context,
                "رابط تنزيل التحديث غير صالح.",
                "The update download URL is invalid."
            )
            return
        }

        val oldId = savedDownloadId()

        if (oldId > 0L) {
            val oldStatus = readDownloadStatus(oldId)

            if (
                oldStatus?.status == DownloadManager.STATUS_RUNNING ||
                oldStatus?.status == DownloadManager.STATUS_PENDING ||
                oldStatus?.status == DownloadManager.STATUS_PAUSED
            ) {
                activeDownloadId = oldId
                isDownloading = true
                errorMessage = null
                return
            }

            if (oldStatus?.status == DownloadManager.STATUS_SUCCESSFUL) {
                activeDownloadId = oldId
                completedDownloadId = oldId
                isDownloading = false
                downloadProgress = 100
                errorMessage = null
                return
            }

            clearSavedDownload()
        }

        try {
            val request = DownloadManager.Request(parsedUrl).apply {
                setTitle(
                    localizedUpdateMessage(
                        context,
                        "تحديث Scanix QR",
                        "Scanix QR update"
                    )
                )
                setDescription(
                    localizedUpdateMessage(
                        context,
                        "جارٍ تنزيل إصدار التطبيق الجديد",
                        "Downloading the new app version"
                    )
                )
                setMimeType(
                    "application/vnd.android.package-archive"
                )
                setNotificationVisibility(
                    DownloadManager.Request
                        .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                setAllowedOverMetered(true)
                setAllowedOverRoaming(false)

                setDestinationInExternalFilesDir(
                    context,
                    null,
                    "scanix-update.apk"
                )
            }

            val id = downloadManager.enqueue(request)

            preferences.edit()
                .putLong(PREF_DOWNLOAD_ID, id)
                .putString(PREF_DOWNLOAD_URL, info.updateUrl)
                .putString(
                    PREF_DOWNLOAD_VERSION,
                    info.versionName
                )
                .putLong(
                    PREF_DOWNLOAD_VERSION_CODE,
                    info.versionCode ?: -1L
                )
                .putLong(
                    PREF_DOWNLOAD_SIZE,
                    info.expectedSizeBytes ?: -1L
                )
                .apply()

            activeDownloadId = id
            completedDownloadId = null
            isDownloading = true
            downloadProgress = 0
            errorMessage = null
        } catch (_: Exception) {
            errorMessage = localizedUpdateMessage(
                context,
                "تعذر بدء تنزيل التحديث. حاول مرة أخرى.",
                "Unable to start the update download. Please try again."
            )
            isDownloading = false
        }
    }

    fun installDownloadedUpdate(id: Long) {
        try {
            val status = readDownloadStatus(id)

            if (status?.status != DownloadManager.STATUS_SUCCESSFUL) {
                errorMessage = localizedUpdateMessage(
                    context,
                    "لم يكتمل تنزيل التحديث بعد.",
                    "The update download has not completed yet."
                )
                return
            }

            val downloadUri = downloadManager.getUriForDownloadedFile(id)
                ?: throw IllegalStateException(
                    "Downloaded update URI unavailable"
                )

            val updatesDirectory = File(context.cacheDir, "updates")

            if (!updatesDirectory.exists() && !updatesDirectory.mkdirs()) {
                throw IllegalStateException(
                    "Unable to create verification directory"
                )
            }

            val apkFile = File(
                updatesDirectory,
                "scanix-update.apk"
            )

            context.contentResolver.openInputStream(downloadUri).use { input ->
                if (input == null) {
                    throw IllegalStateException(
                        "Unable to read downloaded APK"
                    )
                }

                apkFile.outputStream().buffered().use { output ->
                    input.copyTo(output)
                }
            }

            if (!apkFile.isFile || apkFile.length() <= 0L) {
                throw IllegalStateException(
                    "Downloaded APK is empty"
                )
            }

            val expectedSize = preferences.getLong(
                PREF_DOWNLOAD_SIZE,
                -1L
            )

            if (
                expectedSize > 0L &&
                apkFile.length() != expectedSize
            ) {
                apkFile.delete()
                throw IllegalStateException(
                    "Downloaded APK size verification failed"
                )
            }

            val archiveInfo = context.packageManager
                .getPackageArchiveInfo(
                    apkFile.absolutePath,
                    0
                ) ?: throw IllegalStateException(
                "Downloaded file is not a valid APK"
            )

            val downloadedVersion = getInstalledVersion(archiveInfo)

            if (archiveInfo.packageName != context.packageName) {
                apkFile.delete()
                throw IllegalStateException(
                    "Downloaded APK package does not match"
                )
            }

            if (downloadedVersion.first <= currentVersion.first) {
                apkFile.delete()
                throw IllegalStateException(
                    "Downloaded APK is not newer"
                )
            }

            val expectedVersionCode = preferences.getLong(
                PREF_DOWNLOAD_VERSION_CODE,
                -1L
            )

            if (
                expectedVersionCode > 0L &&
                downloadedVersion.first != expectedVersionCode
            ) {
                apkFile.delete()
                throw IllegalStateException(
                    "Downloaded APK version code does not match"
                )
            }

            val expectedVersionName = preferences.getString(
                PREF_DOWNLOAD_VERSION,
                null
            )

            if (
                expectedVersionCode <= 0L &&
                !expectedVersionName.isNullOrBlank() &&
                normalizeVersion(downloadedVersion.second) !=
                normalizeVersion(expectedVersionName)
            ) {
                apkFile.delete()
                throw IllegalStateException(
                    "Downloaded APK version name does not match"
                )
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    apkUri,
                    "application/vnd.android.package-archive"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
        } catch (_: Exception) {
            errorMessage = localizedUpdateMessage(
                context,
                "تعذر التحقق من ملف التحديث أو بدء التثبيت. تأكد من اكتمال التنزيل والسماح بتثبيت التطبيقات من هذا المصدر.",
                "Unable to verify the update or start installation. Ensure the download is complete and installation from this source is allowed."
            )
        }
    }

    LaunchedEffect(updateChannel) {
        val savedId = savedDownloadId()

        if (savedId > 0L) {
            activeDownloadId = savedId

            when (readDownloadStatus(savedId)?.status) {
                DownloadManager.STATUS_PENDING,
                DownloadManager.STATUS_RUNNING,
                DownloadManager.STATUS_PAUSED -> {
                    isDownloading = true
                }

                DownloadManager.STATUS_SUCCESSFUL -> {
                    completedDownloadId = savedId
                    downloadProgress = 100
                    isDownloading = false
                }

                else -> {
                    clearSavedDownload()
                }
            }
        }

        checkForUpdates()
    }

    LaunchedEffect(visible, activeDownloadId) {
        while (visible && activeDownloadId != null) {
            val id = activeDownloadId ?: break
            val status = readDownloadStatus(id)

            when (status?.status) {
                DownloadManager.STATUS_PENDING,
                DownloadManager.STATUS_RUNNING,
                DownloadManager.STATUS_PAUSED -> {
                    isDownloading = true
                    completedDownloadId = null

                    if (
                        status.totalBytes > 0L &&
                        status.downloadedBytes >= 0L
                    ) {
                        downloadProgress = (
                            status.downloadedBytes * 100L /
                                status.totalBytes
                        ).toInt().coerceIn(0, 99)
                    } else {
                        downloadProgress = -1
                    }
                }

                DownloadManager.STATUS_SUCCESSFUL -> {
                    isDownloading = false
                    downloadProgress = 100
                    completedDownloadId = id
                }

                DownloadManager.STATUS_FAILED -> {
                    isDownloading = false
                    completedDownloadId = null
                    downloadProgress = -1
                    errorMessage = localizedUpdateMessage(
                        context,
                        "فشل تنزيل التحديث. يمكنك المحاولة مرة أخرى.",
                        "The update download failed. You can try again."
                    )
                    clearSavedDownload()
                }

                else -> {
                    isDownloading = false
                    completedDownloadId = null
                }
            }

            delay(700)
        }
    }

    val updateAvailable = updateInfo?.let { info ->
        if (updateChannel == "github") {
            compareVersions(
                info.versionName,
                currentVersion.second
            ) > 0
        } else {
            (info.versionCode ?: 0L) > currentVersion.first
        }
    } == true

    LaunchedEffect(updateAvailable, updateInfo?.versionName) {
        val info = updateInfo

        if (
            updateAvailable &&
            info != null &&
            lastNotifiedVersion != info.versionName
        ) {
            lastNotifiedVersion = info.versionName
            onUpdateAvailable(info.versionName)
        }
    }

    LaunchedEffect(
        downloadRequestId,
        updateAvailable,
        updateInfo?.versionName
    ) {
        val info = updateInfo

        if (
            downloadRequestId > 0 &&
            downloadRequestId != lastHandledDownloadRequest &&
            updateAvailable &&
            info != null
        ) {
            lastHandledDownloadRequest = downloadRequestId
            startUpdateDownload(info)
        }
    }
        if (visible) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.update_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(
                            R.string.update_current_version
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = displayVersion(currentVersion.second),
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    when {
                        isDownloading -> {
                            if (downloadProgress >= 0) {
                                CircularProgressIndicator(
                                    progress = {
                                        downloadProgress / 100f
                                    }
                                )

                                Text(
                                    text = localizedUpdateMessage(
                                        context,
                                        "جارٍ تنزيل التحديث: $downloadProgress%",
                                        "Downloading update: $downloadProgress%"
                                    )
                                )
                            } else {
                                CircularProgressIndicator()

                                Text(
                                    text = localizedUpdateMessage(
                                        context,
                                        "جارٍ تنزيل التحديث...",
                                        "Downloading update..."
                                    )
                                )
                            }
                        }

                        isChecking && updateInfo == null -> {
                            CircularProgressIndicator()

                            Text(
                                text = stringResource(
                                    R.string.update_checking
                                )
                            )
                        }

                        errorMessage != null -> {
                            Text(
                                text = errorMessage.orEmpty(),
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        completedDownloadId != null -> {
                            Text(
                                text = localizedUpdateMessage(
                                    context,
                                    "اكتمل تنزيل التحديث وأصبح جاهزًا للتثبيت.",
                                    "The update has been downloaded and is ready to install."
                                ),
                                fontWeight = FontWeight.Medium
                            )

                            Button(
                                onClick = {
                                    completedDownloadId?.let {
                                        installDownloadedUpdate(it)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    localizedUpdateMessage(
                                        context,
                                        "تثبيت التحديث",
                                        "Install update"
                                    )
                                )
                            }
                        }

                        updateInfo != null && updateAvailable -> {
                            Text(
                                text = stringResource(
                                    R.string.update_available
                                ),
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = displayVersion(
                                    updateInfo!!.versionName
                                ),
                                style = MaterialTheme.typography.titleMedium
                            )

                            if (updateInfo!!.releaseNotes.isNotBlank()) {
                                Text(
                                    text = updateInfo!!.releaseNotes,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            if (updateInfo!!.updateUrl.isNotBlank()) {
                                Button(
                                    onClick = {
                                        startUpdateDownload(updateInfo!!)
                                    },
                                    enabled = !isDownloading,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        stringResource(
                                            R.string.update_download
                                        )
                                    )
                                }
                            }
                        }

                        updateInfo != null -> {
                            Text(
                                text = stringResource(
                                    R.string.update_not_available
                                ),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { checkForUpdates() },
                enabled = !isChecking,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.update_check)
                )
            }
        }
    }
}

private fun localizedUpdateMessage(
    context: Context,
    arabic: String,
    english: String
): String {
    val configuration: Configuration =
        context.resources.configuration

    val language = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        configuration.locales[0]?.language.orEmpty()
    } else {
        @Suppress("DEPRECATION")
        configuration.locale?.language.orEmpty()
    }

    return if (language.startsWith("ar", ignoreCase = true)) {
        arabic
    } else {
        english
    }
}

private fun displayVersion(versionName: String): String {
    val cleanVersion = versionName.removePrefix("v").removePrefix("V")
    return "v$cleanVersion"
}

private fun normalizeVersion(versionName: String): String {
    return versionName.trim()
        .removePrefix("v")
        .removePrefix("V")
}

private fun isSupportedReleaseVersion(versionName: String): Boolean {
    return Regex("^[vV]?\\d+(\\.\\d+){3}$")
        .matches(versionName)
}

private fun compareVersions(
    firstVersion: String,
    secondVersion: String
): Int {
    val first = Regex("\\d+")
        .findAll(normalizeVersion(firstVersion))
        .mapNotNull { it.value.toIntOrNull() }
        .toList()

    val second = Regex("\\d+")
        .findAll(normalizeVersion(secondVersion))
        .mapNotNull { it.value.toIntOrNull() }
        .toList()

    val maxParts = maxOf(first.size, second.size)

    for (index in 0 until maxParts) {
        val firstPart = first.getOrElse(index) { 0 }
        val secondPart = second.getOrElse(index) { 0 }

        if (firstPart != secondPart) {
            return firstPart.compareTo(secondPart)
        }
    }

    return 0
}

@Suppress("DEPRECATION")
private fun getInstalledVersion(
    packageInfo: PackageInfo
): Pair<Long, String> {
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        packageInfo.versionCode.toLong()
    }

    return Pair(
        versionCode,
        packageInfo.versionName ?: versionCode.toString()
    )
}
