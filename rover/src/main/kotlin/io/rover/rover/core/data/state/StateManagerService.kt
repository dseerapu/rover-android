package io.rover.rover.core.data.state

import android.os.Handler
import android.os.Looper
import io.rover.rover.core.data.NetworkRequest
import io.rover.rover.core.data.NetworkResult
import io.rover.rover.core.data.graphql.GraphQlApiServiceInterface
import io.rover.rover.core.data.http.WireEncoderInterface
import io.rover.rover.core.logging.log
import io.rover.rover.core.streams.CallbackReceiver
import io.rover.rover.core.streams.PublishSubject
import io.rover.rover.core.streams.Publisher
import io.rover.rover.core.streams.asPublisher
import io.rover.rover.core.streams.flatMap
import io.rover.rover.core.streams.share
import org.json.JSONObject

class StateManagerService(
    private val graphQlApiService: GraphQlApiServiceInterface,
    autoFetch: Boolean = true
): StateManagerServiceInterface {
    override fun updatesForQueryFragment(queryFragment: String): Publisher<NetworkResult<JSONObject>> {
        queryFragments.add(queryFragment)

        // TODO: consider caching and re-emitting latest device state to all new subscribers.
        return updates
    }

    override fun triggerRefresh() {
        log.v("Performing refresh.")

        actionSubject.onNext(Unit)
    }

    private val queryFragments: MutableSet<String> = mutableSetOf()

    private val actionSubject =  PublishSubject<Unit>()

    private val updates = actionSubject.flatMap {
        { callback: CallbackReceiver<NetworkResult<JSONObject>> -> graphQlApiService.operation(DeviceStateNetworkRequest(queryFragments), callback) }.asPublisher()
    }.share()

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

private class DeviceStateNetworkRequest(
    private val queryFragments: Set<String>
): NetworkRequest<JSONObject> {
    override val operationName: String? = "StateRefresh"

    override val mutation: Boolean = false

    override val variables: JSONObject = JSONObject()

    override fun decodePayload(responseObject: JSONObject, wireEncoder: WireEncoderInterface): JSONObject {
        return responseObject.getJSONObject("data").getJSONObject("device")
    }

    override val query: String
        get() = """
            query $operationName {
                device {
                    ${queryFragments.joinToString("\n")}
                }
            }
        """
}
