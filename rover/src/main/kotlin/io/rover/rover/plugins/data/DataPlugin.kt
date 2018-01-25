package io.rover.rover.plugins.data

import android.os.Handler
import android.os.Looper
import io.rover.rover.plugins.data.domain.Context
import io.rover.rover.plugins.data.domain.DeviceState
import io.rover.rover.plugins.data.domain.Event
import io.rover.rover.plugins.data.domain.Experience
import io.rover.rover.plugins.data.domain.ID
import io.rover.rover.plugins.data.http.HttpClientResponse
import io.rover.rover.plugins.data.http.HttpRequest
import io.rover.rover.plugins.data.http.HttpVerb
import io.rover.rover.plugins.data.http.NetworkTask
import io.rover.rover.plugins.data.requests.FetchExperienceRequest
import io.rover.rover.plugins.data.requests.SendEventsRequest
import io.rover.rover.plugins.data.requests.data.FetchStateRequest
import java.io.IOException
import java.net.URL

/**
 * This is the Rover Data plugin.  It contains all the subsystems necessary to
 *
 * @param components
 */
class DataPlugin(
    private val endpoint: URL,
    private val components: DataPluginComponentsInterface
) : DataPluginInterface {

    private val mainThreadHandler = Handler(Looper.getMainLooper())

    private fun urlRequest(mutation: Boolean): HttpRequest = HttpRequest(
        endpoint,
        hashMapOf<String, String>().apply {
            this["Content-Type"] = "application/json"

            if (components.authenticationContext.sdkToken != null) {
                this["x-rover-account-token"] = components.authenticationContext.sdkToken!!
            } else if (components.authenticationContext.bearerToken != null) {
                this["authorization"] = "Bearer ${components.authenticationContext.bearerToken}"
            } else {
                throw RuntimeException("Attempt to use DataPlugin when authentication is not available.")
            }

            this["x-rover-device-identifier"] = components.deviceIdentification.installationIdentifier

            this.entries.forEach { (key, value) ->
                this[key] = value
            }
        },
        if (mutation) {
            HttpVerb.POST
        } else {
            HttpVerb.GET
        }
    )

    private fun <TEntity> httpResult(httpRequest: NetworkRequest<TEntity>, httpResponse: HttpClientResponse): NetworkResult<TEntity> =
        when (httpResponse) {
            is HttpClientResponse.ConnectionFailure -> NetworkResult.Error(httpResponse.reason, true)
            is HttpClientResponse.ApplicationError -> {
                NetworkResult.Error(
                    NetworkError.InvalidStatusCode(httpResponse.responseCode, httpResponse.reportedReason),
                    when {
                    // actually won't see any 200 codes here; already filtered about in the
                    // HttpClient response mapping.
                        httpResponse.responseCode < 300 -> false
                    // 3xx redirects
                        httpResponse.responseCode < 400 -> false
                    // 4xx request errors (we don't want to retry these; onus is likely on
                    // request creator).
                        httpResponse.responseCode < 500 -> false
                    // 5xx - any transient errors from the backend.
                        else -> true
                    }
                )
            }
            is HttpClientResponse.Success -> {
                try {
                    val body = httpResponse.bufferedInputStream.reader(Charsets.UTF_8).readText()
                    when (body) {
                        "" -> NetworkResult.Error<TEntity>(NetworkError.EmptyResponseData(), false)
                        else -> {
                            try {
                                NetworkResult.Success(
                                    httpRequest.decode(body, components.wireEncoder)
                                )
                            } catch (e: APIException) {
                                NetworkResult.Error<TEntity>(
                                    NetworkError.InvalidResponseData(e.message ?: "API returned unknown error."),
                                    // retry is not appropriate when we're getting a domain-level
                                    // error from the GraphQL API.
                                    false
                                )
                            }
                        }
                    }
                } catch (exception: IOException) {
                    NetworkResult.Error<TEntity>(exception, true)
                }
            }
        }

    /**
     * Make a request of the Rover cloud API.  Results are delivered into the provided
     * [completionHandler] callback, on the main thread.
     */
    fun <TEntity> uploadTask(request: NetworkRequest<TEntity>, completionHandler: ((NetworkResult<TEntity>) -> Unit)?): NetworkTask {
        // TODO: once we change urlRequest() to use query parameters and GET for non-mutation
        // requests, replace true `below` with `request.mutation`.
        val urlRequest = urlRequest(true)
        val bodyData = request.encode()

        return components.networkClient.networkTask(urlRequest, bodyData) { httpClientResponse ->
            val result = httpResult(request, httpClientResponse)
            completionHandler?.invoke(result)
        }
    }

    override fun fetchExperienceTask(experienceID: ID, completionHandler: ((NetworkResult<Experience>) -> Unit)): NetworkTask {
        val request = FetchExperienceRequest(experienceID)
        return uploadTask(request) { experienceResult ->
            mainThreadHandler.post {
                completionHandler.invoke(experienceResult)
            }
        }
    }

    override fun sendEventsTask(
        events: List<Event>,
        context: Context,
        completionHandler: ((NetworkResult<String>) -> Unit)
    ): NetworkTask {
        val request = SendEventsRequest(
            events,
            context,
            components.wireEncoder
        )
        return uploadTask(request) { uploadResult ->
            mainThreadHandler.post {
                completionHandler.invoke(uploadResult)
            }
        }
    }

    override fun fetchStateTask(completionHandler: ((NetworkResult<DeviceState>) -> Unit)): NetworkTask {
        val request = FetchStateRequest()
        return uploadTask(request) { uploadResult ->
            mainThreadHandler.post {
                completionHandler.invoke(uploadResult)
            }
        }
    }
}
