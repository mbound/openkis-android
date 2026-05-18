package org.openkis.android

import android.content.Context

object LocaleHelper {
    private const val PREFS_NAME = "openkis_locale"
    private const val KEY_LOCALE = "locale_override"

    fun getLocale(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LOCALE, "") ?: ""

    fun setLocale(context: Context, locale: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LOCALE, locale).apply()
    }
}
