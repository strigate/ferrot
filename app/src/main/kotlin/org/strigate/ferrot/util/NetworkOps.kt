package org.strigate.ferrot.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import org.strigate.ferrot.app.Constants.LOG_TAG

object NetworkOps {
    private fun getConnectivityManager(context: Context): ConnectivityManager {
        return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    fun getActiveNetworkCapabilities(context: Context): NetworkCapabilities? {
        val connectivityManager = getConnectivityManager(context)
        val activeNetwork = connectivityManager.activeNetwork ?: return null
        return connectivityManager.getNetworkCapabilities(activeNetwork)
    }

    fun hasInternetConnection(context: Context): Boolean {
        val capabilities = getActiveNetworkCapabilities(context) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isOnWifiConnection(context: Context): Boolean {
        val capabilities = getActiveNetworkCapabilities(context) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun quickNetworkProbe(context: Context): Pair<Boolean, Boolean> {
        val capabilities = getActiveNetworkCapabilities(context)
        val primaryResult = evaluateNetwork(capabilities)
        if (primaryResult.first) {
            return primaryResult
        }
        @Suppress("DEPRECATION")
        val fallbackResult = getConnectivityManager(context).allNetworks
            .asSequence()
            .mapNotNull { network -> getConnectivityManager(context).getNetworkCapabilities(network) }
            .map(::evaluateNetwork)
            .firstOrNull { it.first } ?: primaryResult

        Log.d(
            LOG_TAG,
            "[quickNetworkProbe] active=${capabilities.describeCapabilities()} -> $fallbackResult"
        )
        return fallbackResult
    }

    private fun evaluateNetwork(capabilities: NetworkCapabilities?): Pair<Boolean, Boolean> {
        val isOnline = capabilities?.let {
            it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
                    it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } ?: false
        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        return isOnline to isWifi
    }

    private fun NetworkCapabilities?.describeCapabilities(): String {
        if (this == null) return "null"
        val transports = buildList {
            if (hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add("WIFI")
            if (hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add("CELL")
            if (hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add("ETH")
            if (hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add("VPN")
        }.joinToString("|").ifEmpty { "-" }
        val capabilities = buildList {
            if (hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) add("INTERNET")
            if (hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) add("VALIDATED")
        }.joinToString("|").ifEmpty { "-" }
        return "transports=$transports caps=$capabilities"
    }
}
