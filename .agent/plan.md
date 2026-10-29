# Project Plan

Erzeuge eine TCP Server App. Sie soll NMEA (inkl. AIS-Daten, die mit '!' beginnen) und alle weiteren Daten aus einer Textdatei lesen und zeilenweise an verbundene Clients senden. Wenn Zeitstempel in RMC NMEA Messages vorhanden sind, sollen diese zur zeitlichen Synchronisation verwendet werden. Es soll einen Pause/Resume Button geben. Die Wiedergabegeschwindigkeit soll über einen Faktor erhöht werden können. Der TCP Port soll konfigurierbar sein. Die NMEA Datei soll über einen Dialog ausgewählt werden können. Die App soll als Foreground Service auch im Hintergrund laufen und einen Exit-Button zum vollständigen Beenden der App und des Services bieten.

## Project Brief

# Project Brief: NMEA TCP Server App

## Features

1. **TCP Data Streaming**: Asynchronous TCP server streaming NMEA and AIS data line-by-line from a text file to connected TCP clients over a user-configurable port.
2. **NMEA File Selection & Time Synchronization**: Integrated system file dialog to select local NMEA text files, automatically synchronizing line delays using timestamp delta parsing from `$GPRMC` messages.
3. **Playback & Speed Control**: Playback controls allowing users to Pause/Resume streaming and scale the playback speed using a configurable multiplier factor in real time.
4. **Foreground Service & Lifecycle Management**: Android Foreground Service with persistent notifications to maintain continuous streaming in the background, featuring an explicit Exit button to immediately stop the service and terminate the application.

## High-Level Tech Stack

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose with Material 3 & **Compose Material Adaptive** library for adaptive, responsive layouts
* **Navigation:** **Jetpack Navigation 3** (state-driven navigation)
* **Asynchronous Execution & Networking:** Kotlin Coroutines & Flow for asynchronous TCP socket handling and file line streaming
* **Background Processing:** Android Foreground Service with System Notification Manager
* **System Integration:** Storage Access Framework (SAF via `ActivityResultContracts.OpenDocument`) for safe NMEA log file selection

## Implementation Steps
**Total Duration:** 11m 57s

### Task_1_NmeaParserAndTcpServerCore: Implement NMEA log parser with timing delay calculation from GPRMC sentences and TCP socket server manager using Kotlin Coroutines for async multi-client streaming with play/pause/stop and playback speed multiplier support.
- **Status:** COMPLETED
- **Updates:** Implemented NmeaSentence parser with GPRMC/GNRMC timestamp extraction, TcpServerManager for multi-client broadcasting, NmeaPlaybackEngine with variable speed factor and play/pause controls, and unit tests.
- **Acceptance Criteria:**
  - NMEA parser correctly extracts relative timestamps from GPRMC/RMC lines
  - TCP server handles client connections and broadcasts NMEA lines asynchronously
  - Playback controller handles play, pause, stop, and dynamic speed multipliers
- **Duration:** 7m 24s

### Task_2_ComposeUiAndViewModel: Create Jetpack Compose Material 3 UI and ViewModel for NMEA TCP Server including system file picker integration, port input, streaming controls, speed factor selection, and real-time status monitor.
- **Status:** COMPLETED
- **Updates:** Integrated system file picker via OpenDocument SAF, port input validation, playback controls, speed factor selection chips, live NMEA terminal streaming view, server status cards, and responsive Compose layout. Verified build and 17 unit tests passed.
- **Acceptance Criteria:**
  - File picker allows selecting NMEA log files and loading lines into streaming engine
  - ViewModel manages UI state for TCP port, active client count, streaming status, and playback progress
  - Composable UI provides controls for port input, file selection, playback (play/pause/stop), speed selection, and real-time client count/progress
- **Duration:** 4m 33s

### Task_3_AppIconAndAssets: Configure app icons, launcher assets, foreground service notification icons, and UI theme adjustments.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - App launcher icon configured
  - UI theme and color palette styled appropriately
- **StartTime:** 2026-10-29 15:18:52 CET

### Task_4_RunAndVerify: Run and verify application stability, verify TCP server streaming, check UI components, ensure no crashes, and confirm alignment with user requirements.
- **Status:** PENDING
- **Acceptance Criteria:**
  - build pass
  - app does not crash
  - make sure all existing tests pass
  - critic_agent verifies application stability and confirms alignment with user requirements

