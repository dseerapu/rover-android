package io.rover.rover.core

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.support.annotation.ColorInt
import io.rover.rover.core.assets.AndroidAssetService
import io.rover.rover.core.assets.AssetService
import io.rover.rover.core.assets.ImageDownloader
import io.rover.rover.core.assets.ImageOptimizationService
import io.rover.rover.core.assets.ImageOptimizationServiceInterface
import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.Container
import io.rover.rover.core.container.Resolver
import io.rover.rover.core.container.Scope
import io.rover.rover.core.data.AuthenticationContext
import io.rover.rover.core.data.ServerKey
import io.rover.rover.core.data.graphql.GraphQlApiService
import io.rover.rover.core.data.graphql.GraphQlApiServiceInterface
import io.rover.rover.core.data.graphql.WireEncoder
import io.rover.rover.core.data.http.AsyncTaskAndHttpUrlConnectionNetworkClient
import io.rover.rover.core.data.http.NetworkClient
import io.rover.rover.core.data.http.WireEncoderInterface
import io.rover.rover.core.data.state.StateManagerService
import io.rover.rover.core.data.state.StateManagerServiceInterface
import io.rover.rover.core.events.ContextProvider
import io.rover.rover.core.events.EventQueueService
import io.rover.rover.core.events.EventQueueServiceInterface
import io.rover.rover.core.events.contextproviders.*
import io.rover.rover.core.logging.AndroidLogger
import io.rover.rover.core.logging.GlobalStaticLogHolder
import io.rover.rover.core.routing.website.EmbeddedWebBrowserDisplay
import io.rover.rover.core.routing.website.EmbeddedWebBrowserDisplayInterface
import io.rover.rover.core.streams.Scheduler
import io.rover.rover.core.streams.forAndroidMainThread
import io.rover.rover.platform.*
import java.net.URL
import java.util.concurrent.Executor

/**
 * The core module of the Rover SDK.
 *
 * You must always pass an instance of Assembler to Rover.initialize().  The Core module provides
 * access to the Rover API and is required for all other Rover functionality.
 */
class CoreAssembler(
    private val accountToken: String,
    private val application: Application,
    private val endpoint: String = "https://api.rover.io/graphql",
    @param:ColorInt
    private val chromeTabBackgroundColor: Int = Color.BLACK
): Assembler {
    override fun assemble(container: Container) {
        // logger, which we "inject" using static scope
        GlobalStaticLogHolder.globalLogEmitter = AndroidLogger()

        container.register(Scope.Singleton, Context::class.java) { _ ->
            application
        }

        container.register(Scope.Singleton, NetworkClient::class.java) { _ ->
            AsyncTaskAndHttpUrlConnectionNetworkClient()
        }

        container.register(Scope.Singleton, DateFormattingInterface::class.java) { _ ->
            DateFormatting()
        }

        container.register(Scope.Singleton, WireEncoderInterface::class.java) { resolver ->
            WireEncoder(resolver.resolveSingletonOrFail(DateFormattingInterface::class.java))
        }

        container.register(Scope.Singleton, Executor::class.java, "io") { _ ->
            IoMultiplexingExecutor.build("io")
        }

        container.register(Scope.Singleton, Scheduler::class.java, "main") { _ ->
            Scheduler.forAndroidMainThread()
        }

        container.register(Scope.Singleton, LocalStorage::class.java) { _ ->
            SharedPreferencesLocalStorage(application)
        }

        container.register(Scope.Singleton, DeviceIdentificationInterface::class.java) { resolver ->
            DeviceIdentification(resolver.resolveSingletonOrFail(LocalStorage::class.java))
        }

        container.register(Scope.Singleton, AuthenticationContext::class.java) { _ ->
            ServerKey(accountToken)
        }

        container.register(Scope.Singleton, GraphQlApiServiceInterface::class.java) { resolver ->
            GraphQlApiService(
                URL(endpoint),
                resolver.resolveSingletonOrFail(AuthenticationContext::class.java),
                resolver.resolveSingletonOrFail(DeviceIdentificationInterface::class.java),
                resolver.resolveSingletonOrFail(WireEncoderInterface::class.java),
                resolver.resolveSingletonOrFail(NetworkClient::class.java)
            )
        }

        container.register(Scope.Singleton, ImageDownloader::class.java) { resolver ->
            ImageDownloader(resolver.resolveSingletonOrFail(Executor::class.java, "io"))
        }

        container.register(Scope.Singleton, AssetService::class.java) { resolver ->
            AndroidAssetService(
                resolver.resolveSingletonOrFail(ImageDownloader::class.java),
                resolver.resolveSingletonOrFail(Executor::class.java, "io")
            )
        }

        container.register(Scope.Singleton, ImageOptimizationServiceInterface::class.java) { resolver ->
            ImageOptimizationService()
        }

        container.register(Scope.Singleton, ContextProvider::class.java, "device") { _ ->
            DeviceContextProvider()
        }

        container.register(Scope.Singleton, ContextProvider::class.java, "locale") { _ ->
            LocaleContextProvider(application.resources)
        }

        container.register(Scope.Singleton, ContextProvider::class.java, "reachability") { _ ->
            ReachabilityContextProvider(application)
        }

        container.register(Scope.Singleton, ContextProvider::class.java, "coreVersion") { _ ->
            RoverSdkCoreVersionContextProvider()
        }

        container.register(Scope.Singleton, ContextProvider::class.java, "screen") { _ ->
            ScreenContextProvider(application.resources)
        }

        container.register(Scope.Singleton, ContextProvider::class.java, "telephony") { _ ->
            TelephonyContextProvider(application)
        }

        container.register(Scope.Singleton, ContextProvider::class.java, "device") { _ ->
            DeviceContextProvider()
        }

        container.register(Scope.Singleton, ContextProvider::class.java, "timeZone") { _ ->
            TimeZoneContextProvider()
        }

        container.register(Scope.Singleton, EventQueueServiceInterface::class.java) { resolver ->
            EventQueueService(
                resolver.resolveSingletonOrFail(GraphQlApiServiceInterface::class.java),
                resolver.resolveSingletonOrFail(LocalStorage::class.java),
                resolver.resolveSingletonOrFail(DateFormattingInterface::class.java),
                application,
                20,
                30.0,
                100,
                1000
            )
        }

        container.register(Scope.Singleton, EmbeddedWebBrowserDisplayInterface::class.java) { resolver ->
            EmbeddedWebBrowserDisplay(
                chromeTabBackgroundColor
            )
        }

        container.register(Scope.Singleton, StateManagerServiceInterface::class.java) { resolver ->
            StateManagerService(
                resolver.resolveSingletonOrFail(GraphQlApiServiceInterface::class.java),
                application
            )
        }
    }

    override fun afterAssembly(resolver: Resolver) {
        val eventQueue = resolver.resolveSingletonOrFail(EventQueueServiceInterface::class.java)

        listOf(
            resolver.resolveSingletonOrFail(ContextProvider::class.java, "device"),
            resolver.resolveSingletonOrFail(ContextProvider::class.java, "locale"),
            resolver.resolveSingletonOrFail(ContextProvider::class.java, "reachability"),
            resolver.resolveSingletonOrFail(ContextProvider::class.java, "coreVersion"),
            resolver.resolveSingletonOrFail(ContextProvider::class.java, "screen"),
            resolver.resolveSingletonOrFail(ContextProvider::class.java, "telephony"),
            resolver.resolveSingletonOrFail(ContextProvider::class.java, "timeZone")
        ).forEach { eventQueue.addContextProvider(it) }

        // TODO: for Profile!
//        resolver.resolveSingletonOrFail(StateManagerServiceInterface::class.java).addStore(
//            //
//        )
    }
}