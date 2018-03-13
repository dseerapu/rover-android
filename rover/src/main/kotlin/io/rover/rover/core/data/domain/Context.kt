package io.rover.rover.core.data.domain

/**
 * A Rover context: describes the device and general situation when an [Event] is generated.
 */
data class Context(
    val appBuild: String?,
    val appName: String?,
    val appNamespace: String?,
    val appVersion: String?,
    val carrierName: String?,
    val deviceManufacturer: String?,
    val deviceModel: String?,
    val isCellularEnabled: Boolean?,
    val isLocationServicesEnabled: Boolean?,
    val isWifiEnabled: Boolean?,
    val locationAuthorization: String?,
    val localeLanguage: String?,
    val localeRegion: String?,
    val localeScript: String?,

    // TODO: enum type
    val notificationAuthorization: String?,

    val operatingSystemName: String?,
    val operatingSystemVersion: String?,
    val pushEnvironment: String?,
    val pushToken: String?,
    val radio: String?,

    /**
     * Screen width, in dp.
     */
    val screenWidth: Int?,

    /**
     * Screen height, in dp.
     */
    val screenHeight: Int?,

    /**
     * All of the Rover libraries currently added to the app, where keys are their identifiers
     * and versions are their version names.  The name "Frameworks" is iOS-inspired.
     *
     * Known identifiers at time of writing:
     *
     * io.rover.rover -> the core library itself
     * etc.
     */
    val frameworks: Map<String, String>,
    val timeZone: String?
) {
    companion object {
        internal fun blank(): Context {
            return Context(
                null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, hashMapOf(), null
            )
        }
    }
}
