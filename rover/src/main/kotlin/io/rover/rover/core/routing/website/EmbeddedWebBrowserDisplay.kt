package io.rover.rover.core.routing.website

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.support.customtabs.CustomTabsIntent
import android.support.design.R.attr.colorPrimary
import android.util.TypedValue
import io.rover.rover.R
import io.rover.rover.core.logging.log


class EmbeddedWebBrowserDisplay(
    private val applicationContext: Context,

    /**
     * Set the background colour for the Chrome Custom tab's title bar.
     *
     * If not supplied, will try to use your theme's primary colour (assuming your theme
     * is based on AppCompat).
     */
    private val backgroundColor: Int? = null
): EmbeddedWebBrowserDisplayInterface {
    override fun intentForViewingWebsiteViaEmbeddedBrowser(url: String): Intent {
        val builder = CustomTabsIntent.Builder()

        builder.setToolbarColor(backgroundColor ?: accentColor())
        val customTabIntentHolder = builder.build()
        customTabIntentHolder.intent.data = Uri.parse(url)
        return customTabIntentHolder.intent
    }

    private fun accentColor(): Int {
        val typedValue = TypedValue()

        val colorAttribute = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.R.attr.colorPrimary
        } else {
            applicationContext.resources.getIdentifier("colorAccent", "attr", applicationContext.packageName)
        }

        applicationContext.theme.resolveAttribute(colorAttribute, typedValue, true)

//        val a = applicationContext.obtainStyledAttributes(R.style.Base_Theme_AppCompat_Light, intArrayOf(R.attr.colorPrimary))
//        val color = a.getColor(0, 0)
//        a.recycle()

        log.v("Perhaps got a colour for custom chrome tab: : ${typedValue.coerceToString()}")

        return typedValue.data
    }
}
