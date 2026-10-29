package com.example.nmeaplayer.server

import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtils {

    /**
     * Resolves the primary non-loopback IPv4 address of the Android device.
     */
    fun getLocalIpAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            var fallbackIp: String? = null

            while (interfaces != null && interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isUp && !networkInterface.isLoopback) {
                    val addrs = networkInterface.inetAddresses
                    while (addrs.hasMoreElements()) {
                        val addr = addrs.nextElement()
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val host = addr.hostAddress
                            if (host != null) {
                                if (networkInterface.name.contains("wlan") || networkInterface.name.contains("eth")) {
                                    return host
                                }
                                fallbackIp = host
                            }
                        }
                    }
                }
            }
            fallbackIp ?: "127.0.0.1"
        } catch (_: Exception) {
            "127.0.0.1"
        }
    }
}
