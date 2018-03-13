package io.rover.rover.core.events

import android.app.Application
import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.Container
import io.rover.rover.core.container.Scope
import io.rover.rover.platform.DateFormatting
import io.rover.rover.platform.DateFormattingInterface
import io.rover.rover.platform.LocalStorage
import io.rover.rover.platform.SharedPreferencesLocalStorage
import io.rover.rover.core.data.DataPluginInterface
import io.rover.rover.core.events.contextproviders.DeviceContextProvider
import io.rover.rover.core.events.contextproviders.FirebasePushTokenContextProvider
import io.rover.rover.core.events.contextproviders.LocaleContextProvider
import io.rover.rover.core.events.contextproviders.ReachabilityContextProvider
import io.rover.rover.core.events.contextproviders.RoverSdkContextProvider
import io.rover.rover.core.events.contextproviders.ScreenContextProvider
import io.rover.rover.core.events.contextproviders.TelephonyContextProvider
import io.rover.rover.core.events.contextproviders.TimeZoneContextProvider

open class EventsPluginComponents(
    override val dataPlugin: DataPluginInterface,
    override val application: Application,
    private val resetPushToken: () -> Unit
): EventsPluginComponentsInterface {
    private val applicationContext = application.applicationContext

    override val localStorage: LocalStorage by lazy {
        SharedPreferencesLocalStorage(application.applicationContext)
    }

    override val dateFormatting: DateFormattingInterface by lazy {
        DateFormatting()
    }

    // TODO: remove EventsPluginComponents from this constructor
    override val contextProviders: List<ContextProvider> by lazy {
        listOf(
            DeviceContextProvider(),
            LocaleContextProvider(applicationContext.resources),
            ReachabilityContextProvider(applicationContext),
            RoverSdkContextProvider(),
            ScreenContextProvider(applicationContext.resources),
            TelephonyContextProvider(applicationContext),
            TimeZoneContextProvider(),
            FirebasePushTokenContextProvider(localStorage, resetPushToken)
        )
    }
}

open class EventsPluginAssembler(
    private val application: Application,

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
): Assembler {

    override fun assemble(container: Container) {
        container.register(Scope.Singleton, EventQueueServiceInterface::class.java) { resolver ->
            val components = EventsPluginComponents(
                resolver.resolveSingletonOrFail(DataPluginInterface::class.java),
                application,
                resetPushToken
            )

            EventQueueService(
                components,
                20,
                30.0,
                100,
                1000
            ).apply {
                components.contextProviders.forEach { contextProvider ->
                    contextProvider.registeredWithEventsPlugin(this)
                    this.addContextProvider(contextProvider)
                }
            }
        }
    }
}
