package com.example.nmeaplayer.server

/**
 * Information about a connected TCP client.
 *
 * @property id Unique identifier for the client connection (e.g. "192.168.1.10:54321").
 * @property ipAddress Client IP address.
 * @property port Client remote port.
 * @property connectedAt Epoch timestamp when client connected.
 */
data class ClientInfo(
    val id: String,
    val ipAddress: String,
    val port: Int,
    val connectedAt: Long = System.currentTimeMillis()
)
