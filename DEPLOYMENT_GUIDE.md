# CTF-Core Deployment Guide

## 🚀 Quick Deployment

### Step 1: Compile the Plugin
```bash
# Make sure you have Maven installed
mvn clean package

# The JAR will be in target/ctf-core-1.1.0.jar
```

### Step 2: Server Installation
1. Stop your Minecraft server
2. Copy `target/ctf-core-1.1.0.jar` to your `plugins/` folder
3. Start your server
4. Plugin will generate default configuration files

### Step 3: Basic Configuration
```bash
# Connect to your server console or as an admin player
/ctfadmin create arena1
/ctfadmin setup arena1 lobby
# Set the lobby spawn at your desired location
/ctfadmin setup arena1 spawn red 1
# Set red team spawn point 1
/ctfadmin setup arena1 spawn blue 1
# Set blue team spawn point 1
/ctfadmin enable arena1
# Arena is now ready for play!
```

## ⚙️ Advanced Configuration

### WorldGuard Integration (Recommended)
1. Install WorldGuard plugin
2. Create a region for your arena: `/region define ctf_arena1`
3. Set the region in arena config: `/ctfadmin setup arena1 region ctf_arena1`

### Server Lobby Setup
```bash
/ctfadmin setserverlobby
# Sets the main server lobby where players go when leaving arenas
```

### Message Customization
Edit `plugins/CTF-Core/messages.yml` to customize all player messages:
```yaml
# Example customizations
welcome-message: "&a🎮 Welcome to our CTF server, {player}!"
instant-respawn: "&a⚡ Lightning respawn! Back in action!"
player-left-game: "&c💨 {team_color}{player} &chas fled the battlefield!"
```

## 🔧 Production Settings

### Recommended config.yml Settings
```yaml
gameplay:
  min-players-to-start: 4      # Minimum players needed to start a game
  max-players-per-arena: 16    # Maximum players per arena
  game-duration-minutes: 15    # Game length in minutes
  spawn-protection-seconds: 3   # Spawn protection duration
  auto-leave-delay-seconds: 10  # Time before auto-removal after game ends

performance:
  respawn-delay-seconds: 0      # Set to 0 for instant respawn (already implemented)
  cleanup-interval-minutes: 5   # How often to clean up old data
```

## 🎯 Testing Your Installation

### Test 1: Basic Functionality
1. Join an arena: `/ctf join arena1`
2. Have another player join the same arena
3. Wait for the game to start
4. Test combat and flag capturing

### Test 2: Leave System
1. Join an arena and start a game
2. Use `/ctf leave` to leave voluntarily
3. Try to rejoin - should be prevented
4. Check that other players see the leave message

### Test 3: Instant Respawn
1. Join a game and die
2. Verify you respawn immediately (no countdown)
3. Check that spawn protection works

### Test 4: Team Abandonment Victory
1. Start a game with players on both teams
2. Have all players from one team leave
3. Verify the remaining team wins automatically

## 🚨 Troubleshooting

### Common Issues

**Plugin won't start:**
- Check Java version (needs Java 21+)
- Verify Paper/Spigot version (needs 1.21+)
- Check server logs for dependency issues

**Players can't join arenas:**
- Verify arena is enabled: `/ctfadmin list`
- Check arena setup is complete: `/ctfadmin info arena1`
- Ensure lobby spawn is set

**Reconnection errors:**
- Check server logs for specific error messages
- Verify player data directory permissions
- Clear player data if corrupted: `/ctfadmin cleardata <player>`

### Log Files
- **Plugin logs**: `logs/latest.log` (search for "[CTF-Core]")
- **Error logs**: Look for stack traces containing "ctfcore"
- **Debug info**: Enable debug mode in config.yml

### Performance Monitoring
```bash
# Monitor active games
/ctfadmin stats

# Check player counts
/ctfadmin list

# View system performance
/minecraft:plugins CTF-Core
```

## 📊 Monitoring & Analytics

### Key Metrics to Track
- Average game duration
- Player retention rate
- Arena popularity
- Error frequency
- Team balance statistics

### Admin Commands for Monitoring
```bash
/ctfadmin stats            # Overall server statistics
/ctfadmin playerstats      # Individual player performance
/ctfadmin arenastats       # Arena usage statistics
/ctfadmin errors          # Recent error summary
```

## 🔄 Updates & Maintenance

### Regular Maintenance Tasks
1. **Weekly**: Review error logs and player feedback
2. **Bi-weekly**: Update plugin if new version available
3. **Monthly**: Archive old player statistics
4. **As needed**: Add new arenas based on player demand

### Backup Strategy
- **Configuration files**: Backup entire `plugins/CTF-Core/` folder
- **Player data**: Backup `plugins/CTF-Core/playerdata/` regularly
- **Arena data**: Export arena configurations before major changes

## 🎮 Going Live Checklist

- [ ] Plugin compiled and installed
- [ ] At least 2 arenas created and enabled
- [ ] Server lobby spawn point set
- [ ] WorldGuard regions configured (if used)
- [ ] Messages customized for your server
- [ ] Admin permissions configured
- [ ] Basic testing completed
- [ ] Player documentation updated
- [ ] Staff trained on admin commands
- [ ] Monitoring systems in place

---

**Need help?** Check the logs first, then review this guide. The enhanced error handling should provide clear information about any issues!