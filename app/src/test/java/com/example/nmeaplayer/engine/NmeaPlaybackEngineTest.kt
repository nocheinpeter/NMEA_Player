package com.example.nmeaplayer.engine

import com.example.nmeaplayer.parser.NmeaParser
import com.example.nmeaplayer.parser.NmeaSentence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NmeaPlaybackEngineTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
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
    fun testLoadSentencesResetsEngineState() {
        val nmeaLog = """
            ${'$'}GPRMC,123519.00,A,4807.038,N,01131.000,E,022.4,084.4,230324,003.1,W*6A
            ${'$'}GPRMC,123520.00,A,4807.040,N,01131.005,E,022.5,084.5,230324,003.1,W*68
        """.trimIndent()

        val sentences = NmeaParser.parseString(nmeaLog)
        engine.loadSentences(sentences)

        assertEquals(2, engine.totalSentences.value)
        assertEquals(0, engine.currentIndex.value)
        assertEquals(PlaybackState.STOPPED, engine.playbackState.value)
        assertEquals("12:35:19.000", engine.currentTimeString.value)
    }

    @Test
    fun testSeekToAndSeekToProgress() {
        val nmeaLog = """
            ${'$'}GPRMC,123519.00,A,4807.038,N,01131.000,E,022.4,084.4,230324,003.1,W*6A
            ${'$'}GPRMC,123520.00,A,4807.040,N,01131.005,E,022.5,084.5,230324,003.1,W*68
            ${'$'}GPRMC,123521.00,A,4807.042,N,01131.010,E,022.6,084.6,230324,003.1,W*6B
        """.trimIndent()

        val sentences = NmeaParser.parseString(nmeaLog)
        engine.loadSentences(sentences)

        engine.seekTo(1)
        assertEquals(1, engine.currentIndex.value)
        assertEquals("12:35:20.000", engine.currentTimeString.value)

        engine.seekToProgress(1.0f)
        assertEquals(2, engine.currentIndex.value)
        assertEquals("12:35:21.000", engine.currentTimeString.value)
    }

    @Test
    fun testPlaybackEmitsSentences() = runTest(testDispatcher) {
        val nmeaLog = """
            ${'$'}GPRMC,123519.00,A,4807.038,N,01131.000,E,022.4,084.4,230324,003.1,W*6A
            ${'$'}GPRMC,123520.00,A,4807.040,N,01131.005,E,022.5,084.5,230324,003.1,W*68
        """.trimIndent()

        val sentences = NmeaParser.parseString(nmeaLog)
        engine.loadSentences(sentences)
        engine.setSpeedMultiplier(10.0f) // 10x speed to accelerate test delay

        val emitted = mutableListOf<NmeaSentence>()
        engine.onSentenceBroadcast = { emitted.add(it) }

        engine.play()
        testScheduler.advanceUntilIdle()

        assertEquals(2, emitted.size)
        assertEquals(PlaybackState.STOPPED, engine.playbackState.value)
    }
}
