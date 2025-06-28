package org.cwresports.ctfcore.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.managers.ArenaManager;
import org.cwresports.ctfcore.models.Arena;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles block break events for flag setup and protection
 */
public class BlockBreakListener implements Listener {
    
    private final CTFCore plugin;
    
    public BlockBreakListener(CTFCore plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Check if player is in flag setup mode
        if (plugin.getArenaManager().isPlayerInSetupMode(player)) {
            handleFlagSetup(event, player, block);
            return;
        }
        
        // Check if block is in a CTF arena
        checkArenaProtection(event, player, block);
    }
    
    /**
     * Handle flag setup when admin breaks a banner
     */
    private void handleFlagSetup(BlockBreakEvent event, Player player, Block block) {
        ArenaManager.SetupSession session = (ArenaManager.SetupSession) plugin.getArenaManager().getPlayerSetupSession(player);
        
        if (session == null || session.mode != ArenaManager.SetupMode.FLAG_PLACEMENT) {
            return;
        }
        
        // Check if block is a banner
        if (!block.getType().name().contains("BANNER")) {
            player.sendMessage(plugin.getConfigManager().getMessage("setup-must-break-banner"));
            event.setCancelled(true);
            return;
        }
        
        // Check if banner color matches team color (optional validation)
        Material expectedBanner = switch (session.teamColor) {
            case RED -> Material.RED_BANNER;
            case BLUE -> Material.BLUE_BANNER;
        };
        
        if (block.getType() != expectedBanner) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("team_name", session.teamColor.getName());
            placeholders.put("team_color", session.teamColor.getColorCode());
            player.sendMessage(plugin.getConfigManager().getMessage("setup-wrong-banner-color", placeholders));
            event.setCancelled(true);
            return;
        }
        
        // Cancel the block break (we don't want to actually destroy it)
        event.setCancelled(true);
        
        // Handle flag placement
        boolean success = plugin.getArenaManager().handleFlagPlacement(player, block.getLocation());
        
        if (success) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("team_name", session.teamColor.getName());
            placeholders.put("team_color", session.teamColor.getColorCode());
            placeholders.put("arena", session.arenaName);
            player.sendMessage(plugin.getConfigManager().getMessage("setup-flag-set", placeholders));
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("error-generic"));
        }
    }
    
    /**
     * Check arena protection and handle flag interactions
     */
    private void checkArenaProtection(BlockBreakEvent event, Player player, Block block) {
        // Check if player is in any active CTF game
        var ctfPlayer = plugin.getGameManager().getCTFPlayer(player);
        if (ctfPlayer == null || !ctfPlayer.isInGame()) {
            return;
        }
        
        var game = ctfPlayer.getGame();
        if (game == null) {
            return;
        }

        Arena arena = game.getArena();
        
        // Check if block is within the arena region
        if (!plugin.getWorldGuardManager().isLocationInRegion(block.getLocation(), arena.getWorldGuardRegion())) {
            return;
        }
        
        // During gameplay, blocks cannot be broken except banners
        if (game.getState() == org.cwresports.ctfcore.models.GameState.PLAYING) {
            // Allow banners to be broken for flag interactions
            if (block.getType().name().contains("BANNER")) {
                handleFlagInteraction(player, ctfPlayer, game, block);
            }
            // Cancel all other block breaking
            event.setCancelled(true);
        } else {
            // During lobby/waiting phase, prevent all block breaking
            event.setCancelled(true);
            
            // Still allow banner interaction for flag setup
            if (block.getType().name().contains("BANNER")) {
                handleFlagInteraction(player, ctfPlayer, game, block);
            }
        }
    }
    
    /**
     * Handle flag taking when player breaks flag banner
     */
    private void handleFlagInteraction(Player player, org.cwresports.ctfcore.models.CTFPlayer ctfPlayer, 
                                      org.cwresports.ctfcore.models.CTFGame game, Block block) {
        
        if (ctfPlayer.getTeam() == null) {
            return;
        }
        
        Arena arena = game.getArena();
        
        // Check each team's flag location
        for (Arena.TeamColor teamColor : Arena.TeamColor.values()) {
            Arena.Team teamData = arena.getTeam(teamColor);
            
            if (teamData.getFlagLocation() != null && 
                teamData.getFlagLocation().getBlock().equals(block)) {
                
                // This is a team's flag
                if (teamColor == ctfPlayer.getTeam()) {
                    // Player trying to take own team's flag
                    player.sendMessage(plugin.getConfigManager().getMessage("flag-own-team"));
                    return;
                }
                
                // Try to take enemy flag
                var flag = game.getFlag(teamColor);
                if (flag != null && flag.isAtBase()) {
                    if (game.takeFlag(ctfPlayer, teamColor)) {
                        // Successfully took flag
                        player.playSound(player.getLocation(), 
                            plugin.getConfigManager().getSound("flag_taken"), 1.0f, 1.0f);
                        
                        // Show capture hint message
                        player.sendMessage(plugin.getConfigManager().getMessage("capture-flag-hint"));
                    } else {
                        // Could not take flag (maybe need to return own flag first)
                        player.sendMessage(plugin.getConfigManager().getMessage("flag-already-taken"));
                    }
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("flag-already-taken"));
                }
                
                break;
            }
        }
    }
}