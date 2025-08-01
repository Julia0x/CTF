package org.cwresports.ctfcore.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.CTFPlayer;

/**
 * Handles player death events in CTF games
 */
public class PlayerDeathListener implements Listener {
    
    private final CTFCore plugin;
    
    public PlayerDeathListener(CTFCore plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(player);
        
        if (ctfPlayer == null || !ctfPlayer.isInGame()) {
            return;
        }
        
        // Clear death message in CTF (we'll handle our own)
        event.setDeathMessage(null);
        
        // Clear drops in CTF
        event.getDrops().clear();
        event.setDroppedExp(0);
        
        // Get killer
        Player killer = player.getKiller();
        
        // Handle death in game manager
        plugin.getGameManager().handlePlayerDeath(player, killer);
        
        // Play death sound
        player.playSound(player.getLocation(), 
            plugin.getConfigManager().getSound("player_death"), 1.0f, 1.0f);
        
        if (killer != null && !killer.equals(player)) {
            // Play kill sound for killer
            killer.playSound(killer.getLocation(), 
                plugin.getConfigManager().getSound("kill_player"), 1.0f, 1.0f);
        }
    }
}