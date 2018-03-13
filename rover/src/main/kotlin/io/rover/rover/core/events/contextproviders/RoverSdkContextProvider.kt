package io.rover.rover.core.events.contextproviders

import io.rover.rover.BuildConfig
import io.rover.rover.core.data.domain.Context
import io.rover.rover.core.events.ContextProvider

/**
 * Captures and adds the Rover SDK version number to [Context].
 */
class RoverSdkContextProvider : ContextProvider {
    override fun captureContext(context: Context): Context {
        return context.copy(
            frameworks = context.frameworks.plus(hashMapOf(
                Pair(BuildConfig.APPLICATION_ID, BuildConfig.VERSION_NAME)
            ))
        )
    }
}
