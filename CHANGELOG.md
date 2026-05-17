# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.9.0] - 2026-05-17

### Fixed
- `DevSiteApi.isCompatible()` used a HEAD request; servers that return HTTP 405 for HEAD were incorrectly treated as incompatible, causing sync to fall back to the legacy `openkis_json.php` path and always report "Never Synced"
- `CsvParser` BOM stripping used an embedded BOM character literal; replaced with an explicit `﻿` prefix string to guarantee correct stripping regardless of source-file encoding

### Added
- Internal debug log (`DebugLogger`): in-memory ring buffer (500 entries) with level, tag, timestamp, and message
- Debug Log card in Settings: view log in a scrollable dialog, export to a text file via the system file picker, or clear the log
- Sync path now logged end-to-end: probe URL and result, routing decision (dev-site CSV vs legacy JSON), per-entity CSV fetch size, total synced count, and errors

## [0.8.0] - 2026-05-17

### Added
- Support for the new `dev.catastogrotte-piemonte.net` export API format
- `CsvParser`: RFC4180 CSV parser for the site's export format (BOM-aware, skips numeric-index header row, header-keyed column lookup)
- `DevSiteApi`: auto-detects compatible servers via a HEAD probe to `/export/cavita-naturali/csv`; falls back to legacy `openkis_json.php` for older servers
- CSV sync for natural caves (`/export/cavita-naturali/csv`), springs (`/export/sorgenti/csv`), and artificial cavities (`/export/cavita-artificiali/csv`)

### Notes
- Springs with UTM coordinates (outside WGS84 decimal-degree range) are stored in the database but displayed at 0,0 on the map; UTM conversion is planned for a future release

## [0.7.0] - 2026-04-28

### Added
- Multi-server support: add, remove, and manage multiple OpenKIS backend servers
- Per-server sync with individual sync buttons
- "Sync All" button to sync all servers at once
- Add server dialog with URL and optional name fields
- Delete server confirmation dialog
- Per-server last sync timestamp display
- Default "Piemonte" server seeded on first launch

### Changed
- Database schema: composite primary key (serverUrl + code) to support data from multiple servers
- Sync now only replaces data from the synced server, preserving other servers' data
- Settings UI redesigned with server list replacing single URL input
- Database version bumped to 2 (data will be cleared on update)

## [0.6.3] - 2026-04-28

### Fixed
- Map default zoom too far out when all data types enabled, caused by outlier coordinates stretching the bounding box

## [0.6.2] - 2026-04-28

### Fixed
- Release APK not installing due to missing signing configuration

## [0.6.1] - 2026-04-28

### Fixed
- Release APK not installing due to R8/ProGuard stripping required classes

### Added
- GitHub Actions CI workflow for automated APK builds and releases
- Gradle wrapper files for reproducible builds

## [0.6.0] - 2026-04-27

### Added
- Reset view button on the map to zoom back to fit all markers
- Zoom +/- buttons in the top-right control column
- Amber highlight on selected map markers for visibility
- Close (X) button on the marker callout card to dismiss selection
- Author, credits, and license information in the About section

### Changed
- Removed adaptive icon foreground (house icon) so AGSP logo PNG is used directly without padding
- Smoother pinch-to-zoom with fractional zoom levels
- Moved zoom controls from bottom (hidden by callout) to top-right FAB column

### Fixed
- App icon showing default green house instead of AGSP logo
- Launcher icon displaying with unnecessary white padding

## [0.5.0] - 2026-04-27

### Added
- Interactive OpenStreetMap with colored markers for caves, springs, and artificial cavities
- Layer toggle panel to show/hide each data type on the map
- User GPS location overlay with permission handling and follow mode
- Browse screen with search and type filter chips
- Detail view with elevation/depth/length highlights, sharing, and Google Maps navigation
- Data export in KML, GPX, and JSON formats via Android file picker
- Server sync via OpenKIS JSON API with configurable server URL
- Offline support with Room SQLite database
- Offline mode toggle in settings
- Persistent data type visibility toggles in settings (caves, springs, artificials)
- AGSP logo as app icon
- Default server URL set to catastogrotte-piemonte.net
- Auto-zoom to fit all markers on first data load
- Max zoom level increased to 21 (OSM maximum)
- Speleological map icons: inverted triangle (caves), circle (springs), square (artificials)

## [0.1.0] - 2026-04-25

### Added
- Initial project structure with Kotlin, Jetpack Compose, and Material 3
- Room database schema for caves, springs, and artificial cavities
- Retrofit API client for OpenKIS server
- Basic navigation with bottom bar (Map, Browse, Export, Settings)
