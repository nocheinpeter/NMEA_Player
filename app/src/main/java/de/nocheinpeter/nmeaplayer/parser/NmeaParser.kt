package de.nocheinpeter.nmeaplayer.parser

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Parser for NMEA log streams and files.
 * Extracts timestamps from RMC sentences (GPRMC, GNRMC, GARMC, etc.)
 * and calculates relative time delays between sentences.
 */
object NmeaParser {

    /**
     * Parses NMEA sentences from an [InputStream].
     */
    fun parseInputStream(
        inputStream: InputStream,
        onProgress: ((linesProcessed: Int, totalLines: Int?) -> Unit)? = null
    ): List<NmeaSentence> {
        return try {
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            parseReader(reader, onProgress = onProgress)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Parses NMEA sentences from an Android [Uri].
     */
    fun parseUri(
        context: Context,
        uri: Uri,
        onProgress: ((linesProcessed: Int, totalLines: Int?) -> Unit)? = null
    ): List<NmeaSentence> {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                parseInputStream(inputStream, onProgress = onProgress)
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Parses NMEA sentences from an Android asset file.
     */
    fun parseAsset(
        context: Context,
        assetFileName: String,
        onProgress: ((linesProcessed: Int, totalLines: Int?) -> Unit)? = null
    ): List<NmeaSentence> {
        return try {
            context.assets.open(assetFileName).use { inputStream ->
                parseInputStream(inputStream, onProgress = onProgress)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Parses NMEA sentences from a [String] containing raw log content.
     */
    fun parseString(
        content: String,
        onProgress: ((linesProcessed: Int, totalLines: Int?) -> Unit)? = null
    ): List<NmeaSentence> {
        val reader = BufferedReader(content.reader())
        return parseReader(reader, onProgress = onProgress)
    }

    /**
     * Parses NMEA sentences from a [BufferedReader].
     */
    fun parseReader(
        reader: BufferedReader,
        totalLines: Int? = null,
        onProgress: ((linesProcessed: Int, totalLines: Int?) -> Unit)? = null
    ): List<NmeaSentence> {
        val sentences = mutableListOf<NmeaSentence>()
        var lineIndex = 0
        var previousRmcEpochMs: Long? = null

        onProgress?.invoke(0, totalLines)

        reader.forEachLine { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isNotBlank()) {
                val parsed = parseLine(trimmed, lineIndex, previousRmcEpochMs)
                if (parsed.isRmc && parsed.timestampEpochMs != null) {
                    previousRmcEpochMs = parsed.timestampEpochMs
                }
                sentences.add(parsed)
                lineIndex++
                if (lineIndex % 500 == 0 || (totalLines != null && lineIndex == totalLines)) {
                    onProgress?.invoke(lineIndex, totalLines)
                }
            }
        }
        onProgress?.invoke(lineIndex, totalLines)
        return sentences
    }

    /**
     * Parses a single NMEA sentence line.
     */
    fun parseLine(
        rawLine: String,
        index: Int,
        previousRmcEpochMs: Long?
    ): NmeaSentence {
        val cleanLine = if (rawLine.endsWith("\r\n")) {
            rawLine
        } else if (rawLine.endsWith("\r") || rawLine.endsWith("\n")) {
            rawLine.trimEnd('\r', '\n') + "\r\n"
        } else {
            "$rawLine\r\n"
        }

        val lineWithoutChecksum = rawLine.takeWhile { it != '*' }
        val tokens = lineWithoutChecksum.split(",")

        if (tokens.isEmpty()) {
            return NmeaSentence(
                id = index,
                raw = rawLine,
                formattedLine = cleanLine,
                isRmc = false,
                timestampEpochMs = null,
                timeFormatted = null,
                baseDelayMs = 0L
            )
        }

        val header = tokens[0].removePrefix("$").removePrefix("!").uppercase(Locale.ROOT)
        val isRmc = header.endsWith("RMC")

        if (!isRmc) {
            return NmeaSentence(
                id = index,
                raw = rawLine,
                formattedLine = cleanLine,
                isRmc = false,
                timestampEpochMs = null,
                timeFormatted = null,
                baseDelayMs = 0L
            )
        }

        val timeToken = tokens.getOrNull(1)
        val dateToken = tokens.getOrNull(9)

        val timestampMs = parseRmcTimestamp(timeToken, dateToken)
        val formattedTimeStr = timestampMs?.let { formatTime(it) }

        var delayMs = 0L
        if (timestampMs != null && previousRmcEpochMs != null) {
            val diff = timestampMs - previousRmcEpochMs
            if (diff >= 0) {
                delayMs = diff
            } else {
                val dayAdjusted = diff + 86_400_000L
                delayMs = if (dayAdjusted in 0..86_400_000L) dayAdjusted else 0L
            }
        }

        return NmeaSentence(
            id = index,
            raw = rawLine,
            formattedLine = cleanLine,
            isRmc = true,
            timestampEpochMs = timestampMs,
            timeFormatted = formattedTimeStr,
            baseDelayMs = delayMs
        )
    }

    /**
     * Parses UTC time (hhmmss.ss) at index 1 and date (ddmmyy) at index 9 into epoch milliseconds.
     */
    fun parseRmcTimestamp(timeStr: String?, dateStr: String?): Long? {
        if (timeStr.isNullOrBlank() || timeStr.length < 6) return null

        return try {
            val hours = timeStr.substring(0, 2).toInt()
            val minutes = timeStr.substring(2, 4).toInt()
            val seconds = timeStr.substring(4, 6).toInt()

            var millis = 0
            val dotIndex = timeStr.indexOf('.')
            if (dotIndex != -1 && dotIndex < timeStr.length - 1) {
                val fracStr = timeStr.substring(dotIndex + 1)
                millis = when (fracStr.length) {
                    1 -> fracStr.toInt() * 100
                    2 -> fracStr.toInt() * 10
                    3 -> fracStr.toInt()
                    else -> fracStr.substring(0, 3).toInt()
                }
            }

            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            if (!dateStr.isNullOrBlank() && dateStr.length >= 6) {
                val day = dateStr.substring(0, 2).toInt()
                val month = dateStr.substring(2, 4).toInt() - 1
                val yy = dateStr.substring(4, 6).toInt()
                val year = if (yy < 80) 2000 + yy else 1900 + yy
                cal.set(year, month, day, hours, minutes, seconds)
            } else {
                cal.set(2000, Calendar.JANUARY, 1, hours, minutes, seconds)
            }
            cal.set(Calendar.MILLISECOND, millis)

            cal.timeInMillis
        } catch (_: Exception) {
            null
        }
    }

    private fun formatTime(epochMs: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(epochMs))
    }
}
