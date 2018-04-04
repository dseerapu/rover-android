package io.rover.location

import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.LocationResult
import com.google.android.gms.nearby.messages.Message
import io.rover.rover.core.data.domain.AttributeValue
import io.rover.rover.core.data.domain.Attributes
import io.rover.rover.core.events.EventQueueServiceInterface
import io.rover.rover.core.events.domain.Event
import io.rover.rover.core.logging.log
import java.util.Date

class GoogleLocationReportingService(
    val eventQueueService: EventQueueServiceInterface
): GoogleLocationReportingServiceInterface {
    override fun trackGeofenceEvent(geofencingEvent: GeofencingEvent) {
        if(geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(
                geofencingEvent.errorCode
            )

            log.w("Unable to capture Geofence message because: $errorMessage")

            when(geofencingEvent.geofenceTransition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> Event(
                    "Geofence Region Entered",
                    hashMapOf(
                        Pair("identifier", AttributeValue.String("")),
                        Pair("latitude", ),
                        Pair("longitude", ),
                        Pair("radius", )
                    )
                )
            }
        }
    }

    override fun trackEnterBeacon(message: Message) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun trackExitBeacon(message: Message) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateLocation(location: LocationResult) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}