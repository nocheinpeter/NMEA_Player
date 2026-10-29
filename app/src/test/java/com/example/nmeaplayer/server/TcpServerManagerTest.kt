package com.example.nmeaplayer.server

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket

class TcpServerManagerTest {

    private var testPort = 0
    private lateinit var serverManager: TcpServerManager

    @Before
    fun setUp() {
        // Find an available local port
        val dummySocket = ServerSocket(0)
        testPort = dummySocket.localPort
        dummySocket.close()

        serverManager = TcpServerManager(defaultPort = testPort)
    }

    @After
    fun tearDown() {
        serverManager.stop()
    }

    @Test
    fun testServerStartAndStopLifecycle() = runBlocking {
        assertEquals(ServerState.STOPPED, serverManager.serverState.value)

        serverManager.start(testPort)
        delay(200)

        assertEquals(ServerState.RUNNING, serverManager.serverState.value)
        assertEquals(testPort, serverManager.port.value)

        serverManager.stop()
        delay(100)

        assertEquals(ServerState.STOPPED, serverManager.serverState.value)
    }

    @Test
    fun testClientConnectionAndBroadcast() = runBlocking {
        serverManager.start(testPort)
        delay(200)

        // Connect client socket
        val clientSocket = Socket("127.0.0.1", testPort)
        val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream(), Charsets.US_ASCII))

        delay(300)
        assertEquals(1, serverManager.activeClientCount.value)

        val testSentence = "${'$'}GPRMC,123519.00,A,4807.038,N,01131.000,E,022.4,084.4,230324,003.1,W*6A"
        serverManager.broadcast(testSentence)

        delay(200)
        val receivedLine = reader.readLine()
        assertEquals(testSentence, receivedLine)

        clientSocket.close()
        delay(300)

        assertEquals(0, serverManager.activeClientCount.value)
    }
}
