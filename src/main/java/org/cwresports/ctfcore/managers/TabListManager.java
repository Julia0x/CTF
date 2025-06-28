package org.cwresports.ctfcore.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.Arena;
import org.cwresports.ctfcore.models.CTFGame;
import org.cwresports.ctfcore.models.CTFPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages tab list display with arena isolation and clean formatting
 */
public class TabListManager {
    
    private final CTFCore plugin;
    private final Map<UUID, Set<UUID>> playerVisiblePlayers;
    private final UpdateTask updateTask;
    
    public TabListManager(CTFCore plugin) {
        this.plugin = plugin;
        this.playerVisiblePlayers = new ConcurrentHashMap<>();
        
        // Start update task if tab list is enabled
        boolean enabled = plugin.getConfigManager().getScoreboards().getBoolean("tablist.enabled", true);
        int updateInterval = plugin.getConfigManager().getScoreboards().getInt("tablist.update-interval-ticks", 20);
        
        if (enabled) {
            this.updateTask = new UpdateTask();
            this.updateTask.runTaskTimer(plugin, 0L, updateInterval);
        } else {
            this.updateTask = null;
        }
    }
    
    /**
     * Update tab list for a specific player
     */
    public void updatePlayerTabList(Player player) {
        if (!plugin.getConfigManager().getScoreboards().getBoolean("tablist.enabled", true)) {
            return;
        }
        
        boolean arenaIsolation = plugin.getConfigManager().getScoreboards().getBoolean("tablist.arena-isolation", true);
        
        // Get header and footer
        String header = plugin.getConfigManager().getScoreboards().getString("tablist.header", "");
        String footer = plugin.getConfigManager().getScoreboards().getString("tablist.footer", "");
        
        header = processPlaceholders(player, header);
        footer = processPlaceholders(player, footer);
        
        // Set header and footer
        player.setPlayerListHeaderFooter(header, footer);
        
        if (arenaIsolation) {
            updateArenaIsolatedTabList(player);
        } else {
            updateGlobalTabList(player);
        }
    }
    
    /**
     * Update tab list showing only arena players
     */
    private void updateArenaIsolatedTabList(Player player) {
        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(player);
        Set<UUID> visiblePlayers = new HashSet<>();
        
        if (ctfPlayer != null && ctfPlayer.isInGame()) {
            CTFGame game = ctfPlayer.getGame();
            if (game != null) {
                // Show only players in the same game
                for (CTFPlayer gamePlayer : game.getPlayers()) {
                    if (gamePlayer.getPlayer() != null && gamePlayer.getPlayer().isOnline()) {
                        visiblePlayers.add(gamePlayer.getPlayer().getUniqueId());
                    }
                }
            }
        } else {
            // Show only lobby players (not in games)
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                CTFPlayer onlineCTFPlayer = plugin.getGameManager().getCTFPlayer(onlinePlayer);
                if (onlineCTFPlayer == null || !onlineCTFPlayer.isInGame()) {
                    visiblePlayers.add(onlinePlayer.getUniqueId());
                }
            }
        }
        
        // Update visible players for this player
        playerVisiblePlayers.put(player.getUniqueId(), visiblePlayers);
        
        // Hide/show players
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (visiblePlayers.contains(onlinePlayer.getUniqueId())) {
                player.showPlayer(plugin, onlinePlayer);
                updatePlayerListName(player, onlinePlayer);
            } else {
                player.hidePlayer(plugin, onlinePlayer);
            }
        }
    }
    
    /**
     * Update global tab list (show all players)
     */
    private void updateGlobalTabList(Player player) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            player.showPlayer(plugin, onlinePlayer);
            updatePlayerListName(player, onlinePlayer);
        }
    }
    
    /**
     * Update player list name with formatting
     */
    private void updatePlayerListName(Player viewer, Player target) {
        String format = plugin.getConfigManager().getScoreboards().getString("tablist.format", "{team_prefix}{level_prefix}&f{player}");
        
        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(target);
        
        // Team prefix
        String teamPrefix = "";
        if (ctfPlayer != null && ctfPlayer.getTeam() != null) {
            Arena.TeamColor teamColor = ctfPlayer.getTeam();
            teamPrefix = teamColor.getColorCode() + "● ";
        }
        
        // Level prefix
        String levelPrefix = "";
        if (ctfPlayer != null) {
            int level = ctfPlayer.getLevel();
            if (level >= 50) {
                levelPrefix = "&6✦ ";
            } else if (level >= 25) {
                levelPrefix = "&e★ ";
            } else if (level >= 10) {
                levelPrefix = "&a⭐ ";
            }
        }
        
        // Replace placeholders
        String displayName = format
                .replace("{team_prefix}", teamPrefix)
                .replace("{level_prefix}", levelPrefix)
                .replace("{player}", target.getName())
                .replace("{level}", ctfPlayer != null ? String.valueOf(ctfPlayer.getLevel()) : "1");
        
        displayName = ChatColor.translateAlternateColorCodes('&', displayName);
        target.setPlayerListName(displayName);
    }
    
    /**
     * Handle player join event
     */
    public void onPlayerJoin(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            updatePlayerTabList(player);
            
            // Update tab list for all other players if arena isolation is enabled
            if (plugin.getConfigManager().getScoreboards().getBoolean("tablist.arena-isolation", true)) {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (!onlinePlayer.equals(player)) {
                        updatePlayerTabList(onlinePlayer);
                    }
                }
            }
        }, 10L); // Delay to ensure player is fully loaded
    }
    
    /**
     * Handle player quit event
     */
    public void onPlayerQuit(Player player) {
        playerVisiblePlayers.remove(player.getUniqueId());
        
        // Update tab list for all other players if arena isolation is enabled
        if (plugin.getConfigManager().getScoreboards().getBoolean("tablist.arena-isolation", true)) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.equals(player)) {
                    updatePlayerTabList(onlinePlayer);
                }
            }
        }
    }
    
    /**
     * Handle player joining/leaving games
     */
    public void onPlayerGameStateChange(Player player) {
        if (plugin.getConfigManager().getScoreboards().getBoolean("tablist.arena-isolation", true)) {
            // Update tab list for all players since game states changed
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                updatePlayerTabList(onlinePlayer);
            }
        }
    }
    
    /**
     * Process placeholders in text
     */
    private String processPlaceholders(Player player, String text) {
        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(player);
        
        text = text.replace("{player}", player.getName());
        text = text.replace("{level}", ctfPlayer != null ? String.valueOf(ctfPlayer.getLevel()) : "1");
        text = text.replace("{online_players}", String.valueOf(Bukkit.getOnlinePlayers().size()));
        
        // Process PlaceholderAPI placeholders
        text = plugin.processPlaceholders(player, text);
        
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    /**
     * Shutdown tab list manager
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        
        // Reset all player list names
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setPlayerListName(player.getName());
            player.setPlayerListHeaderFooter("", "");
        }
        
        playerVisiblePlayers.clear();
    }
    
    /**
     * Update task for periodic tab list updates
     */
    private class UpdateTask extends BukkitRunnable {
        @Override
        public void run() {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updatePlayerTabList(player);
            }
        }
    }
}