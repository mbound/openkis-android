# Architecture & Tech Stack

## Overview

The app follows a clean architecture pattern with unidirectional data flow: UI observes `StateFlow`s exposed by Hilt-injected ViewModels, which in turn read from a `CaveRepository` backed by a Room database. All sync operations are coordinated by a singleton `SyncManager`.

## Package Structure

```
app/src/main/java/org/openkis/android/

data/
├── debug/          DebugLogger — in-memory ring buffer exposed as StateFlow
├── export/         KML / GPX / JSON file builders
├── local/          Room database, DAOs, and entity classes
│   ├── dao/        CaveDao, SpringDao, ArtificialDao, ServerDao
│   └── entity/     CaveEntity, SpringEntity, ArtificialEntity, ServerEntity
├── remote/         Retrofit API client (OpenKisApi), OkHttp-based DevSiteApi,
│                   CsvParser, DynamicBaseUrlInterceptor
└── repository/     CaveRepository, SyncManager, SyncResult

di/                 Hilt modules (AppModule — provides Room, Retrofit, OkHttp)

ui/
├── caves/          Browse list + detail screens, CavesViewModel
├── components/     Shared composables
├── export/         Export screen + ExportViewModel
├── map/            Map screen, MapViewModel, IconCompositor, MarkerIconResolver
├── navigation/     Bottom nav graph
├── settings/       Settings screen + SettingsViewModel
└── theme/          Material 3 colour scheme and typography
```

## Tech Stack

| Component | Library | Version |
|---|---|---|
| Language | Kotlin | 2.1.0 |
| UI | Jetpack Compose (BOM 2024.12.01) + Material 3 | — |
| Maps | osmdroid (OpenStreetMap) | 6.1.20 |
| Local DB | Room | 2.6.1 |
| Networking | Retrofit + kotlinx-serialization converter | 2.11.0 |
| DI | Hilt | 2.53.1 |
| Preferences | DataStore Preferences | 1.1.1 |
| Navigation | Navigation Compose | 2.8.5 |

## Key Design Decisions

### SyncManager
Central coordinator for all network sync and persistence preferences. Responsible for:
- Seeding the default production server and (hidden) dev server on first launch
- Routing sync to the CSV-based dev-site path or the legacy JSON API path based on a single probe request
- Exposing per-user preferences (offline mode, visible data types, dev sources enabled) as DataStore-backed `Flow<Boolean>`s
- `syncAll()` only syncs servers with `visible=true`

### Server routing (dev-site vs legacy)
The production Catastogrotte site and any other OpenKIS-compatible server that serves CSV files take a different sync path than legacy servers that only expose `openkis_json.php`. Routing is decided by attempting to fetch the caves CSV from `<serverUrl>/cavita-naturali` — null means legacy JSON, non-null means dev-site CSV. The cave CSV content is reused directly if caves sync is enabled, avoiding a second download.

### IconCompositor
Map markers are built at runtime by compositing transparent 128×128 PNG layers from `assets/icons/{caves,springs,artificials}/`. A 64-entry `LruCache<String, Bitmap>` keyed by `"$category:${layers.joinToString(",")}"` prevents redundant decompression. Each call returns a new `BitmapDrawable` wrapper so per-marker `setColorFilter` (selection highlight) does not pollute the cache.

### Per-server visibility
Each `ServerEntity` carries a `visible` boolean. Setting it to `false` hides the server's data from the map, browse list, and Sync All without deleting the locally cached records.

## Database

Room database `AppDatabase` (v3, `fallbackToDestructiveMigration`).

Tables: `caves`, `springs`, `artificials`, `servers`.

Primary keys: `caves`, `springs`, `artificials` use a composite `(serverUrl, code)` so data from multiple servers can coexist without collisions.
