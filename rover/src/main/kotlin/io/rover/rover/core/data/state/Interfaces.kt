package io.rover.rover.core.data.state

import io.rover.rover.core.data.domain.DeviceState
import org.json.JSONObject

/**
 * This service is responsible for making timely requests to the endpoint for [DeviceState]s.  These
 * Device State can contain data from multiple concerns in the Rover SDK, so rather than deserialize
 * and persist them all here we instead delegate to the registered [StateStore]s to do so.
 */
interface StateManagerServiceInterface {
    /**
     * Add a [StateStore] to the state manager.  The state store will be responsible for parsing out
     * and perhaps caching/persisting
     */
    fun addStore(stateStore: StateStore)

    /**
     * Instruct the state manager to aggressively attempt to update the device state on app launch.
     */
    fun enableAutoFetch()

    fun disableAutoFetch()

    // TODO: start here when tomorrow when figuring out when/how we should sync
    // https://developer.android.com/about/versions/oreo/android-8.0-changes.html

    // Probably JobScheduler, but need to be sure how to repeat.  Worst case, we have to use a SyncAdapter.
}

/**
 * The [DeviceState] may contain concerns relevant to different verticals of the Rover SDK.
 *
 * Different Rover modules may register a StateStore to handle and update themselves with device
 * state data whenever a state update occurs.
 */
interface StateStore {
    val queryFragment: String

    fun updateState(data: JSONObject)
}
