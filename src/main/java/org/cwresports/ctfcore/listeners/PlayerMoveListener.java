package org.cwresports.ctfcore.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.*;

/**
 * Handles player movement for boundary checking ONLY
 * All flag interactions now require right-click hold - no more auto-capture/pickup
 */
public class PlayerMoveListener implements Listener {
    
    private final CTFCore plugin;
    
    public PlayerMoveListener(CTFCore plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(player);
        
        if (ctfPlayer == null || !ctfPlayer.isInGame()) {
            return;
        }
        
        CTFGame game = ctfPlayer.getGame();
        if (game == null || game.getState() != GameState.PLAYING) {
            return;
        }
        
        Arena arena = game.getArena();
        Location from = event.getFrom();
        Location to = event.getTo();
        
        // Only check if player actually moved to a different block
        if (to == null || (from.getBlockX() == to.getBlockX() && 
                           from.getBlockY() == to.getBlockY() && 
                           from.getBlockZ() == to.getBlockZ())) {
            return;
        }
        
        // Check arena boundaries
        checkArenaBoundaries(player, arena, to, from);
        
        // Check power-up collection
        plugin.getPowerUpManager().checkPowerUpCollection(player, game);
    }
    
    /**
     * Check if player is within arena boundaries
     */
    private void checkArenaBoundaries(Player player, Arena arena, Location to, Location from) {
        if (!plugin.getWorldGuardManager().isLocationInRegion(to, arena.getWorldGuardRegion())) {
            // Player is outside arena boundaries
            
            // Check if teleport back is enabled
            if (plugin.getConfigManager().getMainConfig().getBoolean("boundaries.teleport-back-on-exit", true)) {
                // Try to find a safe location within the arena
                Location safeLocation = plugin.getWorldGuardManager().getSafeLocationInRegion(
                    arena.getWorld(), arena.getWorldGuardRegion());
                
                if (safeLocation != null) {
                    player.teleport(safeLocation);
                } else {
                    // Fallback to previous location
                    player.teleport(from);
                }
            }
            
            // Send warning message if enabled
            if (plugin.getConfigManager().getMainConfig().getBoolean("boundaries.warning-message-enabled", true)) {
                player.sendMessage(plugin.getConfigManager().getMessage("boundary-warning"));
            }
            
            // Apply damage if enabled
            if (plugin.getConfigManager().getMainConfig().getBoolean("boundaries.damage-on-boundary-exit", false)) {
                player.damage(2.0); // 1 heart damage
            }
        }
    }
}