<div align="center">

# 🏹 SpyHunts

**The Ultimate Competitive Manhunt, Speedrun & Practice Engine for Minecraft**

[![Paper](https://img.shields.io/badge/Paper-1.21.1+-00b4d8.svg?style=for-the-badge&logo=minecraft)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg?style=for-the-badge&logo=openjdk)](https://adoptium.net)
[![License](https://img.shields.io/badge/License-MPL--2.0-blue.svg?style=for-the-badge)](LICENSE)
[![Version](https://img.shields.io/badge/Version-2.0.0-emerald.svg?style=for-the-badge)]()
[![Powered by](https://img.shields.io/badge/Powered%20By-SpyCore-purple.svg?style=for-the-badge)](https://github.com/spygamingog/SpyCore)

*Engineered for high-performance networks, competitive esports events, and community servers.*

[Features](#-key-features) • [Installation](#-installation) • [Commands](#-commands) • [Permissions](#-permissions) • [Configuration](#-configuration)

---

</div>

## 🌟 Key Features

### ⚔️ Multi-Game Modes
- **Classic Manhunt**: 1v1, 1v2, 1v3, 2v2, 2v4, and custom team setups. Runners attempt to slay the Ender Dragon while hunters track them down.
- **Speedrun Race**: Multi-runner competitive speedrun races with PvP disabled. The first runner to finish or reach the highest advancement wins on timeout!
- **Practice Arenas**: Zero-stress practice modes for Manhunt and Speedrun with respawn mechanics, allowing players to practice nether bastions, fortresses, and dragon fights without match penalties.

### ⚡ Slot 0 Zero-Lag World Factory
- **Instant Matches**: Background worker generates fresh Overworld, Nether, and End worlds ahead of time.
- **No Generation Lag**: Chunk pre-generation and asynchronous template cloning eliminate the dreaded tick-drops when matches start.
- **Safe Hibernation**: Active arenas are automatically whitelisted from server hibernation engines via `SpyCore`.

### 🧭 Cross-Dimensional Tracking Compass
- **Multi-Dimension Tracking**: When runners enter the Nether or The End, hunter compasses dynamically point to the exact portal the runner used.
- **Fast Refresh & Target Cycling**: Left-click to instantly refresh tracking; right-click to cycle between active speedrunners in multi-runner matches.
- **Lodestone & Vanilla Compass Support**: Smooth compass needle orientation in all three dimensions.

### 🛡️ Session Isolation & Player Management
- **Per-Match Tablist Isolation**: Players only see their opponents and teammates in Tab and chat.
- **Dynamic Scoreboards**: Real-time stats, game timers, role indicators, and queue counters tailored for Lobby, Queue, and In-Game states.
- **Pre-Game Freeze Phase**: Configurable 10x10 boundary confinement during match warmup in Adventure mode.
- **Spectator Integration**: Smooth spectator system allowing eliminated players or admins to spectate active sessions seamlessly.

### 🏆 Titles, Badges & Holograms
- **Custom Player Badges**: Unlockable cosmetic prefixes (`[PRO]`, `[GOD]`, `[ELITE]`, `[LEGEND]`) managed via an interactive GUI.
- **Holographic Leaderboards**: Display top players by wins, losses, or fastest speedrun completion times.

---

## 📥 Installation

### Requirements
- **Server Software**: [Paper](https://papermc.io), [Purpur](https://purpurmc.org), or compatible 1.21.1+ fork.
- **Java**: Java 21 or newer.
- **Core Dependency**: **[SpyCore](https://github.com/spygamingog/SpyCore)** 1.1.2+ (Required for multi-world container management).
- **Recommended Companions**:
  - **[SpyInventories](https://github.com/spygamingog/SpyCore)** (Authoritative companion for multi-world player inventory sync).
  - **[SpyNetherPortals](https://github.com/spygamingog/SpyCore)** (Cross-dimensional portal routing).

### Setup Instructions
1. Download `spyhunts-2.0.0.jar` and place it into your server's `plugins/` directory.
2. Ensure `SpyCore.jar` is also in your `plugins/` directory.
3. Start your server to generate the default configuration files.
4. Set your main lobby spawn using `/lobby set`.
5. Create waiting lobbies using `/lobby create`.
6. Add modes and slots or use the preconfigured ones (`/manhunt add mode 1v1`, `/manhunt add 1v1 slot1`).
7. Open the interactive matchmaking GUI with `/manhunt` or `/mh`!

---

## 🎮 Commands

### Player Commands
| Command | Alias | Description |
| :--- | :--- | :--- |
| `/manhunt` | `/mh` | Open the Manhunt mode & slot selection GUI |
| `/speedrun` | — | Open the Speedrun race selection GUI |
| `/practice` | — | Open the Practice mode selection GUI |
| `/leave` | — | Leave current queue and return to the main lobby |
| `/rejoin` | — | Rejoin your active manhunt or speedrun match |
| `/quit` | — | Forfeit current game and return to lobby (applies cooldown) |
| `/compass` | — | Receive a tracking compass (Hunter only) |
| `/stats [player]` | — | View your or another player's competitive stats |
| `/badge` | `/title`, `/badges` | Open the title & badge selection GUI |
| `/lobby` | — | Teleport to the main manhunt lobby |
| `/hub` | — | Return to the network hub server |

### Administrator Commands
| Command | Permission | Description |
| :--- | :--- | :--- |
| `/manhunt add mode <id> [min] [max]` | `manhunt.admin` / `spyhunts.admin` | Register a new Manhunt mode (e.g. `1v1`, `2v2`) |
| `/manhunt add <mode> <slot>` | `manhunt.admin` / `spyhunts.admin` | Add an instance slot to a mode |
| `/manhunt remove <mode> [slot]` | `manhunt.admin` / `spyhunts.admin` | Remove an instance slot or entire mode |
| `/manhunt stop <mode> <slot>` | `manhunt.admin` / `spyhunts.admin` | Force-terminate an active match |
| `/manhunt start <mode> <slot>` | `manhunt.admin` / `spyhunts.admin` | Force-start a queued match |
| `/manhunt skip start <mode> <slot>` | `manhunt.admin` / `spyhunts.admin` | Force-start immediately skipping warmup freeze |
| `/manhunt cooldown <player> <sec>` | `manhunt.admin` / `spyhunts.admin` | Apply match cooldown to a player |
| `/manhunt removecooldown <player>` | `manhunt.admin` / `spyhunts.admin` | Remove match cooldown from a player |
| `/lobby set` | `manhunt.admin` / `spyhunts.admin` | Set main lobby spawn at current position |
| `/lobby create` | `manhunt.admin` / `spyhunts.admin` | Create a new waiting lobby at current position |
| `/lobby delete <name>` | `manhunt.admin` / `spyhunts.admin` | Delete a waiting lobby |
| `/lobby setup` | `manhunt.admin` / `spyhunts.admin` | Automatically bind available lobbies to slots |
| `/worker status` | `manhunt.admin` / `spyhunts.admin` | View background world factory generation status |
| `/worker pause` / `/worker resume` | `manhunt.admin` / `spyhunts.admin` | Pause or resume background world worker |
| `/worker reset` | `manhunt.admin` / `spyhunts.admin` | Reset worker batch state and clean Slot 0 templates |
| `/worker force <type:mode:slot>` | `manhunt.admin` / `spyhunts.admin` | Force-generate worlds for a specific slot |
| `/status <type> <mode> <slot> <status>` | `manhunt.admin` / `spyhunts.admin` | Manually set slot status (`available`/`unavailable`) |
| `/badge give <player> <title>` | `apexhunter.badge.admin` / `spyhunts.admin` | Grant a badge title to a player |
| `/badge set <player> <title>` | `apexhunter.badge.admin` / `spyhunts.admin` | Set a player's active badge title |
| `/badge remove <player>` | `apexhunter.badge.admin` / `spyhunts.admin` | Clear a player's active badge title |
| `/leaderboard create <id> <type> <mode>`| `apexhunter.admin` / `spyhunts.admin` | Spawn a floating leaderboard hologram |
| `/leaderboard delete <id>` | `apexhunter.admin` / `spyhunts.admin` | Remove a leaderboard hologram |

---

## 🔑 Permissions

| Permission Node | Description | Default |
| :--- | :--- | :--- |
| `spyhunts.admin` | Wildcard granting full administrative control across all subcommands | `op` |
| `manhunt.admin` | Administrative control over Manhunt games & lobbies | `op` |
| `speedrun.admin` | Administrative control over Speedrun modes & slots | `op` |
| `practice.admin` | Administrative control over Practice modes & slots | `op` |
| `manhunt.admin.build` | Bypass lobby build/break protection | `op` |
| `manhunt.admin.fly` | Flight permission in lobby worlds | `op` |
| `apexhunter.badge.admin` | Permission to give, set, and remove player titles | `op` |
| `apexhunter.admin` | Permission to manage leaderboard holograms | `op` |
| `spyhunts.admin.stats` | Permission to reset player statistics | `op` |

---

## ⚙️ Configuration

`SpyHunts` comes with a comprehensive, well-documented `config.yml`:

```yaml
# ==========================================================
#                  SpyHunts Configuration
# ==========================================================

# Main Lobby Settings
lobby_enabled: true
main_lobby:
  world: "world"
  x: 0.5
  y: 64.0
  z: 0.5
  yaw: 0.0
  pitch: 0.0

# General Game Settings
countdown_seconds: 30
quit_cooldown_seconds: 7200       # Cooldown after quitting an active game (2 hours)
game_end_cooldown_seconds: 1800   # Cooldown after match completes normally (30 min)
freeze_before_start: true
freeze_duration_seconds: 120

# World Factory (Background Worker)
factory:
  paused: true                   # Worker starts paused on boot for safety
  wait_minutes: 15               # Rest cycle between batch checks

# Slot Management
slot_count_per_mode: 3           # Target available slots per mode
default_slots_per_mode: 3

# Compass Tracking Settings
compass_update_interval: 20      # Compass target sync interval (in ticks)
movement_update_threshold: 4.0   # Block distance threshold before lodestone update

# Hub Transfer
hub:
  command: "server hub"

# Scoreboards
scoreboards:
  main_lobby:
    title: "&6&lSPY HUNTS"
    lines:
      - "&7----------------"
      - "&fOnline Players: &e{online}"
      - "&fActive Games: &e{active}"
      - "&fQueueing Games: &e{queueing}"
      - "&7----------------"
```

---

## 📄 License

SpyHunts is open-source software licensed under the **[Mozilla Public License 2.0 (MPL-2.0)](https://www.mozilla.org/en-US/MPL/2.0/)**.
Developed with ❤️ by **SpyGamingOG**.
