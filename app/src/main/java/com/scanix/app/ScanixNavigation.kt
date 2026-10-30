package com.scanix.app

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class ScanixPage {
    HOME,
    SCAN,
    HISTORY,
    SETTINGS,
    OCR,
    LANGUAGE,
    APPEARANCE,
    SCAN_HISTORY_PRIVACY,
    SUPPORT,
    UPDATE,
    CREATE_QR
}

private const val PRIVACY_POLICY_URL_AR =
    "https://scanix-qr.pages.dev/privacy.html"

private const val PRIVACY_POLICY_URL_EN =
    "https://scanix-qr.pages.dev/privacy-en.html"

private const val ABOUT_PAGE_URL_AR =
    "https://scanix-qr.pages.dev/about.html"

private const val ABOUT_PAGE_URL_EN =
    "https://scanix-qr.pages.dev/about-en.html"

@Composable
fun ScanixNavigation() {
    val context = LocalContext.current

    var currentPage by rememberSaveable {
        mutableStateOf(ScanixPage.HOME.name)
    }

    var selectedLanguage by rememberSaveable {
        mutableStateOf(
            LanguageSettingsStore
                .getLanguage(context)
                .code
        )
    }

    val isSettingsSubPage = currentPage in setOf(
        ScanixPage.LANGUAGE.name,
        ScanixPage.APPEARANCE.name,
        ScanixPage.SCAN_HISTORY_PRIVACY.name,
        ScanixPage.SUPPORT.name,
        ScanixPage.UPDATE.name
    )

    fun returnFromCurrentPage() {
        currentPage = if (isSettingsSubPage) {
            ScanixPage.SETTINGS.name
        } else {
            ScanixPage.HOME.name
        }
    }

    BackHandler(enabled = currentPage != ScanixPage.HOME.name) {
        returnFromCurrentPage()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (currentPage != ScanixPage.HOME.name) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = { returnFromCurrentPage() }
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(
                                R.string.nav_back
                            )
                        )
                    }

                    Text(
                        text = when (currentPage) {
                            ScanixPage.SCAN.name ->
                                stringResource(R.string.nav_scan)

                            ScanixPage.HISTORY.name ->
                                stringResource(R.string.nav_history)

                            ScanixPage.SETTINGS.name ->
                                stringResource(R.string.nav_settings)

                            ScanixPage.OCR.name ->
                                stringResource(R.string.home_ocr_title)

                            ScanixPage.LANGUAGE.name ->
                                stringResource(R.string.nav_language)

                            ScanixPage.APPEARANCE.name ->
                                stringResource(R.string.settings_appearance)

                            ScanixPage.SCAN_HISTORY_PRIVACY.name ->
                                stringResource(R.string.settings_controls)

                            ScanixPage.SUPPORT.name ->
                                stringResource(R.string.nav_support)

                            ScanixPage.UPDATE.name ->
                                stringResource(R.string.nav_update)

                            ScanixPage.CREATE_QR.name ->
                                if (selectedLanguage.equals(
                                        "ar",
                                        ignoreCase = true
                                    )
                                ) {
                                    "إنشاء QR"
                                } else {
                                    "Create QR"
                                }

                            else ->
                                stringResource(R.string.app_name)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (currentPage) {
                    ScanixPage.HOME.name -> HomeScreen(
                        onScanClick = {
                            currentPage = ScanixPage.SCAN.name
                        },
                        onHistoryClick = {
                            currentPage = ScanixPage.HISTORY.name
                        },
                        onSettingsClick = {
                            currentPage = ScanixPage.SETTINGS.name
                        },
                        onOcrClick = {
                            currentPage = ScanixPage.OCR.name
                        },
                        onCreateQrClick = {
                            currentPage = ScanixPage.CREATE_QR.name
                        }
                    )

                    ScanixPage.SCAN.name -> ScanScreen()

                    ScanixPage.HISTORY.name -> HistoryScreen()

                    ScanixPage.OCR.name -> TextExtractionScreen()

                    ScanixPage.CREATE_QR.name -> CreateQrScreen()

                    ScanixPage.SETTINGS.name -> SettingsScreen(
                        onOpenAppearance = {
                            currentPage = ScanixPage.APPEARANCE.name
                        },
                        onOpenScanHistoryPrivacy = {
                            currentPage =
                                ScanixPage.SCAN_HISTORY_PRIVACY.name
                        },
                        onOpenLanguage = {
                            currentPage = ScanixPage.LANGUAGE.name
                        },
                        onOpenPrivacyPolicy = {
                            val url = if (
                                selectedLanguage.equals(
                                    "ar",
                                    ignoreCase = true
                                )
                            ) {
                                PRIVACY_POLICY_URL_AR
                            } else {
                                PRIVACY_POLICY_URL_EN
                            }

                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(url)
                                )
                            )
                        },
                        onOpenAbout = {
                            val url = if (
                                selectedLanguage.equals(
                                    "ar",
                                    ignoreCase = true
                                )
                            ) {
                                ABOUT_PAGE_URL_AR
                            } else {
                                ABOUT_PAGE_URL_EN
                            }

                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(url)
                                )
                            )
                        },
                        onOpenSupport = {
                            currentPage = ScanixPage.SUPPORT.name
                        },
                        onOpenUpdate = {
                            currentPage = ScanixPage.UPDATE.name
                        }
                    )

                    ScanixPage.APPEARANCE.name ->
                        AppearanceSettingsScreen()

                    ScanixPage.SCAN_HISTORY_PRIVACY.name ->
                        ScanHistoryPrivacySettingsScreen()

                    ScanixPage.LANGUAGE.name -> {
                        LanguageSettingsScreen(
                            selectedLanguage = ScanixLanguage.fromCode(
                                selectedLanguage
                            ),
                            onLanguageSelected = { language ->
                                selectedLanguage = language.code

                                LanguageSettingsStore.setLanguage(
                                    context,
                                    language
                                )
                            }
                        )
                    }

                    ScanixPage.SUPPORT.name -> SupportScreen()

                    ScanixPage.UPDATE.name -> AppUpdateScreen()
                }
            }
        }
    }
}
