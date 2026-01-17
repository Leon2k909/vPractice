# vPractice - Professional PvP Practice Plugin

![Version](https://img.shields.io/badge/version-1.0.0-blue)
![Minecraft](https://img.shields.io/badge/minecraft-1.8.x--1.20.x-green)
![License](https://img.shields.io/badge/license-Free-brightgreen)

A feature-rich, production-ready PvP practice plugin inspired by popular practice servers. **Free and open source!**

## ✨ Features

### Core Features
- **Ranked & Unranked Queues** - ELO-based matchmaking system
- **Multiple Ladders** - NoDebuff, Combo, Gapple, Sumo, Archer (more can be added)
- **Kit Editor** - Players can customize their loadouts
- **Party System** - Create parties for 2v2, FFA, and party vs party matches
- **Spectator Mode** - Watch ongoing matches
- **Leaderboards** - Track top players per ladder and globally
- **Statistics** - Wins, losses, ELO, win streaks, and more

### Quality of Life
- **Rematch System** - Quick rematches after duels
- **Duel Requests** - Challenge any player directly
- **Announcements** - Automated tips and messages
- **Settings Menu** - Toggle scoreboard, duel requests, etc.

### Staff Features
- **Mod Mode** - Vanish, teleport, freeze players
- **Arena Management** - Create/edit arenas in-game
- **Admin Commands** - Full control over the plugin

### Technical Features
- **Dual Storage** - JSON (file-based) or MySQL (database)
- **Performance Optimized** - Async operations, caching, batching
- **Fully Configurable** - Every message and setting customizable
- **qRanks Integration** - Optional rank prefixes and permissions

## 📦 Installation

1. Download `vPractice-1.0.0.jar` and `qRanks-1.0.0.jar`
2. Place both JARs in your `plugins/` folder
3. Start/restart your server
4. Configure `plugins/vPractice/config.yml` as needed
5. Set up spawn with `/setspawn` and editor with `/seteditor`
6. Create arenas with `/arena create <name>`

## 📝 Commands

### Player Commands
| Command | Description |
|---------|-------------|
| `/duel <player>` | Challenge a player to a duel |
| `/accept <player>` | Accept a duel request |
| `/decline <player>` | Decline a duel request |
| `/party <create/invite/accept/leave/kick/disband>` | Party management |
| `/spectate <player>` | Spectate a player's match |
| `/stats [player]` | View player statistics |
| `/leaderboard [ladder]` | View leaderboards |
| `/elo [player]` | View ELO ratings |
| `/kit` | Open kit editor |
| `/leave` | Leave queue/match/spectating |
| `/rematch` | Request/accept rematch |
| `/ping [player]` | Check ping |

### Admin Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/setspawn` | Set spawn location | `vpractice.admin` |
| `/seteditor` | Set editor location | `vpractice.admin` |
| `/arena <create/delete/list/tp>` | Arena management | `vpractice.admin` |
| `/practice <reload/save>` | Plugin management | `vpractice.admin` |
| `/mod [vanish]` | Toggle mod mode | `vpractice.staff` |

## ⚙️ Configuration

### Storage Options

**JSON (Default)** - Simple file-based storage, works out of the box:
```yaml
storage:
  type: json
```

**MySQL** - For networks or larger servers:
```yaml
storage:
  type: mysql
  mysql:
    host: localhost
    port: 3306
    database: vpractice
    username: root
    password: "your_password"
```

### Key Settings

```yaml
# ELO System
elo:
  default: 1000
  k-factor:
    new-player: 40
    normal: 25
    veteran: 16
  minimum: 100

# Queue Settings
queue:
  process-interval: 5
  initial-elo-range: 100
  elo-range-expansion: 10

# Match Settings
match:
  countdown: 3
  end-delay: 100
  pearl-cooldown: 16

# Scoreboard
scoreboard:
  enabled: true
  title: "&6Your Server &7⏐ &fPractice"
```

## 🗺️ Arena Setup

1. Build your arena with two spawn points
2. Run `/arena create <name>`
3. Stand at team A's spawn and run `/arena seta <name>`
4. Stand at team B's spawn and run `/arena setb <name>`
5. (Optional) Set bounds with `/arena setmin <name>` and `/arena setmax <name>`

**For Sumo arenas**, add the `sumo` flag:
- Edit the config directly under `arenas.<name>.sumo: true`

## 🔗 qRanks Integration

qRanks is included for rank/prefix management. It's optional but recommended.

### qRanks Features
- Beautiful MineHQ-style rank GUI
- Grant history tracking
- Permission management
- Tab prefixes and colors

## 📁 File Structure

```
plugins/
├── vPractice/
│   ├── config.yml          # Main configuration
│   ├── elo.yml              # ELO data (legacy)
│   └── data/
│       ├── elo.json         # ELO data (JSON storage)
│       ├── stats.json       # Player statistics
│       ├── kits.json        # Custom kit layouts
│       └── settings.json    # Player settings
└── qRanks/
    ├── config.yml           # qRanks config
    └── (MySQL data)
```

## 🐛 Troubleshooting

**Plugin doesn't load?**
- Check console for errors
- Ensure Java 8+ is installed
- Verify Spigot/Paper 1.8.8+ is being used

**MySQL not connecting?**
- Verify credentials in config
- Ensure MySQL server is running
- Check if database exists

**Arenas not working?**
- Ensure spawns are set with `/arena seta` and `/arena setb`
- Check that the arena is enabled in config

## 📄 License

This plugin is **free and open source**. You may:
- Use it on any server
- Modify the code
- Share it with others

Please credit "Vera Network" if redistributing.

## 🤝 Support

- **Discord**: Join our community for help
- **Issues**: Report bugs on the BuiltByBit page
- **Wiki**: Check the documentation for detailed guides

---

**Made with ❤️ by Vera Network**

*Enjoy your free practice plugin!*
