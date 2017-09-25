package io.rover.rover.services.network

import io.rover.rover.core.logging.log
import io.rover.rover.services.concurrency.Scheduler
import io.rover.rover.services.concurrency.Single
import io.rover.rover.services.concurrency.Subject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

sealed class HttpClientResponse {
    class Success(
        /**
         *
         */
        val bufferedInputStream: BufferedInputStream
    ): HttpClientResponse()

    /**
     * A a session layer or below error (HTTP protocol error, network error, and so on) occurred.
     * Likely culprit is local connectivity issues or possibly even a Rover API outage.
     */
    class ConnectionFailure(): HttpClientResponse()

    /**
     * An application layer (HTTP) error occurred (ie., a non-2xx status code).
     */
    class ApplicationError(
        val responseCode: Int,
        val reportedReason: String
    ): HttpClientResponse()
}

interface HttpClient {
    /**
     * Do a very simple, synchronous (blocking the thread) HTTP post
     */
    fun post(
        url: URL,
        headers: HashMap<String,String>,
        body: String
    ): Single<HttpClientResponse>
}

/**
 * A façade around HttpUrlConnection, the basic HTTP client provided in the Android/Java platform,
 * intended to be readily mockable.  Simple, stock, but sadly synchronous.
 */
class PlatformSimpleHttpClient(
    private val scheduler: Scheduler
): HttpClient {
    override fun post(
        url: URL,
        headers: HashMap<String, String>,
        body: String
    ): Single<HttpClientResponse> {

        // we'll use a Subject<T>.
        // although I really just need Single.just and Single.defer.

        // val subject = Subject<HttpClientResponse>()

        return scheduler.scheduleOperation {
            log.d("POST $url")
            val connection = url
                .openConnection() as HttpsURLConnection

            val requestBody = body.toByteArray(Charsets.UTF_8)

            connection
                .apply {
                    // TODO: set read and connect timeouts.
                    // TODO: set a nice user agent.

                    setFixedLengthStreamingMode(requestBody.size)

                    // add the request headers.
                    headers.onEach { (field, value) -> setRequestProperty(field, value) }

                    // sets HttpUrlConnection to use POST.
                    doOutput = true
                    requestMethod = "POST"
                }


            // synchronously write the body to the connection.
            DataOutputStream(connection.outputStream).write(requestBody)

            val responseCode = connection.responseCode

            val inputStream = BufferedInputStream(
                connection.inputStream
            )

            // TODO: better composition needed here once the concurrency package improves.
            when (responseCode) {
                in 200..299 -> {
                    HttpClientResponse.Success(
                        BufferedInputStream(
                            // TODO I still need a story for how clients will be notified of read errors
                            // that happen in the midst of the client code reading the stream.  Right
                            // now HTTPUrlConnection exceptions will leak through and be yielded to
                            // the Single's observers.
                            connection.inputStream
                        )
                    )
                }
                else -> {
                    // we don't support handling redirects as anything other than an error for now.
                    HttpClientResponse.ApplicationError(
                        responseCode,
                        inputStream.reader(Charsets.UTF_8).readText()
                    )
                }
            }
        }
    }
}
