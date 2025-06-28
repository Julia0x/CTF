package org.cwresports.ctfcore.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.cwresports.ctfcore.CTFCore;

/**
 * Handles player join events
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
        
        // EDGE CASE FIX: Handle player reconnection to prevent inventory duplication
        plugin.getGameManager().handlePlayerReconnection(event.getPlayer());
        
        // Update scoreboard for new player
        plugin.getScoreboardManager().updatePlayerScoreboard(event.getPlayer());
    }
}