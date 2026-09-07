# Changelog

All notable changes to **SpyHunts** are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.0.0] - 2026-09-08

### Added
- **Modernized Tracking Compass**: Ported advanced tracking architecture from `SpyHunt-Compass`:
  - Real-time `lastSeen` multi-world position cache fed by `CompassMovementListener`.
  - Robust multi-dimensional resolution (Same dimension -> live tracking; Different dimension -> portal cache fallback; Offline -> last-seen coordinate fallback).
  - Uniform `setLodestoneTracked(false)` and `setLodestone` across Overworld, Nether, and The End, eliminating erratic needle spin.
  - Movement threshold (4 blocks) and 1-second debounce packet throttling to prevent client inventory lag.
  - Dynamic lore showing target runner name, online/offline status, and total active runners.
- **Admin Wildcard Permission**: Added `spyhunts.admin` wildcard permission encompassing all admin subcommands (`/badge`, `/leaderboard`, `/lobby`, `/practice`, `/speedrun`, `/stats`, `/worker`).
- **Thread-Safe Safe-Async Storage**: Implemented atomic in-memory snapshotting in `MultiDataManager` to eliminate file corruption and concurrency race conditions during YAML disk writes.

### Changed
- **Ecosystem Alignment (SpyCore 1.1.2 & SpyInventories)**:
  - Cleanly decoupled inventory management to defer authoritatively to `SpyInventories`, eliminating duplicate YAML persistence and race conditions.
  - Retained robust fallback base64 inventory serialization when `SpyInventories` is not present.
  - Aligned with `SpyNetherPortals` for dimensional routing and `SpyCore` VFS containers.
- **Chunky Feature Removal**: Completely removed Chunky dependencies and task hooks from `WorldFactoryManager` and `pom.xml`, eliminating server startup conflicts and `NoClassDefFoundError`.
- **TabList Performance**: Refactored `TabListManager` with context pre-computation and upper-triangle iteration, reducing comparison complexity from $O(N^2)$ to $O(N \cdot (N-1) / 2)$.
- **Dynamic Configurable Scoreboards & Hub**: Scoreboard lines and titles now dynamically evaluate from `config.yml` templates with full placeholder support (`{online}`, `{active}`, `{queueing}`, `{slot_name}`, `{mode_id}`, `{players}`, `{max_players}`, `{time_left}`, `{role}`). `/hub` now executes configurable server transfers or commands.
- **Dead Code Pruning**: Removed redundant `SlotType.java` enum and duplicate event listeners in `FreezeManager`.

### Fixed
- **Issue 1.1 (Broken Compass Cycling)**: Fixed multi-runner target switching by parsing 3-part composite slot identifiers (`type:mode:slot`) in `CompassInteractListener`, and added a 2-second block-break debounce.
- **Issue 1.2 (Inventory Loss on Rejoin)**: Added safe inventory restoration on `/rejoin` guarded by `SpyInventories` availability.
- **Issue 1.3 (Solo Speedrun Restriction)**: Permitted 0-hunter singleplayer speedrun configurations in `SpeedrunCommand`.
- **Issue 3.1 (Hologram Main-Thread Lookups)**: Cached player UUID-to-name mappings in `PlayerDataManager` and `HologramManager` on `PlayerJoinEvent`, removing laggy blocking lookups during leaderboard rendering.
- **Issue 4.1 (Hardcoded Cooldown Text)**: Cooldown countdowns and messages now format dynamically based on `config.yml` settings (`quit_cooldown_seconds` and `game_end_cooldown_seconds`).

---

## [1.0.0] - Initial Release

### Added
- Core Manhunt engine supporting dynamic modes (1v1, 1v2, 2v2, custom).
- Multi-world instance slot manager with Overworld, Nether, and The End dimension routing.
- Slot 0 World Factory worker for background world cloning and template management.
- Multi-dimensional hunter tracking compass with lodestone simulation.
- Session isolation via per-match TabList and scoreboard management.
- Multi-file YAML data layer (`players.yml`, `slots.yml`, `lobbies.yml`, `worker.yml`, `leaderboards.yml`).
- Holographic floating leaderboards and cosmetic player badge GUI.
