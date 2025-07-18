package org.cwresports.ctfcore.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.cwresports.ctfcore.CTFCore;

/**
 * Enhanced player join listener with improved reconnection handling
 * Handles player join events with comprehensive reconnection support
 */
public class PlayerJoinListener implements Listener {
    
    private final CTFCore plugin;
    
    public PlayerJoinListener(CTFCore plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Remove default vanilla join message
        event.setJoinMessage(null);
        
        // ENHANCED: Handle player reconnection with improved state restoration
        plugin.getGameManager().handlePlayerReconnection(event.getPlayer());
        
        // Update scoreboard for new/returning player
        plugin.getScoreboardManager().updatePlayerScoreboard(event.getPlayer());
        
        // Update tab list for new player and others
        plugin.getTabListManager().onPlayerJoin(event.getPlayer());
        
        // Send welcome message after reconnection processing
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                // Check if player was successfully reconnected to a game
                if (plugin.getGameManager().getCTFPlayer(event.getPlayer()) != null) {
                    // Player was reconnected to a game - welcome message was sent by GameManager
                    return;
                }
                
                // Player is in server lobby - send general welcome message
                event.getPlayer().sendMessage(plugin.getConfigManager().getMessage("welcome-message", 
                    java.util.Collections.singletonMap("player", event.getPlayer().getName())));
            }
        }, 20L); // 1 second delay to allow reconnection processing
    }
}