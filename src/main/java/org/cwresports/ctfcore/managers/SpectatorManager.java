package org.cwresports.ctfcore.managers;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.CTFGame;
import org.cwresports.ctfcore.models.CTFPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages spectator mode for players who want to watch ongoing games
 * Provides enhanced spectator experience with teleportation and information
 */
public class SpectatorManager {
    
    private final CTFCore plugin;
    private final Map<UUID, SpectatorData> spectators;
    
    private static class SpectatorData {
        private final Player player;
        private final CTFGame game;
        private final GameMode originalGameMode;
        private final boolean originalAllowFlight;
        
        public SpectatorData(Player player, CTFGame game) {
            this.player = player;
            this.game = game;
            this.originalGameMode = player.getGameMode();
            this.originalAllowFlight = player.getAllowFlight();
        }
        
        public void restore() {
            player.setGameMode(originalGameMode);
            player.setAllowFlight(originalAllowFlight);
            player.setFlying(false);
        }
        
        public CTFGame getGame() { return game; }
    }
    
    public SpectatorManager(CTFCore plugin) {
        this.plugin = plugin;
        this.spectators = new HashMap<>();
    }
    
    /**
     * Add a player as spectator to a game
     */
    public boolean addSpectator(Player player, CTFGame game) {
        if (spectators.containsKey(player.getUniqueId())) {
            return false; // Already spectating
        }
        
        // Check if player is in any game
        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(player);
        if (ctfPlayer != null && ctfPlayer.isInGame()) {
            return false; // Player is already in a game
        }
        
        SpectatorData data = new SpectatorData(player, game);
        spectators.put(player.getUniqueId(), data);
        
        // Set spectator mode
        player.setGameMode(GameMode.SPECTATOR);
        
        // Teleport to game arena center
        if (game.getArena().getLobbySpawn() != null) {
            player.teleport(game.getArena().getLobbySpawn());
        }
        
        // Send welcome message
        player.sendMessage("§a§l⚡ SPECTATOR MODE ACTIVATED!");
        player.sendMessage("§eYou are now spectating the game in §b" + game.getArena().getName());
        player.sendMessage("§7Use §e/ctf leave §7to stop spectating");
        
        // Start spectator info updates
        startSpectatorInfoUpdates(player);
        
        return true;
    }
    
    /**
     * Remove a player from spectating
     */
    public boolean removeSpectator(Player player) {
        SpectatorData data = spectators.remove(player.getUniqueId());
        if (data == null) {
            return false; // Not spectating
        }
        
        // Restore original game mode
        data.restore();
        
        // Teleport to server lobby
        plugin.getServerLobbyManager().teleportToServerLobby(player);
        
        player.sendMessage("§e§l⚡ Spectator mode deactivated!");
        
        return true;
    }
    
    /**
     * Check if a player is spectating
     */
    public boolean isSpectating(Player player) {
        return spectators.containsKey(player.getUniqueId());
    }
    
    /**
     * Get the game a player is spectating
     */
    public CTFGame getSpectatingGame(Player player) {
        SpectatorData data = spectators.get(player.getUniqueId());
        return data != null ? data.getGame() : null;
    }
    
    /**
     * Start sending periodic game information to spectator
     */
    private void startSpectatorInfoUpdates(Player player) {
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (!player.isOnline() || !isSpectating(player)) {
                    cancel();
                    return;
                }
                
                SpectatorData data = spectators.get(player.getUniqueId());
                if (data == null) {
                    cancel();
                    return;
                }
                
                CTFGame game = data.getGame();
                if (game == null || game.getState() != org.cwresports.ctfcore.models.GameState.PLAYING) {
                    // Game ended, remove spectator
                    removeSpectator(player);
                    cancel();
                    return;
                }
                
                // Send game info every 10 seconds
                if (ticks % 200 == 0) {
                    sendGameInfo(player, game);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    /**
     * Send current game information to spectator
     */
    private void sendGameInfo(Player player, CTFGame game) {
        player.sendMessage("§8§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("§e§lSPECTATING: §b" + game.getArena().getName());
        player.sendMessage("§6§lTime Left: §f" + game.getFormattedTimeLeft());
        player.sendMessage("§c§lRed Team: §f" + game.getScore(org.cwresports.ctfcore.models.Arena.TeamColor.RED) + " captures");
        player.sendMessage("§9§lBlue Team: §f" + game.getScore(org.cwresports.ctfcore.models.Arena.TeamColor.BLUE) + " captures");
        player.sendMessage("§a§lPlayers: §f" + game.getPlayers().size());
        player.sendMessage("§8§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }
    
    /**
     * Remove all spectators (called on plugin disable)
     */
    public void removeAllSpectators() {
        for (UUID playerId : spectators.keySet()) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                removeSpectator(player);
            }
        }
        spectators.clear();
    }
    
    /**
     * Handle game end - remove all spectators of that game
     */
    public void handleGameEnd(CTFGame game) {
        spectators.entrySet().removeIf(entry -> {
            if (entry.getValue().getGame().equals(game)) {
                Player player = plugin.getServer().getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    removeSpectator(player);
                }
                return true;
            }
            return false;
        });
    }
}