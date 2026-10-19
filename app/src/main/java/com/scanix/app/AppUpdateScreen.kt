package com.scanix.app

import android.content.Intent
import android.net.Uri
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val SCANIX_UPDATE_INFO_URL =
    "https://raw.githubusercontent.com/ymflap/Scanix-QR/main/updates/latest.json"

private data class ScanixUpdateInfo(
    val versionCode: Long,
    val versionName: String,
    val updateUrl: String,
    val releaseNotes: String
)

@Composable
fun AppUpdateScreen() {
    val context = LocalContext.current

    val isArabic = remember(context) {
        context.resources.configuration.locales[0].language == "ar"
    }

    val currentVersion = remember(context) {
        getInstalledVersion(
            context.packageManager.getPackageInfo(
                context.packageName,
                0
            )
        )
    }

    val updateChannel = BuildConfig.UPDATE_CHANNEL

    var isChecking by remember { mutableStateOf(false) }
    var updateInfo by remember {
        mutableStateOf<ScanixUpdateInfo?>(null)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun checkForUpdates() {
        if (isChecking) return

        isChecking = true
        errorMessage = null
        updateInfo = null

        Thread {
            var connection: HttpURLConnection? = null

            try {
                connection = URL(SCANIX_UPDATE_INFO_URL)
                    .openConnection() as HttpURLConnection

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

                val responseCode = connection.responseCode

                if (responseCode !in 200..299) {
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
                        "Missing update channels"
                    )

                val channel = channels.optJSONObject(updateChannel)
                    ?: throw IllegalStateException(
                        "Update channel unavailable"
                    )

                val versionCode = channel.optLong("versionCode", 0L)
                val versionName = channel.optString("versionName", "")
                val updateUrl = channel.optString("updateUrl", "")
                val releaseNotes = channel.optString("releaseNotes", "")

                if (versionCode <= 0L || versionName.isBlank()) {
                    throw IllegalStateException(
                        "Invalid update information"
                    )
                }

                if (updateUrl.isNotBlank()) {
                    val parsedUrl = Uri.parse(updateUrl)
                    if (parsedUrl.scheme != "https" ||
                        parsedUrl.host.isNullOrBlank()
                    ) {
                        throw IllegalStateException(
                            "Invalid update URL"
                        )
                    }
                }

                val result = ScanixUpdateInfo(
                    versionCode = versionCode,
                    versionName = versionName,
                    updateUrl = updateUrl,
                    releaseNotes = releaseNotes
                )

                Handler(Looper.getMainLooper()).post {
                    updateInfo = result
                    isChecking = false
                }
            } catch (_: Exception) {
                Handler(Looper.getMainLooper()).post {
                    errorMessage = if (isArabic) {
                        "تعذر التحقق من تحديثات قناة التوزيع الحالية. تحقق من الإنترنت وحاول مرة أخرى."
                    } else {
                        "Couldn't check updates for this distribution channel. Check your internet connection and try again."
                    }
                    isChecking = false
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    LaunchedEffect(updateChannel) {
        checkForUpdates()
    }

    val updateAvailable = updateInfo?.let {
        it.versionCode > currentVersion.first
    } == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isArabic) "تحديث التطبيق" else "App updates",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = if (isArabic) {
                "يتحقق التطبيق من التحديثات الخاصة بقناة التوزيع الحالية عبر الإنترنت."
            } else {
                "The app checks online for updates belonging to the current distribution channel."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    text = if (isArabic) {
                        "الإصدار المثبّت"
                    } else {
                        "Installed version"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = currentVersion.second,
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = if (isArabic) {
                        "قناة التوزيع: ${channelDisplayName(updateChannel, true)}"
                    } else {
                        "Distribution channel: ${channelDisplayName(updateChannel, false)}"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                when {
                    isChecking -> {
                        CircularProgressIndicator()
                        Text(
                            text = if (isArabic) {
                                "جارٍ الاتصال بالإنترنت والتحقق من التحديثات..."
                            } else {
                                "Connecting to the internet and checking for updates..."
                            }
                        )
                    }

                    errorMessage != null -> {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    updateInfo != null && updateAvailable -> {
                        Text(
                            text = if (isArabic) {
                                "يتوفر إصدار أحدث: ${updateInfo!!.versionName}"
                            } else {
                                "A newer version is available: ${updateInfo!!.versionName}"
                            },
                            fontWeight = FontWeight.Bold
                        )

                        if (updateInfo!!.releaseNotes.isNotBlank()) {
                            Text(updateInfo!!.releaseNotes)
                        }

                        if (updateInfo!!.updateUrl.isNotBlank()) {
                            Button(
                                onClick = {
                                    try {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(updateInfo!!.updateUrl)
                                            )
                                        )
                                    } catch (_: Exception) {
                                        errorMessage = if (isArabic) {
                                            "تعذر فتح رابط التحديث على هذا الجهاز."
                                        } else {
                                            "Couldn't open the update link on this device."
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (isArabic) {
                                        "عرض التحديث"
                                    } else {
                                        "View update"
                                    }
                                )
                            }
                        } else {
                            Text(
                                text = if (isArabic) {
                                    "يتوفر إصدار أحدث، لكن رابط التنزيل غير متاح حاليًا."
                                } else {
                                    "A newer version is available, but its download link is not available yet."
                                }
                            )
                        }
                    }

                    updateInfo != null -> {
                        Text(
                            text = if (isArabic) {
                                "أنت تستخدم أحدث إصدار متاح لهذه القناة."
                            } else {
                                "You're using the latest available version for this channel."
                            },
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
                if (isArabic) {
                    "التحقق من التحديثات"
                } else {
                    "Check for updates"
                }
            )
        }
    }
}

private fun channelDisplayName(
    channel: String,
    isArabic: Boolean
): String {
    return when (channel) {
        "github" -> if (isArabic) "GitHub" else "GitHub"
        "google_play" -> if (isArabic) "Google Play" else "Google Play"
        "other_store" -> if (isArabic) "متجر آخر" else "Other store"
        else -> if (isArabic) "غير معروف" else "Unknown"
    }
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
