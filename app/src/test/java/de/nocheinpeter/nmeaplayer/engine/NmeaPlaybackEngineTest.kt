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

    @Test
    fun testSeekingWithNearestPrecedingTimestamp() {
        val rawLog = """
            ${'$'}GPRMC,123519,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A
            ${'$'}GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47
            ${'$'}GPGSA,A,3,04,05,09,12,14,24,25,29,,,,1.2,0.9,0.8*03
            ${'$'}GPRMC,123525,A,4807.040,N,01131.002,E,022.6,084.6,230394,003.1,W*6D
            ${'$'}GPGSV,2,1,08,01,40,083,46,02,17,308,41,04,07,028,32,05,24,144,45*70
        """.trimIndent()
        val sentences = NmeaParser.parseString(rawLog)

        engine.loadSentences(sentences)

        assertEquals("12:35:19.000", engine.currentTimeString.value)
        assertEquals("12:35:19.000", engine.currentUtcTime.value)

        // Seek to line 2 (GPGSA, non-RMC with null timeFormatted)
        engine.seekTo(2)
        assertEquals(2, engine.currentIndex.value)
        // Should fall back to nearest preceding timestamp (line 0: 12:35:19.000)
        assertEquals("12:35:19.000", engine.currentTimeString.value)
        assertEquals("12:35:19.000", engine.currentUtcTime.value)

        // Seek to line 3 (GPRMC 123525)
        engine.seekTo(3)
        assertEquals("12:35:25.000", engine.currentTimeString.value)

        // Seek to line 4 (GPGSV, non-RMC)
        engine.seekTo(4)
        // Should fall back to nearest preceding timestamp (line 3: 12:35:25.000)
        assertEquals("12:35:25.000", engine.currentTimeString.value)
    }

    @Test
    fun testSeekToLineAndSeekToProgressUpdatesUtcTime() {
        val rawLog = """
            ${'$'}GPRMC,100000,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A
            ${'$'}GPRMC,100010,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6B
            ${'$'}GPRMC,100020,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6C
        """.trimIndent()
        val sentences = NmeaParser.parseString(rawLog)
        engine.loadSentences(sentences)

        engine.seekToLine(1)
        assertEquals("10:00:10.000", engine.currentTimeString.value)

        engine.seekToProgress(1.0f)
        assertEquals("10:00:20.000", engine.currentTimeString.value)
    }
}
