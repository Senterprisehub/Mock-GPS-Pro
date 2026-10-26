# Mock GPS Pro — Complete Functional Guide & Product Documentation

**Product Name:** Mock GPS Pro  
**Platform:** Android (8.0 Oreo and above, optimized for Android 12–15)  
**Category:** Location Simulation, Route Testing, & GNSS Telemetry Suite  

---

## 1. Executive Summary & Product Overview
**Mock GPS Pro** is an enterprise-grade GNSS location simulation, route automation, and telemetry diagnostic suite designed for QA engineers, mobile app developers, geospatial researchers, and privacy-conscious power users. 

Built on Android’s official `LocationManager` and Google Play Services `FusedLocationProviderClient` test provider pipelines, Mock GPS Pro injects realistic, physics-accurate GPS coordinates, altitude, bearing, speed, and satellite signal metrics across your device in real time—without requiring root access.

---

## 2. Core Functional Modules

### 2.1 Interactive Multi-Layer Map Studio (`Map` Tab)
The Map Studio serves as the visual command center for real-time coordinate targeting, dual-position comparison, and manual movement control.
* **Multi-Source High-Resolution Tile Engine:**
  * **Google Streets HD:** Full road networks, transit hubs, landmarks, and localized street labels.
  * **Google Hybrid Satellite:** High-resolution aerial photography with road and place-name overlays.
  * **Google Terrain Topo:** Shaded relief, elevation contours, and natural geographical features.
  * **OpenStreetMap (OSM):** Community-verified global street and footpath mapping.
* **Dual-Pin Live Tracking:**
  * **Mock Target Pin (Blue):** Displays the exact simulated coordinate being injected into the Android OS, complete with an accuracy radius ring and directional heading cone.
  * **Real Hardware GPS Pin (Green):** Simultaneously displays your physical GPS location (when hardware GPS is active) and calculates the real-time **Mock vs. Real Distance Delta** (`Δ km/m`).
* **Never-Lost Target Pin Radar:**
  * If you pan or zoom away from your active Mock Target Pin, an interactive floating radar banner (`📍 Target Pin: X.X km • Tap to Center`) with a live directional arrow appears at the top of the viewport. Tapping it immediately snaps the camera back to your target pin.
* **Universal Place Search & Reverse Geocoding:**
  * Search any global street address, town, landmark, or postal code with instant autocomplete suggestions, or enter direct `Latitude, Longitude` pairs.
  * Live reverse-geocoding banner at the top of the map displays the human-readable street address and locality of your current target pin.
* **360° Analog Virtual Joystick:**
  * Smooth, proportional omnidirectional thumbstick for real-time manual walking, cycling, or driving movement on the map.
  * Adjustable movement speed profiles and instant heading rotation.
* **Saved Bookmarks & Quick Presets:**
  * Bookmark any coordinate with a custom name for one-tap teleportation, or choose from curated global city and regional presets.

---

### 2.2 Multi-Stop Route Planner & GPX Automation (`Routes` Tab)
Simulate realistic journeys along real road networks or custom multi-waypoint paths.
* **A-to-B & Multi-Stop Road Routing:**
  * Search and add Origin, Destination, and unlimited intermediate Waypoints (`Via stops`).
  * Automatically computes road-snapped polylines using the OSRM Routing Engine so simulated movement follows actual streets, highways, and turns rather than cutting through buildings.
* **Interactive Waypoint Editor:**
  * Reorder stops, adjust **Dwell / Pause Timers** at specific stops (e.g., pause 30 seconds at a delivery checkpoint), or add waypoints directly by tapping on the map.
* **Transport Speed Profiles & Physics:**
  * Choose from preset transport modes (**Walk** ~5 km/h, **Run** ~12 km/h, **Bicycle** ~22 km/h, **City Car** ~50 km/h, **Highway** ~100 km/h, **High-Speed Rail / Drone** ~250+ km/h) or dial in a custom speed slider.
  * **Loop Mode:** Continuously repeat the route(A → B → A or circular loop) for endurance testing.
* **Live Playback Controls & ETA Telemetry:**
  * Start, Pause, Resume, or Stop route simulation at any time.
  * Real-time progress bar showing distance covered, remaining distance, next waypoint countdown, and estimated time of arrival (ETA).
* **GPX Import & Export:**
  * Import standard `.gpx` track/route files or paste GPX XML payloads to replay recorded real-world drives and hikes, or export your custom routes to share with teammates.

---

### 2.3 Intelligent Route Assistant (`Copilot` Tab)
An integrated natural-language location and route assistant for rapid scenario generation.
* **Natural Language Teleportation:**
  * Type commands such as *"Teleport to Victoria Memorial, Kolkata"* or *"Move 2.5 km northeast of current position"* to immediately resolve coordinates and jump the mock target pin.
* **Automated Multi-Stop Scenario Builder:**
  * Ask the assistant to generate realistic test itineraries (e.g., *"Create a 4-stop delivery route around Salt Lake Sector V at 35 km/h"*) and load them directly into the Route Simulator with a single tap.
* **Instant Coordinate & Geodesic Calculations:**
  * Query distances, bearings, elevation estimates, and bounding boxes for geofence testing.

---

### 2.4 Real-Time GNSS Telemetry & NMEA Diagnostics (`Logs` Tab)
Verify that your simulated location is indistinguishable from genuine hardware GNSS output.
* **Live Telemetry Dashboard:**
  * Displays real-time **Latitude**, **Longitude**, **Altitude (m)**, **Speed (m/s & km/h)**, **Bearing (°)**, **Horizontal/Vertical Accuracy (m)**, and **Simulated Satellite Count (SVs)**.
* **Live NMEA-0183 Sentence Generator:**
  * Generates and streams authentic `$GPGGA` and `$GPRMC` NMEA sentences matching your simulated coordinates, UTC timestamp, fix quality, and ground speed.
* **Event & Injection Audit Log:**
  * Chronological log of every provider registration, coordinate tick, route waypoint arrival, and system status event.
  * One-tap **Copy to Clipboard** or **Clear Logs** for QA bug reports and test documentation.

---

### 2.5 Realism Physics, Permissions & System Setup (`Setup` Tab)
Configure anti-detection physics and verify Android Developer Options in one guided checklist.
* **Interactive Setup & Diagnostic Checklist:**
  * Live status indicators for **Location Permissions**, **Notification Permissions** (for foreground service persistence), and **Android Developer Mock Location App Registration**.
  * One-tap deep links directly into Android’s **Developer Options** screen so users don't have to hunt through system menus.
* **Realism & Anti-Detection Physics Engine:**
  * **Micro-Jitter (Natural GPS Drift):** Adds subtle, Gaussian coordinate fluctuations (configurable in meters) to mimic natural atmospheric and multipath GPS drift when stationary or moving.
  * **Altitude Fluctuation:** Simulates natural barometric/GNSS vertical variance.
  * **Sensor-Fusion Pedometer / Step Cadence Simulation:** Synchronizes simulated movement cadence with walking/running speeds.
  * **Update Interval Control:** Configure injection frequency (e.g., `100ms` ultra-high frequency up to `2000ms` battery-saving intervals) to ensure zero "rubber-banding" back to real GPS.

---

## 3. Background Reliability & Floating Overlay Controls
* **Persistent Foreground Simulation Service:**
  * Once you tap **START MOCK**, a high-priority Android Foreground Service keeps injecting coordinates continuously even when you minimize Mock GPS Pro and switch to Google Maps, Uber, Strava, delivery apps, or your own app under test.
* **System Notification Controls:**
  * View current simulated coordinates and stop or pause simulation directly from the Android notification shade.

---

## 4. Quick-Start Guide for Customers (3 Easy Steps)

1. **Enable Mock Location App in Android Settings (One-Time Setup):**
   * Open the **Setup** tab in Mock GPS Pro.
   * Tap **Open Developer Options** (if Developer Options is not yet enabled on your phone, go to *Settings → About Phone* and tap **Build Number** 7 times).
   * Scroll to **Select mock location app** and choose **Mock GPS Pro**.
2. **Choose Your Target Location or Route:**
   * Go to the **Map** tab, search for any city/street or tap directly on the map to place the **Blue Mock Target Pin**, OR go to the **Routes** tab to build a road-snapped A-to-B journey.
3. **Start Simulation:**
   * Tap the green **START MOCK** button in the top status bar (or **Start Route** in the Routes tab).
   * The status pill will turn green (**MOCK ACTIVE**). You can now open any map or location-based app on your device and see your simulated location live!

---

## 5. Frequently Asked Questions (FAQ) & Troubleshooting

* **Q: Why does my location briefly jump back ("rubber-band") to my real location in some apps?**  
  **A:** Ensure that your update interval in the **Setup** tab is set to **500 ms or faster**, and enable **Realism Micro-Jitter**. Mock GPS Pro injects into both the `GPS_PROVIDER` and Google Play Services `FusedLocationProviderClient` to prevent rubber-banding.
* **Q: What happens when I tap "STOP MOCK"?**  
  **A:** Mock GPS Pro cleanly removes the test providers and restores your phone’s native hardware GPS receiver within 1–2 seconds.
* **Q: Does Mock GPS Pro require root access?**  
  **A:** No. It uses standard, official Android Developer Mock Location APIs safe for all production devices and emulators.

