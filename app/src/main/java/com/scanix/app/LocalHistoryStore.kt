package com.scanix.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores scan history locally on the device.
 * No account, server, or additional library is required.
 */
object LocalHistoryStore {

    private const val PREFS_NAME = "scanix_local_history"
    private const val HISTORY_KEY = "items"
    private const val MAX_ITEMS = 200

    fun save(
        context: Context,
        value: String,
        format: Int
    ) {
        if (value.isBlank()) return

        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val existing = readArray(prefs.getString(HISTORY_KEY, null))
        val updated = JSONArray()

        updated.put(
            JSONObject()
                .put("value", value)
                .put("format", format)
                .put("timestamp", System.currentTimeMillis())
        )

        for (index in 0 until existing.length()) {
            if (updated.length() >= MAX_ITEMS) break

            val item = existing.optJSONObject(index) ?: continue
            if (item.optString("value") == value) continue

            updated.put(item)
        }

        prefs.edit()
            .putString(HISTORY_KEY, updated.toString())
            .apply()
    }

    fun getAll(context: Context): List<HistoryEntry> {
        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val array = readArray(prefs.getString(HISTORY_KEY, null))
        val entries = mutableListOf<HistoryEntry>()

        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val value = item.optString("value")

            if (value.isBlank()) continue

            entries.add(
                HistoryEntry(
                    value = value,
                    format = item.optInt("format"),
                    timestamp = item.optLong("timestamp")
                )
            )
        }

        return entries
    }

    fun delete(
        context: Context,
        value: String
    ) {
        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val existing = readArray(prefs.getString(HISTORY_KEY, null))
        val updated = JSONArray()

        for (index in 0 until existing.length()) {
            val item = existing.optJSONObject(index) ?: continue

            if (item.optString("value") != value) {
                updated.put(item)
            }
        }

        prefs.edit()
            .putString(HISTORY_KEY, updated.toString())
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .remove(HISTORY_KEY)
            .apply()
    }

    private fun readArray(raw: String?): JSONArray {
        if (raw.isNullOrBlank()) return JSONArray()

        return try {
            JSONArray(raw)
        } catch (_: Exception) {
            JSONArray()
        }
    }
}

data class HistoryEntry(
    val value: String,
    val format: Int,
    val timestamp: Long
)
