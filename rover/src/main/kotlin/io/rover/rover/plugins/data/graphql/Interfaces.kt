package io.rover.rover.plugins.data.graphql

import io.rover.rover.plugins.data.NetworkResult
import io.rover.rover.plugins.data.domain.Context
import io.rover.rover.plugins.data.domain.DeviceState
import io.rover.rover.plugins.data.domain.Event
import io.rover.rover.plugins.data.domain.Experience
import io.rover.rover.plugins.data.domain.ID
import io.rover.rover.plugins.data.http.NetworkTask

interface GraphQlApiServiceInterface {
    /**
     * Retrieve the experience.
     *
     * @param completionHandler callback will be called with a result.
     */
    fun fetchExperienceTask(
        experienceID: ID,
        completionHandler: ((NetworkResult<Experience>) -> Unit)
    ): NetworkTask

    /**
     * Retrieve the device state.
     *
     * @param completionHandler callback will be called with a result.
     */
    fun fetchStateTask(
        completionHandler: ((NetworkResult<DeviceState>) -> Unit)
    ): NetworkTask


    /**
     * Submit analytics events.
     *
     * @param completionHandler callback will be called with a result.
     */
    fun sendEventsTask(
        events: List<Event>,
        context: Context,
        completionHandler: ((NetworkResult<String>) -> Unit)
    ): NetworkTask
}
