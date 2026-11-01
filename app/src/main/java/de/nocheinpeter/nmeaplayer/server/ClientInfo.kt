package de.nocheinpeter.nmeaplayer.server

/**
 * Represents metadata of an active TCP client connection.
 */
data class ClientInfo(
    val id: String,
    val address: String,
    val port: Int,
    val connectedTimeMs: Long
) {
    val formattedAddress: String
        get() = "$address:$port"
}
