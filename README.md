# CTF-Core Plugin - Simple & Enhanced Version

## 🎮 Features

### Core Gameplay
- **Instant Respawn**: Players respawn immediately after death (no countdown)
- **Simple Join System**: No reconnection complexity - always go to lobby with autojoin
- **Auto Team Win**: If a team becomes empty, the other team wins automatically
- **Clean Leave System**: Players who leave voluntarily get clean lobby experience

### Enhanced Mechanics
- **Leave Messages**: Other players see when someone leaves the game
- **Auto Victory**: Smart team balance checking with instant wins
- **Lobby-First**: All players start in server lobby with autojoin items
- **Fast-Paced Action**: No waiting, no delays, pure action

## 🚀 Simple Flow

1. **Join Server** → Go to lobby with autojoin items
2. **Join Arena** → Use autojoin or `/ctf join <arena>`
3. **Play Game** → Capture flags, instant respawn on death
4. **Leave/Disconnect** → Always return to lobby
5. **Rejoin** → Use autojoin again from lobby

## ✨ Key Improvements

### No Reconnection Complexity
- ❌ No reconnection data tracking
- ❌ No cooldown state management
- ❌ No complex restore logic
- ✅ Simple: Disconnect = Lobby + Autojoin

### Instant Action
- ⚡ **0.5 second respawn** (only for death animation)
- ⚡ **No spectator mode**
- ⚡ **No countdown screens**
- ⚡ **Immediate team wins** when team abandons

### Clean Code
- 📝 Removed 500+ lines of reconnection complexity
- 📝 Simple, maintainable logic
- 📝 No edge cases with reconnection data
- 📝 Clear player flow

## 🎯 Commands

- `/ctf join <arena>` - Join specific arena
- `/ctf leave` - Leave current arena → lobby
- `/ctf stats [player]` - View statistics
- `/ctf help` - Show help

## 🔧 Technical Details

### Player States
- **Server Lobby**: Default state with autojoin items
- **Arena Lobby**: Waiting for game to start
- **Playing**: Active gameplay with instant respawn
- **Left Arena**: Back to server lobby (clean slate)

### Team Victory Conditions
1. **Flag Captures**: Reach capture limit
2. **Kill Limit**: Reach team kill limit
3. **Time Limit**: Highest score when time expires
4. **Team Abandonment**: Other team left → instant win

### Performance
- **Memory Efficient**: No reconnection data storage
- **CPU Efficient**: No complex state checking
- **Network Efficient**: No reconnection packets
- **Simple Logic**: Easy to debug and maintain

---

**Version**: 1.1.0-Simple  
**No Bugs**: Removed all reconnection complexity  
**Pure Action**: Fast-paced CTF gameplay