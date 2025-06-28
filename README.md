# CTF-Core

![CTF-Core Logo](https://img.shields.io/badge/CTF--Core-v1.0.0-brightgreen) ![Minecraft](https://img.shields.io/badge/Minecraft-1.21+-blue) ![Paper](https://img.shields.io/badge/Paper-Required-orange) ![License](https://img.shields.io/badge/License-Custom-red)

**The Ultimate Capture the Flag Plugin for Minecraft Paper 1.21+**

Transform your server with the most comprehensive CTF plugin available, featuring real-time hologram leaderboards, advanced gameplay mechanics, and professional-grade administration tools.

---

## 🏆 **Key Features**

### 🎮 **Core Gameplay**
- **Multi-Arena Support** - Create unlimited CTF arenas with WorldGuard integration
- **Team-Based Combat** - Red vs Blue team battles with balanced gameplay
- **Flag Mechanics** - Advanced flag capture and return systems with visual feedback
- **Spectator Mode** - Watch games in progress with dedicated spectator features
- **Player Progression** - Level system with XP rewards and persistent statistics

### 📊 **Hologram Leaderboards** ⭐ **NEW!**
- **Real-Time Statistics** - Live hologram displays powered by DecentHolograms
- **Multiple Types** - Kills, Captures, Level, Games Won leaderboards
- **Beautiful Design** - Professional formatting with rank colors (Gold #1, Yellow #2, Red #3)
- **Admin Management** - Create, move, delete, and customize leaderboards anywhere
- **Auto-Updates** - Leaderboards refresh every 30 seconds automatically
- **Persistent Storage** - Leaderboards survive server restarts

### ⚡ **Advanced Features**
- **Power-Up System** - Collectible items that enhance gameplay
- **Kill Streaks** - Reward skilled players with streak bonuses
- **Custom Scoreboards** - Dynamic in-game information display
- **Particle Effects** - Epic visual feedback for all actions
- **Currency Integration** - Vault support for economy rewards
- **PlaceholderAPI** - Extensive placeholder support for other plugins

### 🛠 **Administration Tools**
- **Admin Toolkit** - Visual setup tools with progress tracking
- **Boss Bar Guidance** - Real-time setup progress indicators
- **Comprehensive Commands** - Complete arena and leaderboard management
- **Configuration System** - Highly customizable settings and messages
- **Validation System** - Smart arena validation with helpful error messages

---

## 📋 **Requirements**

### **Required Dependencies**
- **Minecraft Server:** Paper 1.21+ (Spigot compatible but Paper recommended)
- **Java Version:** Java 21+
- **WorldGuard:** Latest version (7.0.10+)
- **WorldEdit:** Latest version (7.3.0+)

### **Optional Dependencies**
- **DecentHolograms:** 2.9.2+ (Required for hologram leaderboards)
- **PlaceholderAPI:** 2.11.5+ (For placeholder support)
- **Vault:** 1.7+ (For economy integration)

---

## 🚀 **Installation**

### **Step 1: Install Dependencies**
1. Download and install **WorldGuard** and **WorldEdit**
2. (Optional) Install **DecentHolograms** for leaderboard features
3. (Optional) Install **PlaceholderAPI** and **Vault** for extended features
4. Restart your server to load dependencies

### **Step 2: Install CTF-Core**
1. Download `CTF-Core.jar` from the releases page
2. Place the jar file in your server's `/plugins/` folder
3. Restart your server
4. Plugin will generate default configuration files

### **Step 3: Basic Configuration**
1. Edit `/plugins/CTF-Core/config.yml` for general settings
2. Modify `/plugins/CTF-Core/messages.yml` for custom messages
3. Reload with `/ctfadmin reload` or restart server

---

## 🎯 **Quick Start Guide**

### **Creating Your First Arena**
```bash
# 1. Create a WorldGuard region for your arena
/rg define myarena

# 2. Create the CTF arena
/ctfadmin create myarena myarena

# 3. Set up the arena (you'll get an admin toolkit)
/ctfadmin setup myarena

# 4. Configure spawn points, flags, and capture zones using the toolkit
# 5. Save the arena when setup is complete
/ctfadmin save myarena
```

### **Creating Hologram Leaderboards**
```bash
# Create a kills leaderboard (top 10)
/ctfadmin leaderboard create kills 10

# Create a level leaderboard (top 5)
/ctfadmin leaderboard create level 5

# List all leaderboards
/ctfadmin leaderboard list

# Move a leaderboard to your location
/ctfadmin leaderboard move kills_1647891234567
```

### **Player Commands**
```bash
# Join a CTF game
/ctf join

# Leave current game
/ctf leave

# View your statistics
/ctf stats
```

---

## 📖 **Commands Reference**

### **Admin Commands (`/ctfadmin`)**
| Command | Description | Permission |
|---------|-------------|------------|
| `create <arena> <region>` | Create new arena with WorldGuard region | `ctf.admin` |
| `setup <arena>` | Get admin toolkit for arena setup | `ctf.admin` |
| `delete <arena>` | Delete an existing arena | `ctf.admin` |
| `setlobby <arena>` | Set lobby spawn point | `ctf.admin` |
| `setspawn <arena> <team> <1-4>` | Set team spawn points | `ctf.admin` |
| `setflag <arena> <team>` | Set flag location | `ctf.admin` |
| `setcapture <arena> <team>` | Set capture point | `ctf.admin` |
| `status <arena>` | View arena configuration status | `ctf.admin` |
| `save <arena>` | Validate and enable arena | `ctf.admin` |
| `list` | List all arenas | `ctf.admin` |
| `reload` | Reload plugin configuration | `ctf.admin` |
| `forcestart <arena>` | Force start a game | `ctf.admin` |

### **Leaderboard Commands (`/ctfadmin leaderboard`)**
| Command | Description | Permission |
|---------|-------------|------------|
| `create <type> [size]` | Create hologram leaderboard | `ctf.admin` |
| `delete <id>` | Delete leaderboard | `ctf.admin` |
| `move <id>` | Move leaderboard to your location | `ctf.admin` |
| `list` | List all leaderboards | `ctf.admin` |
| `reload` | Force update all leaderboards | `ctf.admin` |

**Leaderboard Types:** `kills`, `captures`, `level`, `games_won`

### **Player Commands (`/ctf`)**
| Command | Description | Permission |
|---------|-------------|------------|
| `join [arena]` | Join a CTF game | `ctf.play` |
| `leave` | Leave current game | `ctf.play` |
| `stats [player]` | View statistics | `ctf.play` |

---

## ⚙️ **Configuration**

### **Main Configuration (`config.yml`)**
```yaml
# Game Settings
game:
  min-players: 4
  max-players: 20
  countdown-time: 30
  game-duration: 600
  respawn-delay: 5

# Experience System
experience:
  per-kill: 10
  per-capture: 50
  per-flag-return: 25
  level-up-base-xp: 100
  level-up-multiplier: 50

# Power-ups
powerups:
  enabled: true
  spawn-chance: 0.3
  duration: 30
```

### **Leaderboard Configuration (`leaderboards.yml`)**
```yaml
settings:
  update-interval: 30
  default-size: 5
  max-size: 15
  auto-cleanup: true

# Leaderboards are automatically managed
# Manual editing not recommended
```

---

## 🎨 **Hologram Leaderboard Design**

Leaderboards feature professional formatting with rank-based colors:

```
§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
§e§l🏆 TOP 5 TOP KILLS 🏆
§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬

§6#1 §fPlayerName §7- §6123
§e#2 §fPlayer2 §7- §e98
§c#3 §fPlayer3 §7- §c76
§7#4 §fPlayer4 §7- §754
§7#5 §fPlayer5 §7- §732

§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
```

**Color Scheme:**
- **Rank 1:** Gold (§6)
- **Rank 2:** Yellow (§e)
- **Rank 3:** Red (§c)
- **Rank 4+:** Gray (§7)

---

## 🛡️ **Permissions**

| Permission | Description | Default |
|------------|-------------|---------|
| `ctf.admin` | Access to all admin commands | OP |
| `ctf.play` | Permission to join CTF games | True |
| `ctf.chat.colors` | Use color codes in chat | OP |

---

## 🔧 **API & Integration**

### **PlaceholderAPI Placeholders**
```
%ctf_player_level% - Player's current level
%ctf_player_kills% - Total kills
%ctf_player_deaths% - Total deaths
%ctf_player_captures% - Total captures
%ctf_player_kd% - Kill/Death ratio
%ctf_arena_status% - Current arena status
```

### **Vault Economy Integration**
- Reward players with money for kills, captures, and wins
- Configurable reward amounts in `config.yml`
- Requires Vault and compatible economy plugin

---

## 🐛 **Troubleshooting**

### **Common Issues**

**Leaderboards not showing:**
- Ensure DecentHolograms is installed and enabled
- Check console for dependency warnings
- Verify leaderboard creation commands were successful

**Arena setup issues:**
- Confirm WorldGuard region exists
- Ensure player has proper permissions
- Check arena validation with `/ctfadmin status <arena>`

**Players can't join games:**
- Verify arena is enabled with `/ctfadmin list`
- Check minimum player requirements in config
- Ensure players have `ctf.play` permission

### **Getting Help**
1. Check the console for error messages
2. Review configuration files for syntax errors
3. Use `/ctfadmin status <arena>` for arena diagnostics
4. Enable debug mode in config for detailed logging

---

## 📊 **Statistics Tracking**

CTF-Core tracks comprehensive player statistics:

- **Kills** - Total player eliminations
- **Deaths** - Times eliminated by other players
- **Captures** - Successful flag captures
- **Flag Returns** - Enemy flags returned to base
- **Games Played** - Total games participated in
- **Games Won** - Victories achieved
- **Level** - Current progression level
- **Experience** - XP points earned
- **K/D Ratio** - Kill to death ratio
- **Win Rate** - Percentage of games won

All statistics persist across server restarts and are used for leaderboard calculations.

---

## 🔄 **Updates & Changelog**

### **Version 1.0.0 - Initial Release**
- Complete CTF gamemode implementation
- Multi-arena support with WorldGuard integration
- Real-time hologram leaderboard system
- Player progression with levels and XP
- Power-up system and kill streaks
- Comprehensive admin toolkit
- PlaceholderAPI and Vault integration
- Professional visual effects and messaging

---

## 📄 **License**

This plugin is released under a custom license. You are free to:
- ✅ Use on your server (personal or commercial)
- ✅ Modify configuration files
- ✅ Request feature additions or modifications

You may not:
- ❌ Redistribute or resell this plugin
- ❌ Decompile or reverse engineer the code
- ❌ Claim this work as your own

---

## 🙏 **Credits & Acknowledgments**

**Created by:** CWReSports Team

**Dependencies:**
- [WorldGuard](https://enginehub.org/worldguard) by EngineHub
- [WorldEdit](https://enginehub.org/worldedit) by EngineHub
- [DecentHolograms](https://github.com/DecentSoftware-eu/DecentHolograms) by DecentSoftware
- [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) by PlaceholderAPI Team
- [Vault](https://github.com/MilkBowl/Vault) by MilkBowl

**Special Thanks:**
- Paper development team for the excellent server software
- Bukkit/Spigot community for extensive documentation and support
- Beta testers who helped refine the gameplay experience

---

## 📞 **Support**

For support, bug reports, or feature requests:
- Create an issue on our GitHub repository
- Join our Discord community
- Post in the SpigotMC discussion thread

**Please include:**
- Plugin version
- Server version (Paper/Spigot)
- Relevant error messages or logs
- Steps to reproduce any issues

---

*Transform your server with CTF-Core - Where competition meets innovation!* 🏆