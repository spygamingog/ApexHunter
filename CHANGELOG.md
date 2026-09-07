# Changelog

All notable changes to **SpyHunts** are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.0.0] - 2026-09-08

### Changed
- **Dependency Migration**: Upgraded core dependency `spycore` to `1.1.2` (supporting enhanced multi-world containers, safe teleports, and hibernation controls).
- **Documentation Overhaul**: Created comprehensive developer architecture documentation (`project_brain.md`), technical manual (`DOCUMENTATION.md`), public showcase (`README.md`), and in-depth code audit (`ISSUES_AND_FLAWS.md`).
- **Repository Cleanliness**: Configured clean `.gitignore` to maintain internal agent docs locally while tracking only public distribution files. Purged stale build and compilation logs.

### Fixed
- **Slot 0 Worker Deadlock**: Resolved critical server hang caused by nested synchronous schedulers in Slot 0 world creation by introducing a safe `CountDownLatch` pattern in `runSync`.
- **World Deletion Pipeline**: Streamlined slot world cleanup by leveraging unified asynchronous `SpyAPI.deleteWorld` operations with OS file lock release delays.
- **Version Alignment**: Corrected startup banner and configuration discrepancies to reflect `2.0.0` accurately.

---

## [1.0.0] - Initial Release

### Added
- Core Manhunt engine supporting dynamic modes (1v1, 1v2, 2v2, custom).
- Multi-world instance slot manager with Overworld, Nether, and The End dimension routing.
- Slot 0 World Factory worker with Chunky background pre-generation.
- Multi-dimensional hunter tracking compass with lodestone simulation.
- Session isolation via per-match TabList and scoreboard management.
- Multi-file YAML data layer (`players.yml`, `slots.yml`, `lobbies.yml`, `worker.yml`, `leaderboards.yml`).
- Holographic floating leaderboards and cosmetic player badge GUI.
