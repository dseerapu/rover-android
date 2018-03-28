package io.rover.rover.core.data.state

import io.rover.rover.core.data.domain.DeviceState
import io.rover.rover.core.data.NetworkRequest
import io.rover.rover.core.data.http.WireEncoderInterface
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
        wireEncoder.decodeDeviceState(responseObject.getJSONObject("data").getJSONObject("device"))
}
