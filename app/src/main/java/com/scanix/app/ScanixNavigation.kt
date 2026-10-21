
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
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
    SCAN,
    HISTORY,
    SETTINGS,
    LANGUAGE,
    PRIVACY,
    ABOUT,
    SUPPORT,
    UPDATE
}

private const val PRIVACY_POLICY_URL =
    "https://scanix-qr.pages.dev/privacy.html"

private const val ABOUT_PAGE_URL =
    "https://scanix-qr.pages.dev/about.html"

@Composable
fun ScanixNavigation() {
    val context = LocalContext.current

    var currentPage by rememberSaveable {
        mutableStateOf(ScanixPage.SCAN.name)
    }

    var selectedLanguage by rememberSaveable {
        mutableStateOf(
            LanguageSettingsStore
                .getLanguage(context)
                .code
        )
    }

    val isSubPage = currentPage in setOf(
        ScanixPage.LANGUAGE.name,
        ScanixPage.PRIVACY.name,
        ScanixPage.ABOUT.name,
        ScanixPage.SUPPORT.name,
        ScanixPage.UPDATE.name
    )

    fun returnToSettings() {
        currentPage = ScanixPage.SETTINGS.name
    }

    BackHandler(enabled = isSubPage) {
        returnToSettings()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!isSubPage) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentPage == ScanixPage.SCAN.name,
                        onClick = {
                            currentPage = ScanixPage.SCAN.name
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.QrCodeScanner,
                                contentDescription = stringResource(
                                    R.string.nav_scan
                                )
                            )
                        },
                        label = {
                            Text(stringResource(R.string.nav_scan))
                        }
                    )

                    NavigationBarItem(
                        selected = currentPage == ScanixPage.HISTORY.name,
                        onClick = {
                            currentPage = ScanixPage.HISTORY.name
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = stringResource(
                                    R.string.nav_history
                                )
                            )
                        },
                        label = {
                            Text(stringResource(R.string.nav_history))
                        }
                    )

                    NavigationBarItem(
                        selected = currentPage == ScanixPage.SETTINGS.name,
                        onClick = {
                            currentPage = ScanixPage.SETTINGS.name
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = stringResource(
                                    R.string.nav_settings
                                )
                            )
                        },
                        label = {
                            Text(stringResource(R.string.nav_settings))
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isSubPage) {
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
                        onClick = { returnToSettings() }
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
                            ScanixPage.LANGUAGE.name ->
                                stringResource(R.string.nav_language)

                            ScanixPage.PRIVACY.name ->
                                stringResource(R.string.nav_privacy)

                            ScanixPage.ABOUT.name ->
                                stringResource(R.string.nav_about)

                            ScanixPage.SUPPORT.name ->
                                stringResource(R.string.nav_support)

                            ScanixPage.UPDATE.name ->
                                stringResource(R.string.nav_update)

                            else -> ""
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
                    ScanixPage.SCAN.name -> ScanScreen()

                    ScanixPage.HISTORY.name -> HistoryScreen()

                    ScanixPage.SETTINGS.name -> SettingsScreen(
                        onOpenLanguage = {
                            currentPage = ScanixPage.LANGUAGE.name
                        },
                        onOpenPrivacyPolicy = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(PRIVACY_POLICY_URL)
                                )
                            )
                        },
                        onOpenAbout = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(ABOUT_PAGE_URL)
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

                    ScanixPage.PRIVACY.name -> PrivacyPolicyScreen()

                    ScanixPage.ABOUT.name -> AboutScreen()

                    ScanixPage.SUPPORT.name -> SupportScreen()

                    ScanixPage.UPDATE.name -> AppUpdateScreen()
                }
            }
        }
    }
}
