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
        
        // Simple join handling - always send to lobby with autojoin items
        plugin.getGameManager().handlePlayerJoin(event.getPlayer());
        
        // Update scoreboard for new/returning player
        plugin.getScoreboardManager().updatePlayerScoreboard(event.getPlayer());
        
        // Update tab list for new player and others
        plugin.getTabListManager().onPlayerJoin(event.getPlayer());
        
        // Send welcome message after lobby setup
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                // Player is in server lobby - send general welcome message
                event.getPlayer().sendMessage(plugin.getConfigManager().getMessage("welcome-message", 
                    java.util.Collections.singletonMap("player", event.getPlayer().getName())));
            }
        }, 20L); // 1 second delay
    }
}