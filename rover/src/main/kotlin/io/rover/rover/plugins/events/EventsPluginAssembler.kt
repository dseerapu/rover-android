package io.rover.rover.plugins.events

import android.content.Context
import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.Container
import io.rover.rover.platform.DateFormatting
import io.rover.rover.platform.DateFormattingInterface
import io.rover.rover.platform.LocalStorage
import io.rover.rover.platform.SharedPreferencesLocalStorage
import io.rover.rover.plugins.data.DataPluginInterface
import io.rover.rover.plugins.events.contextproviders.DeviceContextProvider
import io.rover.rover.plugins.events.contextproviders.LocaleContextProvider
import io.rover.rover.plugins.events.contextproviders.ReachabilityContextProvider
import io.rover.rover.plugins.events.contextproviders.RoverSdkContextProvider
import io.rover.rover.plugins.events.contextproviders.ScreenContextProvider
import io.rover.rover.plugins.events.contextproviders.TelephonyContextProvider
import io.rover.rover.plugins.events.contextproviders.TimeZoneContextProvider

open class EventsPluginComponents(
    override val dataPlugin: DataPluginInterface,
    applicationContext: Context
): EventsPluginComponentsInterface {
    override val localStorage: LocalStorage by lazy {
        SharedPreferencesLocalStorage(applicationContext)
    }

    override val dateFormatting: DateFormattingInterface by lazy {
        DateFormatting()
    }
}

open class EventsPluginAssembler(
    private val applicationContext: Context
): Assembler {
    open val contextProviders: List<ContextProvider> = listOf(
        DeviceContextProvider(),
        LocaleContextProvider(applicationContext.resources),
        ReachabilityContextProvider(applicationContext),
        RoverSdkContextProvider(),
        ScreenContextProvider(applicationContext.resources),
        TelephonyContextProvider(applicationContext),
        TimeZoneContextProvider()
    )

    override fun register(container: Container) {
        container.register(EventsPluginInterface::class.java) { resolver ->
            EventsPlugin(
                EventsPluginComponents(
                    resolver.resolveOrFail(DataPluginInterface::class.java),
                    applicationContext
                ),
                20,
                30.0,
                100,
                1000
            ).apply {
                contextProviders.forEach { this.addContextProvider(it) }
            }
        }
    }
}
