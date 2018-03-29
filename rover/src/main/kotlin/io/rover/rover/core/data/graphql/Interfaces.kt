package io.rover.rover.core.data.graphql

import io.rover.rover.core.data.NetworkRequest
import io.rover.rover.core.data.NetworkResult
import io.rover.rover.core.data.domain.EventSnapshot
import io.rover.rover.core.data.domain.Experience
import io.rover.rover.core.data.domain.ID
import io.rover.rover.core.data.http.NetworkTask

interface GraphQlApiServiceInterface {
    /**
     * Retrieve the experience.
     *
     * @param completionHandler callback will be called with a result.
     */
    fun fetchExperienceTask(
        experienceId: ID,
        campaignId: ID?,
        completionHandler: ((NetworkResult<Experience>) -> Unit)
    ): NetworkTask

    /**
     * Retrieve the device state.
     *
     * @param completionHandler
     */
    fun <TEntity> operation(
        request: NetworkRequest<TEntity>,
        completionHandler: ((NetworkResult<TEntity>) -> Unit)?
    ): NetworkTask

    /**
     * Submit analytics events.
     *
     * @param completionHandler callback will be called with a result.
     */
    fun sendEventsTask(
        events: List<EventSnapshot>,
        completionHandler: ((NetworkResult<String>) -> Unit)
    ): NetworkTask
}

