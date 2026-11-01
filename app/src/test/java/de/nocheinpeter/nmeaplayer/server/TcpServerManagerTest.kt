package de.nocheinpeter.nmeaplayer.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket

@OptIn(ExperimentalCoroutinesApi::class)
class TcpServerManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var serverManager: TcpServerManager
    private var freePort = 10110

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val dummySocket = ServerSocket(0)
        freePort = dummySocket.localPort
        dummySocket.close()

        serverManager = TcpServerManager(defaultPort = freePort)
    }

    @After
    fun tearDown() {
        serverManager.stop()
        Dispatchers.resetMain()
    }

    @Test
    fun testServerLifecycle() = runBlocking {
        assertEquals(ServerState.STOPPED, serverManager.serverState.value)

        serverManager.start(freePort)
        delay(200)

        assertEquals(ServerState.RUNNING, serverManager.serverState.value)
        assertEquals(freePort, serverManager.port.value)

        serverManager.stop()
        delay(100)

        assertEquals(ServerState.STOPPED, serverManager.serverState.value)
    }

    @Test
    fun testClientConnectionAndBroadcasting() = runBlocking {
        serverManager.start(freePort)
        delay(200)

        assertEquals(ServerState.RUNNING, serverManager.serverState.value)

        val clientSocket = Socket("127.0.0.1", freePort)
        delay(200)

        assertTrue(serverManager.activeClientCount.value >= 1)

        val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream(), Charsets.UTF_8))
        val testMessage = "${'$'}GPRMC,123519,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A\r\n"

        serverManager.broadcast(testMessage)
        delay(200)

        val received = reader.readLine()
        assertEquals(testMessage.trimEnd('\r', '\n'), received?.trimEnd('\r', '\n'))

        clientSocket.close()
        serverManager.stop()
    }
}
