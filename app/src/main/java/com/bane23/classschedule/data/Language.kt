package com.bane23.classschedule.data

/** Languages the app is translated into; [tag] is the BCP 47 tag used for the Android locale. */
enum class AppLanguage(val tag: String) {
    /** Serbian in the Latin script, the original language of the app. */
    SERBIAN("sr-Latn"),
    ENGLISH("en"),
    ;

    companion object {
        /** The language for a stored or system language code ("sr", "sr-Latn-RS", "en-US", ...); null when unsupported. */
        fun fromCode(code: String?): AppLanguage? = when (code?.lowercase()?.substringBefore('-')?.substringBefore('_')) {
            "sr", "hr", "bs", "sh", "cnr" -> SERBIAN
            "en" -> ENGLISH
            else -> null
        }
    }
}

/**
 * The language the app is shown in. Set once at startup (see LanguageStore) and when the user picks another language,
 * so every entry point (the screens, notifications, the widget) reads the same choice without needing a Context.
 */
object Lang {
    @Volatile
    var current: AppLanguage = AppLanguage.SERBIAN

    val english: Boolean get() = current == AppLanguage.ENGLISH
}

/**
 * Picks the text for the current language. Texts are kept side by side in the code rather than in string resources,
 * because many of them are built from Serbian grammar forms (cases, plurals) that have no one-to-one English match.
 */
fun tr(sr: String, en: String): String = if (Lang.english) en else sr
