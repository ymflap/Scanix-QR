package com.scanix.app

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object ScanixLocale {

    fun wrap(context: Context): Context {
        val language = LanguageSettingsStore.getLanguage(context)
        val locale = Locale.forLanguageTag(language.code)

        val configuration = Configuration(
            context.resources.configuration
        )

        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)

        return context.createConfigurationContext(configuration)
    }
}
