package com.example.coursessupermarche.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton pour surveiller et exposer l'état de la connexion réseau
 */
object NetworkMonitor {

    private val _isOnlineFlow = MutableStateFlow(false)
    val isOnlineFlow: StateFlow<Boolean> = _isOnlineFlow.asStateFlow()

    // État actuel de la connexion
    var isOnline: Boolean = false
        private set

    fun init(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Vérifier la connexion initiale
        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        isOnline = networkCapabilities != null &&
                (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))

        _isOnlineFlow.value = isOnline

        // Surveiller les changements de connexion
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOnline = true
                _isOnlineFlow.value = true
            }

            override fun onLost(network: Network) {
                isOnline = false
                _isOnlineFlow.value = false
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }
}