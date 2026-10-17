package com.scanix.app

import android.content.Context
import androidx.compose.foundation.clickable
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

    fun isVibrationEnabled(context: Context): Boolean {
        return preferences(context).getBoolean(
            KEY_VIBRATION,
            true
        )
    }

    fun setVibrationEnabled(
        context: Context,
        enabled: Boolean
    ) {
        preferences(context).edit()
            .putBoolean(KEY_VIBRATION, enabled)
            .apply()
    }

    fun isSaveHistoryEnabled(context: Context): Boolean {
        return preferences(context).getBoolean(
            KEY_SAVE_HISTORY,
            true
        )
    }

    fun setSaveHistoryEnabled(
        context: Context,
        enabled: Boolean
    ) {
        preferences(context).edit()
            .putBoolean(KEY_SAVE_HISTORY, enabled)
            .apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
}

@Composable
fun SettingsScreen(
    onOpenLanguage: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onOpenSupport: () -> Unit = {}
) {
    val context = LocalContext.current

    var appearance by remember {
        mutableStateOf(
            ScanixSettingsStore.getAppearance(context)
        )
    }

    var vibrationEnabled by remember {
        mutableStateOf(
            ScanixSettingsStore.isVibrationEnabled(context)
        )
    }

    var saveHistoryEnabled by remember {
        mutableStateOf(
            ScanixSettingsStore.isSaveHistoryEnabled(context)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 20.dp,
                vertical = 16.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "الإعدادات",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "خصّص تجربة استخدام Scanix",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SettingsCard(title = "المظهر") {
            AppearanceOption(
                title = "حسب إعدادات النظام",
                selected = appearance == ScanixAppearance.SYSTEM
            ) {
                appearance = ScanixAppearance.SYSTEM
                ScanixSettingsStore.setAppearance(
                    context,
                    appearance
                )
            }

            AppearanceOption(
                title = "فاتح",
                selected = appearance == ScanixAppearance.LIGHT
            ) {
                appearance = ScanixAppearance.LIGHT
                ScanixSettingsStore.setAppearance(
                    context,
                    appearance
                )
            }

            AppearanceOption(
                title = "داكن",
                selected = appearance == ScanixAppearance.DARK
            ) {
                appearance = ScanixAppearance.DARK
                ScanixSettingsStore.setAppearance(
                    context,
                    appearance
                )
            }
        }

        SettingsCard(title = "المسح") {
            SettingsSwitchRow(
                title = "الاهتزاز عند اكتشاف رمز",
                description = "تنبيه بالاهتزاز عند قراءة الرمز",
                checked = vibrationEnabled,
                onCheckedChange = { enabled ->
                    vibrationEnabled = enabled
                    ScanixSettingsStore.setVibrationEnabled(
                        context,
                        enabled
                    )
                }
            )
        }

        SettingsCard(title = "السجل والخصوصية") {
            SettingsSwitchRow(
                title = "حفظ نتائج المسح",
                description = "الاحتفاظ بالنتائج على الجهاز",
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
                text = "تُحفظ النتائج محليًا على جهازك، ولا تُرسل إلى خادم خارجي.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SettingsCard(title = "صفحات التطبيق") {
            SettingsDestinationCard(
                title = "اللغة",
                description = "اختيار العربية أو الإنجليزية",
                onClick = onOpenLanguage
            )

            SettingsDestinationCard(
                title = "سياسة الخصوصية",
                description = "معرفة كيفية التعامل مع بياناتك",
                onClick = onOpenPrivacyPolicy
            )

            SettingsDestinationCard(
                title = "حول التطبيق",
                description = "معلومات Scanix والمطور والإصدار",
                onClick = onOpenAbout
            )

            SettingsDestinationCard(
                title = "الدعم",
                description = "التواصل مع المطور والحصول على المساعدة",
                onClick = onOpenSupport
            )
        }
    }
}

@Composable
private fun SettingsDestinationCard(
    title: String,
    description: String,
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
        modifier = Modifier.fillMaxWidth(),
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
