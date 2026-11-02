package de.nocheinpeter.nmeaplayer.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.nocheinpeter.nmeaplayer.R
import de.nocheinpeter.nmeaplayer.engine.PlaybackState
import de.nocheinpeter.nmeaplayer.server.ClientInfo
import de.nocheinpeter.nmeaplayer.server.ServerState
import de.nocheinpeter.nmeaplayer.ui.theme.NMEAPlayerTheme
import de.nocheinpeter.nmeaplayer.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NmeaPlayerScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel(),
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val snackbarHostState = remember { SnackbarHostState() }

    val serverState by viewModel.serverState.collectAsStateWithLifecycle()
    val serverPort by viewModel.serverPort.collectAsStateWithLifecycle()
    val deviceIp by viewModel.deviceIp.collectAsStateWithLifecycle()
    val activeClients by viewModel.activeClientCount.collectAsStateWithLifecycle()
    val connectedClients by viewModel.connectedClients.collectAsStateWithLifecycle()

    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val currentTimeString by viewModel.currentTimeString.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentIndex.collectAsStateWithLifecycle()
    val totalSentences by viewModel.totalSentences.collectAsStateWithLifecycle()
    val speedMultiplier by viewModel.speedMultiplier.collectAsStateWithLifecycle()
    val repeatEnabled by viewModel.repeatEnabled.collectAsStateWithLifecycle()

    val isFileLoading by viewModel.isFileLoading.collectAsStateWithLifecycle()
    val fileLoadingProgress by viewModel.fileLoadingProgress.collectAsStateWithLifecycle()
    val fileLoadingMessage by viewModel.fileLoadingMessage.collectAsStateWithLifecycle()

    val fileName by viewModel.fileName.collectAsStateWithLifecycle()
    val recentLogs by viewModel.recentLogs.collectAsStateWithLifecycle()
    val portInputText by viewModel.portInputText.collectAsStateWithLifecycle()
    val portError by viewModel.portError.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    if (isFileLoading) {
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = {
                Text(
                    text = "NMEA-Datei wird eingelesen...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val currentProgress = fileLoadingProgress
                    if (currentProgress != null) {
                        LinearProgressIndicator(
                            progress = { currentProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        text = fileLoadingMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {}
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            var displayName: String? = null
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if ((nameIndex != -1) && cursor.moveToFirst()) {
                        displayName = cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            viewModel.loadUri(context, uri, displayName)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadSavedPort(context)
        viewModel.loadSavedData(context)
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 12.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.app_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshIp() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh_ip),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Button(
                        onClick = { viewModel.exitApp(context, activity) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = stringResource(R.string.exit_app)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.exit_app_button))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isTabletWide = maxWidth >= 600.dp

            if (isTabletWide) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ServerConfigCard(
                            serverState = serverState,
                            portInputText = portInputText,
                            portError = portError,
                            onPortInputChanged = { input -> viewModel.onPortInputChanged(input, context) },
                            onStartServer = {
                                checkAndRequestNotificationPermission()
                                viewModel.startServer(context)
                            },
                            onStopServer = { viewModel.stopServer(context) },
                            onRefreshIp = viewModel::refreshIp
                        )

                        FileSelectionCard(
                            fileName = fileName,
                            totalSentences = totalSentences,
                            onOpenFile = {
                                filePickerLauncher.launch(
                                    arrayOf("*/*", "text/plain", "text/*")
                                )
                            },
                            onLoadSample = { viewModel.loadSampleData(context) }
                        )

                        PlaybackControlsCard(
                            playbackState = playbackState,
                            currentTimeString = currentTimeString,
                            currentIndex = currentIndex,
                            totalSentences = totalSentences,
                            progress = progress,
                            speedMultiplier = speedMultiplier,
                            repeatEnabled = repeatEnabled,
                            onPlay = {
                                checkAndRequestNotificationPermission()
                                viewModel.play(context)
                            },
                            onPause = viewModel::pause,
                            onResume = {
                                checkAndRequestNotificationPermission()
                                viewModel.resume(context)
                            },
                            onStop = viewModel::stop,
                            onSeekProgress = viewModel::seekToProgress,
                            onSetSpeed = viewModel::setSpeedMultiplier,
                            onToggleRepeat = { viewModel.setRepeatEnabled(!repeatEnabled) }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatusMonitorCard(
                            serverState = serverState,
                            playbackState = playbackState,
                            deviceIp = deviceIp,
                            serverPort = serverPort,
                            activeClients = activeClients,
                            connectedClients = connectedClients
                        )

                        LiveConsoleCard(
                            recentLogs = recentLogs,
                            onClearLogs = viewModel::clearRecentLogs,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        StatusMonitorCard(
                            serverState = serverState,
                            playbackState = playbackState,
                            deviceIp = deviceIp,
                            serverPort = serverPort,
                            activeClients = activeClients,
                            connectedClients = connectedClients
                        )
                    }

                    item {
                        ServerConfigCard(
                            serverState = serverState,
                            portInputText = portInputText,
                            portError = portError,
                            onPortInputChanged = { input -> viewModel.onPortInputChanged(input, context) },
                            onStartServer = {
                                checkAndRequestNotificationPermission()
                                viewModel.startServer(context)
                            },
                            onStopServer = { viewModel.stopServer(context) },
                            onRefreshIp = viewModel::refreshIp
                        )
                    }

                    item {
                        FileSelectionCard(
                            fileName = fileName,
                            totalSentences = totalSentences,
                            onOpenFile = {
                                filePickerLauncher.launch(
                                    arrayOf("*/*", "text/plain", "text/*")
                                )
                            },
                            onLoadSample = { viewModel.loadSampleData(context) }
                        )
                    }

                    item {
                        PlaybackControlsCard(
                            playbackState = playbackState,
                            currentTimeString = currentTimeString,
                            currentIndex = currentIndex,
                            totalSentences = totalSentences,
                            progress = progress,
                            speedMultiplier = speedMultiplier,
                            repeatEnabled = repeatEnabled,
                            onPlay = {
                                checkAndRequestNotificationPermission()
                                viewModel.play(context)
                            },
                            onPause = viewModel::pause,
                            onResume = {
                                checkAndRequestNotificationPermission()
                                viewModel.resume(context)
                            },
                            onStop = viewModel::stop,
                            onSeekProgress = viewModel::seekToProgress,
                            onSetSpeed = viewModel::setSpeedMultiplier,
                            onToggleRepeat = { viewModel.setRepeatEnabled(!repeatEnabled) }
                        )
                    }

                    item {
                        LiveConsoleCard(
                            recentLogs = recentLogs,
                            onClearLogs = viewModel::clearRecentLogs,
                            modifier = Modifier.height(240.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusMonitorCard(
    serverState: ServerState,
    playbackState: PlaybackState,
    deviceIp: String,
    serverPort: Int,
    activeClients: Int,
    connectedClients: List<ClientInfo>,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Real-Time Server Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(
                        label = when (serverState) {
                            ServerState.RUNNING -> "Server: Running"
                            ServerState.STOPPED -> "Server: Stopped"
                            ServerState.STARTING -> "Server: Starting..."
                            ServerState.ERROR -> "Server: Error"
                        },
                        color = when (serverState) {
                            ServerState.RUNNING -> MaterialTheme.colorScheme.primaryContainer
                            ServerState.STOPPED -> MaterialTheme.colorScheme.surfaceContainer
                            ServerState.STARTING -> MaterialTheme.colorScheme.tertiaryContainer
                            ServerState.ERROR -> MaterialTheme.colorScheme.errorContainer
                        },
                        textColor = when (serverState) {
                            ServerState.RUNNING -> MaterialTheme.colorScheme.onPrimaryContainer
                            ServerState.STOPPED -> MaterialTheme.colorScheme.onSurfaceVariant
                            ServerState.STARTING -> MaterialTheme.colorScheme.onTertiaryContainer
                            ServerState.ERROR -> MaterialTheme.colorScheme.onErrorContainer
                        }
                    )

                    StatusBadge(
                        label = when (playbackState) {
                            PlaybackState.PLAYING -> "Streaming"
                            PlaybackState.PAUSED -> "Paused"
                            PlaybackState.STOPPED -> "Idle"
                        },
                        color = when (playbackState) {
                            PlaybackState.PLAYING -> MaterialTheme.colorScheme.tertiaryContainer
                            PlaybackState.PAUSED -> MaterialTheme.colorScheme.secondaryContainer
                            PlaybackState.STOPPED -> MaterialTheme.colorScheme.surfaceContainer
                        },
                        textColor = when (playbackState) {
                            PlaybackState.PLAYING -> MaterialTheme.colorScheme.onTertiaryContainer
                            PlaybackState.PAUSED -> MaterialTheme.colorScheme.onSecondaryContainer
                            PlaybackState.STOPPED -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Endpoint Address:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "$deviceIp:$serverPort",
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Connected Clients:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp),
                                tint = if (activeClients > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "$activeClients active",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (activeClients > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    AnimatedVisibility(visible = connectedClients.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Connected Endpoints:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            connectedClients.forEach { (clientIp, clientPort) ->
                                Text(
                                    text = " • $clientIp:$clientPort",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(
    label: String,
    color: Color,
    textColor: Color
) {
    Surface(
        color = color,
        shape = CircleShape,
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ServerConfigCard(
    serverState: ServerState,
    portInputText: String,
    portError: String?,
    onPortInputChanged: (String) -> Unit,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit,
    onRefreshIp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TCP Port Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                IconButton(onClick = onRefreshIp) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh IP"
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = portInputText,
                    onValueChange = onPortInputChanged,
                    label = { Text("TCP Port") },
                    isError = portError != null,
                    supportingText = {
                        if (portError != null) {
                            Text(portError, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Default: 10110")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = serverState == ServerState.STOPPED,
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                if (serverState == ServerState.RUNNING) {
                    Button(
                        onClick = onStopServer,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Stop Server")
                    }
                } else {
                    Button(
                        onClick = onStartServer,
                        enabled = portError == null,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Start Server")
                    }
                }
            }
        }
    }
}

@Composable
fun FileSelectionCard(
    fileName: String,
    totalSentences: Int,
    onOpenFile: () -> Unit,
    onLoadSample: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "NMEA Log File Source",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "$totalSentences sentences parsed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onOpenFile,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open File")
                }

                OutlinedButton(
                    onClick = onLoadSample,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Load Sample")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaybackControlsCard(
    playbackState: PlaybackState,
    currentTimeString: String,
    currentIndex: Int,
    totalSentences: Int,
    progress: Float,
    speedMultiplier: Float,
    repeatEnabled: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSeekProgress: (Float) -> Unit,
    onSetSpeed: (Float) -> Unit,
    onToggleRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Streaming Playback Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "UTC: $currentTimeString",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "Line ${if (totalSentences > 0) currentIndex + 1 else 0} / $totalSentences",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Slider(
                value = progress,
                onValueChange = onSeekProgress,
                enabled = totalSentences > 0,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleRepeat,
                    modifier = Modifier.background(
                        if (repeatEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Toggle Repeat",
                        tint = if (repeatEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                when (playbackState) {
                    PlaybackState.PLAYING -> {
                        Button(
                            onClick = onPause,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            )
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause")
                            Spacer(Modifier.width(6.dp))
                            Text("Pause")
                        }
                    }

                    PlaybackState.PAUSED -> {
                        Button(
                            onClick = onResume,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                            Spacer(Modifier.width(6.dp))
                            Text("Resume")
                        }
                    }

                    PlaybackState.STOPPED -> {
                        Button(
                            onClick = onPlay,
                            enabled = totalSentences > 0
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                            Spacer(Modifier.width(6.dp))
                            Text("Play")
                        }
                    }
                }

                Button(
                    onClick = onStop,
                    enabled = playbackState != PlaybackState.STOPPED,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop")
                    Spacer(Modifier.width(4.dp))
                    Text("Stop")
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Speed Factor Multiplier:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0.5f, 1.0f, 2.0f, 5.0f, 10.0f).forEach { speed ->
                        FilterChip(
                            selected = (speedMultiplier == speed),
                            onClick = { onSetSpeed(speed) },
                            label = { Text("${speed}x") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveConsoleCard(
    recentLogs: List<String>,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val consoleListState = rememberLazyListState()

    LaunchedEffect(recentLogs.size) {
        if (recentLogs.isNotEmpty()) {
            consoleListState.animateScrollToItem(recentLogs.size - 1)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Live NMEA TCP Stream Console",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${recentLogs.size} lines in scroll buffer",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                IconButton(onClick = onClearLogs) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear Console Logs"
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = MaterialTheme.colorScheme.inverseSurface,
                shape = RoundedCornerShape(8.dp)
            ) {
                if (recentLogs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Stream console idle. Start playback to view live NMEA sentences...",
                            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        state = consoleListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        items(recentLogs) { logLine ->
                            Text(
                                text = logLine,
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Phone Portrait", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun NmeaPlayerScreenPhonePreview() {
    NMEAPlayerTheme {
        NmeaPlayerScreen()
    }
}

@Preview(name = "Tablet Landscape", showBackground = true, device = "spec:width=1280dp,height=800dp")
@Composable
fun NmeaPlayerScreenTabletPreview() {
    NMEAPlayerTheme {
        NmeaPlayerScreen()
    }
}

@Preview(name = "App Icon Preview", showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun AppIconPreview() {
    NMEAPlayerTheme {
        Box(
            modifier = Modifier
                .padding(32.dp)
                .width(200.dp)
                .height(200.dp)
                .clip(RoundedCornerShape(44.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
