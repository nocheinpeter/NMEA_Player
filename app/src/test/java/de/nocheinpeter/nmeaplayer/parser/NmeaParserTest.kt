package de.nocheinpeter.nmeaplayer.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NmeaParserTest {

    @Test
    fun testParseLineRmcSentence() {
        val raw = "${'$'}GPRMC,123519.00,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A"
        val sentence = NmeaParser.parseLine(raw, 0, null)

        assertTrue(sentence.isRmc)
        assertNotNull(sentence.timestampEpochMs)
        assertEquals("12:35:19.000", sentence.timeFormatted)
        assertEquals(0L, sentence.baseDelayMs)
        assertTrue(sentence.formattedLine.endsWith("\r\n"))
    }

    @Test
    fun testParseLineNonRmcSentence() {
        val raw = "${'$'}GPGGA,123519.00,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47"
        val sentence = NmeaParser.parseLine(raw, 1, null)

        assertFalse(sentence.isRmc)
        assertNull(sentence.timestampEpochMs)
        assertNull(sentence.timeFormatted)
        assertEquals(0L, sentence.baseDelayMs)
    }

    @Test
    fun testRmcDelayCalculationBetweenSentences() {
        val rmc1 = "${'$'}GPRMC,123519.00,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A"
        val rmc2 = "${'$'}GPRMC,123520.00,A,4807.039,N,01131.001,E,022.5,084.5,230394,003.1,W*6B"

        val parsed1 = NmeaParser.parseLine(rmc1, 0, null)
        val parsed2 = NmeaParser.parseLine(rmc2, 1, parsed1.timestampEpochMs)

        assertEquals(1000L, parsed2.baseDelayMs)
    }

    @Test
    fun testParseRmcTimestampValid() {
        val timeStr = "123519.50"
        val dateStr = "230394"

        val epochMs = NmeaParser.parseRmcTimestamp(timeStr, dateStr)
        assertNotNull(epochMs)
    }

    @Test
    fun testParseRmcTimestampInvalid() {
        assertNull(NmeaParser.parseRmcTimestamp(null, "230394"))
        assertNull(NmeaParser.parseRmcTimestamp("123", "230394"))
        assertNull(NmeaParser.parseRmcTimestamp("invalid", "230394"))
    }

    @Test
    fun testParseStringMultipleLines() {
        val rawLog = """
            ${'$'}GPRMC,123519.00,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A
            ${'$'}GPGGA,123519.00,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47
            ${'$'}GPRMC,123520.00,A,4807.039,N,01131.001,E,022.5,084.5,230394,003.1,W*6B
        """.trimIndent()

        val sentences = NmeaParser.parseString(rawLog)

        assertEquals(3, sentences.size)
        assertTrue(sentences[0].isRmc)
        assertFalse(sentences[1].isRmc)
        assertTrue(sentences[2].isRmc)
        assertEquals(1000L, sentences[2].baseDelayMs)
    }
}
