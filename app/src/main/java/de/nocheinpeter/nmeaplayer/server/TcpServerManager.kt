package de.nocheinpeter.nmeaplayer.server

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages TCP server listening socket, client connection threads, and sentence broadcasting.
 */
class TcpServerManager(
    defaultPort: Int = 10110,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null

    private val clientsMap = ConcurrentHashMap<String, ClientSocketHolder>()

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

    init {
        refreshIpAddress()
    }

    fun refreshIpAddress() {
        scope.launch {
            val ip = withContext(Dispatchers.IO) { NetworkUtils.getLocalIpAddress() }
            _deviceIpAddress.value = ip
        }
    }

    fun setPort(newPort: Int) {
        if (_serverState.value == ServerState.STOPPED) {
            _port.value = newPort
        }
    }

    fun start(portToUse: Int = _port.value) {
        if (_serverState.value == ServerState.RUNNING || _serverState.value == ServerState.STARTING) {
            return
        }

        _port.value = portToUse
        _serverState.value = ServerState.STARTING
        refreshIpAddress()

        acceptJob = scope.launch {
            try {
                val ss = ServerSocket(portToUse)
                serverSocket = ss
                _serverState.value = ServerState.RUNNING

                while (serverSocket?.isClosed == false) {
                    val clientSocket = ss.accept()
                    handleNewClient(clientSocket)
                }
            } catch (e: SocketException) {
                if (serverSocket == null || serverSocket?.isClosed == true) {
                    _serverState.value = ServerState.STOPPED
                } else {
                    _serverState.value = ServerState.ERROR
                }
            } catch (e: Exception) {
                if (serverSocket == null || serverSocket?.isClosed == true) {
                    _serverState.value = ServerState.STOPPED
                } else {
                    _serverState.value = ServerState.ERROR
                }
            } finally {
                closeServer()
            }
        }
    }

    fun stop() {
        closeServer()
        _serverState.value = ServerState.STOPPED
    }

    private fun handleNewClient(socket: Socket) {
        val clientId = UUID.randomUUID().toString()
        val address = socket.inetAddress?.hostAddress ?: "Unknown"
        val clientPort = socket.port
        val info = ClientInfo(clientId, address, clientPort, System.currentTimeMillis())

        try {
            val writer = PrintWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8), true)
            val holder = ClientSocketHolder(info, socket, writer)
            clientsMap[clientId] = holder
            updateClientState()

            scope.launch {
                monitorClientConnection(holder)
            }
        } catch (e: Exception) {
            try {
                socket.close()
            } catch (_: Exception) {}
        }
    }

    private suspend fun monitorClientConnection(holder: ClientSocketHolder) {
        withContext(Dispatchers.IO) {
            try {
                val input = holder.socket.getInputStream()
                val buffer = ByteArray(128)
                while (!holder.socket.isClosed) {
                    val bytesRead = input.read(buffer)
                    if (bytesRead == -1) break
                }
            } catch (_: Exception) {
            } finally {
                removeClient(holder.info.id)
            }
        }
    }

    fun broadcast(line: String) {
        if (clientsMap.isEmpty()) return

        val formatted = if (line.endsWith("\r\n")) line else if (line.endsWith("\n")) line.replace("\n", "\r\n") else "$line\r\n"

        scope.launch(Dispatchers.IO) {
            val deadClients = mutableListOf<String>()
            for ((id, holder) in clientsMap) {
                try {
                    holder.writer.print(formatted)
                    holder.writer.flush()
                    if (holder.writer.checkError()) {
                        deadClients.add(id)
                    }
                } catch (e: Exception) {
                    deadClients.add(id)
                }
            }
            for (id in deadClients) {
                removeClient(id)
            }
        }
    }

    private fun removeClient(clientId: String) {
        val removed = clientsMap.remove(clientId)
        if (removed != null) {
            try {
                removed.socket.close()
            } catch (_: Exception) {}
            updateClientState()
        }
    }

    private fun updateClientState() {
        val currentList = clientsMap.values.map { it.info }
        _connectedClients.value = currentList
        _activeClientCount.value = currentList.size
    }

    private fun closeServer() {
        acceptJob?.cancel()
        acceptJob = null

        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null

        for (holder in clientsMap.values) {
            try {
                holder.socket.close()
            } catch (_: Exception) {}
        }
        clientsMap.clear()
        updateClientState()
    }

    private data class ClientSocketHolder(
        val info: ClientInfo,
        val socket: Socket,
        val writer: PrintWriter
    )
}
