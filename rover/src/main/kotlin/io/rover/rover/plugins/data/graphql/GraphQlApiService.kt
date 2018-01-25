package io.rover.rover.plugins.data.graphql

import android.os.Handler
import android.os.Looper
import io.rover.rover.platform.DeviceIdentificationInterface
import io.rover.rover.plugins.data.APIException
import io.rover.rover.plugins.data.AuthenticationContext
import io.rover.rover.plugins.data.DataPlugin
import io.rover.rover.plugins.data.NetworkError
import io.rover.rover.plugins.data.NetworkRequest
import io.rover.rover.plugins.data.NetworkResult
import io.rover.rover.plugins.data.domain.Context
import io.rover.rover.plugins.data.domain.DeviceState
import io.rover.rover.plugins.data.domain.Event
import io.rover.rover.plugins.data.domain.Experience
import io.rover.rover.plugins.data.domain.ID
import io.rover.rover.plugins.data.http.HttpClientResponse
import io.rover.rover.plugins.data.http.HttpRequest
import io.rover.rover.plugins.data.http.HttpVerb
import io.rover.rover.plugins.data.http.NetworkClient
import io.rover.rover.plugins.data.http.NetworkTask
import io.rover.rover.plugins.data.http.WireEncoderInterface
import io.rover.rover.plugins.data.graphql.operations.FetchExperienceRequest
import io.rover.rover.plugins.data.graphql.operations.SendEventsRequest
import io.rover.rover.plugins.data.graphql.operations.data.FetchStateRequest
import java.io.IOException
import java.net.URL

/**
 * Responsible for providing access the Rover cloud API, powered by GraphQL.
 *
 * If you would like to override or augment any of the behaviour here, you may override it in
 * [DataPlugin].
 */
class GraphQlApiService(
    private val endpoint: URL,
    private val authenticationContext: AuthenticationContext,
    private val deviceIdentification: DeviceIdentificationInterface,
    private val wireEncoder: WireEncoderInterface,
    private val networkClient: NetworkClient
): GraphQlApiServiceInterface {
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    private fun urlRequest(mutation: Boolean): HttpRequest = HttpRequest(
        endpoint,
        hashMapOf<String, String>().apply {
            this["Content-Type"] = "application/json"

            if (authenticationContext.sdkToken != null) {
                this["x-rover-account-token"] = authenticationContext.sdkToken!!
            } else if (authenticationContext.bearerToken != null) {
                this["authorization"] = "Bearer ${authenticationContext.bearerToken}"
            } else {
                throw RuntimeException("Attempt to use DataPlugin when authentication is not available.")
            }

            this["x-rover-device-identifier"] = deviceIdentification.installationIdentifier

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
                        "" -> NetworkResult.Error(NetworkError.EmptyResponseData(), false)
                        else -> {
                            try {
                                NetworkResult.Success(
                                    httpRequest.decode(body, wireEncoder)
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

        return networkClient.networkTask(urlRequest, bodyData) { httpClientResponse ->
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
            wireEncoder
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