# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
