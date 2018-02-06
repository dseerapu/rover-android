package io.rover.rover.plugins.events.contextproviders

import io.rover.rover.BuildConfig
import io.rover.rover.plugins.data.domain.Context
import io.rover.rover.plugins.events.ContextProvider

/**
 * Captures and adds the Rover SDK version number to [Context].
 */
class RoverSdkContextProvider : ContextProvider {
    override fun captureContext(context: Context): Context {
        return context.copy(
            sdkVersion = BuildConfig.VERSION_NAME
        )
    }
}
