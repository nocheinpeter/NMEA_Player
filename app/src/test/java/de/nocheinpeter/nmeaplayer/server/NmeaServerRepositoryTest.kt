package de.nocheinpeter.nmeaplayer.server

import de.nocheinpeter.nmeaplayer.engine.PlaybackState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NmeaServerRepositoryTest {

    @Test
    fun testRepositoryInstances() {
        assertNotNull(NmeaServerRepository.tcpServerManager)
        assertNotNull(NmeaServerRepository.playbackEngine)
    }

    @Test
    fun testTriggerAppExitStopsServices() = runTest {
        NmeaServerRepository.playbackEngine.play()
        NmeaServerRepository.triggerAppExit()

        assertEquals(PlaybackState.STOPPED, NmeaServerRepository.playbackEngine.playbackState.value)
        assertEquals(ServerState.STOPPED, NmeaServerRepository.tcpServerManager.serverState.value)
    }
}
