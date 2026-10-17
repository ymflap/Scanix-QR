package com.scanix.app

import android.content.Context

object LanguageSettingsStore {

    private const val PREFS_NAME = "scanix_settings"
    private const val KEY_LANGUAGE = "app_language"

    fun getLanguage(context: Context): ScanixLanguage {
        val savedCode = context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .getString(
                KEY_LANGUAGE,
                ScanixLanguage.ARABIC.code
            )

        return ScanixLanguage.fromCode(
            savedCode ?: ScanixLanguage.ARABIC.code
        )
    }

    fun setLanguage(
        context: Context,
        language: ScanixLanguage
    ) {
        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(KEY_LANGUAGE, language.code)
            .apply()
    }
}
