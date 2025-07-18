package org.cwresports.ctfcore.listeners;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.CTFPlayer;
import org.cwresports.ctfcore.models.GameState;

/**
 * Enhanced block place listener with comprehensive tracking
 * Handles block placement during CTF games with proper tracking and restrictions
 */
public class BlockPlaceListener implements Listener {
    
    private final CTFCore plugin;
    
    public BlockPlaceListener(CTFCore plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(player);
        
        // If player is not in a CTF game, allow normal block placement
        if (ctfPlayer == null || !ctfPlayer.isInGame()) {
            return;
        }
        
        var game = ctfPlayer.getGame();
        if (game == null) {
            return;
        }
        
        var arena = game.getArena();
        
        // Check if block is within the arena region
        if (!plugin.getWorldGuardManager().isLocationInRegion(block.getLocation(), arena.getWorldGuardRegion())) {
            return; // Outside arena, let WorldGuard handle it
        }
        
        // During lobby/waiting phase, prevent all block placement
        if (game.getState() != GameState.PLAYING) {
            event.setCancelled(true);
            String message = plugin.getMessageManager().processMessage("&c⛔ You cannot place blocks while the game is not active!");
            player.sendMessage(message);
            return;
        }
        
        // During gameplay, allow block placement but track it
        if (game.getState() == GameState.PLAYING) {
            // Track the block placement for cleanup later
            plugin.getBlockTrackingManager().trackPlacedBlock(player, block, arena);
            
            // Log the block placement
            plugin.getLogger().info("Player " + player.getName() + " placed " + block.getType().name() + 
                                   " at " + block.getLocation().getBlockX() + "," + 
                                   block.getLocation().getBlockY() + "," + 
                                   block.getLocation().getBlockZ() + " in arena " + arena.getName());
        }
    }
}