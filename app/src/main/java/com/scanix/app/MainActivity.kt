package com.scanix.app

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

private val ScanixLightColors = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    secondary = Color.DarkGray,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    outline = Color.LightGray
)

private val ScanixDarkColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = Color.LightGray,
    onSecondary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF121212),
    onSurface = Color.White,
    outline = Color.DarkGray
)

class MainActivity : ComponentActivity() {

    private val appearanceState = mutableStateOf(
        ScanixAppearance.SYSTEM
    )

    private val languageState = mutableStateOf(
        ScanixLanguage.ENGLISH
    )

    private val settingsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "appearance" -> {
                    appearanceState.value =
                        ScanixSettingsStore.getAppearance(this)
                }

                "app_language" -> {
                    languageState.value =
                        LanguageSettingsStore.getLanguage(this)

                    recreate()
                }
            }
        }

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(ScanixLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appearanceState.value =
            ScanixSettingsStore.getAppearance(this)

        languageState.value =
            LanguageSettingsStore.getLanguage(this)

        getSharedPreferences(
            "scanix_settings",
            MODE_PRIVATE
        ).registerOnSharedPreferenceChangeListener(
            settingsListener
        )

        setContent {
            val appearance by appearanceState
            val language by languageState

            ScanixTheme(
                appearance = appearance
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScanixNavigation(
                        selectedLanguageCode = language.code
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        getSharedPreferences(
            "scanix_settings",
            MODE_PRIVATE
        ).unregisterOnSharedPreferenceChangeListener(
            settingsListener
        )

        super.onDestroy()
    }
}

@Composable
private fun ScanixTheme(
    appearance: ScanixAppearance,
    content: @Composable () -> Unit
) {
    val systemDarkTheme = isSystemInDarkTheme()

    val darkTheme = when (appearance) {
        ScanixAppearance.SYSTEM -> systemDarkTheme
        ScanixAppearance.LIGHT -> false
        ScanixAppearance.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (darkTheme) {
            ScanixDarkColors
        } else {
            ScanixLightColors
        },
        content = content
    )
}
