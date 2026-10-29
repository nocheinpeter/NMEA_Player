package com.example.nmeaplayer.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

/**
 * TCP Socket Server Manager for streaming NMEA data to multiple concurrent clients.
 */
class TcpServerManager(
    private val defaultPort: Int = 10110
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null

    private val _serverState = MutableStateFlow(ServerState.STOPPED)
    val serverState: StateFlow<ServerState> = _serverState.asStateFlow()

    private val _port = MutableStateFlow(defaultPort)
    val port: StateFlow<Int> = _port.asStateFlow()

    private val _deviceIpAddress = MutableStateFlow("127.0.0.1")
    val deviceIpAddress: StateFlow<String> = _deviceIpAddress.asStateFlow()

    private val _activeClientCount = MutableStateFlow(0)
    val activeClientCount: StateFlow<Int> = _activeClientCount.asStateFlow()

    private val _connectedClients = MutableStateFlow<List<ClientInfo>>(emptyList())
    val connectedClients: StateFlow<List<ClientInfo>> = _connectedClients.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private data class ClientHolder(
        val info: ClientInfo,
        val socket: Socket,
        val outputStream: OutputStream,
        val job: Job
    )

    private val activeClientsMap = ConcurrentHashMap<String, ClientHolder>()

    init {
        refreshIpAddress()
    }

    /**
     * Starts the TCP server listening on [port].
     */
    fun start(port: Int = _port.value) {
        if (_serverState.value == ServerState.RUNNING || _serverState.value == ServerState.STARTING) {
            return
        }

        _port.value = port
        _serverState.value = ServerState.STARTING
        _errorMessage.value = null

        acceptJob = scope.launch(Dispatchers.IO) {
            _deviceIpAddress.value = NetworkUtils.getLocalIpAddress()
            try {
                val ss = ServerSocket(port)
                serverSocket = ss
                _serverState.value = ServerState.RUNNING

                while (_serverState.value == ServerState.RUNNING && !ss.isClosed) {
                    try {
                        val clientSocket = ss.accept()
                        handleNewClient(clientSocket)
                    } catch (e: Exception) {
                        if (ss.isClosed || _serverState.value == ServerState.STOPPED) {
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to start server on port $port"
                _serverState.value = ServerState.ERROR
            }
        }
    }

    /**
     * Handles a newly accepted client socket connection.
     */
    private fun handleNewClient(clientSocket: Socket) {
        val clientIp = clientSocket.inetAddress?.hostAddress ?: "Unknown"
        val clientPort = clientSocket.port
        val clientId = "$clientIp:$clientPort"
        val clientInfo = ClientInfo(
            id = clientId,
            ipAddress = clientIp,
            port = clientPort
        )

        try {
            val outputStream = clientSocket.getOutputStream()

            // Monitor client disconnection on a background job
            val readJob = scope.launch {
                try {
                    val inputStream = clientSocket.getInputStream()
                    val buffer = ByteArray(128)
                    while (_serverState.value == ServerState.RUNNING && !clientSocket.isClosed) {
                        val bytesRead = withContext(Dispatchers.IO) { inputStream.read(buffer) }
                        if (bytesRead == -1) {
                            break
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    removeClient(clientId)
                }
            }

            val holder = ClientHolder(
                info = clientInfo,
                socket = clientSocket,
                outputStream = outputStream,
                job = readJob
            )

            activeClientsMap[clientId] = holder
            updateClientState()
        } catch (e: Exception) {
            try {
                clientSocket.close()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Broadcasts an NMEA sentence line to all currently connected TCP clients.
     * Guarantees CRLF (`\r\n`) line ending.
     */
    fun broadcast(line: String) {
        if (_serverState.value != ServerState.RUNNING || activeClientsMap.isEmpty()) {
            return
        }

        val cleanLine = when {
            line.endsWith("\r\n") -> line
            line.endsWith("\n") -> line.dropLast(1) + "\r\n"
            line.endsWith("\r") -> line + "\n"
            else -> "$line\r\n"
        }

        val bytes = cleanLine.toByteArray(Charsets.US_ASCII)

        activeClientsMap.values.forEach { holder ->
            scope.launch(Dispatchers.IO) {
                try {
                    synchronized(holder.outputStream) {
                        holder.outputStream.write(bytes)
                        holder.outputStream.flush()
                    }
                } catch (_: Exception) {
                    removeClient(holder.info.id)
                }
            }
        }
    }

    /**
     * Removes a client by ID and safely releases socket resources.
     */
    fun removeClient(clientId: String) {
        val holder = activeClientsMap.remove(clientId) ?: return
        try {
            holder.job.cancel()
            holder.socket.close()
        } catch (_: Exception) {
        }
        updateClientState()
    }

    private fun updateClientState() {
        val list = activeClientsMap.values.map { it.info }
        _connectedClients.value = list
        _activeClientCount.value = list.size
    }

    /**
     * Refreshes the local device IP address state asynchronously on I/O.
     */
    fun refreshIpAddress() {
        scope.launch(Dispatchers.IO) {
            _deviceIpAddress.value = NetworkUtils.getLocalIpAddress()
        }
    }

    /**
     * Stops the TCP server and disconnects all clients.
     */
    fun stop() {
        _serverState.value = ServerState.STOPPED

        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null

        acceptJob?.cancel()
        acceptJob = null

        activeClientsMap.keys.toList().forEach { clientId ->
            removeClient(clientId)
        }
        activeClientsMap.clear()
        updateClientState()
    }
}
