package io.rover.rover.plugins.events.contextproviders

import android.content.res.Resources
import android.os.Build
import android.support.v4.content.res.ResourcesCompat
import io.rover.rover.plugins.data.domain.Context
import io.rover.rover.plugins.events.ContextProvider
import java.util.Locale

/**
 * Captures and adds the user's locale information from [resources] and adds it to [Context].
 */
class LocaleContextProvider(
    private val resources: Resources
): ContextProvider {

    private fun getLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            resources.configuration.locales.get(0);
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale;
        }
    }

    override fun captureContext(context: Context): Context {
        val locale = getLocale()
        return context.copy(
            // ISO 639 alpha-2.
            localeLanguage = locale.language,

            // ISO 15924 alpha-4.
            localeScript = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                locale.script
            } else {
                // no straight-forward way to get the locale Script on older Android, alas.
                null
            },

            // ISO 3166 alpha-2.
            localeRegion = locale.country
        )
    }
}
