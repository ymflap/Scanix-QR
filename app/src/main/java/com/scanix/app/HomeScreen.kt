package com.scanix.app

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onScanClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onOcrClick: () -> Unit = {},
    updateAvailableVersion: String? = null,
    onUpdateDownloadClick: () -> Unit = {},
    onCreateQrClick: () -> Unit = {}
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold,
                        fontSize = 25.sp
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(
                    horizontal = 20.dp,
                    vertical = 18.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.home_tagline),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            if (!updateAvailableVersion.isNullOrBlank()) {
                UpdateAvailableCard(
                    version = updateAvailableVersion,
                    onDownloadClick = onUpdateDownloadClick
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeActionCard(
                    title = stringResource(R.string.home_scan_title),
                    description = stringResource(
                        R.string.home_scan_description
                    ),
                    icon = Icons.Outlined.QrCodeScanner,
                    onClick = onScanClick,
                    modifier = Modifier.weight(1f)
                )

                HomeActionCard(
                    title = stringResource(R.string.home_ocr_title),
                    description = stringResource(
                        R.string.home_ocr_description
                    ),
                    icon = Icons.Outlined.TextFields,
                    onClick = onOcrClick,
                    isNew = true,
                    newLabel = stringResource(R.string.home_new),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeActionCard(
                    title = stringResource(R.string.nav_history),
                    description = stringResource(
                        R.string.home_history_description
                    ),
                    icon = Icons.Outlined.History,
                    onClick = onHistoryClick,
                    modifier = Modifier.weight(1f)
                )

                HomeActionCard(
                    title = stringResource(R.string.nav_settings),
                    description = stringResource(
                        R.string.settings_subtitle
                    ),
                    icon = Icons.Outlined.Settings,
                    onClick = onSettingsClick,
                    modifier = Modifier.weight(1f)
                )
            }

            HomeActionCard(
                title = localizedHomeText(
                    context,
                    "إنشاء الباركود",
                    "Create QR Code"
                ),
                description = localizedHomeText(
                    context,
                    "إنشاء رموز QR — الميزة قيد التطوير",
                    "Create QR codes — feature in development"
                ),
                icon = Icons.Outlined.QrCode,
                onClick = onCreateQrClick,
                isComingSoon = true,
                comingSoonLabel = localizedHomeText(
                    context,
                    "قريبًا",
                    "COMING SOON"
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
        }
    }
}
@Composable
private fun UpdateAvailableCard(
    version: String,
    onDownloadClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = localizedHomeText(
                    context,
                    "يتوفر تحديث جديد",
                    "A new update is available"
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Text(
                text = localizedHomeText(
                    context,
                    "الإصدار $version متاح للتنزيل.",
                    "Version $version is available to download."
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Button(
                onClick = onDownloadClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    localizedHomeText(
                        context,
                        "تنزيل التحديث",
                        "Download update"
                    )
                )
            }
        }
    }
}

@Composable
private fun HomeActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isNew: Boolean = false,
    newLabel: String = "NEW",
    isComingSoon: Boolean = false,
    comingSoonLabel: String = "COMING SOON"
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(164.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
        ),
        contentPadding = PaddingValues(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(34.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall
            )

            when {
                isComingSoon -> {
                    Spacer(modifier = Modifier.height(5.dp))

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFFD54F),
                        contentColor = Color(0xFF332600)
                    ) {
                        Text(
                            text = comingSoonLabel,
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 3.dp
                            ),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                isNew -> {
                    Spacer(modifier = Modifier.height(5.dp))

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF2E7D32),
                        contentColor = Color.White
                    ) {
                        Text(
                            text = newLabel,
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 3.dp
                            ),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3
            )
        }
    }
}

private fun localizedHomeText(
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
