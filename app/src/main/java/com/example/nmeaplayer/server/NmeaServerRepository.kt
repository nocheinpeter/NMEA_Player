package com.example.nmeaplayer.server

import com.example.nmeaplayer.engine.NmeaPlaybackEngine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Shared repository providing singleton access to [TcpServerManager] and [NmeaPlaybackEngine]
 * across the UI components, ViewModels, and Foreground Service.
 */
object NmeaServerRepository {

    val tcpServerManager: TcpServerManager by lazy { TcpServerManager() }
    val playbackEngine: NmeaPlaybackEngine by lazy { NmeaPlaybackEngine() }

    private val _exitAppEvent = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1)
    val exitAppEvent: SharedFlow<Unit> = _exitAppEvent.asSharedFlow()

    /**
     * Triggers clean application shutdown: stops playback, stops TCP server,
     * and broadcasts exit signal to activity and foreground service.
     */
    fun triggerAppExit() {
        playbackEngine.stop()
        tcpServerManager.stop()
        _exitAppEvent.tryEmit(Unit)
    }
}
