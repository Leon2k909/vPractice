# vPractice + qRanks - Quick Setup Guide

## 📦 Files Included

- `vPractice-1.0.0.jar` - Main practice plugin
- `qRanks-1.0.0.jar` - Rank management system (optional but recommended)
- `README.md` - Full documentation

## ⚡ Quick Start (5 Minutes)

### Step 1: Install Plugins
1. Stop your server
2. Copy both JAR files to your `plugins/` folder
3. Start your server

### Step 2: Set Locations
1. Stand at your desired spawn location
2. Run: `/setspawn`
3. Stand at your kit editor area
4. Run: `/seteditor`

### Step 3: Create Arenas
For each arena you want:
```
1. /arena create <name>     (e.g., /arena create arena1)
2. Go to Team A spawn point
3. /arena seta <name>       (e.g., /arena seta arena1)
4. Go to Team B spawn point  
5. /arena setb <name>       (e.g., /arena setb arena1)
```

### Step 4: Configure (Optional)
Edit `plugins/vPractice/config.yml` to customize:
- Server name and branding
- ELO settings
- Enabled ladders
- Messages

That's it! Your practice server is ready!

## 🗄️ Storage Options

### JSON (Default - No Setup Required)
Works out of the box. Data stored in `plugins/vPractice/data/`

### MySQL (For Networks)
Edit `config.yml`:
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

## 📋 Quick Commands Reference

| Command | What it does |
|---------|-------------|
| `/setspawn` | Set lobby spawn |
| `/seteditor` | Set kit editor location |
| `/arena create <name>` | Create new arena |
| `/arena seta <name>` | Set spawn A |
| `/arena setb <name>` | Set spawn B |
| `/practice reload` | Reload config |

## 🎮 Player Commands

| Command | What it does |
|---------|-------------|
| `/duel <player>` | Challenge to duel |
| `/party create` | Create party |
| `/stats` | View your stats |
| `/leaderboard` | View rankings |
| `/kit` | Edit your kit |

## ❓ Troubleshooting

**"No arenas available"**
- Create at least one arena with `/arena create`
- Set both spawn points with `/arena seta` and `/arena setb`

**"qRanks not found" warning**
- This is just a warning, plugin works without qRanks
- Install qRanks for rank features

**Players can't queue**
- Ensure spawn is set with `/setspawn`
- Ensure at least one arena is fully configured

## 📞 Support

Join our Discord for help or report issues on BuiltByBit!

---

**Enjoy your free practice server! ❤️**
