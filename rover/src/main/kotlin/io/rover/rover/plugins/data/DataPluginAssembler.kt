package io.rover.rover.plugins.data

import android.content.Context
import io.rover.rover.DataPluginComponents
import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.Container
import io.rover.rover.platform.DateFormatting
import io.rover.rover.platform.DateFormattingInterface
import io.rover.rover.platform.DeviceIdentification
import io.rover.rover.platform.DeviceIdentificationInterface
import io.rover.rover.platform.LocalStorage
import io.rover.rover.platform.SharedPreferencesLocalStorage
import java.net.URL
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

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

// TODO: should the assembler provide an instance of what is currently called DataPluginComponents (and rename
// it to DataPluginComponents or similar)?

/**
 * These are all the internal dependencies needed by the [LiveDataPlugin].
 */
class LiveDataPluginComponents(
    override val authenticationContext: AuthenticationContext,
    applicationContext: Context
): DataPluginComponents {

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

    override val ioExecutor: ThreadPoolExecutor by lazy {
        ThreadPoolExecutor(
            10,
            Runtime.getRuntime().availableProcessors() * 200,
            2,
            TimeUnit.SECONDS,
            LinkedBlockingQueue<Runnable>()
        )
    }

    override val deviceIdentification: DeviceIdentificationInterface by lazy {
        DeviceIdentification(localStorage)
    }

    private val localStorage: LocalStorage by lazy {
        SharedPreferencesLocalStorage(applicationContext)
    }
}

/**
 *
 */
open class DataPluginAssembler(
    private val sdkKey: String,
    private val applicationContext: Context
) : Assembler {
    override fun register(container: Container) {
        container.register(DataPluginInterface::class.java) { resolver ->
            DataPlugin(
                URL("https://api.rover.io/graphql"),
                LiveDataPluginComponents(
                    ServerKey(sdkKey),
                    applicationContext
                )
            )
        }
    }
}
