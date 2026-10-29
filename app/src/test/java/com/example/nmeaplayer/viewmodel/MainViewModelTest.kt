package com.example.nmeaplayer.viewmodel

import com.example.nmeaplayer.engine.NmeaPlaybackEngine
import com.example.nmeaplayer.engine.PlaybackState
import com.example.nmeaplayer.server.ServerState
import com.example.nmeaplayer.server.TcpServerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var tcpServerManager: TcpServerManager
    private lateinit var playbackEngine: NmeaPlaybackEngine
    private lateinit var viewModel: MainViewModel
    private var testPort = 10110

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val dummySocket = ServerSocket(0)
        testPort = dummySocket.localPort
        dummySocket.close()

        tcpServerManager = TcpServerManager(defaultPort = testPort)
        playbackEngine = NmeaPlaybackEngine(dispatcher = testDispatcher)

        viewModel = MainViewModel(tcpServerManager, playbackEngine)
    }

    @After
    fun tearDown() {
        viewModel.stopServer()
        playbackEngine.shutdown()
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() {
        assertEquals(ServerState.STOPPED, viewModel.serverState.value)
        assertEquals(PlaybackState.STOPPED, viewModel.playbackState.value)
        assertTrue(viewModel.totalSentences.value > 0) // loaded sample data by default
        assertEquals("Sample GPS Track", viewModel.fileName.value)
        assertNull(viewModel.portError.value)
    }

    @Test
    fun testPortInputValidation() {
        // Valid ports
        viewModel.onPortInputChanged("8080")
        assertEquals("8080", viewModel.portInputText.value)
        assertNull(viewModel.portError.value)

        viewModel.onPortInputChanged("65535")
        assertNull(viewModel.portError.value)

        viewModel.onPortInputChanged("1")
        assertNull(viewModel.portError.value)

        // Invalid ports
        viewModel.onPortInputChanged("0")
        assertNotNull(viewModel.portError.value)

        viewModel.onPortInputChanged("70000")
        assertNotNull(viewModel.portError.value)

        viewModel.onPortInputChanged("abc")
        assertNotNull(viewModel.portError.value)

        viewModel.onPortInputChanged("")
        assertNotNull(viewModel.portError.value)
    }

    @Test
    fun testServerStartAndStop() = runBlocking {
        viewModel.onPortInputChanged(testPort.toString())
        viewModel.startServer()
        delay(200)

        assertEquals(ServerState.RUNNING, viewModel.serverState.value)
        assertEquals(testPort, viewModel.serverPort.value)

        viewModel.stopServer()
        delay(100)
        assertEquals(ServerState.STOPPED, viewModel.serverState.value)
    }

    @Test
    fun testPlaybackEngineControls() {
        viewModel.play()
        assertEquals(PlaybackState.PLAYING, viewModel.playbackState.value)

        viewModel.pause()
        assertEquals(PlaybackState.PAUSED, viewModel.playbackState.value)

        viewModel.resume()
        assertEquals(PlaybackState.PLAYING, viewModel.playbackState.value)

        viewModel.stop()
        assertEquals(PlaybackState.STOPPED, viewModel.playbackState.value)
        assertEquals(0, viewModel.currentIndex.value)
    }

    @Test
    fun testSpeedAndRepeatSettings() {
        viewModel.setSpeedMultiplier(5.0f)
        assertEquals(5.0f, viewModel.speedMultiplier.value, 0.001f)

        viewModel.setRepeatEnabled(true)
        assertTrue(viewModel.repeatEnabled.value)
    }

    @Test
    fun testClearRecentLogs() {
        viewModel.clearRecentLogs()
        assertTrue(viewModel.recentLogs.value.isEmpty())
    }
}
