package com.example.nmeaplayer.viewmodel

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nmeaplayer.engine.NmeaPlaybackEngine
import com.example.nmeaplayer.engine.PlaybackState
import com.example.nmeaplayer.parser.NmeaParser
import com.example.nmeaplayer.parser.NmeaSentence
import com.example.nmeaplayer.parser.SampleNmeaData
import com.example.nmeaplayer.server.ClientInfo
import com.example.nmeaplayer.server.NmeaServerRepository
import com.example.nmeaplayer.server.ServerState
import com.example.nmeaplayer.server.TcpServerManager
import com.example.nmeaplayer.service.NmeaServerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    val tcpServerManager: TcpServerManager = NmeaServerRepository.tcpServerManager,
    val playbackEngine: NmeaPlaybackEngine = NmeaServerRepository.playbackEngine,
) : ViewModel() {

    val serverState: StateFlow<ServerState> = tcpServerManager.serverState
    val serverPort: StateFlow<Int> = tcpServerManager.port
    val deviceIp: StateFlow<String> = tcpServerManager.deviceIpAddress
    val activeClientCount: StateFlow<Int> = tcpServerManager.activeClientCount
    val connectedClients: StateFlow<List<ClientInfo>> = tcpServerManager.connectedClients

    val playbackState: StateFlow<PlaybackState> = playbackEngine.playbackState
    val currentSentence: StateFlow<NmeaSentence?> = playbackEngine.currentSentence
    val currentTimeString: StateFlow<String> = playbackEngine.currentTimeString
    val progress: StateFlow<Float> = playbackEngine.progress
    val currentIndex: StateFlow<Int> = playbackEngine.currentIndex
    val totalSentences: StateFlow<Int> = playbackEngine.totalSentences
    val speedMultiplier: StateFlow<Float> = playbackEngine.speedMultiplier
    val repeatEnabled: StateFlow<Boolean> = playbackEngine.repeatEnabled

    private val _fileName = MutableStateFlow("Sample GPS Log")
    val fileName: StateFlow<String> = _fileName.asStateFlow()

    private val _recentLogs = MutableStateFlow<List<String>>(emptyList())
    val recentLogs: StateFlow<List<String>> = _recentLogs.asStateFlow()

    private val _portInputText = MutableStateFlow("10110")
    val portInputText: StateFlow<String> = _portInputText.asStateFlow()

    private val _portError = MutableStateFlow<String?>(null)
    val portError: StateFlow<String?> = _portError.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        _portInputText.value = tcpServerManager.port.value.toString()

        // Broadcast streamed sentence to TCP clients and add to live UI log
        playbackEngine.onSentenceBroadcast = { sentence ->
            tcpServerManager.broadcast(sentence.formattedLine)
            val line = sentence.raw.trimEnd('\r', '\n')
            _recentLogs.update { current ->
                val updated = current.toMutableList()
                if (updated.size >= 100) {
                    updated.removeAt(0)
                }
                updated.add(line)
                updated
            }
        }

        // Load default sample sentences
        loadSampleData()
    }

    fun onPortInputChanged(input: String) {
        _portInputText.value = input
        val parsedPort = input.toIntOrNull()
        if (parsedPort == null || (parsedPort !in 1..65535)) {
            _portError.value = "Port must be a number between 1 and 65535"
        } else {
            _portError.value = null
        }
    }

    fun loadSampleData() {
        val sentences = NmeaParser.parseString(SampleNmeaData.SAMPLE_LOG)
        _fileName.value = "Sample GPS Track"
        _recentLogs.value = emptyList()
        playbackEngine.loadSentences(sentences)
        _statusMessage.value = "Loaded ${sentences.size} sample NMEA sentences"
    }

    fun loadUri(context: Context, uri: Uri, name: String?) {
        viewModelScope.launch {
            try {
                try {
                    val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (_: Exception) {}

                val sentences = withContext(Dispatchers.IO) {
                    NmeaParser.parseUri(context, uri)
                }

                if (sentences.isEmpty()) {
                    _fileName.value = name ?: "Unknown Log (0 lines)"
                    _statusMessage.value = "No valid NMEA sentences found in file"
                } else {
                    _fileName.value = name ?: "Loaded Log (${sentences.size} lines)"
                    _recentLogs.value = emptyList()
                    playbackEngine.loadSentences(sentences)
                    _statusMessage.value = "Successfully loaded ${sentences.size} sentences from ${name ?: "file"}"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error reading file: ${e.localizedMessage}"
            }
        }
    }

    fun play(context: Context? = null) {
        playbackEngine.play()
        context?.let { NmeaServerService.startService(it) }
    }

    fun pause() = playbackEngine.pause()

    fun resume(context: Context? = null) {
        playbackEngine.play()
        context?.let { NmeaServerService.startService(it) }
    }

    fun stop() = playbackEngine.stop()

    fun seekTo(index: Int) = playbackEngine.seekTo(index)

    fun seekToProgress(fraction: Float) = playbackEngine.seekToProgress(fraction)

    fun setSpeedMultiplier(speed: Float) = playbackEngine.setSpeedMultiplier(speed)

    fun setRepeatEnabled(enabled: Boolean) = playbackEngine.setRepeatEnabled(enabled)

    fun startServer(context: Context? = null) {
        val port = _portInputText.value.toIntOrNull()
        if (port != null && port in 1..65535) {
            tcpServerManager.start(port)
            _statusMessage.value = "TCP Server started on port $port"
            context?.let { NmeaServerService.startService(it) }
        } else {
            _portError.value = "Port must be a number between 1 and 65535"
        }
    }

    fun startServer(port: Int, context: Context? = null) {
        _portInputText.value = port.toString()
        _portError.value = null
        tcpServerManager.start(port)
        _statusMessage.value = "TCP Server started on port $port"
        context?.let { NmeaServerService.startService(it) }
    }

    fun stopServer(context: Context? = null) {
        tcpServerManager.stop()
        _statusMessage.value = "TCP Server stopped"
        context?.let { NmeaServerService.stopService(it) }
    }

    fun exitApp(context: Context? = null, activity: Activity? = null) {
        context?.let { NmeaServerService.stopService(it) }
        NmeaServerRepository.triggerAppExit()
        activity?.finishAndRemoveTask()
    }

    fun refreshIp() {
        tcpServerManager.refreshIpAddress()
    }

    fun clearRecentLogs() {
        _recentLogs.value = emptyList()
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    override fun onCleared() {
        playbackEngine.shutdown()
        tcpServerManager.stop()
    }
}
