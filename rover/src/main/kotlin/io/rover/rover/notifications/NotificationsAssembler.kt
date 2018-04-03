package io.rover.rover.notifications

import android.content.Context
import android.support.annotation.DrawableRes
import io.rover.rover.core.assets.AssetService
import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.Container
import io.rover.rover.core.container.Resolver
import io.rover.rover.core.container.Scope
import io.rover.rover.core.data.http.WireEncoderInterface
import io.rover.rover.core.data.state.StateManagerServiceInterface
import io.rover.rover.core.events.ContextProvider
import io.rover.rover.core.events.EventQueueServiceInterface
import io.rover.rover.core.events.contextproviders.FirebasePushTokenContextProvider
import io.rover.rover.core.routing.website.EmbeddedWebBrowserDisplayInterface
import io.rover.rover.core.streams.Scheduler
import io.rover.rover.experiences.DefaultTopLevelNavigation
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

    /**
     * Rover deep links are customized for each app in this way:
     *
     * rv-myapp://...
     *
     * You must select an appropriate slug without spaces or special characters to be used in place
     * of `myapp` above.  You must also configure this in your Rover settings TODO explain how
     *
     * You should also consider adding the handler to the manifest.  While this is not needed for
     * any Rover functionality to work, it is required for clickable deep/universal links to work from
     * anywhere else. TODO explain how once the stuff to do so is built
     */
    private val deepLinkSchemaSlug: String,

    private val defaultChannelId: String = "rover",

    /**
     * While normally your `FirebaseInstanceIdService` class will be responsible for being
     * informed of push token changes, from time to time (particularly on app upgrades or when
     * Rover 2.0 is first integrated in your app) Rover may need to force a reset of your Firebase
     * push token.
     *
     * This closure will be called on a background worker thread.  Please pass a block with
     * the following contents:
     *
     * ```kotlin
     * FirebaseInstanceId.getInstance().deleteInstanceId()
     * FirebaseInstanceId.getInstance().token
     * ```
     */
    private val resetPushToken: () -> Unit
) : Assembler {
    override fun assemble(container: Container) {
        container.register(
            Scope.Singleton,
            NotificationsRepositoryInterface::class.java
        ) { resolver ->
            NotificationsRepository(
                resolver.resolveSingletonOrFail(DateFormattingInterface::class.java),
                resolver.resolveSingletonOrFail(Executor::class.java, "io"),
                resolver.resolveSingletonOrFail(Scheduler::class.java, "main"),
                resolver.resolveSingletonOrFail(EventQueueServiceInterface::class.java),
                resolver.resolveSingletonOrFail(StateManagerServiceInterface::class.java),
                resolver.resolveSingletonOrFail(LocalStorage::class.java)
            )
        }

        // adds an additional context provider to the Events system (which itself is in Core)
        // to capture the push token and ship it up via an Event.
        container.register(Scope.Singleton, ContextProvider::class.java, "pushToken") { resolver ->
            FirebasePushTokenContextProvider(
                resolver.resolveSingletonOrFail(LocalStorage::class.java),
                resetPushToken
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
            ActionRoutingBehaviourInterface::class.java
        ) { resolver ->
            ActionRoutingBehaviour(
                applicationContext,
                resolver.resolveSingletonOrFail(TopLevelNavigation::class.java),
                resolver.resolveSingletonOrFail(EmbeddedWebBrowserDisplayInterface::class.java),
                deepLinkSchemaSlug
            )
        }

        container.register(
            Scope.Singleton,
            NotificationContentPendingIntentSynthesizerInterface::class.java
        ) { resolver ->
            NotificationContentPendingIntentSynthesizer(
                applicationContext,
                resolver.resolveSingletonOrFail(TopLevelNavigation::class.java)
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
                resolver.resolveSingletonOrFail(ActionRoutingBehaviourInterface::class.java),
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
                resolver.resolveSingletonOrFail(NotificationsRepositoryInterface::class.java),
                // more likely to be overridden by user.
                resolver.resolveSingletonOrFail(NotificationOpenInterface::class.java),
                resolver.resolveSingletonOrFail(AssetService::class.java),
                smallIconResId,
                smallIconDrawableLevel,
                defaultChannelId
            )
        }

        container.register(Scope.Singleton, ContextProvider::class.java,"notification") { _ ->
            NotificationContextProvider(applicationContext)
        }
    }

    override fun afterAssembly(resolver: Resolver) {
        val eventQueue = resolver.resolveSingletonOrFail(EventQueueServiceInterface::class.java)
        // wire up the push context provider such that the current push token can always be
        // included with outgoing events.
        val pushTokenContextProvider = resolver.resolveSingletonOrFail(ContextProvider::class.java, "pushToken")
        eventQueue.addContextProvider(
            pushTokenContextProvider
        )

        resolver.resolveSingletonOrFail(EventQueueServiceInterface::class.java).addContextProvider(
            resolver.resolveSingletonOrFail(ContextProvider::class.java, "notification")
        )

        resolver.resolveSingletonOrFail(NotificationsRepositoryInterface::class.java)
    }
}