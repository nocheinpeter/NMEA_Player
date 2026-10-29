package com.example.nmeaplayer.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NmeaParserTest {

    @Test
    fun testParseRmcTimestampValidDateAndTime() {
        // Time 12:35:19.50 (hhmmss.ss) -> 12:35:19.500 UTC, Date 23/03/24 (ddmmyy) -> March 23, 2024
        val timeStr = "123519.50"
        val dateStr = "230324"

        val timestampEpochMs = NmeaParser.parseRmcTimestamp(timeStr, dateStr)
        assertNotNull(timestampEpochMs)

        // Verify Calendar parsing result
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = timestampEpochMs!!

        assertEquals(2024, cal.get(java.util.Calendar.YEAR))
        assertEquals(java.util.Calendar.MARCH, cal.get(java.util.Calendar.MONTH))
        assertEquals(23, cal.get(java.util.Calendar.DAY_OF_MONTH))
        assertEquals(12, cal.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(35, cal.get(java.util.Calendar.MINUTE))
        assertEquals(19, cal.get(java.util.Calendar.SECOND))
        assertEquals(500, cal.get(java.util.Calendar.MILLISECOND))
    }

    @Test
    fun testParseRmcTimestampMissingDateUsesFallbackDate() {
        val timeStr = "081530" // 08:15:30.000 UTC
        val dateStr: String? = null

        val timestampEpochMs = NmeaParser.parseRmcTimestamp(timeStr, dateStr)
        assertNotNull(timestampEpochMs)

        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = timestampEpochMs!!

        assertEquals(2000, cal.get(java.util.Calendar.YEAR))
        assertEquals(8, cal.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(15, cal.get(java.util.Calendar.MINUTE))
        assertEquals(30, cal.get(java.util.Calendar.SECOND))
    }

    @Test
    fun testParseRmcTimestampInvalidReturnsNull() {
        assertNull(NmeaParser.parseRmcTimestamp("invalid", "230324"))
        assertNull(NmeaParser.parseRmcTimestamp(null, "230324"))
        assertNull(NmeaParser.parseRmcTimestamp("123", "230324"))
    }

    @Test
    fun testParseStringWithRmcAndNonRmcSentences() {
        val nmeaLog = """
            ${'$'}GPRMC,123519.00,A,4807.038,N,01131.000,E,022.4,084.4,230324,003.1,W*6A
            ${'$'}GPGGA,123519.00,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47
            ${'$'}GNRMC,123520.50,A,4807.040,N,01131.005,E,022.5,084.5,230324,003.1,W*68
            ${'$'}GARMC,123522.00,A,4807.042,N,01131.010,E,022.6,084.6,230324,003.1,W*6B
        """.trimIndent()

        val parsedList = NmeaParser.parseString(nmeaLog)

        assertEquals(4, parsedList.size)

        // Line 0: GPRMC @ 12:35:19.000 -> baseDelay = 0ms
        val sentence0 = parsedList[0]
        assertTrue(sentence0.isRmc)
        assertEquals(0L, sentence0.baseDelayMs)
        assertNotNull(sentence0.timeFormatted)

        // Line 1: GPGGA -> non-RMC -> baseDelay = 0ms
        val sentence1 = parsedList[1]
        assertFalse(sentence1.isRmc)
        assertEquals(0L, sentence1.baseDelayMs)

        // Line 2: GNRMC @ 12:35:20.500 -> diff from GPRMC @ 12:35:19.000 = 1500ms
        val sentence2 = parsedList[2]
        assertTrue(sentence2.isRmc)
        assertEquals(1500L, sentence2.baseDelayMs)

        // Line 3: GARMC @ 12:35:22.000 -> diff from GNRMC @ 12:35:20.500 = 1500ms
        val sentence3 = parsedList[3]
        assertTrue(sentence3.isRmc)
        assertEquals(1500L, sentence3.baseDelayMs)
    }

    @Test
    fun testParseLineEnsuresCrlfLineEndings() {
        val rawLineWithoutCrlf = "${'$'}GPRMC,123519,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A"
        val sentence = NmeaParser.parseLine(rawLineWithoutCrlf, 0, null)

        assertTrue(sentence.formattedLine.endsWith("\r\n"))
    }

    @Test
    fun testParseAisDataSentencesAndRawTextLines() {
        val mixedLog = """
            !AIVDM,1,1,,A,13aEO?0P00000000000000000000,0*26
            !AIVDO,1,1,,A,13aEO?0P00000000000000000000,0*27
            ${'$'}GPRMC,123519.00,A,4807.038,N,01131.000,E,022.4,084.4,230324,003.1,W*6A
            CUSTOM_LOG_LINE_DATA
        """.trimIndent()

        val parsedList = NmeaParser.parseString(mixedLog)

        assertEquals(4, parsedList.size)

        // Line 0: AIS !AIVDM
        val sentence0 = parsedList[0]
        assertFalse(sentence0.isRmc)
        assertEquals("!AIVDM,1,1,,A,13aEO?0P00000000000000000000,0*26", sentence0.raw)
        assertTrue(sentence0.formattedLine.endsWith("\r\n"))

        // Line 1: AIS !AIVDO
        val sentence1 = parsedList[1]
        assertFalse(sentence1.isRmc)
        assertEquals("!AIVDO,1,1,,A,13aEO?0P00000000000000000000,0*27", sentence1.raw)
        assertTrue(sentence1.formattedLine.endsWith("\r\n"))

        // Line 2: GPRMC
        val sentence2 = parsedList[2]
        assertTrue(sentence2.isRmc)

        // Line 3: Custom raw line
        val sentence3 = parsedList[3]
        assertFalse(sentence3.isRmc)
        assertEquals("CUSTOM_LOG_LINE_DATA", sentence3.raw)
        assertEquals("CUSTOM_LOG_LINE_DATA\r\n", sentence3.formattedLine)
    }
}
