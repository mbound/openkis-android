# Dev Data Sources Toggle

## Context

The production Piemonte server (`catastogrotte-piemonte.net`) is seeded by default on first launch. The dev server (`dev.catastogrotte-piemonte.net`) holds staging/experimental data that may be incomplete or unstable, so it should be opt-in. A checkbox in Settings (just above the Debug Log card) controls whether the dev server is active: when off, the server is invisible and excluded from sync; when on, it behaves like a regular server.

---

## Behaviour

| Checkbox state | Dev server in servers list | Shown on map/list | Included in Sync All |
|---|---|---|---|
| OFF (default) | Hidden | No | No |
| ON | Displayed as a regular server row | Yes | Yes |

When enabled, the dev server row can be individually synced, have its sync types changed, or be removed — same as any other server.

`syncAll()` is updated to skip servers where `visible=false`. This makes the per-server visibility toggle consistently control both UI display and bulk sync across all servers. Individual per-server sync (via the sync button on each row) is unaffected.

---

## Implementation

### `SyncManager.kt`
- Add `const val DEV_SERVER_URL = "https://dev.catastogrotte-piemonte.net"` to companion object
- Add `private val KEY_DEV_SOURCES = stringPreferencesKey("dev_sources_enabled")`
- Add `val devSourcesEnabled: Flow<Boolean>` from DataStore (default `false`)
- Update `ensureDefaultServer()` to also seed the dev server with `visible=false` if it doesn't already exist in the DB
- Add `suspend fun setDevSourcesEnabled(enabled: Boolean)`:
  - Writes to DataStore
  - If `enabled=true`: inserts dev server (name "Dev — Piemonte") if absent, then `updateVisible(true)`
  - If `enabled=false`: `updateVisible(false)` — data is preserved in the DB, server is just hidden
- Update `syncAll()`: iterate only over `serverList.filter { it.visible }`

### `SettingsViewModel.kt`
- Expose `val devSourcesEnabled: StateFlow<Boolean>` from `syncManager.devSourcesEnabled`
- Add `fun setDevSourcesEnabled(enabled: Boolean)` delegating to SyncManager

### `SettingsScreen.kt`
- Collect `val devSourcesEnabled by viewModel.devSourcesEnabled.collectAsState()`
- Filter the displayed server list: exclude `SyncManager.DEV_SERVER_URL` when `devSourcesEnabled=false`
- Add a Card between the `HorizontalDivider` and the Debug Log card:

```
[Science icon]  Dev Data Sources                    [checkbox]
                Include experimental dev server data
                (dev.catastogrotte-piemonte.net).
                Data may be incomplete or unstable.
```

---

## Version
- `versionCode` 17 → 18, `versionName` 1.1.0 → 1.1.1
- CHANGELOG [1.1.1]: add opt-in Dev Data Sources toggle in Settings; `syncAll()` now skips invisible servers

---

## Verification
1. Fresh install: only Piemonte server shown in Settings; Dev Data Sources checkbox unchecked
2. Check Dev Data Sources: dev server row appears in server list
3. Manually sync dev server: data appears on map and in browse list
4. Sync All with Dev enabled: both Piemonte and dev servers are synced
5. Uncheck Dev Data Sources: dev server row disappears from server list; Sync All skips it; map shows no dev data
6. Re-check Dev Data Sources: row reappears; previously synced data is immediately visible without a re-sync
7. Confirm regular Sync All does not sync any server with `visible=false` (including user-hidden servers)
