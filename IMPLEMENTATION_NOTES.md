# CTF-Core v1.1.0 Implementation Notes

## Overview
This implementation adds two major features to the CTF-Core plugin:

1. **Config Migration System** - Automatically updates configuration files from legacy versions
2. **Autojoin Functionality** - Allows players to automatically join games with a lobby item

## Changes Made

### 1. Config Migration System

#### New Files:
- `/src/main/java/org/cwresports/ctfcore/managers/ConfigMigrationManager.java`
  - Handles automatic migration from legacy configs (no version) to versioned configs
  - Creates backups of old configs before migration
  - Supports version-specific migrations for future updates

#### Modified Files:
- `/src/main/java/org/cwresports/ctfcore/CTFCore.java`
  - Added ConfigMigrationManager initialization
  - Migration runs BEFORE config loading to ensure proper order

#### Configuration Files Updated:
- `/src/main/resources/config.yml` - Added version "1.1.0" and autojoin settings
- `/src/main/resources/messages.yml` - Added version "1.1.0" and autojoin messages
- `/src/main/resources/scoreboards.yml` - Added version "1.1.0"
- `/src/main/resources/leaderboards.yml` - Added version "1.1.0"

### 2. Autojoin Functionality

#### New Files:
- `/src/main/java/org/cwresports/ctfcore/managers/AutojoinManager.java`
  - Manages autojoin item creation and handling
  - Implements smart game selection (prefers games with more players)
  - Includes cooldown system to prevent spam
  - Provides autojoin statistics for admin monitoring

#### Modified Files:
- `/src/main/java/org/cwresports/ctfcore/CTFCore.java`
  - Added AutojoinManager initialization and getter
  - Added autojoin manager to shutdown sequence

- `/src/main/java/org/cwresports/ctfcore/managers/LobbyManager.java`
  - Updated `giveServerLobbyItems()` to include autojoin item (Paper item)
  - Modified `handleHotbarClick()` to process autojoin item clicks
  - Updated `cleanupLobbyItems()` to remove autojoin items when needed

## Key Features

### Config Migration
- **Automatic Detection**: Detects legacy configs without version numbers
- **Backup Creation**: Creates timestamped backups before migration
- **Safe Migration**: Preserves existing settings while adding new ones
- **Future-Proof**: Supports migrations between any version numbers

### Autojoin System
- **Smart Selection**: Prioritizes games with more waiting players
- **Fallback Logic**: Joins random arena if all games are empty
- **Cooldown Protection**: 3-second cooldown between autojoin attempts
- **User Feedback**: Clear messages about join status and results
- **Admin Statistics**: Tracking of autojoin usage and performance

## Configuration Options

### Autojoin Settings in config.yml:
```yaml
autojoin:
  enabled: true                    # Enable/disable autojoin
  prefer-populated-games: true     # Prefer games with more players
  min-players-threshold: 2         # Minimum players to prefer a game
  item-name: "&a&lAuto Join Game"  # Display name of autojoin item
  item-lore:                       # Lore text for autojoin item
    - "&7Click to automatically join"
    - "&7a game with other players!"
    - ""
    - "&ePrefers games with more players"
```

### Server Lobby Settings:
```yaml
server-lobby:
  give-items: true      # Give lobby items to players
  clear-inventory: true # Clear inventory before giving items
```

## Messages Added

### Autojoin Messages in messages.yml:
- `autojoin-searching`: Message when searching for games
- `autojoin-found`: Message when game is found
- `autojoin-no-games`: Message when no games available
- `autojoin-joined-populated`: Message when joining populated game
- `autojoin-joined-random`: Message when joining empty game
- `autojoin-failed`: Message when join attempt fails
- `autojoin-cooldown`: Message during cooldown period
- `autojoin-disabled`: Message when autojoin is disabled

## Technical Implementation

### Migration Process:
1. Plugin starts up
2. ConfigMigrationManager checks all config files
3. If no version found, treats as "legacy" and migrates
4. Creates backup with timestamp
5. Adds new settings and version number
6. Saves updated config
7. Regular config loading continues

### Autojoin Process:
1. Player clicks autojoin item (Paper)
2. AutojoinManager checks eligibility and cooldown
3. Searches all enabled arenas for best match
4. Prioritizes arenas with more waiting players
5. Attempts to join selected arena
6. Provides feedback to player about result
7. Handles failures with retry logic

## Compatibility

- **Backward Compatible**: Legacy configs without versions are automatically migrated
- **Forward Compatible**: Version system allows for future migrations
- **Safe Upgrades**: Backups ensure no data loss during migration
- **Minimal Impact**: Migration only runs when needed, not on every startup

## Testing Recommendations

1. **Config Migration Testing**:
   - Test with legacy configs (no version numbers)
   - Verify backup creation
   - Check that all settings are preserved
   - Confirm new settings are added with correct defaults

2. **Autojoin Testing**:
   - Test with multiple arenas in different states
   - Verify player preference logic works correctly
   - Test cooldown system
   - Confirm proper error handling

3. **Integration Testing**:
   - Test lobby item display and removal
   - Verify interaction with existing game join system
   - Test with various player counts and game states

## Future Enhancements

The system is designed to be easily extensible:
- Add more migration types for future versions
- Extend autojoin with additional matching criteria
- Add admin commands for autojoin management
- Include more detailed statistics and monitoring