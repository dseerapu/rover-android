package io.rover.location

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.Container
import io.rover.rover.core.container.Resolver
import io.rover.rover.core.container.Scope
import io.rover.rover.core.data.state.StateManagerServiceInterface
import io.rover.rover.core.events.EventQueueServiceInterface
import io.rover.rover.core.streams.Scheduler

/**
 * Location Assembler contains the Rover SDK subsystems for Geofence, Beacon, and location tracking.
 */
class LocationAssembler(

    /**
     *
     */
    private val automaticLocationTracking: Boolean = true,

    /**
     * The Google
     */
    private val automaticRegionManagement: Boolean = true
) : Assembler {
    override fun assemble(container: Container) {

        container.register(
            Scope.Singleton,
            GoogleLocationReportingServiceInterface::class.java
        ) { resolver ->
            GoogleLocationReportingService(
                resolver.resolveSingletonOrFail(EventQueueServiceInterface::class.java)
            )
        }

        container.register(
            Scope.Singleton,
            RegionRepositoryInterface::class.java
        ) { resolver ->
            RegionRepository(
                resolver.resolveSingletonOrFail(StateManagerServiceInterface::class.java),
                resolver.resolveSingletonOrFail(Scheduler::class.java, "main")
            )
        }

        // if automatic location/region tracking, then register our Google-powered services:
        if(automaticLocationTracking) {
            container.register(
                Scope.Singleton,
                FusedLocationProviderClient::class.java
            ) { resolver ->
                LocationServices.getFusedLocationProviderClient(
                    resolver.resolveSingletonOrFail(Context::class.java)
                )
            }

            container.register(
                Scope.Singleton,
                GoogleBackgroundLocationServiceInterface::class.java
            ) { resolver ->
                GoogleBackgroundLocationService(
                    resolver.resolveSingletonOrFail(
                        FusedLocationProviderClient::class.java
                    ),
                    resolver.resolveSingletonOrFail(Context::class.java),
                    resolver.resolveSingletonOrFail(GoogleLocationReportingServiceInterface::class.java)
                )
            }
        }

        if(automaticRegionManagement) {
            container.register(
                Scope.Singleton,
                GeofencingClient::class.java
            ) { resolver ->
                LocationServices.getGeofencingClient(
                    resolver.resolveSingletonOrFail(Context::class.java)
                )
            }

            container.register(
                Scope.Singleton,
                GoogleGeofenceServiceInterface::class.java
            ) { resolver ->
                GoogleGeofenceService(
                    resolver.resolveSingletonOrFail(GeofencingClient::class.java),
                    resolver.resolveSingletonOrFail(
                        GoogleLocationReportingServiceInterface::class.java
                    )
                )
            }
        }
    }

    override fun afterAssembly(resolver: Resolver) {
        if(automaticRegionManagement) {
            // register our GoogleGeofenceService as an observer of the Rover regions (geofences),
            // if the user wants the managed solution.
            resolver.resolveSingletonOrFail(RegionRepositoryInterface::class.java).registerObserver(
                resolver.resolveSingletonOrFail(GoogleGeofenceServiceInterface::class.java)
            )
        }

        if(automaticLocationTracking) {
            // greedily poke for GoogleBackgroundLocationService to force the DI to evaluate
            // it and therefore have it start monitoring.
            resolver.resolveSingletonOrFail(GoogleBackgroundLocationServiceInterface::class.java)
        }
    }
}
