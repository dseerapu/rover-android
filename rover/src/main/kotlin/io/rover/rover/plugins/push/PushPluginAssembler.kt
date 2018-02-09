package io.rover.rover.plugins.push

import android.content.Context
import android.support.annotation.DrawableRes
import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.Container


class PushPluginAssembler(
    private val applicationContext: Context,
    /**
     * A small icon is necessary for Android push notifications.  Pass a resid.
     *
     * Android design guidelines suggest that you use a multi-level drawable for your application
     * icon, such that you can specify one of its levels that is most appropriate as a single-colour
     * silhouette that can be used in the Android notification drawer.
     */
    @param:DrawableRes
    private val smallIconResId: Int,

    /**
     * The drawable level of [smallIconResId] that should be used for the icon silhouette used in
     * the notification drawer.
     */
    private val smallIconDrawableLevel: Int = 0,

    private val defaultChannelId: String? = null
) : Assembler {
    override fun register(container: Container) {
        container.register(PushPluginInterface::class.java) { _ ->
            PushPlugin(
                applicationContext,
                smallIconResId,
                smallIconDrawableLevel,
                defaultChannelId
            )
        }
    }
}