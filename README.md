# NMEA Player (TCP Server App)

**Application ID:** `de.nocheinpeter.nmeaplayer`

---

## 📌 Überblick & Verwendungszweck / Overview & Purpose

**NMEA Player** ist eine Android-Anwendung zum Streamen von NMEA 0183- und AIS-Daten über TCP an verbundene Navigationsclients (z. B. OpenCPN, AVNav).

Die App liest NMEA 0183- und AIS-Daten zeilenweise aus lokalen Textdateien ein und überträgt diese über ein lokales TCP-Netzwerk an verbundene Marine- und GPS-Navigationsclients.

Aktuelle Pre-Release: https://github.com/nocheinpeter/NMEA_Player/releases/download/release2061101/NMEAPlayer-release.apk


# Konfiguration
![Beispiel Konfiguration](doc/NMEA%20Player%20Config.png)
---

## 🇬🇧 English Feature Summary

- **TCP Server Streaming:** Configurable TCP port (default `10110`), stored persistently in `SharedPreferences`. Asynchronous broadcast of NMEA/AIS data to multiple connected clients.
- **NMEA & AIS Data Support:** Reads standard NMEA 0183 sentences (`$GPRMC`, `$GPGGA`, etc.) and AIS data lines starting with `!` (`!AIVDM`, `!AIVDO`), as well as all other text lines in the file.
- **Timing Synchronization:** Calculates relative time deltas using `$GPRMC` UTC timestamps for realistic timing (max cap 2s between sentences).
- **Playback Controls & Variable Speed:** Play, Pause, Resume, Stop, Loop toggle, interactive progress seekbar, and playback speed multipliers (0.5x, 1x, 2x, 5x, 10x).
- **File Selection & Sample Data:** System file picker via Storage Access Framework (`.nmea`, `.txt`, `.log`) and built-in sample dataset (`sample_data.nmea`).
- **Background Execution (Foreground Service):** Android `NmeaServerService` with ongoing notification for continuous streaming when minimized.
- **Single Exit Button:** Exit button in the top app bar and notification to cleanly terminate background service and exit the app.
- **Material 3 & Adaptive UI:** Responsive Jetpack Compose layout for phone and tablet screens with live stream console log.

---

## ✨ Funktionale Merkmale (Details auf Deutsch)

### 1. TCP Server & Multi-Client Broadcast
- **Konfigurierbarer TCP-Port:** Der Standardport lautet `10110` (Standard für NMEA 0183 Marine-Netzwerke). Der Port lässt sich in der App konfigurieren und wird dauerhaft in den `SharedPreferences` gespeichert.
- **Multi-Client:** Unterstützung für mehrere gleichzeitig verbundene Navigations-Clients.
- **IP-Anzeige:** Zeigt die lokale IP-Adresse des Geräts zur einfachen Konfiguration der Clients an.

### 2. NMEA & AIS Datenunterstützung
- **Standard NMEA 0183:** Liest Datensätze mit `$`-Präfix (`$GPRMC`, `$GPGGA`, `$GPGLL`, `$SDDBT`, `$WWTMW`, etc.).
- **AIS Daten:** Unterstützt AIS-Sätze mit `!`-Präfix (`!AIVDM`, `!AIVDO`).
- **Vollständige Log-Abdeckung:** Verarbeitet alle weiteren Zeilen einer Datei fehlerfrei.

### 3. Zeit-Synchronisation (Timing Synchronization)
- Extrahiert UTC-Zeitstempel aus `$GPRMC`-Sätzen zur Berechnung relativer Zeit-Deltas zwischen Datensätzen.
- **Maximum Cap:** Zeitabstände zwischen Datensätzen werden auf maximal 2,0 Sekunden gedeckelt, um lange Pausen im Datenstrom zu vermeiden.

### 4. Wiedergabesteuerung & Variable Geschwindigkeit
- **Steuerungsfunktionen:** Play, Pause, Resume, Stop und Loop-Schalter (Endlosschleife).
- **Interaktiver Fortschrittsbalken (Seekbar):** Ermöglicht das Springen an eine beliebige Stelle in der Datei.
- **Geschwindigkeitsmultiplikatoren:** Einstellbare Playback-Geschwindigkeiten (0.5x, 1x, 2x, 5x, 10x).

### 5. Dateiauswahl & Beispieldaten
- **System File Picker:** Integrierte Dateiauswahl über das Storage Access Framework (SAF) für `.nmea`, `.txt` und `.log` Dateien.
- **Mitgelieferte Beispieldaten:** Integriertes Beispielsdataset (`sample_data.nmea`) für sofortiges Testen ohne externe Dateien.

### 6. Hintergrundausführung (Foreground Service)
- **Android `NmeaServerService`:** Ausführung als Foreground Service mit dauerhafter Benachrichtigung.
- Gewährleistet kontinuierliches Streaming und Verbindungskontinuität bei minimierter App oder gesperrtem Bildschirm.

### 7. Saubere Beendigung (Single Exit Button)
- **Exit Button:** Dedizierter Exit-Button in der Top App Bar sowie in der Service-Benachrichtigung.
- Stoppt den TCP-Server, beendet den Foreground Service und schließt die Anwendung sauber.

### 8. Material 3 & Adaptive UI
- **Jetpack Compose Layout:** Entworfen nach Material Design 3 Richtlinien.
- **Adaptive UI:** Responsive Layout-Anpassung für Smartphones und Tablets.
- **Live Console Log:** Integrierte Konsole zur Live-Anzeige des gepufferten NMEA/AIS-Datenstroms.

---

## 🛠 Technischer Stack & Architektur

- **Sprache:** Kotlin
- **Benutzeroberfläche (UI):** Jetpack Compose (Material 3, Adaptive Layout)
- **Asynchronität & I/O:** Coroutines & Flow für asynchrone Socket-Kommunikation
- **Architektur:** MVVM (Model-View-ViewModel) mit `StateFlow`

---

## 🏗 Bauen & Testen / Build & Test Instructions

### Debug APK bauen
```bash
./gradlew assembleDebug
```

### Release APK bauen
```bash
./gradlew assembleRelease
```

### Unit Tests ausführen
```bash
./gradlew testDebugUnitTest
```
