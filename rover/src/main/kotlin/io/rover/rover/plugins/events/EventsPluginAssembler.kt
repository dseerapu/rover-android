package io.rover.rover.plugins.events

import android.content.Context
import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.Container
import io.rover.rover.platform.DateFormatting
import io.rover.rover.platform.DateFormattingInterface
import io.rover.rover.platform.LocalStorage
import io.rover.rover.platform.SharedPreferencesLocalStorage
import io.rover.rover.plugins.data.DataPluginInterface

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
            )
        }
    }
}
