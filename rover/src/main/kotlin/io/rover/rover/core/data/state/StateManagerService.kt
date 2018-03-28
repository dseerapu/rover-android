package io.rover.rover.core.data.state

import android.app.Application
import android.os.Handler
import android.os.Looper
import io.rover.rover.core.data.NetworkRequest
import io.rover.rover.core.data.graphql.GraphQlApiServiceInterface
import io.rover.rover.core.data.http.WireEncoderInterface
import io.rover.rover.core.logging.log
import org.json.JSONObject

class StateManagerService(
    private val graphQlApiService: GraphQlApiServiceInterface,
    private val application: Application
): StateManagerServiceInterface, NetworkRequest<StateFetchSuccess> {
    override fun enableAutoFetch() {
        triggerRefresh()
    }

    override fun addStore(stateStore: StateStore) {
        stores.add(stateStore)
    }

    override fun triggerRefresh() {
        // TODO schedule or direct?

        log.v("Performing refresh.")

        //
        graphQlApiService.operation(this) { networkResult ->
            when(networkResult) {
                // TODO: retry behaviour?  however we do not need to process the results here.
            }
        }
    }

    override val operationName: String?
        get() = "State refresh for ${stores.joinToString(", ") { it.javaClass.name } }."

    override val mutation: Boolean = false

    override val query: String
        get() = """
            query {
                devices {
                    ${stores.joinToString("\n") { it.queryFragment }}
                }
            }
        """

    override val variables: JSONObject = JSONObject()

    override fun decodePayload(responseObject: JSONObject, wireEncoder: WireEncoderInterface): StateFetchSuccess {
        // deliver the update to each registered store as a side-effect.

        stores.forEach { it.updateState(responseObject) }
        return StateFetchSuccess()
    }

    private val stores: MutableSet<StateStore> = mutableSetOf()

    init {
        // trigger refresh for the next loop of the Android main looper.  This will happen
        // after all of the Rover DI has completed and thus all of the stores have been registered.

        Handler(Looper.getMainLooper()).post {
            triggerRefresh()
        }
    }
}

class StateFetchSuccess
