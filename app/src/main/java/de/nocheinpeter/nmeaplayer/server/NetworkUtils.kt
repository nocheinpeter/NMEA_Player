package de.nocheinpeter.nmeaplayer.server

import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {

    /**
     * Finds the active local IP address (IPv4) of the device.
     */
    fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback || intf.isVirtual) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val hostAddress = addr.hostAddress ?: continue
                        val isIPv4 = hostAddress.indexOf(':') < 0
                        if (isIPv4) {
                            return hostAddress
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }
}
