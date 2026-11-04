package de.nocheinpeter.nmeaplayer.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.nocheinpeter.nmeaplayer.engine.NmeaPlaybackEngine
import de.nocheinpeter.nmeaplayer.engine.PlaybackState
import de.nocheinpeter.nmeaplayer.parser.NmeaParser
import de.nocheinpeter.nmeaplayer.parser.NmeaSentence
import de.nocheinpeter.nmeaplayer.parser.SampleNmeaData
import de.nocheinpeter.nmeaplayer.server.ClientInfo
import de.nocheinpeter.nmeaplayer.server.NmeaServerRepository
import de.nocheinpeter.nmeaplayer.server.ServerState
import de.nocheinpeter.nmeaplayer.server.TcpServerManager
import de.nocheinpeter.nmeaplayer.service.NmeaServerService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainViewModel(
    val tcpServerManager: TcpServerManager = NmeaServerRepository.tcpServerManager,
    val playbackEngine: NmeaPlaybackEngine = NmeaServerRepository.playbackEngine,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    companion object {
        const val PREFS_NAME = "nmea_player_prefs"
        const val KEY_PORT = "tcp_port"
        const val KEY_URI = "saved_nmea_uri"
        const val KEY_FILE_NAME = "saved_file_name"
        const val KEY_LAST_DIRECTORY_URI = "key_last_directory_uri"
        const val DEFAULT_PORT = 10110
    }

    val serverState: StateFlow<ServerState> = tcpServerManager.serverState
    val serverPort: StateFlow<Int> = tcpServerManager.port
    val deviceIp: StateFlow<String> = tcpServerManager.deviceIpAddress
    val activeClientCount: StateFlow<Int> = tcpServerManager.activeClientCount
    val connectedClients: StateFlow<List<ClientInfo>> = tcpServerManager.connectedClients

    val playbackState: StateFlow<PlaybackState> = playbackEngine.playbackState
    val currentSentence: StateFlow<NmeaSentence?> = playbackEngine.currentSentence
    val currentTimeString: StateFlow<String> = playbackEngine.currentTimeString
    val currentUtcTime: StateFlow<String> = playbackEngine.currentUtcTime
    val progress: StateFlow<Float> = playbackEngine.progress
    val currentIndex: StateFlow<Int> = playbackEngine.currentIndex
    val totalSentences: StateFlow<Int> = playbackEngine.totalSentences
    val speedMultiplier: StateFlow<Float> = playbackEngine.speedMultiplier
    val repeatEnabled: StateFlow<Boolean> = playbackEngine.repeatEnabled

    private val _isFileLoading = MutableStateFlow(false)
    val isFileLoading: StateFlow<Boolean> = _isFileLoading.asStateFlow()

    private val _fileLoadingProgress = MutableStateFlow<Float?>(null)
    val fileLoadingProgress: StateFlow<Float?> = _fileLoadingProgress.asStateFlow()

    private val _fileLoadingMessage = MutableStateFlow("0 Zeilen verarbeitet...")
    val fileLoadingMessage: StateFlow<String> = _fileLoadingMessage.asStateFlow()

    private val _fileName = MutableStateFlow("Sample GPS Track")
    val fileName: StateFlow<String> = _fileName.asStateFlow()

    private val _recentLogs = MutableStateFlow<List<String>>(emptyList())
    val recentLogs: StateFlow<List<String>> = _recentLogs.asStateFlow()

    private val _portInputText = MutableStateFlow(DEFAULT_PORT.toString())
    val portInputText: StateFlow<String> = _portInputText.asStateFlow()

    private val _portError = MutableStateFlow<String?>(null)
    val portError: StateFlow<String?> = _portError.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        _portInputText.value = tcpServerManager.port.value.toString()

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

        val initialSentences = SampleNmeaData.loadSampleSentences(null)
        playbackEngine.loadSentences(initialSentences)
    }

    fun loadSavedPort(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedPort = prefs.getInt(KEY_PORT, DEFAULT_PORT)
            if (savedPort in 1..65535) {
                _portInputText.value = savedPort.toString()
                _portError.value = null
                tcpServerManager.setPort(savedPort)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun savePort(context: Context, port: Int) {
        try {
            if (port in 1..65535) {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putInt(KEY_PORT, port).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveUri(context: Context, uri: Uri, name: String?) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_URI, uri.toString())
                .putString(KEY_FILE_NAME, name)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearSavedUri(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .remove(KEY_URI)
                .remove(KEY_FILE_NAME)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getDirectoryUri(context: Context, uri: Uri): Uri {
        try {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val authority = uri.authority
                if (docId != null && authority != null) {
                    val lastSlash = docId.lastIndexOf('/')
                    if (lastSlash != -1) {
                        val parentDocId = docId.substring(0, lastSlash)
                        return DocumentsContract.buildDocumentUri(authority, parentDocId)
                    } else {
                        val colonIndex = docId.indexOf(':')
                        if (colonIndex != -1) {
                            val parentDocId = docId.substring(0, colonIndex + 1)
                            return DocumentsContract.buildDocumentUri(authority, parentDocId)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return uri
    }

    fun saveLastDirectoryUri(context: Context, uri: Uri) {
        try {
            val dirUri = getDirectoryUri(context, uri)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_LAST_DIRECTORY_URI, dirUri.toString())
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLastDirectoryUri(context: Context): Uri? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val uriString = prefs.getString(KEY_LAST_DIRECTORY_URI, null)
            if (!uriString.isNullOrEmpty()) {
                Uri.parse(uriString)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun loadSavedData(context: Context) {
        loadSampleData(context)
    }

    fun onPortInputChanged(input: String, context: Context? = null) {
        try {
            _portInputText.value = input
            val parsedPort = input.toIntOrNull()
            if (parsedPort == null || (parsedPort !in 1..65535)) {
                _portError.value = "Port must be a number between 1 and 65535"
            } else {
                _portError.value = null
                tcpServerManager.setPort(parsedPort)
                context?.let { savePort(it, parsedPort) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _portError.value = "Port must be a number between 1 and 65535"
        }
    }

    fun loadSampleData(context: Context? = null) {
        context?.let { clearSavedUri(it) }
        viewModelScope.launch {
            _isFileLoading.value = true
            _fileLoadingProgress.value = null
            _fileLoadingMessage.value = "0 Zeilen verarbeitet..."
            try {
                val sentences = withContext(ioDispatcher) {
                    SampleNmeaData.loadSampleSentences(context) { linesProcessed, totalLines ->
                        val formatted = String.format(Locale.GERMAN, "%,d", linesProcessed)
                        _fileLoadingMessage.value = "$formatted Zeilen verarbeitet..."
                        _fileLoadingProgress.value = if (totalLines != null && totalLines > 0) {
                            linesProcessed.toFloat() / totalLines
                        } else null
                    }
                }
                _fileName.value = if (context != null) "sample_data.nmea" else "Sample GPS Log"
                _recentLogs.value = emptyList()
                playbackEngine.loadSentences(sentences)
                _statusMessage.value = "Loaded ${sentences.size} sample NMEA sentences"
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.value = "Error loading sample data: ${e.localizedMessage}"
            } finally {
                _isFileLoading.value = false
            }
        }
    }

    fun loadUri(context: Context, uri: Uri, name: String?) {
        viewModelScope.launch {
            _isFileLoading.value = true
            _fileLoadingProgress.value = null
            _fileLoadingMessage.value = "0 Zeilen verarbeitet..."
            try {
                try {
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val sentences = withContext(ioDispatcher) {
                    NmeaParser.parseUri(context, uri) { linesProcessed, totalLines ->
                        val formatted = String.format(Locale.GERMAN, "%,d", linesProcessed)
                        _fileLoadingMessage.value = "$formatted Zeilen verarbeitet..."
                        _fileLoadingProgress.value = if (totalLines != null && totalLines > 0) {
                            linesProcessed.toFloat() / totalLines
                        } else null
                    }
                }

                if (sentences.isEmpty()) {
                    _fileName.value = name ?: "Unknown Log (0 lines)"
                    _statusMessage.value = "No valid NMEA sentences found in file"
                } else {
                    _fileName.value = name ?: "Loaded Log (${sentences.size} lines)"
                    _recentLogs.value = emptyList()
                    playbackEngine.loadSentences(sentences)
                    _statusMessage.value = "Successfully loaded ${sentences.size} sentences from ${name ?: "file"}"
                    saveUri(context, uri, name)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.value = "Error reading file: ${e.localizedMessage}"
            } finally {
                _isFileLoading.value = false
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

    fun seekToLine(line: Int) = playbackEngine.seekToLine(line)

    fun seekToProgress(fraction: Float) = playbackEngine.seekToProgress(fraction)

    fun setSpeedMultiplier(speed: Float) = playbackEngine.setSpeedMultiplier(speed)

    fun setRepeatEnabled(enabled: Boolean) = playbackEngine.setRepeatEnabled(enabled)

    fun startServer(context: Context? = null) {
        try {
            val port = _portInputText.value.toIntOrNull()
            if (port != null && port in 1..65535) {
                tcpServerManager.start(port)
                context?.let { savePort(it, port) }
                _statusMessage.value = "TCP Server started on port $port"
                context?.let { NmeaServerService.startService(it) }
            } else {
                _portError.value = "Port must be a number between 1 and 65535"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _statusMessage.value = "Failed to start server: ${e.localizedMessage}"
        }
    }

    fun startServer(port: Int, context: Context? = null) {
        try {
            _portInputText.value = port.toString()
            _portError.value = null
            tcpServerManager.start(port)
            context?.let { savePort(it, port) }
            _statusMessage.value = "TCP Server started on port $port"
            context?.let { NmeaServerService.startService(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            _statusMessage.value = "Failed to start server: ${e.localizedMessage}"
        }
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
