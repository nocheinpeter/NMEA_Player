package de.nocheinpeter.nmeaplayer.parser

/**
 * Data class representing a parsed NMEA sentence line.
 */
data class NmeaSentence(
    val id: Int,
    val raw: String,
    val formattedLine: String,
    val isRmc: Boolean,
    val timestampEpochMs: Long?,
    val timeFormatted: String?,
    val baseDelayMs: Long
)
