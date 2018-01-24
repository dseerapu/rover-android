package io.rover.rover.plugins.data

import android.content.Context
import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.Container
import io.rover.rover.platform.DateFormatting
import io.rover.rover.platform.DateFormattingInterface
import io.rover.rover.platform.DeviceIdentification
import io.rover.rover.platform.DeviceIdentificationInterface
import io.rover.rover.platform.IoMultiplexingExecutor
import io.rover.rover.platform.LocalStorage
import io.rover.rover.platform.SharedPreferencesLocalStorage
import java.net.URL
import java.util.concurrent.Executor

/**
 * A version of Authentication Context that works with simply a standard SDK Key, acquired from
 * [Rover Profile/Account settings](https://app.rover.io/settings/overview), as "Server Key" under
 * the "Account Tokens"->"Android" section.
 */
data class ServerKey(
    override val sdkToken: String?
): AuthenticationContext {
    override val bearerToken: String? = ""
}

/**
 * These are all the internal dependencies needed by the [DataPlugin].
 */
class DataPluginComponents(
    override val authenticationContext: AuthenticationContext,
    applicationContext: Context
): DataPluginComponentsInterface {

    override val networkClient: NetworkClient by lazy {
        AsyncTaskAndHttpUrlConnectionNetworkClient()
    }

    override val wireEncoder: WireEncoderInterface by lazy {
        WireEncoder(
            dateFormatting
        )
    }

    override val dateFormatting: DateFormattingInterface by lazy {
        DateFormatting()
    }

    override val ioExecutor: Executor by lazy {
        IoMultiplexingExecutor.build("data")
    }

    override val deviceIdentification: DeviceIdentificationInterface by lazy {
        DeviceIdentification(localStorage)
    }

    private val localStorage: LocalStorage by lazy {
        SharedPreferencesLocalStorage(applicationContext)
    }
}

/**
 * You must always pass an instance of this to Rover.initialize().  The Data Plugin provides access
 * to the Rover API and is required for all other Rover functionality.
 */
open class DataPluginAssembler(
    private val sdkKey: String,
    private val applicationContext: Context
) : Assembler {
    override fun register(container: Container) {
        container.register(DataPluginInterface::class.java) { resolver ->
            DataPlugin(
                URL("https://api.rover.io/graphql"),
                DataPluginComponents(
                    ServerKey(sdkKey),
                    applicationContext
                )
            )
        }
    }
}
