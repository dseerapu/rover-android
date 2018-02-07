package io.rover.rover.plugins.events.contextproviders

import android.content.res.Resources
import android.util.DisplayMetrics
import android.view.WindowManager
import io.rover.rover.plugins.data.domain.Context
import io.rover.rover.plugins.events.ContextProvider
import io.rover.rover.plugins.userexperience.types.pxAsDp
import kotlin.math.roundToInt

/**
 * Captures and adds the screen geometry (as dps, not pixels) to a [Context].
 */
class ScreenContextProvider(
    private val resources: Resources
) : ContextProvider {
    override fun captureContext(context: Context): Context {
         val metrics = resources.displayMetrics
        // note that this includes *all* screen space, including the status bar and navigation bar.
        return context.copy(
            screenWidth = metrics.widthPixels.pxAsDp(metrics).roundToInt(),
            screenHeight = metrics.heightPixels.pxAsDp(metrics).roundToInt()
        )
    }
}
