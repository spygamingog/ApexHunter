# SpyHunts 🎯

[![Modrinth](https://img.shields.io/modrinth/v/spyhunts?label=Modrinth&logo=modrinth&color=green)](https://modrinth.com/plugin/spyhunts)
[![GitHub Release](https://img.shields.io/github/v/release/spygamingog/SpyHunts)](https://github.com/spygamingog/SpyHunts/releases)
![License](https://img.shields.io/github/license/spygamingog/SpyHunts)
![Java](https://img.shields.io/badge/Java-21%2B-orange)
![Paper](https://img.shields.io/badge/Paper-1.21.11%2B-blue)

A professional manhunt plugin for Paper servers featuring automated queues, GUI interface, lobby management, and intelligent rejoin functionality.

## 📋 Features

### 🎮 **Intuitive GUI System**
- Clean GUI with three game modes: **1v1**, **1v2**, **2v4**
- Real-time status display (Available/Occupied/Running/Unavailable)
- Live player count and queue size indicators

### ⚡ **Automated Matchmaking**
- Smart queue system with configurable team sizes
- 30-second countdown when minimum players reached
- Automatic cancellation if players leave before start
- Instant teleport to assigned lobbies

### 🏰 **Lobby Management**
- Dynamic lobby assignment and creation
- Automatic assignment to slots missing lobbies
- Works with existing Multiverse worlds
- Configurable world names per slot

### 🔄 **Smart Player Handling**
- **Rejoin System**: Leave and return to exact position
- Persistent player state tracking
- Temporary leave with `/manhuntleave`
- Return to match with `/manhuntrejoin`

### 🛠️ **Administrative Controls**
- Complete command suite for server management
- Configurable via YAML configuration
- Permission system for different access levels
- Graceful match stopping and cleanup

## 🚀 Quick Start

### Installation
1. Download the latest jar from [Modrinth](https://modrinth.com/plugin/spyhunts) or [GitHub Releases](https://github.com/spygamingog/SpyHunts/releases)
2. Place `SpyHunts.jar` in your server's `plugins/` folder
3. Restart your server
4. Configure as needed (see Configuration below)

### Basic Setup
```bash
# Set main lobby location
/manhuntlobby set

# Create game lobbies
/manhunt lobby create

# Open GUI for players
/manhunt
```
## 📖 Commands

### Player Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/manhunt` | Open the Manhunt GUI | `spyhunts.use` |
| `/manhuntlobby` | Teleport to main manhunt lobby | `spyhunts.use` |
| `/manhuntleave` | Leave active match temporarily | `spyhunts.use` |
| `/manhuntrejoin` | Rejoin your active match | `spyhunts.use` |

### Admin Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/manhunt lobby create` | Create and assign a new lobby | `spyhunts.lobby` |
| `/manhunt lobby list` | List all available lobbies | `spyhunts.lobby` |
| `/manhunt lobby remove <name>` | Remove a lobby | `spyhunts.lobby` |
| `/manhunt stop` | Stop an active match | `spyhunts.admin` |
| `/manhuntlobby set` | Set the main lobby location | `spyhunts.admin` |

## ⚙️ Configuration

Default `config.yml`:

```yaml
# Slot configurations
1v1:
  min_players: 2
  max_players: 2
  world_name: "1v1_manhunt"
  
1v2:
  min_players: 3
  max_players: 3
  world_name: "1v2_manhunt"
  
2v4:
  min_players: 6
  max_players: 6
  world_name: "2v4_manhunt"

# General settings
countdown_seconds: 30
main_lobby:
  world: "world"
  x: 0
  y: 64
  z: 0
```
## ⚙️ Configurable Options

- **Team sizes and player limits**: Adjust min/max players for each slot
- **World names for each game mode**: Customize world names for 1v1, 1v2, and 2v4 modes
- **Countdown duration**: Change the 30-second default countdown timer
- **Main lobby location**: Configure the main spawn point for players
- **Slot-specific settings**: Each game mode can have unique configurations

## 📦 Dependencies

- **Paper API 1.21.11** (Required)
- **Java 21+** (Required)
- **Multiverse-Core** (Required, for world management)

## 🐛 Reporting Issues

Found a bug? Please open an issue with:

1. **Description of the problem**
2. **Steps to reproduce**
3. **Expected vs actual behavior**
4. **Server logs** (if applicable)

## 📄 License

This project is licensed under the **MPL-2.0 License**.

## 📞 Support

- **GitHub Issues**: [Report bugs or request features](https://github.com/spygamingog/SpyHunts/issues)
- **Modrinth**: [Download and rate](https://modrinth.com/plugin/spyhunts)
- **Discord**: [Join our community](https://discord.gg/fzcGYV7E9Q) (Optional)

## 🙏 Acknowledgements

- **PaperMC** for the excellent server software
- **Spigot/Paper API** developers
- All **contributors** and **testers**
- The **Minecraft plugin development** community

## 📊 Status Indicators

- **🟢 Available**: Open for queueing
- **🟡 Occupied**: Queue is full
- **🔴 Running**: Game in progress
- **⚫ Unavailable**: Admin-stopped

---

**Made with ❤️ for the Minecraft community**
