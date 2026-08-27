package com.klortek.velora.i18n

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import java.util.Locale

/** Shared language and playback-preference catalog for every Android form factor. */
data class VeloraLanguage(
    val tag: String,
    val label: String,
    val nativeLabel: String
)

object VeloraLocale {
    const val AUTO = "auto"
    const val SUBTITLES_OFF = "off"
    const val SUBTITLES_PREFERRED = "preferred"
    const val SUBTITLES_FORCED = "forced"
    const val SUBTITLES_AUTO = "auto"

    val languages: List<VeloraLanguage> = listOf(
        VeloraLanguage(AUTO, "Automático", "Automático"),
        VeloraLanguage("es", "Español", "Español"),
        VeloraLanguage("en", "Inglés", "English"),
        VeloraLanguage("pt", "Portugués", "Português"),
        VeloraLanguage("fr", "Francés", "Français"),
        VeloraLanguage("de", "Alemán", "Deutsch"),
        VeloraLanguage("it", "Italiano", "Italiano"),
        VeloraLanguage("ja", "Japonés", "日本語"),
        VeloraLanguage("ko", "Coreano", "한국어"),
        VeloraLanguage("zh", "Chino", "中文"),
        VeloraLanguage("ru", "Ruso", "Русский"),
        VeloraLanguage("ar", "Árabe", "العربية"),
        VeloraLanguage("tr", "Turco", "Türkçe")
    )

    fun systemLanguageTag(context: Context): String {
        val configuration = context.resources.configuration
        val locale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            configuration.locale
        }
        return locale.toLanguageTag().ifBlank { "es" }
    }

    fun effectiveLanguageTag(context: Context): String {
        val selected = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getString("language_tag", AUTO) ?: AUTO
        return if (selected == AUTO) systemLanguageTag(context) else selected
    }

    fun effectiveAudioLanguage(context: Context): String? {
        val selected = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getString("preferred_audio_language", AUTO) ?: AUTO
        return selected.takeUnless { it == AUTO }
    }

    fun selectedSubtitleMode(context: Context): String {
        return context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getString("subtitle_mode", SUBTITLES_OFF) ?: SUBTITLES_OFF
    }

    fun effectiveSubtitleLanguage(context: Context): String? {
        val selected = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getString("preferred_subtitle_language", AUTO) ?: AUTO
        return selected.takeUnless { it == AUTO }
    }

    /** Wraps resources before Compose is created; AUTO deliberately follows the device. */
    fun wrap(context: Context): Context {
        val selected = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getString("language_tag", AUTO) ?: AUTO
        if (selected == AUTO) return context
        val locale = Locale.forLanguageTag(selected)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }

    fun restart(context: Context) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return
        launchIntent.addFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
        context.startActivity(launchIntent)
        (context as? Activity)?.finish()
    }
}
