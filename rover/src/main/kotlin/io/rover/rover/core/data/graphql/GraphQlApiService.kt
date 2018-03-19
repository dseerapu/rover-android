package io.rover.rover.core.data.graphql

import android.os.Handler
import android.os.Looper
import io.rover.rover.core.logging.log
import io.rover.rover.platform.DeviceIdentificationInterface
import io.rover.rover.core.data.APIException
import io.rover.rover.core.data.AuthenticationContext
import io.rover.rover.core.data.NetworkError
import io.rover.rover.core.data.NetworkRequest
import io.rover.rover.core.data.NetworkResult
import io.rover.rover.core.data.domain.DeviceState
import io.rover.rover.core.data.domain.EventSnapshot
import io.rover.rover.core.data.domain.Experience
import io.rover.rover.core.data.domain.ID
import io.rover.rover.core.data.http.HttpClientResponse
import io.rover.rover.core.data.http.HttpRequest
import io.rover.rover.core.data.http.HttpVerb
import io.rover.rover.core.data.http.NetworkClient
import io.rover.rover.core.data.http.NetworkTask
import io.rover.rover.core.data.http.WireEncoderInterface
import io.rover.rover.core.data.graphql.operations.FetchExperienceRequest
import io.rover.rover.core.data.graphql.operations.SendEventsRequest
import io.rover.rover.core.data.graphql.operations.FetchStateRequest
import org.json.JSONException
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

            when {
                authenticationContext.sdkToken != null -> this["x-rover-account-token"] = authenticationContext.sdkToken!!
                authenticationContext.bearerToken != null -> this["authorization"] = "Bearer ${authenticationContext.bearerToken}"
                else -> throw RuntimeException("Attempt to use DataPlugin when authentication is not available.")
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
                log.w("Given GraphQL error reason: ${httpResponse.reportedReason}")
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
                    val body = httpResponse.bufferedInputStream.use {
                        it.reader(Charsets.UTF_8).readText()
                    }

                    log.v("RESPONSE BODY: $body")
                    when (body) {
                        "" -> NetworkResult.Error(NetworkError.EmptyResponseData(), false)
                        else -> {
                            try {
                                NetworkResult.Success(
                                    // TODO This could emit, say, a JSON decode exception!  Need a story.
                                    httpRequest.decode(body, wireEncoder)
                                )
                            } catch (e: APIException) {
                                log.w("API error: $e")
                                NetworkResult.Error<TEntity>(
                                    NetworkError.InvalidResponseData(e.message ?: "API returned unknown error."),
                                    // retry is not appropriate when we're getting a domain-level
                                    // error from the GraphQL API.
                                    false
                                )
                            } catch (e: JSONException) {
                                // because the traceback information has some utility for diagnosing
                                // JSON decode errors, even though we're treating them as soft
                                // errors, throw the traceback onto the console:
                                log.w("JSON decode problem details: $e, ${e.stackTrace.joinToString("\n")}")

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
    private fun <TEntity> uploadTask(request: NetworkRequest<TEntity>, completionHandler: ((NetworkResult<TEntity>) -> Unit)?): NetworkTask {
        // TODO: once we change urlRequest() to use query parameters and GET for non-mutation
        // requests, replace true `below` with `request.mutation`.
        val urlRequest = urlRequest(true)
        val bodyData = request.encode()

        log.v("REQUEST BODY: $bodyData")

        return networkClient.networkTask(urlRequest, bodyData) { httpClientResponse ->
            val result = httpResult(request, httpClientResponse)
            completionHandler?.invoke(result)
        }
    }

    override fun fetchExperienceTask(experienceId: ID, campaignId: ID?, completionHandler: ((NetworkResult<Experience>) -> Unit)): NetworkTask {
        val request = FetchExperienceRequest(FetchExperienceRequest.ExperienceQueryIdentifier.ById(experienceId.rawValue, campaignId?.rawValue))
        return uploadTask(request) { experienceResult ->
            mainThreadHandler.post {
                completionHandler.invoke(experienceResult)
            }
        }
    }

    override fun sendEventsTask(
        events: List<EventSnapshot>,
        completionHandler: ((NetworkResult<String>) -> Unit)
    ): NetworkTask {
        if(!authenticationContext.isAvailable()) {
            log.w("Events may not be submitted without a Rover authentication context being configured.")

            return object : NetworkTask {
                override fun cancel() { /* no-op */}

                override fun resume() {
                    completionHandler(
                        NetworkResult.Error(
                            Exception("Attempt to submit Events without Rover authentication context being configured."),
                            false
                        )
                    )
                }
            }
        }
        val request = SendEventsRequest(
            events,
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