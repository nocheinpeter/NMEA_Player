package de.nocheinpeter.nmeaplayer.parser

import android.content.Context

object SampleNmeaData {

    const val SAMPLE_ASSET_FILE = "sample_data.nmea"

    val FALLBACK_RAW_LOG = """
        ${'$'}GPRMC,123519.00,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A
        ${'$'}GPGGA,123519.00,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47
        ${'$'}GPGSA,A,3,04,05,09,12,14,24,25,29,,,,1.2,0.9,0.8*03
        ${'$'}GPGSV,2,1,08,01,40,083,46,02,17,308,41,04,07,028,32,05,24,144,45*70
        ${'$'}GPRMC,123520.00,A,4807.039,N,01131.001,E,022.5,084.5,230394,003.1,W*6B
        ${'$'}GPGGA,123520.00,4807.039,N,01131.001,E,1,08,0.9,545.5,M,46.9,M,,*46
    """.trimIndent()

    fun loadSampleSentences(context: Context?): List<NmeaSentence> {
        if (context != null) {
            val fromAsset = NmeaParser.parseAsset(context, SAMPLE_ASSET_FILE)
            if (fromAsset.isNotEmpty()) {
                return fromAsset
            }
        }
        return NmeaParser.parseString(FALLBACK_RAW_LOG)
    }
}
