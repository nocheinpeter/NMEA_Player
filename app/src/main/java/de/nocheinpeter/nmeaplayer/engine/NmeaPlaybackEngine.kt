package de.nocheinpeter.nmeaplayer.engine

import de.nocheinpeter.nmeaplayer.parser.NmeaSentence
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/**
 * Core engine managing async playback, speed multiplication, line progress, and seeking for NMEA sentences.
 */
class NmeaPlaybackEngine(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private var playbackJob: Job? = null

    private var sentencesList: List<NmeaSentence> = emptyList()

    private val _playbackState = MutableStateFlow(PlaybackState.STOPPED)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _currentSentence = MutableStateFlow<NmeaSentence?>(null)
    val currentSentence: StateFlow<NmeaSentence?> = _currentSentence.asStateFlow()

    private val _currentTimeString = MutableStateFlow("--:--:--")
    val currentTimeString: StateFlow<String> = _currentTimeString.asStateFlow()
    val currentUtcTime: StateFlow<String> = currentTimeString

    private val _progress = MutableStateFlow(0.0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _speedMultiplier = MutableStateFlow(1.0f)
    val speedMultiplier: StateFlow<Float> = _speedMultiplier.asStateFlow()

    private val _totalSentences = MutableStateFlow(0)
    val totalSentences: StateFlow<Int> = _totalSentences.asStateFlow()

    private val _repeatEnabled = MutableStateFlow(false)
    val repeatEnabled: StateFlow<Boolean> = _repeatEnabled.asStateFlow()

    /**
     * Listener callback invoked whenever an NMEA sentence is emitted/broadcast.
     */
    var onSentenceBroadcast: ((NmeaSentence) -> Unit)? = null

    /**
     * Finds the UTC time formatted string for the sentence at [index].
     * If the sentence at [index] does not have a timestamp, looks up the nearest preceding sentence
     * with a valid timestamp so that the UTC timestamp display updates smoothly while seeking.
     */
    fun findFormattedTimeForIndex(index: Int): String {
        val list = sentencesList
        if (list.isEmpty()) return "--:--:--"
        val targetIndex = index.coerceIn(0, max(0, list.size - 1))

        for (i in targetIndex downTo 0) {
            val time = list[i].timeFormatted
            if (time != null) {
                return time
            }
        }

        for (i in targetIndex + 1 until list.size) {
            val time = list[i].timeFormatted
            if (time != null) {
                return time
            }
        }

        return "--:--:--"
    }

    /**
     * Loads a list of parsed NMEA sentences and resets engine state.
     */
    fun loadSentences(sentences: List<NmeaSentence>) {
        stop()
        sentencesList = sentences
        _totalSentences.value = sentences.size
        _currentIndex.value = 0
        _progress.value = 0.0f
        if (sentences.isNotEmpty()) {
            val first = sentences[0]
            _currentSentence.value = first
            _currentTimeString.value = findFormattedTimeForIndex(0)
        } else {
            _currentSentence.value = null
            _currentTimeString.value = "--:--:--"
        }
    }

    /**
     * Starts or resumes NMEA playback.
     */
    fun play() {
        if (sentencesList.isEmpty()) return

        if (_playbackState.value == PlaybackState.STOPPED && _currentIndex.value >= sentencesList.size) {
            _currentIndex.value = 0
        }

        _playbackState.value = PlaybackState.PLAYING
        startPlaybackLoop()
    }

    /**
     * Pauses playback at the current line position.
     */
    fun pause() {
        _playbackState.value = PlaybackState.PAUSED
    }

    /**
     * Stops playback and resets line index to 0.
     */
    fun stop() {
        _playbackState.value = PlaybackState.STOPPED
        playbackJob?.cancel()
        playbackJob = null
        _currentIndex.value = 0
        _progress.value = 0.0f
        if (sentencesList.isNotEmpty()) {
            val first = sentencesList[0]
            _currentSentence.value = first
            _currentTimeString.value = findFormattedTimeForIndex(0)
        }
    }

    /**
     * Seeks to a specific sentence index in the log.
     */
    fun seekTo(index: Int) {
        val list = sentencesList
        if (list.isEmpty()) return
        val targetIndex = index.coerceIn(0, max(0, list.size - 1))
        _currentIndex.value = targetIndex
        val sentence = list.getOrNull(targetIndex) ?: return
        _currentSentence.value = sentence
        _currentTimeString.value = findFormattedTimeForIndex(targetIndex)
        _progress.value = if (list.size > 1) {
            targetIndex.toFloat() / (list.size - 1)
        } else 1.0f
    }

    /**
     * Seeks to a specific line/sentence index in the log (alias for [seekTo]).
     */
    fun seekToLine(lineIndex: Int) = seekTo(lineIndex)

    /**
     * Seeks playback to a fractional progress (0.0f to 1.0f).
     */
    fun seekToProgress(fraction: Float) {
        val list = sentencesList
        if (list.isEmpty()) return
        val clampedFraction = fraction.coerceIn(0.0f, 1.0f)
        val targetIndex = (clampedFraction * (list.size - 1)).toInt()
        seekTo(targetIndex)
    }

    /**
     * Updates the playback speed factor multiplier (e.g. 0.5x, 1.0x, 2.0x, 5.0x, 10.0x).
     */
    fun setSpeedMultiplier(speed: Float) {
        _speedMultiplier.value = speed.coerceIn(0.1f, 100.0f)
    }

    /**
     * Toggles automatic playback looping.
     */
    fun setRepeatEnabled(enabled: Boolean) {
        _repeatEnabled.value = enabled
    }

    private fun startPlaybackLoop() {
        if (playbackJob?.isActive == true) return

        playbackJob = scope.launch {
            while (_playbackState.value == PlaybackState.PLAYING) {
                val list = sentencesList
                val index = _currentIndex.value
                if (list.isEmpty() || index >= list.size) break

                val sentence = list.getOrNull(index) ?: break

                _currentSentence.value = sentence
                _currentTimeString.value = findFormattedTimeForIndex(index)
                _progress.value = if (list.size > 1) {
                    index.toFloat() / (list.size - 1)
                } else 1.0f

                // Emit sentence to TCP server or listener
                onSentenceBroadcast?.invoke(sentence)

                // Compute speed-adjusted delay
                val baseDelay = min(a = sentence.baseDelayMs, b = 2000)
                val currentSpeed = _speedMultiplier.value.coerceAtLeast(0.1f)
                val adjustedDelayMs = (baseDelay / currentSpeed).toLong()

                if (adjustedDelayMs > 0) {
                    var remaining = adjustedDelayMs
                    val chunkSize = 50L
                    while (remaining > 0 && _playbackState.value == PlaybackState.PLAYING) {
                        val sleepTime = min(remaining, chunkSize)
                        delay(sleepTime)
                        remaining -= sleepTime
                    }
                }

                if (_playbackState.value == PlaybackState.PLAYING) {
                    val nextIndex = index + 1
                    val currentListSize = sentencesList.size
                    if (nextIndex < currentListSize) {
                        _currentIndex.value = nextIndex
                    } else if (_repeatEnabled.value && currentListSize > 0) {
                        _currentIndex.value = 0
                    } else {
                        _playbackState.value = PlaybackState.STOPPED
                        _currentIndex.value = 0
                        break
                    }
                }
            }
        }
    }

    /**
     * Cancels background playback jobs and releases resources.
     */
    fun shutdown() {
        stop()
        sentencesList = emptyList()
        onSentenceBroadcast = null
    }
}
