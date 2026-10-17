package com.scanix.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }

    var historyEntries by remember {
        mutableStateOf(emptyList<HistoryEntry>())
    }

    var showClearConfirmation by rememberSaveable {
        mutableStateOf(false)
    }

    fun refreshHistory() {
        historyEntries = LocalHistoryStore.getAll(context)
    }

    LaunchedEffect(Unit) {
        refreshHistory()
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshHistory()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val filteredEntries = historyEntries.filter { entry ->
        val formatName = DecodedBarcode(
            value = entry.value,
            format = entry.format
        ).formatName

        entry.value.contains(searchQuery, ignoreCase = true) ||
            formatName.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "السجل",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.size(4.dp))

                Text(
                    text = "نتائج عمليات المسح السابقة",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (historyEntries.isNotEmpty()) {
                TextButton(
                    onClick = {
                        showClearConfirmation = true
                    }
                ) {
                    Text("مسح الكل")
                }
            }
        }

        Spacer(modifier = Modifier.size(18.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            label = {
                Text("البحث في السجل")
            },
            placeholder = {
                Text("ابحث عن النص أو نوع الرمز")
            }
        )

        Spacer(modifier = Modifier.size(16.dp))

        if (filteredEntries.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.size(16.dp))

                Text(
                    text = when {
                        searchQuery.isNotBlank() ->
                            "لا توجد نتائج مطابقة للبحث"

                        historyEntries.isEmpty() ->
                            "لا توجد نتائج محفوظة حتى الآن"

                        else ->
                            "لا توجد نتائج مطابقة للبحث"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.size(8.dp))

                Text(
                    text = if (searchQuery.isBlank()) {
                        "ستظهر هنا نتائج المسح المحفوظة على جهازك."
                    } else {
                        "جرّب البحث باستخدام كلمات أخرى."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = filteredEntries,
                    key = { entry -> entry.value }
                ) { entry ->
                    HistoryEntryCard(
                        entry = entry,
                        onDelete = {
                            LocalHistoryStore.delete(
                                context = context,
                                value = entry.value
                            )
                            refreshHistory()
                        }
                    )
                }
            }
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showClearConfirmation = false
            },
            title = {
                Text("مسح السجل بالكامل؟")
            },
            text = {
                Text("سيتم حذف جميع نتائج المسح المحفوظة على هذا الجهاز.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        LocalHistoryStore.clear(context)
                        refreshHistory()
                        showClearConfirmation = false
                    }
                ) {
                    Text("مسح الكل")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                    }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun HistoryEntryCard(
    entry: HistoryEntry,
    onDelete: () -> Unit
) {
    val formatName = DecodedBarcode(
        value = entry.value,
        format = entry.format
    ).formatName

    val formattedDate = remember(entry.timestamp) {
        SimpleDateFormat(
            "yyyy/MM/dd - HH:mm",
            Locale.getDefault()
        ).format(Date(entry.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onDelete
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "حذف النتيجة",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Text(
                text = entry.value,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
