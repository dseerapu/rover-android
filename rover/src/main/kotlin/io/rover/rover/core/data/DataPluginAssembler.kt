package io.rover.rover.core.data

import android.content.Context
import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.Container
import io.rover.rover.core.container.Scope
import io.rover.rover.core.logging.AndroidLogger
import io.rover.rover.core.logging.LogEmitter
import io.rover.rover.platform.DateFormatting
import io.rover.rover.platform.DateFormattingInterface
import io.rover.rover.platform.DeviceIdentification
import io.rover.rover.platform.DeviceIdentificationInterface
import io.rover.rover.platform.IoMultiplexingExecutor
import io.rover.rover.platform.LocalStorage
import io.rover.rover.platform.SharedPreferencesLocalStorage
import io.rover.rover.core.data.graphql.GraphQlApiService
import io.rover.rover.core.data.graphql.GraphQlApiServiceInterface
import io.rover.rover.core.data.http.AsyncTaskAndHttpUrlConnectionNetworkClient
import io.rover.rover.core.data.http.NetworkClient
import io.rover.rover.core.data.graphql.WireEncoder
import io.rover.rover.core.data.http.WireEncoderInterface
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
open class DataPluginComponents(
    private val endpoint: URL,
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

    override val graphQlApiService: GraphQlApiServiceInterface by lazy {
        GraphQlApiService(
            endpoint,
            authenticationContext,
            deviceIdentification,
            wireEncoder,
            networkClient
        )
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
    private val accountToken: String,
    private val applicationContext: Context,
    private val url: String = "https://api.rover.io/graphql"
) : Assembler {

    override fun assemble(container: Container) {
        container.register(
            Scope.Singleton,
            AuthenticationContext::class.java
        ) { _ ->
            ServerKey(accountToken)
        }

        container.register(Scope.Singleton, DataPluginInterface::class.java) { resolver ->
            DataPlugin(
                DataPluginComponents(
                    URL(url),
                    resolver.resolveSingletonOrFail(AuthenticationContext::class.java),
                    applicationContext
                )
            )
        }

        container.register(Scope.Singleton, LogEmitter::class.java) { _ ->
            AndroidLogger()
        }
    }
}
