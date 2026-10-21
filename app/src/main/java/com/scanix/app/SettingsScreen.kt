package com.scanix.app

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class ScanixAppearance {
    SYSTEM,
    LIGHT,
    DARK
}

object ScanixSettingsStore {

    private const val PREFS_NAME = "scanix_settings"
    private const val KEY_APPEARANCE = "appearance"
    private const val KEY_VIBRATION = "vibration_enabled"
    private const val KEY_SAVE_HISTORY = "save_history_enabled"

    fun getAppearance(context: Context): ScanixAppearance {
        val value = preferences(context).getString(
            KEY_APPEARANCE,
            ScanixAppearance.SYSTEM.name
        )

        return runCatching {
            ScanixAppearance.valueOf(
                value ?: ScanixAppearance.SYSTEM.name
            )
        }.getOrDefault(ScanixAppearance.SYSTEM)
    }

    fun setAppearance(
        context: Context,
        appearance: ScanixAppearance
    ) {
        preferences(context).edit()
            .putString(KEY_APPEARANCE, appearance.name)
            .apply()
    }

    fun isVibrationEnabled(context: Context): Boolean =
        preferences(context).getBoolean(KEY_VIBRATION, true)

    fun setVibrationEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit()
            .putBoolean(KEY_VIBRATION, enabled)
            .apply()
    }

    fun isSaveHistoryEnabled(context: Context): Boolean =
        preferences(context).getBoolean(KEY_SAVE_HISTORY, true)

    fun setSaveHistoryEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit()
            .putBoolean(KEY_SAVE_HISTORY, enabled)
            .apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

@Composable
fun SettingsScreen(
    onOpenAppearance: () -> Unit = {},
    onOpenScanHistoryPrivacy: () -> Unit = {},
    onOpenLanguage: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onOpenSupport: () -> Unit = {},
    onOpenUpdate: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(R.string.settings_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SettingsDestinationCard(
            title = stringResource(R.string.settings_appearance),
            description = stringResource(R.string.settings_system),
            onClick = onOpenAppearance
        )

        SettingsDestinationCard(
            title = stringResource(R.string.settings_history_privacy),
            description = stringResource(
                R.string.settings_vibration_description
            ),
            onClick = onOpenScanHistoryPrivacy
        )

        SettingsDestinationCard(
            title = stringResource(R.string.settings_language),
            description = stringResource(
                R.string.settings_language_description
            ),
            onClick = onOpenLanguage
        )

        SettingsCard(
            title = stringResource(R.string.settings_app_pages)
        ) {
            SettingsDestinationCard(
                title = stringResource(R.string.settings_privacy),
                description = stringResource(
                    R.string.settings_privacy_description
                ),
                onClick = onOpenPrivacyPolicy
            )

            SettingsDestinationCard(
                title = stringResource(R.string.settings_about),
                description = stringResource(
                    R.string.settings_about_description
                ),
                onClick = onOpenAbout
            )

            SettingsDestinationCard(
                title = stringResource(R.string.settings_support),
                description = stringResource(
                    R.string.settings_support_description
                ),
                onClick = onOpenSupport
            )
        }

        SettingsDestinationCard(
            title = stringResource(R.string.settings_update),
            description = stringResource(
                R.string.settings_update_description
            ),
            onClick = onOpenUpdate
        )
    }
}

@Composable
fun AppearanceSettingsScreen() {
    val context = LocalContext.current

    var appearance by remember {
        mutableStateOf(ScanixSettingsStore.getAppearance(context))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsCard(
            title = stringResource(R.string.settings_appearance)
        ) {
            AppearanceOption(
                title = stringResource(R.string.settings_system),
                selected = appearance == ScanixAppearance.SYSTEM
            ) {
                appearance = ScanixAppearance.SYSTEM
                ScanixSettingsStore.setAppearance(context, appearance)
            }

            AppearanceOption(
                title = stringResource(R.string.settings_light),
                selected = appearance == ScanixAppearance.LIGHT
            ) {
                appearance = ScanixAppearance.LIGHT
                ScanixSettingsStore.setAppearance(context, appearance)
            }

            AppearanceOption(
                title = stringResource(R.string.settings_dark),
                selected = appearance == ScanixAppearance.DARK
            ) {
                appearance = ScanixAppearance.DARK
                ScanixSettingsStore.setAppearance(context, appearance)
            }
        }
    }
}

@Composable
fun ScanHistoryPrivacySettingsScreen() {
    val context = LocalContext.current

    var vibrationEnabled by remember {
        mutableStateOf(ScanixSettingsStore.isVibrationEnabled(context))
    }

    var saveHistoryEnabled by remember {
        mutableStateOf(ScanixSettingsStore.isSaveHistoryEnabled(context))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsCard(
            title = stringResource(R.string.settings_history_privacy)
        ) {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_vibration),
                description = stringResource(
                    R.string.settings_vibration_description
                ),
                checked = vibrationEnabled,
                onCheckedChange = { enabled ->
                    vibrationEnabled = enabled
                    ScanixSettingsStore.setVibrationEnabled(
                        context,
                        enabled
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsSwitchRow(
                title = stringResource(R.string.settings_save_results),
                description = stringResource(
                    R.string.settings_save_results_description
                ),
                checked = saveHistoryEnabled,
                onCheckedChange = { enabled ->
                    saveHistoryEnabled = enabled
                    ScanixSettingsStore.setSaveHistoryEnabled(
                        context,
                        enabled
                    )
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(
                    R.string.settings_local_storage_note
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsDestinationCard(
    title: String,
    description: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (description != null) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            content()
        }
    }
}

@Composable
private fun AppearanceOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
