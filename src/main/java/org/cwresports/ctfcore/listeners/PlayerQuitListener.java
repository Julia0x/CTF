package org.cwresports.ctfcore.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.CTFPlayer;

/**
 * Enhanced player quit listener with comprehensive cleanup
 * Handles player quit events with level system data saving and proper cleanup
 */
public class PlayerQuitListener implements Listener {

    private final CTFCore plugin;

    public PlayerQuitListener(CTFCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save player data before they quit
        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(event.getPlayer());
        if (ctfPlayer != null) {
            plugin.getPlayerDataManager().savePlayerData(ctfPlayer);
        }

        // ENHANCED: Handle flag carrier disconnect edge case
        plugin.getGameManager().handlePlayerDisconnect(event.getPlayer());

        // Handle setup mode cleanup and incomplete arena deletion
        plugin.getArenaManager().handlePlayerQuit(event.getPlayer());

        // Clear scoreboard and admin arena viewing
        plugin.getScoreboardManager().clearPlayerScoreboard(event.getPlayer());
        plugin.getScoreboardManager().removeAdminViewingArena(event.getPlayer());
        
        // Update tab list for remaining players
        plugin.getTabListManager().onPlayerQuit(event.getPlayer());

        // Clean up lobby manager state
        plugin.getLobbyManager().onPlayerQuit(event.getPlayer());

        // Clean up player move listener capture attempts
        if (plugin.getServer().getPluginManager().getPlugin("CTF-Core") != null) {
            try {
                // Clean up any active automatic capture attempts
                // This is handled by the PlayerMoveListener's cleanup method
                Object moveListener = plugin.getServer().getPluginManager().getPlugin("CTF-Core");
                if (moveListener instanceof PlayerMoveListener) {
                    ((PlayerMoveListener) moveListener).cleanupPlayer(event.getPlayer());
                }
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }
}