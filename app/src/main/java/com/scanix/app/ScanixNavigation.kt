package com.scanix.app

import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

private enum class ScanixPage {
    SCAN,
    HISTORY,
    CREATE,
    SETTINGS
}

/**
 * Main navigation for Scanix.
 * Keeps navigation state while switching between app pages.
 */
@Composable
fun ScanixNavigation() {
    var currentPage by rememberSaveable {
        mutableStateOf(ScanixPage.SCAN.name)
    }

    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentPage == ScanixPage.SCAN.name,
                    onClick = {
                        currentPage = ScanixPage.SCAN.name
                    },
                    icon = {},
                    label = { Text("مسح") }
                )

                NavigationBarItem(
                    selected = currentPage == ScanixPage.HISTORY.name,
                    onClick = {
                        currentPage = ScanixPage.HISTORY.name
                    },
                    icon = {},
                    label = { Text("السجل") }
                )

                NavigationBarItem(
                    selected = currentPage == ScanixPage.CREATE.name,
                    onClick = {
                        currentPage = ScanixPage.CREATE.name
                    },
                    icon = {},
                    label = { Text("إنشاء") }
                )

                NavigationBarItem(
                    selected = currentPage == ScanixPage.SETTINGS.name,
                    onClick = {
                        currentPage = ScanixPage.SETTINGS.name
                    },
                    icon = {},
                    label = { Text("الإعدادات") }
                )
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .androidxPadding(innerPadding)
        ) {
            when (currentPage) {
                ScanixPage.SCAN.name -> ScanScreen()

                ScanixPage.HISTORY.name -> HistoryScreen()

                ScanixPage.CREATE.name -> CreateQrScreen()

                ScanixPage.SETTINGS.name -> SettingsScreen()
            }
        }
    }
}

private fun Modifier.androidxPadding(
    padding: androidx.compose.foundation.layout.PaddingValues
): Modifier = this.then(
    Modifier.paddingValues(padding)
)

private fun Modifier.paddingValues(
    padding: androidx.compose.foundation.layout.PaddingValues
): Modifier = androidx.compose.foundation.layout.padding(
    this,
    padding
)
