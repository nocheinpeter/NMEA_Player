package com.example.nmeaplayer.parser

/**
 * Represents a parsed NMEA sentence with timing and metadata.
 *
 * @property id 0-based index of sentence in the log file.
 * @property raw Raw NMEA sentence string (e.g., "$GPRMC,...").
 * @property formattedLine Line formatted with proper CRLF line ending for TCP transmission.
 * @property isRmc True if sentence is an RMC variant (GPRMC, GNRMC, GARMC, etc.).
 * @property timestampEpochMs Parsed UTC epoch timestamp in milliseconds, or null if unparseable/non-RMC.
 * @property timeFormatted Formatted UTC time string (e.g. "12:35:19.000"), or null if unavailable.
 * @property baseDelayMs Relative time delay in milliseconds from the preceding RMC sentence (0 for non-RMC).
 */
data class NmeaSentence(
    val id: Int,
    val raw: String,
    val formattedLine: String,
    val isRmc: Boolean,
    val timestampEpochMs: Long?,
    val timeFormatted: String?,
    val baseDelayMs: Long = 0L
)
