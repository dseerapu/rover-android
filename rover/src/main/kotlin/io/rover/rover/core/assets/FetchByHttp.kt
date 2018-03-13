package io.rover.rover.core.assets

import android.net.http.HttpResponseCache
import io.rover.rover.core.logging.log
import io.rover.rover.core.streams.Publisher
import io.rover.rover.core.streams.Subscriber
import io.rover.rover.core.streams.Subscription
import io.rover.rover.core.data.http.AsyncTaskAndHttpUrlConnectionNetworkClient
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executor

/**
 * A simple HTTP downloader.
 */
class ImageDownloader (
    private val ioExecutor: Executor
) {
    /**
     * Download the given URL as a stream.  Supports HTTP and HTTPS, but only GET is supported.
     *
     * @return a async Publisher that yields a single [HttpClientResponse].  That response itself
     * will contain a stream that can be read from until the connection completes.
     */
    fun downloadStreamFromUrl(url: URL): Publisher<HttpClientResponse> {
        // TODO: do the same global http cache check as the version in data plugin is doing.
        // TODO: also need some sort of interception support.

        return object : Publisher<HttpClientResponse> {
            override fun subscribe(subscriber: Subscriber<HttpClientResponse>) {
                var cancelled = false
                val subscription = object : Subscription {
                    override fun cancel() {
                        // We're not going to attempt cancellation because we don't have a way yet
                        // of dispatching this on the thread in the ioExecutor the connection got
                        // scheduled on in the first place.  Instead we'll just inhibit any more
                        // message deliveries.
                        cancelled = true
                    }
                }
                subscriber.onSubscribe(subscription)

                ioExecutor.execute {
                    val connection = url
                        .openConnection() as HttpURLConnection

                    if (!connection.useCaches || HttpResponseCache.getInstalled() == null) {
                        // We expect the user to agree to and implement setting up a global
                        // HttpUrlConnection cache. While we would much prefer to maintain our own,
                        // however, the global-side-effect nature of the Android HttpClient API means
                        // that we won't be able to achieve that without just building our own cache
                        // regime rather than using the stock Android cache, so we'll stick with this
                        // approach for now.
                        //
                        // This is made even more unfortunate because it would have been to our
                        // advantage to set up parallel NetworkClients with different caches in order to
                        // cache different request payloads separately: when the goal is to save
                        // perceptible delay for users small JSON payloads are more valuable to cache
                        // byte-for-byte compared with large bulky asset (say, images) payloads. Leaving
                        // them in the same LRU cache pool will mean that rotating through just a few
                        // large photos will cause the small payloads to be evicted even though their
                        // contribution to consumption of the cache is tiny.
                        throw AsyncTaskAndHttpUrlConnectionNetworkClient.missingCacheException()
                    }

                    connection.apply {
                        requestMethod = "GET"
                    }

                    val responseCode = try {
                        connection.connect()

                        connection.responseCode
                    } catch (e: IOException) {
                        // yield error to publisher.
                        log.w("$url -> connection failure: ${e.message}")

                        if (!cancelled) subscriber.onNext(
                            HttpClientResponse.ConnectionFailure(
                                e
                            )
                        )
                        if (!cancelled) subscriber.onComplete()

                        return@execute
                    }

                    log.v("$url -> HTTP $responseCode")

                    val result = when (responseCode) {
                        in 200..299 -> {
                            try {
                                HttpClientResponse.Success(
                                    CloseableBufferedInputStream(
                                        connection.inputStream
                                    ) {
                                        // when done reading the buffered stream, I can close out
                                        // the connection (actually, the platform usually interprets
                                        // this as returning it to the connection pool).
                                        connection.disconnect()
                                    }
                                )
                            } catch (e: IOException) {
                                HttpClientResponse.ConnectionFailure(
                                    e
                                )
                            }
                        }
                        else -> {
                            // we don't support handling redirects as anything other than an onError for now.
                            try {
                                val stream = BufferedInputStream(
                                    connection.errorStream
                                )
                                val result = HttpClientResponse.ApplicationError(
                                    responseCode,
                                    stream.reader(Charsets.UTF_8).readText()
                                )
                                stream.close()
                                connection.disconnect()
                                result
                            } catch (e: IOException) {
                                HttpClientResponse.ConnectionFailure(
                                    e
                                )
                            }
                        }
                    }

                    if(!cancelled) {
                        subscriber.onNext(result)
                        subscriber.onComplete()
                    }
                }
            }
        }
    }

    sealed class HttpClientResponse {
        class Success(
            /**
             * The HTTP request has gotten a successful reply, and now the server is streaming the
             * response body to us.
             */
            val bufferedInputStream: BufferedInputStream
        ) : HttpClientResponse()

        /**
         * A a session layer or below onError (HTTP protocol onError, network onError, and so on)
         * occurred. Likely culprit is local connectivity issues or possibly even a Rover API outage.
         */
        class ConnectionFailure(val reason: Throwable) : HttpClientResponse()

        /**
         * An application layer (HTTP) onError occurred (ie., a non-2xx status code).
         */
        class ApplicationError(
            val responseCode: Int,
            val reportedReason: String
        ) : HttpClientResponse()
    }

    class CloseableBufferedInputStream(
        input: InputStream,
        private val onClose: () -> Unit
    ) : BufferedInputStream(input) {
        override fun close() {
            super.close()
            onClose()
        }
    }
}
