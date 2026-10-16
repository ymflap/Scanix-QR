package com.scanix.app

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class ScanixAppearance {
    SYSTEM,
    LIGHT,
    DARK
}

@Composable
fun SettingsScreen() {
    var appearanceName by rememberSaveable {
        mutableStateOf(ScanixAppearance.SYSTEM.name)
    }

    var vibrationEnabled by rememberSaveable {
        mutableStateOf(true)
    }

    var saveHistoryEnabled by rememberSaveable {
        mutableStateOf(true)
    }

    val appearance = ScanixAppearance.valueOf(appearanceName)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
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
                appearanceName = ScanixAppearance.SYSTEM.name
            }

            AppearanceOption(
                title = "فاتح",
                selected = appearance == ScanixAppearance.LIGHT
            ) {
                appearanceName = ScanixAppearance.LIGHT.name
            }

            AppearanceOption(
                title = "داكن",
                selected = appearance == ScanixAppearance.DARK
            ) {
                appearanceName = ScanixAppearance.DARK.name
            }
        }

        SettingsCard(title = "المسح") {
            SettingsSwitchRow(
                title = "الاهتزاز عند اكتشاف رمز",
                description = "تنبيه بالاهتزاز عند قراءة الرمز",
                checked = vibrationEnabled,
                onCheckedChange = {
                    vibrationEnabled = it
                }
            )
        }

        SettingsCard(title = "السجل والخصوصية") {
            SettingsSwitchRow(
                title = "حفظ نتائج المسح",
                description = "الاحتفاظ بالنتائج على الجهاز",
                checked = saveHistoryEnabled,
                onCheckedChange = {
                    saveHistoryEnabled = it
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "تُحفظ النتائج محليًا عند ربط إعداد التخزين. لا تُرسل نتائج المسح إلى خادم خارجي.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SettingsCard(title = "حول التطبيق") {
            SettingsInfoRow(
                label = "اسم التطبيق",
                value = "Scanix"
            )

            SettingsInfoRow(
                label = "المطور",
                value = "Mohammed Nasser"
            )

            SettingsInfoRow(
                label = "الإصدار",
                value = "1.0.3.6"
            )

            SettingsInfoRow(
                label = "المنصة",
                value = "Android"
            )
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

@Composable
private fun SettingsInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
