# OpenKIS Android

> **Alpha Software** — This project is in a very early stage of development. Features may be incomplete, unstable, or subject to breaking changes. Use at your own risk.

> **Disclaimer** — This is an independent, community-driven project. It is **not affiliated with, endorsed by, or officially connected to** the original [OpenKIS](https://github.com/speleoalex/openkis) project or its developers. "OpenKIS" refers to the Opensource Karst Information System created by Alessandro Vernassa.

Android companion app for [OpenKIS](https://github.com/speleoalex/openkis) (Opensource Karst Information System) — a web-based platform for managing speleological and karst geological data.

OpenKIS Android connects to your OpenKIS server, syncs cave data for offline use, and lets you browse caves on an interactive map directly from your phone — ideal for field work in areas without signal.

## Features

- **Interactive Map** — Browse caves, karst springs, and artificial cavities on an OpenStreetMap layer with colored markers and layer toggles
- **Search & Browse** — Filter and search across all record types by name, code, or synonyms
- **Detail View** — View elevation, depth, length, coordinates, hydrology, and all metadata for each record; navigate to cave entrances via Google Maps
- **Data Export** — Export all synced data as KML (Google Earth), GPX (GPS devices), or JSON
- **Offline Support** — All data is cached locally in a Room SQLite database; works fully offline after initial sync
- **Server Sync** — Connects to any OpenKIS server instance via its JSON API; configurable server URL

## Screenshots

*Coming soon*

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 35
- JDK 17

### Build & Run

1. Clone the repository:
   ```bash
   git clone https://github.com/mbound/openkis-android.git
   cd openkis-android
   ```

2. Open the project in Android Studio and let Gradle sync.

3. Run on a device or emulator (minimum API 26 / Android 8.0).

4. In the app, go to **Settings**, enter your OpenKIS server URL (e.g. `https://your-server.com/`), and tap **Sync Now**.

## Data Model

The app syncs three main entity types from the OpenKIS server:

- **Caves** — Natural caves with cadastre code, name, coordinates, depth, length, hydrology, meteorology
- **Springs** — Karst springs with flow rates and usage classification
- **Artificial Cavities** — Bunkers, tunnels, mines with construction year, typology, and category

## Export Formats

| Format | Use Case |
|---|---|
| **KML** | View caves on Google Earth or any KML-compatible GIS tool |
| **GPX** | Import waypoints into GPS devices, hiking apps (e.g. OsmAnd, Locus Map) |
| **JSON** | Programmatic use, data analysis, integration with other tools |

## Server Compatibility

The app connects to any standard OpenKIS server via these endpoints (these below are just examples):

- `openkis_json.php?mod=caves` — Cave data
- `openkis_json.php?mod=springs` — Spring data
- `openkis_json.php?mod=artificials` — Artificial cavity data
- `openkis_API.php?op=near&lat=X&lon=Y` — Nearest cave lookup

## Contributing

Contributions are welcome. Please open an issue to discuss proposed changes before submitting a pull request.

## Credits

This project is built upon the work of the original OpenKIS platform:

- **[OpenKIS](https://github.com/speleoalex/openkis)** — Opensource Karst Information System
- **Author**: [Alessandro Vernassa](mailto:speleoalex@gmail.com) (speleoalex)
- **Original Copyright**: Copyright (c) 2011 Alessandro Vernassa
- **Original License**: [GNU General Public License](http://opensource.org/licenses/gpl-license.php)

Additional open-source projects used:

- [osmdroid](https://github.com/osmdroid/osmdroid) — OpenStreetMap for Android
- [Retrofit](https://square.github.io/retrofit/) — HTTP client by Square
- [Room](https://developer.android.com/training/data-storage/room) — SQLite abstraction by Google

## License

Copyright (c) 2026

This project is free software: you can redistribute it and/or modify it under the terms of the **GNU General Public License** as published by the Free Software Foundation, either version 2 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the [GNU General Public License](https://www.gnu.org/licenses/gpl-2.0.html) for more details.

This license is consistent with the original OpenKIS project, which is licensed under the [GNU General Public License](http://opensource.org/licenses/gpl-license.php).
