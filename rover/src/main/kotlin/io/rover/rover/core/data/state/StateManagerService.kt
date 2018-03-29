package io.rover.rover.core.data.state

import android.app.Application
import android.os.Handler
import android.os.Looper
import io.rover.rover.core.data.NetworkRequest
import io.rover.rover.core.data.NetworkResult
import io.rover.rover.core.data.graphql.GraphQlApiServiceInterface
import io.rover.rover.core.data.http.WireEncoderInterface
import io.rover.rover.core.logging.log
import org.json.JSONObject

class StateManagerService(
    private val graphQlApiService: GraphQlApiServiceInterface,
    private val application: Application,
    private val autoFetch: Boolean = true
): StateManagerServiceInterface, NetworkRequest<StateFetchSuccess> {
    override fun addStore(stateStore: StateStore) {
        stores.add(stateStore)
    }

    override fun triggerRefresh() {
        log.v("Performing refresh.")
        graphQlApiService.operation(this) { networkResult ->
            log.v("Got result: $networkResult")

            // we do not need to process the results here because the StateStores have done so on
            // their own.
            if(networkResult is NetworkResult.Error) {
                // TODO: retry behaviour?
                stores.forEach { it.informOfError(networkResult.throwable.message ?: "Unknown") }
            }
        }.resume()
    }

    override val operationName: String? = "StateRefresh"

    override val mutation: Boolean = false

    override val query: String
        get() = """
            query StateRefresh {
                device {
                    ${stores.joinToString("\n") { it.queryFragment }}
                }
            }
        """

    override val variables: JSONObject = JSONObject()

    override fun decodePayload(responseObject: JSONObject, wireEncoder: WireEncoderInterface): StateFetchSuccess {
        // deliver the update to each registered store as a side-effect.
        val device = responseObject.getJSONObject("data").getJSONObject("device")
        stores.forEach { it.updateState(device) }
        return StateFetchSuccess()
    }

    private val stores: MutableSet<StateStore> = mutableSetOf()

    init {
        // trigger refresh for the next loop of the Android main looper.  This will happen
        // after all of the Rover DI has completed and thus all of the stores have been registered.

        if(autoFetch) {
            Handler(Looper.getMainLooper()).post {
                triggerRefresh()
            }
        }
    }
}

class StateFetchSuccess
