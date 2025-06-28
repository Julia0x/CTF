package org.cwresports.ctfcore.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.CTFPlayer;

/**
 * Handles player quit events with level system data saving
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
    }
}