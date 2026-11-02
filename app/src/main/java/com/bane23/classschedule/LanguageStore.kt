package com.bane23.classschedule

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import com.bane23.classschedule.data.AppLanguage
import com.bane23.classschedule.data.Lang
import java.util.Locale

/**
 * Remembers the language picked on first launch or in Settings and applies it. The choice is kept in shared
 * preferences (not in the app data file) so it can be read synchronously before anything is drawn, including in the
 * alarm receivers and the widget.
 *
 * On Android 13+ the choice is also handed to the system's per-app language setting, so system dialogs (date and time
 * pickers) follow it and the app shows up under "App languages" in the phone settings; a language picked there is
 * taken over the next time the app starts. Older versions get the locale through [wrap].
 */
object LanguageStore {
    private const val PREFS = "language"
    private const val KEY = "language"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Whether the user has picked a language yet; until then the app opens on the language choice. */
    fun isChosen(context: Context): Boolean = prefs(context).contains(KEY)

    /** Loads the saved language into [Lang]; call it before any text is built (Application.onCreate, Activity.onCreate). */
    fun load(context: Context) {
        var saved = AppLanguage.fromCode(prefs(context).getString(KEY, null))
        // A language set in the phone's "App languages" settings wins, since the user picked it there last.
        if (saved != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val system = context.getSystemService(LocaleManager::class.java)?.applicationLocales
            val fromSystem = system?.takeIf { !it.isEmpty }?.get(0)?.let { AppLanguage.fromCode(it.toLanguageTag()) }
            if (fromSystem != null && fromSystem != saved) {
                prefs(context).edit().putString(KEY, fromSystem.name).apply()
                saved = fromSystem
            }
        }
        Lang.current = saved ?: deviceLanguage()
    }

    /** The phone's language when the app supports it, otherwise Serbian; preselected on the first-launch choice. */
    fun deviceLanguage(): AppLanguage {
        val locales = Resources.getSystem().configuration.locales
        for (i in 0 until locales.size()) AppLanguage.fromCode(locales[i].language)?.let { return it }
        return AppLanguage.SERBIAN
    }

    /** Saves [language], applies it and redraws [activity] in it. */
    fun choose(activity: Activity, language: AppLanguage) {
        val changed = Lang.current != language || !isChosen(activity)
        prefs(activity).edit().putString(KEY, language.name).commit()
        Lang.current = language
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.getSystemService(LocaleManager::class.java)?.applicationLocales = LocaleList.forLanguageTags(language.tag)
        }
        // Notification channel names and the widget are in the app's language too.
        ScheduleApplication.refreshLanguageDependents(activity.applicationContext)
        // On Android 13+ a new app locale recreates the activity by itself; when the locale already matches (English
        // chosen on an English phone) nothing would redraw it, so it is recreated here.
        val shownIn = AppLanguage.fromCode(activity.resources.configuration.locales[0].language)
        if (changed && (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || shownIn == language)) activity.recreate()
    }

    /** [base] with the chosen language's locale, for Android versions without per-app languages. */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        load(base)
        val locale = Locale.forLanguageTag(Lang.current.tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(config)
    }
}
