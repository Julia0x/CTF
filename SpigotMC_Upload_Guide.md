# SpigotMC.org Plugin Upload Guide - CTF-Core

## Complete SpigotMC Submission Form Data

### **Basic Information Tab**

**Plugin Name:** CTF-Core
**Plugin Version:** 1.0.0
**Plugin Description (Short):**
```
Complete Capture the Flag gamemode with hologram leaderboards, levels, power-ups, and epic gameplay for Paper 1.21+
```

**Plugin Description (Detailed):**
```
🏆 **CTF-Core** - The Ultimate Capture the Flag Experience

Transform your server with the most comprehensive CTF plugin available! CTF-Core delivers an action-packed, feature-rich Capture the Flag gamemode designed for Paper 1.21+ servers.

## ✨ **Key Features**

### 🎮 **Core Gameplay**
- **Multi-Arena Support** - Create unlimited CTF arenas with WorldGuard integration
- **Team-Based Combat** - Red vs Blue team battles with balanced gameplay
- **Flag Mechanics** - Advanced flag capture and return systems
- **Spectator Mode** - Watch games in progress with dedicated spectator features

### 📊 **Hologram Leaderboards** ⭐ **NEW!**
- **Real-Time Statistics** - Live hologram displays powered by DecentHolograms
- **Multiple Leaderboard Types** - Kills, Captures, Level, Games Won
- **Admin Management** - Create, move, and customize leaderboards anywhere
- **Auto-Updates** - Leaderboards refresh every 30 seconds
- **Beautiful Design** - Professional formatting with rank colors

### 🎯 **Player Progression**
- **Level System** - Gain XP and level up through gameplay
- **Persistent Statistics** - Track kills, deaths, captures, and win rates
- **Player Data Management** - Comprehensive statistics tracking

### ⚡ **Power-Ups & Features**
- **Power-Up System** - Collectible items that enhance gameplay
- **Kill Streaks** - Reward skilled players with streak bonuses
- **Currency System** - Vault integration for rewards
- **Custom Scoreboards** - Dynamic in-game information display

### 🛠 **Administration Tools**
- **Admin Toolkit** - Powerful setup and management tools
- **WorldGuard Integration** - Seamless region protection
- **Comprehensive Commands** - Easy arena creation and management
- **Configuration System** - Highly customizable settings

### 🎨 **Visual Effects**
- **Particle Effects** - Epic visual feedback for actions
- **Custom Messages** - Fully configurable chat messages
- **Title Displays** - Dramatic announcements and notifications
- **Boss Bars** - Real-time game status indicators

### 🔧 **Technical Features**
- **Paper 1.21+ Optimized** - Built for the latest Minecraft versions
- **PlaceholderAPI Support** - Extensive placeholder integration
- **Multi-World Support** - Run CTF games across different worlds
- **Performance Optimized** - Efficient code for smooth gameplay
- **Developer API** - Extensible for custom integrations

## 📋 **Requirements**
- **Server:** Paper 1.21+ (Recommended)
- **Dependencies:** WorldGuard, WorldEdit
- **Optional:** PlaceholderAPI, Vault, DecentHolograms (for leaderboards)

## 🚀 **Getting Started**
1. Install the plugin and dependencies
2. Create your first arena with `/ctfadmin create <name> <region>`
3. Set up spawn points, flags, and capture zones
4. Add hologram leaderboards with `/ctfadmin leaderboard create <type>`
5. Players join with `/ctf join`

## 💬 **Commands Overview**
- **Admin:** `/ctfadmin` - Complete arena management
- **Player:** `/ctf` - Join games and view stats
- **Leaderboards:** `/ctfadmin leaderboard` - Manage hologram displays

Perfect for **PvP servers**, **minigame networks**, and **community servers** looking to add competitive team-based gameplay!

Join thousands of servers already using CTF-Core for the ultimate Capture the Flag experience! 🎯
```

**Category:** PvP
**Sub-Category:** Minigames

### **Technical Information Tab**

**Minecraft Version Compatibility:**
- ✅ 1.21
- ✅ 1.21.1
- ✅ 1.21.2 (Latest)

**Server Software:**
- ✅ Paper (Recommended)
- ✅ Spigot
- ❌ Bukkit (Not Supported - requires Paper/Spigot)

**Dependencies:**
```
Required:
- WorldGuard (Latest)
- WorldEdit (Latest)

Optional:
- PlaceholderAPI (2.11.5+)
- Vault (1.7+)
- DecentHolograms (2.9.2+) - Required for hologram leaderboards
```

**Java Version:** 21+

**Plugin Size:** ~2-5 MB

### **Pricing & Licensing Tab**

**Price:** FREE
**License Type:** Custom License
**License Terms:**
```
CTF-Core Plugin License Agreement

1. USAGE RIGHTS
- Free for personal and commercial server use
- No redistribution without permission
- No reverse engineering or decompilation

2. SUPPORT
- Community support via SpigotMC discussion
- Bug reports welcome on resource page

3. LIABILITY
- Plugin provided "as-is" without warranty
- Author not liable for any server issues

4. MODIFICATIONS
- Server owners may request custom features
- No modification of plugin files without permission
```

### **Media Tab**

**Plugin Icon/Logo:** Upload a 64x64 PNG with CTF-themed design (crossed flags, trophy, etc.)

**Screenshots Required:**
1. **In-Game CTF Arena** - Players battling with flags visible
2. **Hologram Leaderboards** - Multiple leaderboard types displayed
3. **Admin Setup Interface** - Arena creation process
4. **Scoreboard & GUI** - Player interface and statistics
5. **Power-ups & Effects** - Visual effects in action

**Video Trailer (Optional):**
```
Title: "CTF-Core - Ultimate Capture the Flag Plugin"
Length: 2-3 minutes
Content:
- Arena showcase
- Hologram leaderboards demo
- Gameplay footage
- Admin setup walkthrough
```

### **Additional Information Tab**

**Tags (Maximum 10):**
```
ctf, capture-the-flag, pvp, minigames, leaderboards, holograms, teams, arena, worldguard, paper
```

**External Links:**
```
Documentation: [Your documentation site]
GitHub: [Your GitHub repository - if public]
Discord: [Your Discord server - if available]
Support: [Support contact information]
```

**Installation Instructions:**
```
🔧 INSTALLATION GUIDE

1. DEPENDENCIES SETUP
   - Install WorldGuard and WorldEdit on your server
   - (Optional) Install PlaceholderAPI, Vault, and DecentHolograms
   
2. PLUGIN INSTALLATION
   - Download CTF-Core.jar
   - Place in your server's /plugins/ folder
   - Restart your server

3. BASIC CONFIGURATION
   - Edit /plugins/CTF-Core/config.yml as needed
   - Configure messages in /plugins/CTF-Core/messages.yml

4. CREATE YOUR FIRST ARENA
   - Create a WorldGuard region for your arena
   - Use: /ctfadmin create <arena-name> <region-name>
   - Follow the setup prompts with the admin toolkit

5. SETUP HOLOGRAM LEADERBOARDS (Optional)
   - Ensure DecentHolograms is installed
   - Use: /ctfadmin leaderboard create <type> [size]
   - Types: kills, captures, level, games_won

6. TESTING
   - Join your arena: /ctf join
   - Test all features and report any issues

For detailed setup guides and troubleshooting, visit our documentation!
```

**Configuration Examples:**
```
📝 EXAMPLE CONFIGURATIONS

# Basic Arena Setup
1. /ctfadmin create myarena myregion
2. /ctfadmin setlobby myarena
3. /ctfadmin setspawn myarena red 1
4. /ctfadmin setspawn myarena blue 1
5. /ctfadmin setflag myarena red
6. /ctfadmin setcapture myarena red
7. /ctfadmin save myarena

# Hologram Leaderboards
1. /ctfadmin leaderboard create kills 10
2. /ctfadmin leaderboard create level 5
3. /ctfadmin leaderboard list
4. /ctfadmin leaderboard move kills_1647891234567

# Player Commands
- /ctf join - Join available game
- /ctf leave - Leave current game
- /ctf stats - View your statistics
```

### **Update History Tab**

**Version 1.0.0 - Initial Release**
```
🎉 CTF-Core Initial Release

✅ CORE FEATURES
- Complete CTF gamemode implementation
- Multi-arena support with WorldGuard integration
- Team-based gameplay (Red vs Blue)
- Flag capture and return mechanics
- Player progression system with levels and XP

🆕 HOLOGRAM LEADERBOARDS
- Real-time statistics display
- Support for Kills, Captures, Level, Games Won
- Admin management commands
- Auto-updating every 30 seconds
- Professional formatting with rank colors

⚡ GAMEPLAY FEATURES
- Power-up system
- Kill streak rewards
- Spectator mode
- Currency integration (Vault)
- Custom scoreboards

🛠 ADMIN TOOLS
- Comprehensive setup toolkit
- Arena creation and management
- Configuration system
- Player data management

🎨 VISUAL EFFECTS
- Particle effects
- Custom messages and titles
- Boss bar displays
- PlaceholderAPI integration

📋 TECHNICAL
- Paper 1.21+ optimized
- Multi-world support
- Performance optimized
- Extensive configuration options
```

### **Support Information**

**Support Method:** SpigotMC Discussion + Discord (if available)

**Response Time:** 24-48 hours for bug reports, 1-7 days for feature requests

**Supported Languages:** English (Primary)

**Bug Report Template:**
```
🐛 BUG REPORT TEMPLATE

**Plugin Version:** 
**Server Version:** 
**Dependencies:** 

**Description:**
[Clear description of the bug]

**Steps to Reproduce:**
1. 
2. 
3. 

**Expected Behavior:**
[What should happen]

**Actual Behavior:**
[What actually happens]

**Error Messages/Logs:**
[Paste any console errors]

**Additional Info:**
[Any other relevant information]
```

---

## 🚀 **Pre-Submission Checklist**

- [ ] Plugin thoroughly tested on clean server
- [ ] All dependencies confirmed working
- [ ] Configuration files validated
- [ ] Commands and permissions tested
- [ ] Screenshots/videos prepared
- [ ] Description proofread and formatted
- [ ] Version numbers consistent across all files
- [ ] License terms reviewed
- [ ] Support channels established
- [ ] Documentation prepared (if external)

## 📋 **Post-Upload Tasks**

1. **Monitor Initial Reviews** - Respond quickly to early feedback
2. **Update Documentation** - Based on user questions
3. **Prepare for Updates** - Track feature requests and bugs
4. **Engage Community** - Respond to discussions and questions
5. **Plan Future Features** - Based on user feedback

---

*This guide ensures a professional, comprehensive submission that highlights CTF-Core's unique features, especially the new hologram leaderboard system!* 🏆