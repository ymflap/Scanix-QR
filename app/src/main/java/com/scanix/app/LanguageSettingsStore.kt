package com.scanix.app

import android.content.Context
import android.os.Build

object LanguageSettingsStore {

    private const val PREFS_NAME = "scanix_settings"
    private const val KEY_LANGUAGE = "app_language"

    fun getLanguage(context: Context): ScanixLanguage {
        val preferences = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val savedCode = preferences.getString(
            KEY_LANGUAGE,
            null
        )

        // إذا اختار المستخدم لغة سابقًا، نحتفظ باختياره.
        if (!savedCode.isNullOrBlank()) {
            return when (savedCode.lowercase()) {
                "ar" -> ScanixLanguage.ARABIC
                "en" -> ScanixLanguage.ENGLISH
                else -> getDeviceLanguage(context)
            }
        }

        // عند أول تشغيل، نتبع لغة الجهاز المدعومة.
        return getDeviceLanguage(context)
    }

    fun setLanguage(
        context: Context,
        language: ScanixLanguage
    ) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putString(KEY_LANGUAGE, language.code)
            .apply()
    }

    private fun getDeviceLanguage(
        context: Context
    ): ScanixLanguage {
        val configuration =
            context.resources.configuration

        val languageCode = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        ) {
            configuration.locales[0].language
        } else {
            @Suppress("DEPRECATION")
            configuration.locale?.language
        }

        return when (languageCode?.lowercase()) {
            "ar" -> ScanixLanguage.ARABIC
            "en" -> ScanixLanguage.ENGLISH
            else -> ScanixLanguage.ENGLISH
        }
    }
}
