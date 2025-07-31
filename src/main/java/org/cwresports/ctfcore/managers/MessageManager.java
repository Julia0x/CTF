package org.cwresports.ctfcore.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.CTFGame;
import org.cwresports.ctfcore.models.CTFPlayer;
import org.cwresports.ctfcore.models.GameState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Enhanced message manager with comprehensive color code support and proper formatting
 * Handles title messages, boss bars, and chat messages with consistent color processing
 */
public class MessageManager {
    
    private final CTFCore plugin;
    private final Map<UUID, BossBar> playerBossBars;
    private final Map<UUID, BossBar> spawnProtectionBars;
    private final Map<UUID, BukkitTask> spawnProtectionTasks;

    public MessageManager(CTFCore plugin) {
        this.plugin = plugin;
        this.playerBossBars = new ConcurrentHashMap<>();
        this.spawnProtectionBars = new ConcurrentHashMap<>();
        this.spawnProtectionTasks = new ConcurrentHashMap<>();
    }
    
    /**
     * Send title message to player with enhanced color code handling
     */
    public void sendTitle(Player player, String titleKey, String subtitleKey, Map<String, String> placeholders) {
        if (!plugin.getConfigManager().getMainConfig().getBoolean("messages.show-titles", true)) {
            return;
        }
        
        String title = plugin.getConfigManager().getMessage(titleKey, placeholders);
        String subtitle = subtitleKey != null ? plugin.getConfigManager().getMessage(subtitleKey, placeholders) : "";
        
        // Process PlaceholderAPI placeholders
        title = plugin.processPlaceholders(player, title);
        subtitle = plugin.processPlaceholders(player, subtitle);
        
        // Remove prefix from title messages
        String prefix = plugin.getConfigManager().getMessage("plugin-prefix", new HashMap<>());
        title = title.replace(prefix, "");
        subtitle = subtitle.replace(prefix, "");
        
        // Send title with proper timing
        player.sendTitle(title, subtitle, 10, 40, 10);
    }
    
    /**
     * Send title message to all players in game
     */
    public void sendGameTitle(CTFGame game, String titleKey, String subtitleKey, Map<String, String> placeholders) {
        for (CTFPlayer ctfPlayer : game.getPlayers()) {
            Player player = ctfPlayer.getPlayer();
            if (player != null && player.isOnline()) {
                sendTitle(player, titleKey, subtitleKey, placeholders);
            }
        }
    }
    
    /**
     * Create or update boss bar for player with enhanced color processing
     */
    public void updateBossBar(Player player, String messageKey, Map<String, String> placeholders, double progress) {
        if (!plugin.getConfigManager().getMainConfig().getBoolean("messages.show-boss-bar", true)) {
            return;
        }
        
        BossBar bossBar = playerBossBars.get(player.getUniqueId());
        if (bossBar == null) {
            String colorName = plugin.getConfigManager().getMainConfig().getString("messages.boss-bar-color", "YELLOW");
            String styleName = plugin.getConfigManager().getMainConfig().getString("messages.boss-bar-style", "SOLID");
            
            BarColor color;
            try {
                color = BarColor.valueOf(colorName.toUpperCase());
            } catch (IllegalArgumentException e) {
                color = BarColor.YELLOW;
            }
            
            BarStyle style;
            try {
                style = BarStyle.valueOf(styleName.toUpperCase());
            } catch (IllegalArgumentException e) {
                style = BarStyle.SOLID;
            }
            
            bossBar = Bukkit.createBossBar("", color, style);
            bossBar.addPlayer(player);
            playerBossBars.put(player.getUniqueId(), bossBar);
        }
        
        String message = plugin.getConfigManager().getMessage(messageKey, placeholders);
        
        // Process PlaceholderAPI placeholders
        message = plugin.processPlaceholders(player, message);
        
        // Remove prefix from boss bar messages
        String prefix = plugin.getConfigManager().getMessage("plugin-prefix", new HashMap<>());
        message = message.replace(prefix, "");
        
        bossBar.setTitle(message);
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
    }
    
    /**
     * Update boss bar for all players in game
     */
    public void updateGameBossBar(CTFGame game, String messageKey, Map<String, String> placeholders, double progress) {
        for (CTFPlayer ctfPlayer : game.getPlayers()) {
            Player player = ctfPlayer.getPlayer();
            if (player != null && player.isOnline()) {
                updateBossBar(player, messageKey, placeholders, progress);
            }
        }
    }
    
    /**
     * Show enhanced spawn protection boss bar with proper color formatting
     */
    public void showSpawnProtectionBossBar(Player player) {
        // Remove any existing spawn protection boss bar
        removeSpawnProtectionBossBar(player);
        
        // Create new spawn protection boss bar
        BossBar spawnBar = Bukkit.createBossBar("", BarColor.GREEN, BarStyle.SOLID);
        spawnBar.addPlayer(player);
        spawnProtectionBars.put(player.getUniqueId(), spawnBar);
        
        // Start countdown task
        BukkitTask task = new BukkitRunnable() {
            int timeLeft = 5;
            
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    removeSpawnProtectionBossBar(player);
                    return;
                }
                
                if (timeLeft <= 0) {
                    // Protection ended
                    String endMessage = ChatColor.translateAlternateColorCodes('&', "&c&l⚠ SPAWN PROTECTION ENDED");
                    spawnBar.setTitle(endMessage);
                    spawnBar.setColor(BarColor.RED);
                    spawnBar.setProgress(0.0);
                    
                    // Remove after 1 second
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        removeSpawnProtectionBossBar(player);
                    }, 20L);
                    
                    cancel();
                    return;
                }
                
                // Update boss bar
                String protectionText = ChatColor.translateAlternateColorCodes('&', String.format("&a&l⛨ SPAWN PROTECTION &a- %d seconds remaining", timeLeft));
                spawnBar.setTitle(protectionText);
                spawnBar.setProgress((double) timeLeft / 5.0);
                
                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
        
        spawnProtectionTasks.put(player.getUniqueId(), task);
    }
    
    /**
     * Remove spawn protection boss bar immediately (called when player attacks)
     */
    public void removeSpawnProtectionBossBar(Player player) {
        BossBar spawnBar = spawnProtectionBars.remove(player.getUniqueId());
        if (spawnBar != null) {
            spawnBar.removeAll();
        }
        
        BukkitTask task = spawnProtectionTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * Update lobby boss bar for game
     */
    public void updateLobbyBossBar(CTFGame game) {
        int currentPlayers = game.getPlayers().size();
        int minPlayers = plugin.getConfigManager().getGameplaySetting("min-players-to-start", 8);
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("current", String.valueOf(currentPlayers));
        placeholders.put("min", String.valueOf(minPlayers));
        
        double progress = Math.min(1.0, (double) currentPlayers / minPlayers);
        
        if (game.getState() == GameState.WAITING) {
            updateGameBossBar(game, "bossbar-waiting", placeholders, progress);
        }
    }
    
    /**
     * Update countdown boss bar for game
     */
    public void updateCountdownBossBar(CTFGame game, int timeLeft) {
        int countdownTime = plugin.getConfigManager().getGameplaySetting("pre-game-countdown-seconds", 20);
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("time", String.valueOf(timeLeft));
        
        double progress = (double) timeLeft / countdownTime;
        updateGameBossBar(game, "bossbar-countdown", placeholders, progress);
    }
    
    /**
     * Update game time boss bar with enhanced color processing
     */
    public void updateGameTimeBossBar(CTFGame game) {
        int totalTime = plugin.getConfigManager().getGameplaySetting("game-duration-minutes", 10) * 60;
        int timeLeft = game.getTimeLeft();
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("time", game.getFormattedTimeLeft());
        
        // Get team kills and flag scores
        Map<org.cwresports.ctfcore.models.Arena.TeamColor, Integer> teamKills = game.getTeamKills();
        Map<org.cwresports.ctfcore.models.Arena.TeamColor, Integer> flagScores = game.getScores();
        
        // Apply color processing for team kills (KILLS FIRST)
        String redKillsText = ChatColor.translateAlternateColorCodes('&', org.cwresports.ctfcore.models.Arena.TeamColor.RED.getColorCode() +
                                                     teamKills.getOrDefault(org.cwresports.ctfcore.models.Arena.TeamColor.RED, 0));
        String blueKillsText = ChatColor.translateAlternateColorCodes('&', org.cwresports.ctfcore.models.Arena.TeamColor.BLUE.getColorCode() +
                                                      teamKills.getOrDefault(org.cwresports.ctfcore.models.Arena.TeamColor.BLUE, 0));
        
        // Apply color processing for flag scores
        String redFlagsText = ChatColor.translateAlternateColorCodes('&', org.cwresports.ctfcore.models.Arena.TeamColor.RED.getColorCode() +
                                                     flagScores.getOrDefault(org.cwresports.ctfcore.models.Arena.TeamColor.RED, 0));
        String blueFlagsText = ChatColor.translateAlternateColorCodes('&', org.cwresports.ctfcore.models.Arena.TeamColor.BLUE.getColorCode() +
                                                      flagScores.getOrDefault(org.cwresports.ctfcore.models.Arena.TeamColor.BLUE, 0));
        
        placeholders.put("red_kills", redKillsText);
        placeholders.put("blue_kills", blueKillsText);
        placeholders.put("red_flags", redFlagsText);
        placeholders.put("blue_flags", blueFlagsText);
        
        double progress = (double) timeLeft / totalTime;
        
        // Check if we should use combined scoring mode
        if (game.isFlagsTiedAt2()) {
            updateGameBossBar(game, "bossbar-combined-scoring", placeholders, progress);
        } else {
            updateGameBossBar(game, "bossbar-kills-first", placeholders, progress);
        }
    }
    
    /**
     * Update boss bar for combined scoring mode (2-2 flags)
     */
    public void updateCombinedScoringBossBar(CTFGame game) {
        int totalTime = plugin.getConfigManager().getGameplaySetting("game-duration-minutes", 10) * 60;
        int timeLeft = game.getTimeLeft();
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("time", game.getFormattedTimeLeft());
        
        // Get combined scores
        Map<org.cwresports.ctfcore.models.Arena.TeamColor, Integer> combinedScores = game.getCombinedScores();
        
        // Apply color processing for combined scores
        String redScoreText = ChatColor.translateAlternateColorCodes('&', org.cwresports.ctfcore.models.Arena.TeamColor.RED.getColorCode() +
                                                     combinedScores.getOrDefault(org.cwresports.ctfcore.models.Arena.TeamColor.RED, 0));
        String blueScoreText = ChatColor.translateAlternateColorCodes('&', org.cwresports.ctfcore.models.Arena.TeamColor.BLUE.getColorCode() +
                                                      combinedScores.getOrDefault(org.cwresports.ctfcore.models.Arena.TeamColor.BLUE, 0));
        
        placeholders.put("red_score", redScoreText);
        placeholders.put("blue_score", blueScoreText);
        
        double progress = (double) timeLeft / totalTime;
        updateGameBossBar(game, "bossbar-combined-scoring", placeholders, progress);
    }
    
    /**
     * Update victory boss bar for game end
     */
    public void updateVictoryBossBar(CTFGame game, org.cwresports.ctfcore.models.Arena.TeamColor winner) {
        Map<String, String> placeholders = new HashMap<>();
        
        if (winner != null) {
            placeholders.put("team_color", ChatColor.translateAlternateColorCodes('&', winner.getColorCode()));
            placeholders.put("team_name", winner.getName().toUpperCase());
            updateGameBossBar(game, "bossbar-victory", placeholders, 1.0);
        } else {
            updateGameBossBar(game, "bossbar-draw", placeholders, 1.0);
        }
    }

    
    /**
     * Clear boss bar for player
     */
    public void clearBossBar(Player player) {
        BossBar bossBar = playerBossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            bossBar.removeAll();
        }
        
        // Also clear spawn protection boss bar
        removeSpawnProtectionBossBar(player);
    }
    
    /**
     * Clear boss bars for all players in game
     */
    public void clearGameBossBars(CTFGame game) {
        for (CTFPlayer ctfPlayer : game.getPlayers()) {
            Player player = ctfPlayer.getPlayer();
            if (player != null) {
                clearBossBar(player);
            }
        }
    }
    
    /**
     * Shutdown message manager
     */
    public void shutdown() {
        for (BossBar bossBar : playerBossBars.values()) {
            bossBar.removeAll();
        }
        playerBossBars.clear();
        
        for (BossBar spawnBar : spawnProtectionBars.values()) {
            spawnBar.removeAll();
        }
        spawnProtectionBars.clear();
        
        for (BukkitTask task : spawnProtectionTasks.values()) {
            task.cancel();
        }
        spawnProtectionTasks.clear();
    }
}