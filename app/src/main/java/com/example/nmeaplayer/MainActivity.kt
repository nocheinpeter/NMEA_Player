package com.example.nmeaplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.nmeaplayer.server.NmeaServerRepository
import com.example.nmeaplayer.service.NmeaServerService
import com.example.nmeaplayer.ui.NmeaPlayerScreen
import com.example.nmeaplayer.ui.theme.NMEAPlayerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleExitIntent(intent)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                NmeaServerRepository.exitAppEvent.collect {
                    finishAndRemoveTask()
                }
            }
        }

        setContent {
            NMEAPlayerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NmeaPlayerScreen()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleExitIntent(intent)
    }

    private fun handleExitIntent(intent: Intent?) {
        if (intent?.action == NmeaServerService.ACTION_EXIT) {
            NmeaServerRepository.triggerAppExit()
            finishAndRemoveTask()
        }
    }
}
