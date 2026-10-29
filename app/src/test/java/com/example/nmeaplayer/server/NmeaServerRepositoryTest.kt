package com.example.nmeaplayer.server

import com.example.nmeaplayer.engine.PlaybackState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NmeaServerRepositoryTest {

    @Test
    fun testRepositoryInitializationAndState() {
        assertNotNull(NmeaServerRepository.tcpServerManager)
        assertNotNull(NmeaServerRepository.playbackEngine)
        assertEquals(ServerState.STOPPED, NmeaServerRepository.tcpServerManager.serverState.value)
        assertEquals(PlaybackState.STOPPED, NmeaServerRepository.playbackEngine.playbackState.value)
    }

    @Test
    fun testTriggerAppExitStopsPlaybackAndServerAndEmitsExitEvent() = runBlocking {
        var exitReceived = false
        val job = launch {
            NmeaServerRepository.exitAppEvent.first()
            exitReceived = true
        }

        NmeaServerRepository.triggerAppExit()

        job.join()

        assertEquals(ServerState.STOPPED, NmeaServerRepository.tcpServerManager.serverState.value)
        assertEquals(PlaybackState.STOPPED, NmeaServerRepository.playbackEngine.playbackState.value)
        assertEquals(true, exitReceived)
    }
}
