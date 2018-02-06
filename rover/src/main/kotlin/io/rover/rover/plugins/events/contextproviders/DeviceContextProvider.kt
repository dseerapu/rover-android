package io.rover.rover.plugins.events.contextproviders

import android.os.Build
import io.rover.rover.plugins.data.domain.Context
import io.rover.rover.plugins.events.ContextProvider

/**
 * Captures and adds details about the product details of the user's device and its running Android
 * version to [Context].
 */
class DeviceContextProvider: ContextProvider {
    override fun captureContext(context: Context): Context {
        return context.copy(
            operatingSystemVersion = Build.VERSION.RELEASE,
            operatingSystemName = "Android",
            deviceManufacturer = Build.MANUFACTURER,
            deviceModel = Build.MODEL
        )
    }
}
