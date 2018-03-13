package io.rover.rover.core.data

import io.rover.rover.Rover
import io.rover.rover.core.data.graphql.GraphQlApiServiceInterface

/**
 * This is the Rover Data plugin.  It contains all the subsystems necessary for the other Plugins in
 * the Rover SDK to connect to the Rover API.  It is always required.
 *
 * You must include [DataPluginAssembler] in your call to [Rover.initialize].
 *
 * @param components
 */
class DataPlugin(
    private val components: DataPluginComponentsInterface
) : DataPluginInterface, GraphQlApiServiceInterface by components.graphQlApiService
