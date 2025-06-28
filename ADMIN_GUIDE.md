# CTF-Core Administrator Guide

**Complete setup and management guide for server administrators**

---

## 📋 **Table of Contents**

1. [Initial Setup](#-initial-setup)
2. [Arena Creation & Management](#-arena-creation--management)
3. [Hologram Leaderboard Management](#-hologram-leaderboard-management)
4. [Configuration Guide](#-configuration-guide)
5. [Player Management](#-player-management)
6. [Troubleshooting](#-troubleshooting)
7. [Advanced Configuration](#-advanced-configuration)
8. [Best Practices](#-best-practices)

---

## 🚀 **Initial Setup**

### **Prerequisites Checklist**
- [ ] Paper 1.21+ server installed
- [ ] WorldGuard plugin installed and configured
- [ ] WorldEdit plugin installed
- [ ] DecentHolograms installed (for leaderboards)
- [ ] PlaceholderAPI installed (optional)
- [ ] Vault + Economy plugin (optional)

### **First-Time Installation**

1. **Install CTF-Core**
   ```bash
   # Place CTF-Core.jar in your /plugins/ folder
   # Restart server
   ```

2. **Verify Installation**
   ```bash
   /plugins
   # Look for CTF-Core in green (enabled)
   ```

3. **Check Dependencies**
   ```bash
   /ctfadmin
   # Verify you have admin permissions and commands work
   ```

4. **Configure Basic Settings**
   - Edit `/plugins/CTF-Core/config.yml`
   - Customize `/plugins/CTF-Core/messages.yml`
   - Set up permissions with your permission plugin

### **Essential Permissions Setup**
```yaml
# LuckPerms example
/lp group admin permission set ctf.admin true
/lp group default permission set ctf.play true
/lp group vip permission set ctf.chat.colors true
```

---

## 🏗️ **Arena Creation & Management**

### **Step-by-Step Arena Creation**

#### **Phase 1: Planning Your Arena**
1. **Choose Location**: Select a suitable area (recommended: 100x100 blocks minimum)
2. **Design Layout**: Plan team bases, flag locations, and capture points
3. **WorldGuard Region**: Create a protective region around your arena

#### **Phase 2: WorldGuard Setup**
```bash
# Select your arena area with WorldEdit
//pos1  # Click first corner
//pos2  # Click opposite corner

# Create the region
/rg define ctf_arena_1

# Set basic flags (optional)
/rg flag ctf_arena_1 pvp allow
/rg flag ctf_arena_1 build deny
/rg flag ctf_arena_1 chest-access deny
```

#### **Phase 3: CTF Arena Creation**
```bash
# Create the arena
/ctfadmin create arena1 ctf_arena_1

# You'll receive an admin toolkit automatically
# If you need it again later:
/ctfadmin setup arena1
```

#### **Phase 4: Configure Arena Components**

**4.1 Set Lobby Spawn**
```bash
# Stand where players should spawn when joining
/ctfadmin setlobby arena1
```

**4.2 Set Team Spawn Points (4 per team required)**
```bash
# Red team spawns
/ctfadmin setspawn arena1 red 1
/ctfadmin setspawn arena1 red 2
/ctfadmin setspawn arena1 red 3
/ctfadmin setspawn arena1 red 4

# Blue team spawns
/ctfadmin setspawn arena1 blue 1
/ctfadmin setspawn arena1 blue 2
/ctfadmin setspawn arena1 blue 3
/ctfadmin setspawn arena1 blue 4
```

**4.3 Set Flag Locations**
```bash
# Stand at red team's flag location
/ctfadmin setflag arena1 red
# Click the block where the flag should be placed

# Stand at blue team's flag location
/ctfadmin setflag arena1 blue
# Click the block where the flag should be placed
```

**4.4 Set Capture Points**
```bash
# Stand at red team's capture zone
/ctfadmin setcapture arena1 red
# Click the block for capture zone center

# Stand at blue team's capture zone
/ctfadmin setcapture arena1 blue
# Click the block for capture zone center
```

#### **Phase 5: Validation & Activation**
```bash
# Check arena status
/ctfadmin status arena1

# Save and enable arena (only works if fully configured)
/ctfadmin save arena1
```

### **Arena Management Commands**

#### **Viewing Arena Information**
```bash
# List all arenas
/ctfadmin list

# Detailed arena status
/ctfadmin status <arena_name>

# View setup progress with boss bar
/ctfadmin setup <arena_name>
```

#### **Modifying Existing Arenas**
```bash
# Re-enter setup mode
/ctfadmin setup <arena_name>

# Change specific components
/ctfadmin setlobby <arena_name>
/ctfadmin setspawn <arena_name> <team> <number>
/ctfadmin setflag <arena_name> <team>
/ctfadmin setcapture <arena_name> <team>

# Save changes
/ctfadmin save <arena_name>
```

#### **Arena Deletion**
```bash
# Delete an arena (cannot be undone!)
/ctfadmin delete <arena_name>
```

#### **Game Management**
```bash
# Force start a game (minimum 2 players)
/ctfadmin forcestart <arena_name>

# Check game status
/ctf status
```

---

## 🏆 **Hologram Leaderboard Management**

### **Prerequisites for Holograms**
- DecentHolograms plugin must be installed and enabled
- Server restart required after installing DecentHolograms

### **Creating Leaderboards**

#### **Basic Leaderboard Creation**
```bash
# Create a kills leaderboard (default size: 5)
/ctfadmin leaderboard create kills

# Create leaderboard with custom size (1-15)
/ctfadmin leaderboard create captures 10
/ctfadmin leaderboard create level 8
/ctfadmin leaderboard create games_won 5
```

#### **Available Leaderboard Types**
| Type | Description | Data Source |
|------|-------------|-------------|
| `kills` | Total player kills | `total_kills` |
| `captures` | Total flag captures | `total_captures` |
| `level` | Player levels | `level` |
| `games_won` | Games won | `games_won` |

### **Managing Leaderboards**

#### **Viewing Leaderboards**
```bash
# List all leaderboards with status
/ctfadmin leaderboard list

# Example output:
# kills_1647891234567 (Top Kills, Size: 10) - Enabled ✔
# level_1647891234568 (Top Levels, Size: 5) - Enabled ✔
```

#### **Moving Leaderboards**
```bash
# Move leaderboard to your current location
/ctfadmin leaderboard move <leaderboard_id>

# Example:
/ctfadmin leaderboard move kills_1647891234567
```

#### **Deleting Leaderboards**
```bash
# Delete a leaderboard (cannot be undone!)
/ctfadmin leaderboard delete <leaderboard_id>
```

#### **Updating Leaderboards**
```bash
# Force immediate update of all leaderboards
/ctfadmin leaderboard reload
```

### **Leaderboard Placement Strategy**

#### **Recommended Locations**
- **Spawn/Lobby Areas**: High visibility for all players
- **Arena Entrances**: Build excitement before games
- **Hall of Fame Areas**: Dedicated statistics viewing areas
- **Tournament Areas**: Special event spaces

#### **Placement Tips**
- Place at eye level (Y-coordinate around 65-70)
- Ensure clear line of sight
- Avoid placing too close to walls or obstructions
- Leave 3-4 block spacing between multiple leaderboards
- Consider lighting for better visibility

### **Leaderboard Design Customization**

#### **Size Recommendations**
- **Lobby Areas**: 5-10 entries (balanced visibility)
- **Hall of Fame**: 10-15 entries (comprehensive view)
- **Tournament Areas**: 3-5 entries (focused competition)
- **Arena Entrances**: 5 entries (quick motivation)

#### **Type Selection Strategy**
- **PvP Servers**: Kills, K/D ratio leaderboards
- **Community Servers**: Level, games won leaderboards
- **Competitive Servers**: All types for comprehensive tracking
- **Casual Servers**: Captures, level leaderboards

---

## ⚙️ **Configuration Guide**

### **Main Configuration (`config.yml`)**

#### **Game Settings**
```yaml
game:
  min-players: 4              # Minimum players to start
  max-players: 20             # Maximum players per arena
  countdown-time: 30          # Countdown before game starts (seconds)
  game-duration: 600          # Game length (seconds) - 10 minutes
  respawn-delay: 5            # Respawn delay (seconds)
  capture-time: 3             # Time to capture flag (seconds)
  flag-return-time: 30        # Auto-return dropped flag (seconds)
```

#### **Experience System**
```yaml
experience:
  per-kill: 10                # XP for killing enemy
  per-capture: 50             # XP for capturing flag
  per-flag-return: 25         # XP for returning team flag
  per-win: 100               # XP for winning game
  level-up-base-xp: 100      # XP needed for level 2
  level-up-multiplier: 50    # Additional XP per level
```

#### **Power-Up System**
```yaml
powerups:
  enabled: true               # Enable power-ups
  spawn-chance: 0.3          # Chance to spawn on kill (30%)
  duration: 30               # Power-up effect duration (seconds)
  types:
    speed: true              # Speed boost
    strength: true           # Damage boost
    jump: true              # Jump boost
    invisibility: true      # Temporary invisibility
```

#### **Economy Integration**
```yaml
economy:
  enabled: true              # Requires Vault + economy plugin
  rewards:
    kill: 10                 # Money per kill
    capture: 50              # Money per capture
    flag-return: 25          # Money per flag return
    win: 100                 # Money per game win
```

### **Message Configuration (`messages.yml`)**

#### **Important Messages to Customize**
```yaml
# Server branding
prefix: "&6[CTF] &f"
server-name: "Your Server Name"

# Game messages
game-start: "&aThe battle begins! Capture the enemy flag!"
game-end: "&eGame Over! {winner_team} team wins!"
flag-captured: "&6{player} &acaptured the {team_color}{team_name} &aflag!"

# Player feedback
kill-message: "&c+{xp} XP &7for eliminating &c{victim}&7!"
level-up: "&6&lLEVEL UP! &eYou are now level {level}!"
```

### **Leaderboard Configuration (`leaderboards.yml`)**

#### **Global Settings**
```yaml
settings:
  update-interval: 30        # Update frequency (seconds)
  default-size: 5           # Default leaderboard size
  max-size: 15              # Maximum allowed size
  auto-cleanup: true        # Remove broken leaderboards
```

**Note**: Individual leaderboard data is automatically managed. Manual editing not recommended.

---

## 👥 **Player Management**

### **Player Statistics**

#### **Viewing Player Stats**
```bash
# View any player's statistics
/ctf stats <player_name>

# View your own statistics
/ctf stats
```

#### **Statistics Tracked**
- **Total Kills**: Lifetime enemy eliminations
- **Total Deaths**: Times eliminated by enemies
- **Total Captures**: Successful flag captures
- **Flag Returns**: Enemy flags returned to base
- **Games Played**: Total games participated in
- **Games Won**: Victories achieved
- **Level**: Current progression level
- **Experience**: XP points earned
- **K/D Ratio**: Calculated kill/death ratio
- **Win Rate**: Percentage of games won

### **Player Data Management**

#### **Data Storage Location**
```
/plugins/CTF-Core/playerdata.yml
```

#### **Manual Data Editing** (Advanced)
```yaml
players:
  "uuid-here":
    level: 5
    experience: 250
    total_kills: 45
    total_deaths: 23
    total_captures: 12
    total_flag_returns: 8
    games_played: 15
    games_won: 9
```

**Warning**: Always backup before manual editing!

### **Player Commands Reference**
```bash
# Players can use these commands:
/ctf join [arena]     # Join game (auto-selects arena if none specified)
/ctf leave            # Leave current game
/ctf stats [player]   # View statistics
/ctf help             # Show available commands
```

---

## 🔧 **Troubleshooting**

### **Common Issues & Solutions**

#### **Issue: Leaderboards Not Displaying**
**Symptoms**: Created leaderboards don't appear in-game

**Solutions**:
1. **Check DecentHolograms**:
   ```bash
   /plugins
   # Ensure DecentHolograms is green (enabled)
   ```

2. **Verify Creation**:
   ```bash
   /ctfadmin leaderboard list
   # Check if leaderboard exists and is enabled
   ```

3. **Check Console**:
   ```
   Look for errors mentioning:
   - "DecentHolograms not found"
   - "Failed to create hologram"
   ```

4. **Manual Reload**:
   ```bash
   /ctfadmin leaderboard reload
   ```

#### **Issue: Arena Won't Save/Enable**
**Symptoms**: "/ctfadmin save" fails with validation errors

**Solutions**:
1. **Check Status**:
   ```bash
   /ctfadmin status <arena_name>
   # Review what's missing (lobby, spawns, flags, captures)
   ```

2. **Common Missing Components**:
   - **Lobby**: Set with `/ctfadmin setlobby <arena>`
   - **Spawns**: Need 4 per team (8 total)
   - **Flags**: Need both red and blue flag locations
   - **Captures**: Need both red and blue capture points

3. **WorldGuard Region**:
   ```bash
   /rg info <region_name>
   # Ensure region exists and is properly defined
   ```

#### **Issue: Players Can't Join Games**
**Symptoms**: Players get error messages when trying to join

**Solutions**:
1. **Check Permissions**:
   ```bash
   /lp user <player> permission check ctf.play
   ```

2. **Arena Status**:
   ```bash
   /ctfadmin list
   # Ensure arena shows as "Enabled"
   ```

3. **Game Requirements**:
   - Check if minimum players requirement is met
   - Verify arena isn't already full
   - Ensure game isn't already in progress

#### **Issue: Statistics Not Updating**
**Symptoms**: Player stats or leaderboards show old data

**Solutions**:
1. **Force Leaderboard Update**:
   ```bash
   /ctfadmin leaderboard reload
   ```

2. **Check Player Data File**:
   ```bash
   # Verify /plugins/CTF-Core/playerdata.yml exists and has data
   ```

3. **Game Completion**:
   - Stats only update after games complete
   - Ensure games are ending properly

### **Debug Mode**

#### **Enable Debug Logging**
Add to `config.yml`:
```yaml
debug:
  enabled: true
  verbose: true
```

This will provide detailed console output for troubleshooting.

#### **Analyzing Log Files**
```bash
# Check recent logs for CTF-Core messages
tail -n 100 logs/latest.log | grep CTF

# Look for specific error patterns:
# - "Failed to create"
# - "Could not load"
# - "Permission denied"
# - "Invalid configuration"
```

---

## 🔮 **Advanced Configuration**

### **Custom Game Modes** (Configuration-Based)

#### **Quick Games Mode**
```yaml
game:
  min-players: 2           # Lower requirement
  countdown-time: 10       # Faster start
  game-duration: 300       # 5-minute games
  respawn-delay: 2         # Quick respawn
```

#### **Hardcore Mode**
```yaml
game:
  respawn-delay: 15        # Longer death penalty
  flag-return-time: 60     # Longer return time
  capture-time: 5          # Harder captures

experience:
  per-kill: 20             # Double XP rewards
  per-capture: 100
```

#### **Casual Mode**
```yaml
game:
  min-players: 2           # Easy to start
  respawn-delay: 3         # Quick respawn
  capture-time: 2          # Easy captures

powerups:
  spawn-chance: 0.5        # More power-ups
  duration: 45             # Longer effects
```

### **Multi-World Setup**

#### **Arena Distribution Strategy**
- **World 1**: Casual/beginner arenas
- **World 2**: Competitive/ranked arenas
- **World 3**: Tournament/event arenas

#### **Per-World Configuration**
```yaml
# You can have different configs per world
# Create separate arena files for organization

arenas:
  world_pvp:
    - arena_casual_1
    - arena_casual_2
  world_competitive:
    - arena_ranked_1
    - arena_tournament
```

### **Advanced Leaderboard Setups**

#### **Tournament Hall of Fame**
```bash
# Create comprehensive leaderboard display
/ctfadmin leaderboard create kills 15
/ctfadmin leaderboard create captures 10
/ctfadmin leaderboard create level 10
/ctfadmin leaderboard create games_won 10

# Arrange in organized layout
# Space leaderboards 5-7 blocks apart
# Use consistent height levels
```

#### **Arena-Specific Displays**
```bash
# Create themed leaderboards for different arena types
# PvP Arena: Focus on combat stats
/ctfadmin leaderboard create kills 10

# Objective Arena: Focus on captures
/ctfadmin leaderboard create captures 8

# Training Arena: Focus on progression
/ctfadmin leaderboard create level 5
```

### **Integration with Other Plugins**

#### **Discord Integration** (with DiscordSRV)
```yaml
# Add to DiscordSRV config for game announcements
# Channel: game-updates
# Messages: game-start, game-end, flag-captured
```

#### **Rewards Integration** (with Custom plugins)
```yaml
# Economy rewards can trigger other plugin events
# Example: Rank promotions, cosmetic unlocks, etc.
```

---

## 🏅 **Best Practices**

### **Arena Design Guidelines**

#### **Layout Principles**
1. **Symmetrical Design**: Ensure fair gameplay for both teams
2. **Multiple Routes**: Provide various paths between bases
3. **Strategic Elements**: Include cover, high ground, chokepoints
4. **Clear Sight Lines**: Avoid overly complex maze designs
5. **Appropriate Scale**: 80-120 block distance between flags optimal

#### **Spawn Point Placement**
- **Safe Zones**: Spawns should be protected from enemy camping
- **Multiple Options**: 4 spawns per team prevent spawn killing
- **Strategic Positions**: Near but not on the flag location
- **Quick Access**: Clear path to flag and capture points

#### **Flag and Capture Positioning**
- **Defendable Locations**: Flags should be protectable but not impossible to reach
- **Clear Markers**: Use distinctive blocks/structures for flag locations
- **Capture Point Distance**: 20-40 blocks from enemy flag optimal
- **Visual Clarity**: Make capture zones obvious with colored blocks

### **Server Performance Optimization**

#### **Recommended Settings for Large Servers**
```yaml
# For 100+ concurrent players
leaderboards:
  settings:
    update-interval: 60    # Reduce frequency
    max-size: 10          # Limit size

game:
  max-players: 16        # Per arena limit
  
performance:
  async-saves: true      # If available in future updates
```

#### **Resource Management**
- **Limit Concurrent Games**: 3-5 active arenas maximum
- **Monitor Entity Count**: Power-ups and effects add entities
- **Regular Cleanup**: Use `/ctfadmin reload` during low-traffic times

### **Community Management**

#### **Rank System Integration**
```yaml
# Example progression rewards
# Level 10: VIP rank
# Level 25: Elite rank
# Level 50: Legend rank

# Implement with permission plugin hooks
```

#### **Tournament Organization**
```bash
# Weekly tournament setup:
# 1. Create dedicated tournament arena
# 2. Set up comprehensive leaderboards
# 3. Use /ctfadmin forcestart for organized matches
# 4. Track statistics for prizes/rewards
```

#### **Player Engagement Strategies**
- **Daily/Weekly Leaderboard Resets**: Keep competition fresh
- **Seasonal Events**: Special arena themes and rewards
- **Achievement Systems**: Use statistics for milestone rewards
- **Community Challenges**: Server-wide capture goals

### **Maintenance Schedule**

#### **Daily Tasks**
- Check console for errors
- Monitor active games
- Review player feedback

#### **Weekly Tasks**
- Backup configuration files
- Review and clean up playerdata.yml
- Update leaderboard positions if desired
- Check for plugin updates

#### **Monthly Tasks**
- Analyze player statistics trends
- Consider new arena additions
- Review and optimize configuration
- Plan community events

---

## 📞 **Support & Resources**

### **Getting Help**
1. **Check This Guide**: Most issues covered in troubleshooting section
2. **Console Logs**: Enable debug mode for detailed error information
3. **Community Forums**: Post on SpigotMC resource page
4. **Discord Support**: Join our Discord server for real-time help

### **Reporting Issues**
When reporting problems, include:
- CTF-Core version
- Server version (Paper/Spigot + version)
- Relevant error messages
- Steps to reproduce
- Configuration files (if relevant)

### **Feature Requests**
We welcome suggestions for new features:
- Use GitHub issues for feature requests
- Provide detailed use cases
- Consider backward compatibility
- Suggest implementation approaches if technical

---

**Congratulations! You now have comprehensive knowledge to manage CTF-Core like a pro.** 🎯

*This guide covers everything from basic setup to advanced configuration. Bookmark this page and refer back as needed while managing your CTF server!*