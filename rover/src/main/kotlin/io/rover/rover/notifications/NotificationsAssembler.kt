package io.rover.rover.notifications

import android.content.Context
import android.support.annotation.DrawableRes
import io.rover.rover.core.assets.AssetService
import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.Container
import io.rover.rover.core.container.Scope
import io.rover.rover.core.data.graphql.GraphQlApiServiceInterface
import io.rover.rover.core.data.http.WireEncoderInterface
import io.rover.rover.core.events.EventQueueServiceInterface
import io.rover.rover.core.streams.Scheduler
import io.rover.rover.experiences.DefaultTopLevelNavigation
import io.rover.rover.experiences.NotificationOpen
import io.rover.rover.experiences.NotificationOpenInterface
import io.rover.rover.experiences.TopLevelNavigation
import io.rover.rover.notifications.ui.NotificationCenterListViewModel
import io.rover.rover.notifications.ui.NotificationCenterListViewModelInterface
import io.rover.rover.notifications.ui.NotificationsRepositoryInterface
import io.rover.rover.platform.DateFormattingInterface
import io.rover.rover.platform.LocalStorage
import java.util.concurrent.Executor

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
        container.register(
            Scope.Singleton,
            NotificationsRepositoryInterface::class.java
        ) { resolver ->
            NotificationsRepository(
                resolver.resolveSingletonOrFail(GraphQlApiServiceInterface::class.java),
                resolver.resolveSingletonOrFail(DateFormattingInterface::class.java),
                resolver.resolveSingletonOrFail(Executor::class.java, "io"),
                resolver.resolveSingletonOrFail(Scheduler::class.java, "main"),
                resolver.resolveSingletonOrFail(EventQueueServiceInterface::class.java),
                resolver.resolveSingletonOrFail(LocalStorage::class.java)
            )
        }

        container.register(
            Scope.Singleton, // can be a singleton because it is stateless and has no parameters.
            NotificationCenterListViewModelInterface::class.java
        ) { resolver ->
            NotificationCenterListViewModel(
                resolver.resolveSingletonOrFail(NotificationsRepositoryInterface::class.java)
            )
        }

        container.register(
            Scope.Singleton,
            TopLevelNavigation::class.java
        ) { resolver ->
            DefaultTopLevelNavigation(applicationContext)
        }

        // this one will be oft overridden!
        container.register(
            Scope.Singleton,
            NotificationActionRoutingBehaviourInterface::class.java
        ) { resolver ->
            NotificationActionRoutingBehaviour(
                applicationContext,
                resolver.resolveSingletonOrFail(TopLevelNavigation::class.java)
            )
        }

        container.register(
            Scope.Singleton,
            NotificationContentPendingIntentSynthesizerInterface::class.java
        ) { resolver ->
            NotificationContentPendingIntentSynthesizer(
                applicationContext,
                resolver.resolveSingletonOrFail(TopLevelNavigation::class.java),
                resolver.resolveSingletonOrFail(NotificationActionRoutingBehaviourInterface::class.java)
            )
        }

        container.register(
            Scope.Singleton,
            NotificationOpenInterface::class.java
        ) { resolver ->
            NotificationOpen(
                applicationContext,
                resolver.resolveSingletonOrFail(WireEncoderInterface::class.java),
                resolver.resolveSingletonOrFail(EventQueueServiceInterface::class.java),
                resolver.resolveSingletonOrFail(NotificationActionRoutingBehaviourInterface::class.java),
                resolver.resolveSingletonOrFail(NotificationContentPendingIntentSynthesizerInterface::class.java)
            )
        }

        container.register(
            Scope.Singleton,
            NotificationHandlerInterface::class.java
        ) { resolver ->
            NotificationHandler(
                applicationContext,
                resolver.resolveSingletonOrFail(EventQueueServiceInterface::class.java),
                resolver.resolveSingletonOrFail(WireEncoderInterface::class.java),

                // more likely to be overridden by user.
                resolver.resolveSingletonOrFail(NotificationOpenInterface::class.java),
                resolver.resolveSingletonOrFail(AssetService::class.java),
                smallIconResId,
                smallIconDrawableLevel,
                defaultChannelId
            )
        }
    }
}