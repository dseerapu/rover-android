package io.rover.rover.plugins.push

import android.content.Context
import android.support.annotation.DrawableRes
import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.Container
import io.rover.rover.platform.DateFormatting
import io.rover.rover.plugins.data.graphql.WireEncoder
import io.rover.rover.plugins.events.EventsPluginInterface
import io.rover.rover.plugins.userexperience.DefaultTopLevelNavigation
import io.rover.rover.plugins.userexperience.TopLevelNavigation

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
        container.register(PushPluginInterface::class.java) { resolver ->
            PushPlugin(
                applicationContext,
                // we need the Events Plugin because push notifications cannot work until the Events
                // plugin reports an event containing our Firebase Push Token to the Rover API.
                // TODO: once we expose internals to the DI layer directly inject FirebasePushTokenContextProvider here.
                resolver.resolveOrFail(EventsPluginInterface::class.java),
                WireEncoder(DateFormatting()), // TODO: borrowed from data plugin, will be OK after transitioning to Sean's new layout
                DefaultTopLevelNavigation(applicationContext), // TODO: borrowed from User Experience plugin, will be OK after...
                NotificationActionRoutingBehaviour(applicationContext), // more likely to be overridden by user.
                smallIconResId,
                smallIconDrawableLevel,
                defaultChannelId
            )
        }
    }
}