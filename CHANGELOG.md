# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3.4] - 2026-05-18

### Added
- "Clear Survey Data" button in the cave/artificial detail screen (shown when surveys are loaded) — removes local images and cached records for that entry only, freeing space without affecting other caves
- "Clear Survey Data" button in Settings — removes all survey images and records globally while keeping the core cave, spring, and artificial dataset intact

## [1.3.3] - 2026-05-18

### Added
- Survey images are now downloaded to local storage when tapping "Download Surveys"; the full-resolution image is stored in internal app files and displayed offline without a network connection
- Tapping a survey image opens a fullscreen in-app viewer (black background, tap anywhere to dismiss) instead of the browser; surveys downloaded before this update fall back gracefully to the browser

### Changed
- Survey images show full resolution instead of server-generated thumbnails
- "Clear Cache" now also deletes locally stored survey images
- DB version bumped to 6 (destructive migration; re-sync required — survey data is re-downloadable)

## [1.3.2] - 2026-05-18

### Added
- Full Italian localization (all UI labels, field names, legend entries, metadata values, error messages) with automatic fallback to English for unsupported locales
- Manual locale override in Settings: choose System default, English, or Italian; change takes effect immediately
- Metadata value decoding: meteorology, hydrology, epoch, closure status, spring usage and utilization codes are now displayed as human-readable labels instead of raw API strings — values are also localized

### Changed
- All UI strings moved to `res/values/strings.xml` (English) and `res/values-it/strings.xml` (Italian); no hardcoded strings remain in Compose code

## [1.3.1] - 2026-05-17

### Added
- Import from JSON in the Export screen: tap "Import from JSON" to pick a previously exported OpenKIS JSON file; caves, springs, and artificial cavities are upserted — existing records with matching codes are updated, new ones are added
- Artificial cavity icons on the map and in the legend now reflect vertical trend (horizontal, descending, ascending), type (resurgence, absorbing), and closure status — matching the full icon set from the OpenKIS PHP backend
- Export screen now always shows the Import section at the bottom, independent of what data is cached

### Fixed
- Artificial cavities were missing `depthNegative`, `depthPositive`, and `closed` fields in the entity — they were parsed from the server but discarded. These are now stored and used for icon composition.

### Changed
- DB version bumped to 5 (automatic migration; no data loss)

## [1.3.0] - 2026-05-17

### Added
- On-demand survey download in cave and artificial cavity detail screens: tap "Download Surveys" to fetch, parse, and cache all survey records (plan drawings, sections, authors, dates, licenses) from the server detail page
- Surveys are stored in the local Room database; subsequent opens show cached data immediately without re-fetching
- Survey images load inline from their server URLs via Coil; tapping a thumbnail opens the full-size image in the browser
- Separate "Export Surveys" and "Share Surveys" options in the Export screen — survey metadata is exported as a distinct JSON file, independent of the main KML/GPX/JSON map export
- Export screen now shows a survey record count and the survey export section only appears when surveys have been downloaded

### Changed
- DB version bumped to 4 (re-sync required after upgrade; data is not lost, only the local cache needs refreshing)

## [1.2.1] - 2026-05-17

### Added
- JSON export now includes all fields for every record regardless of whether coordinates are present — suitable for full offline backup and data analysis
- Share button on the Export screen: exports to a temporary file and opens the Android share sheet (Bluetooth, Wi-Fi Direct, email, USB, etc.) without requiring storage permission

## [1.2.0] - 2026-05-17

### Added
- Map legend: tap the new Info FAB to open a scrollable panel showing every icon variant for natural caves, karst springs, and artificial cavities with a plain-language description of each modifier (trend, hydrology, meteorology, closure status)
- Layer toggle buttons in the map now use the actual OpenKIS base PNG icons (caves, springs, artificials) instead of coloured dots; icons are desaturated when a layer is hidden

### Changed
- Layers and Legend FABs are mutually exclusive — opening one closes the other; both highlight their container colour when active

## [1.1.2] - 2026-05-17

### Fixed
- APK updates now work correctly; previous releases were signed with an ephemeral debug keystore regenerated on each CI runner, causing Android to reject updates with "App not installed"

## [1.1.1] - 2026-05-17

### Added
- Dev Data Sources toggle in Settings (just above the Debug Log card): when enabled, the `dev.catastogrotte-piemonte.net` experimental server is added to the server list and included in sync; when disabled the server is hidden from both the map/list and the Settings servers section and excluded from Sync All
- Dev server is seeded in the local DB on first launch with `visible=false`, so enabling it later never requires a manual add

### Changed
- `syncAll()` now skips servers with `visible=false`; individual per-server sync is unaffected

## [1.1.0] - 2026-05-17

### Added
- Per-server visibility toggle: each server in Settings can be shown or hidden on the map and in the browse list independently
- Per-server sync type selector: three checkboxes (Caves, Springs, Artificials) per server in Settings control which data types are fetched during sync; helps reduce data usage on slow connections or when only one category is needed
- Map and browse list now filter entities by visible server: hiding a server instantly removes its data from both views without requiring a re-sync

### Fixed
- `SyncManager.syncServer()` re-downloaded the caves CSV a second time to detect server type when `syncCaves=false`; the probe now always reuses the single caves CSV fetch regardless of whether caves sync is enabled

## [1.0.1] - 2026-05-17

### Fixed
- Compilation error: `CaveMarker`, `SpringMarker`, `ArtificialMarker` theme color imports were accidentally removed from `MapScreen.kt` during the icon refactor; they are still used by the layer toggle panel

## [1.0.0] - 2026-05-17

### Added
- OpenKIS PNG icon set for all three data types, bundled as app assets (`assets/icons/{caves,springs,artificials}/`)
- `IconCompositor`: composites multiple transparent 128×128 px PNG layers into a single `BitmapDrawable` at runtime, with a 64-entry `LruCache` to avoid re-decoding on each map refresh
- `MarkerIconResolver`: maps entity metadata to the ordered layer list, mirroring the PHP backend's `openkis_GetIcon()` logic — cave icons encode vertical trend (asc/desc/hori), hydrology type (water/emitting/absorbent), meteorology (blow/suck during heat/cold), and closure status
- Design document at `doc/design-docs/map-icons.md`

### Changed
- Map marker anchor changed from center-bottom (pin style) to center-center (symbol style) to match the OpenKIS square icon convention

## [0.9.1] - 2026-05-17

### Fixed
- Dev-site sync made two full CSV downloads per entity: one for the compatibility probe and one for the actual data. The probe is now eliminated — the caves CSV fetch doubles as the compatibility check, and its content is passed directly to the parser. Springs and artificials are each fetched exactly once. Total downloads per sync reduced from 7 to 3 for dev-site servers.

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
