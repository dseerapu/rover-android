package io.rover.rover.core.events.contextproviders

import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import io.rover.rover.core.data.domain.Context
import io.rover.rover.core.events.ContextProvider

/**
 * Captures and adds information about the default route (ie., Wifi vs mobile data) to the [Context]
 *
 * NB.  Not to be confused with "Reachability", an iOS screen-ducking feature for thumb access.
 */
class ReachabilityContextProvider(
    applicationContext: android.content.Context
): ContextProvider {
    private val connectionManager = applicationContext.applicationContext.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private fun getNetworkInfoForType(networkType: Int): List<NetworkInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            connectionManager.allNetworks.map { network ->
                connectionManager.getNetworkInfo(network)
            }.filter { networkInfo ->
                networkInfo.type == networkType
            }.toList()
        } else {
            listOf(connectionManager.getNetworkInfo(networkType))
        }
    }

    override fun captureContext(context: Context): Context {
        val wifis = getNetworkInfoForType(ConnectivityManager.TYPE_WIFI)

        val basebands = getNetworkInfoForType(ConnectivityManager.TYPE_MOBILE)

        return context.copy(
            isWifiEnabled = wifis.any { it.isAvailable },
            isCellularEnabled = basebands.any { it.isAvailable }
        )
    }
}
