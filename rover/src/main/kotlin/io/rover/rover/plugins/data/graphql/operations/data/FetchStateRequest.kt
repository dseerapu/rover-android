package io.rover.rover.plugins.data.graphql.operations.data

import io.rover.rover.plugins.data.domain.DeviceState
import io.rover.rover.plugins.data.NetworkRequest
import io.rover.rover.plugins.data.http.WireEncoderInterface
import org.json.JSONObject

class FetchStateRequest : NetworkRequest<DeviceState> {
    override val query: String
        get() = """
            query {
                device {
                    profile {
                        identifier
                        attributes
                    }
                    regions {
                        __typename
                        ... on BeaconRegion {
                            uuid
                            major
                            minor
                        }
                        ... on GeofenceRegion {
                            latitude
                            longitude
                            radius
                        }
                    }
                }
            }
            """

    override val variables: JSONObject
        get() = JSONObject()

    override fun decodePayload(responseObject: JSONObject, wireEncoder: WireEncoderInterface): DeviceState =
        DeviceState.decodeJson(responseObject.getJSONObject("data").getJSONObject("device"))
}
