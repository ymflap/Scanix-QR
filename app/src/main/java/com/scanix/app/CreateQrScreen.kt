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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class QrContentType(
    val label: String
) {
    TEXT("نص"),
    URL("رابط"),
    CONTACT("جهة اتصال"),
    WIFI("Wi-Fi")
}

@Composable
fun CreateQrScreen() {
    var selectedTypeName by rememberSaveable {
        mutableStateOf(QrContentType.TEXT.name)
    }

    var text by rememberSaveable {
        mutableStateOf("")
    }

    var contactName by rememberSaveable {
        mutableStateOf("")
    }

    var phone by rememberSaveable {
        mutableStateOf("")
    }

    var wifiName by rememberSaveable {
        mutableStateOf("")
    }

    var wifiPassword by rememberSaveable {
        mutableStateOf("")
    }

    var wifiSecurity by rememberSaveable {
        mutableStateOf("WPA")
    }

    val selectedType = QrContentType.valueOf(selectedTypeName)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "إنشاء QR",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "أنشئ رمزًا وشاركه بسهولة",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QrTypeButton(
                    label = "نص",
                    selected = selectedType == QrContentType.TEXT,
                    modifier = Modifier.weight(1f)
                ) {
                    selectedTypeName = QrContentType.TEXT.name
                }

                QrTypeButton(
                    label = "رابط",
                    selected = selectedType == QrContentType.URL,
                    modifier = Modifier.weight(1f)
                ) {
                    selectedTypeName = QrContentType.URL.name
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QrTypeButton(
                    label = "جهة اتصال",
                    selected = selectedType == QrContentType.CONTACT,
                    modifier = Modifier.weight(1f)
                ) {
                    selectedTypeName = QrContentType.CONTACT.name
                }

                QrTypeButton(
                    label = "شبكة Wi-Fi",
                    selected = selectedType == QrContentType.WIFI,
                    modifier = Modifier.weight(1f)
                ) {
                    selectedTypeName = QrContentType.WIFI.name
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        when (selectedType) {
            QrContentType.TEXT -> {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("النص") },
                    placeholder = {
                        Text("اكتب النص الذي تريد تحويله")
                    },
                    minLines = 3,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            QrContentType.URL -> {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("رابط الموقع") },
                    placeholder = {
                        Text("https://example.com")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            QrContentType.CONTACT -> {
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("اسم جهة الاتصال") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("رقم الهاتف") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            QrContentType.WIFI -> {
                OutlinedTextField(
                    value = wifiName,
                    onValueChange = { wifiName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("اسم الشبكة") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = wifiPassword,
                    onValueChange = { wifiPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("كلمة مرور الشبكة") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "نوع حماية الشبكة",
                    style = MaterialTheme.typography.labelLarge
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("WPA", "WEP", "بدون حماية").forEach { option ->
                        QrTypeButton(
                            label = option,
                            selected = wifiSecurity == option,
                            modifier = Modifier.weight(1f)
                        ) {
                            wifiSecurity = option
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "لن تُحفظ كلمة المرور تلقائيًا في سجل التطبيق.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Button(
            onClick = {
                // سيتم ربط الزر بمولّد QR في ملف مستقل لاحقًا.
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Text(
                text = "إنشاء الرمز",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun QrTypeButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(label)
        }
    }
}
