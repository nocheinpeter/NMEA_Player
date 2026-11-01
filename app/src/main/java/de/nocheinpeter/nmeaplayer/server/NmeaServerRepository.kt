package de.nocheinpeter.nmeaplayer.server

import de.nocheinpeter.nmeaplayer.engine.NmeaPlaybackEngine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Global singleton repository providing shared engine and TCP server instances
 * across Activity, Service, and ViewModel components.
 */
object NmeaServerRepository {

    val tcpServerManager = TcpServerManager()
    val playbackEngine = NmeaPlaybackEngine()

    private val _exitAppEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val exitAppEvent: SharedFlow<Unit> = _exitAppEvent.asSharedFlow()

    fun triggerAppExit() {
        playbackEngine.stop()
        tcpServerManager.stop()
        _exitAppEvent.tryEmit(Unit)
    }
}
