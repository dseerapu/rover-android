package io.rover.rover.notifications

import android.content.Context
import android.support.annotation.DrawableRes
import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.Container
import io.rover.rover.core.container.Scope
import io.rover.rover.platform.DateFormatting
import io.rover.rover.platform.IoMultiplexingExecutor
import io.rover.rover.core.data.graphql.WireEncoder
import io.rover.rover.core.events.EventQueueServiceInterface
import io.rover.rover.experiences.DefaultTopLevelNavigation
import io.rover.rover.experiences.NotificationOpen
import io.rover.rover.core.assets.AndroidAssetService
import io.rover.rover.core.assets.ImageDownloader

class NotificationsAssembler(
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
    override fun assemble(container: Container) {
        container.register(Scope.Singleton, NotificationHandlerInterface::class.java) { resolver ->
            val routingBehaviour = NotificationActionRoutingBehaviour(
                applicationContext,
                DefaultTopLevelNavigation(applicationContext)
            )

            val intentSynth = NotificationContentPendingIntentSynthesizer(
                applicationContext,
                DefaultTopLevelNavigation(applicationContext),  // TODO: borrowed from User Experience plugin, will be OK after...
                routingBehaviour
            )

            val ioExecutor = IoMultiplexingExecutor.build("push plugin temporary")

            NotificationHandler(
                applicationContext,
                // we need the Events Plugin because push notifications cannot work until the Events
                // plugin reports an event containing our Firebase Push Token to the Rover API.
                // TODO: once we expose internals to the DI layer directly inject FirebasePushTokenContextProvider here.
                resolver.resolveSingletonOrFail(EventQueueServiceInterface::class.java),
                WireEncoder(DateFormatting()), // TODO: borrowed from data plugin, will be OK after transitioning to Sean's new layout
                // more likely to be overridden by user.
                NotificationOpen(
                    applicationContext,
                    WireEncoder(DateFormatting()),
                    resolver.resolveSingletonOrFail(EventQueueServiceInterface::class.java),
                    routingBehaviour,
                    intentSynth
                ),
                AndroidAssetService(ImageDownloader(ioExecutor), ioExecutor),
                smallIconResId,
                smallIconDrawableLevel,
                defaultChannelId
            )
        }
    }
}