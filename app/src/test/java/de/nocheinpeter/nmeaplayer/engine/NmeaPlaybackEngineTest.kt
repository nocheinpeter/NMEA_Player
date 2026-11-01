package de.nocheinpeter.nmeaplayer.engine

import de.nocheinpeter.nmeaplayer.parser.NmeaParser
import de.nocheinpeter.nmeaplayer.parser.NmeaSentence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NmeaPlaybackEngineTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var engine: NmeaPlaybackEngine

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        engine = NmeaPlaybackEngine(dispatcher = testDispatcher)
    }

    @After
    fun tearDown() {
        engine.shutdown()
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadSentencesResetsState() {
        val rawLog = """
            ${'$'}GPRMC,123519,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A
            ${'$'}GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47
        """.trimIndent()
        val sentences = NmeaParser.parseString(rawLog)

        engine.loadSentences(sentences)

        assertEquals(2, engine.totalSentences.value)
        assertEquals(0, engine.currentIndex.value)
        assertEquals(PlaybackState.STOPPED, engine.playbackState.value)
        assertNotNull(engine.currentSentence.value)
    }

    @Test
    fun testSeeking() {
        val rawLog = """
            ${'$'}GPRMC,123519,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A
            ${'$'}GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47
            ${'$'}GPRMC,123520,A,4807.039,N,01131.001,E,022.5,084.5,230394,003.1,W*6B
        """.trimIndent()
        val sentences = NmeaParser.parseString(rawLog)

        engine.loadSentences(sentences)

        engine.seekTo(1)
        assertEquals(1, engine.currentIndex.value)
        assertEquals(sentences[1], engine.currentSentence.value)

        engine.seekToProgress(1.0f)
        assertEquals(2, engine.currentIndex.value)
    }

    @Test
    fun testPlaybackBroadcastCallback() = runBlocking {
        val rawLog = """
            ${'$'}GPRMC,123519,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A
            ${'$'}GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47
        """.trimIndent()
        val sentences = NmeaParser.parseString(rawLog)

        val broadcastedList = mutableListOf<NmeaSentence>()
        engine.onSentenceBroadcast = { broadcastedList.add(it) }

        engine.loadSentences(sentences)
        engine.setSpeedMultiplier(100.0f)
        engine.play()

        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(broadcastedList.isNotEmpty())
    }
}
