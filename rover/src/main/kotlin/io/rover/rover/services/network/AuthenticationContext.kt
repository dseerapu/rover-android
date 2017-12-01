package io.rover.rover.services.network

/**
 * Implementers can furnish consumers with a Rover API account SDK authentication token, or a Bearer
 * OAuth token or null if authentication is not available.
 */
interface AuthenticationContext {
    val sdkToken: String?

    val bearerToken: String?
}
