package io.rover.rover.plugins.data

import io.rover.rover.DataPluginComponents
import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.Container
import io.rover.rover.platform.DateFormatting
import io.rover.rover.platform.DateFormattingInterface
import io.rover.rover.platform.DeviceIdentificationInterface
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
class LiveDataPluginComponents(
    serverKey: ServerKey
): DataPluginComponents {
    override val authenticationContext: AuthenticationContext = serverKey

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
            Runtime.getRuntime().availableProcessors() * 20,
            2,
            TimeUnit.SECONDS,
            LinkedBlockingQueue<Runnable>()
        )
    }

    override val deviceIdentification: DeviceIdentificationInterface
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}

open class DataPluginAssembler(
    private val sdkKey: String
) : Assembler {
    override fun register(container: Container) {
        container.register(DataPlugin::class.java) { resolver ->
            DataPlugin(
                URL("https://api.rover.io"),
                LiveDataPluginComponents(
                    ServerKey(sdkKey)
                )
            )
        }
    }
}
