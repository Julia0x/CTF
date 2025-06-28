package org.cwresports.ctfcore.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.HologramLeaderboard;
import org.cwresports.ctfcore.models.LeaderboardEntry;
import org.cwresports.ctfcore.models.LeaderboardType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages all hologram leaderboards for the CTF plugin
 */
public class HologramLeaderboardManager {

    private final CTFCore plugin;
    private final Map<String, HologramLeaderboard> leaderboards;
    private final Map<LeaderboardType, List<LeaderboardEntry>> cachedData;
    private BukkitTask updateTask;
    private boolean decentHologramsEnabled = false;

    public HologramLeaderboardManager(CTFCore plugin) {
        this.plugin = plugin;
        this.leaderboards = new ConcurrentHashMap<>();
        this.cachedData = new ConcurrentHashMap<>();
        
        checkDecentHologramsDependency();
        loadConfiguration();
        startUpdateTask();
    }

    /**
     * Check if DecentHolograms plugin is available
     */
    private void checkDecentHologramsDependency() {
        if (Bukkit.getPluginManager().getPlugin("DecentHolograms") != null) {
            decentHologramsEnabled = true;
            plugin.getLogger().info("DecentHolograms found! Hologram leaderboards enabled.");
        } else {
            plugin.getLogger().warning("DecentHolograms not found! Hologram leaderboards will be disabled.");
        }
    }

    /**
     * Create a new hologram leaderboard
     */
    public boolean createLeaderboard(String id, LeaderboardType type, Location location, int size) {
        if (!decentHologramsEnabled) {
            plugin.getLogger().warning("Cannot create leaderboard - DecentHolograms not available!");
            return false;
        }

        if (leaderboards.containsKey(id)) {
            plugin.getLogger().warning("Leaderboard with ID '" + id + "' already exists!");
            return false;
        }

        if (size < 1 || size > 15) {
            plugin.getLogger().warning("Invalid leaderboard size: " + size + ". Must be between 1 and 15.");
            return false;
        }

        try {
            HologramLeaderboard leaderboard = new HologramLeaderboard(id, type, location, size);
            leaderboards.put(id, leaderboard);
            leaderboard.createHologram();
            
            saveConfiguration();
            plugin.getLogger().info("Created leaderboard '" + id + "' of type " + type + " with size " + size);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create leaderboard '" + id + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Delete a hologram leaderboard
     */
    public boolean deleteLeaderboard(String id) {
        HologramLeaderboard leaderboard = leaderboards.get(id);
        if (leaderboard == null) {
            return false;
        }

        leaderboard.deleteHologram();
        leaderboards.remove(id);
        saveConfiguration();
        
        plugin.getLogger().info("Deleted leaderboard: " + id);
        return true;
    }

    /**
     * Move a leaderboard to a new location
     */
    public boolean moveLeaderboard(String id, Location newLocation) {
        HologramLeaderboard leaderboard = leaderboards.get(id);
        if (leaderboard == null) {
            return false;
        }

        leaderboard.moveHologram(newLocation);
        saveConfiguration();
        
        plugin.getLogger().info("Moved leaderboard '" + id + "' to new location");
        return true;
    }

    /**
     * Update all leaderboards with fresh data
     */
    public void updateAllLeaderboards() {
        if (!decentHologramsEnabled) {
            return;
        }

        // Refresh cached data
        refreshCachedData();

        // Update all active leaderboards
        for (HologramLeaderboard leaderboard : leaderboards.values()) {
            if (leaderboard.isEnabled() && leaderboard.isActive()) {
                leaderboard.updateContent();
            }
        }
    }

    /**
     * Get top players for a specific leaderboard type
     */
    public List<LeaderboardEntry> getTopPlayers(LeaderboardType type, int limit) {
        List<LeaderboardEntry> cached = cachedData.get(type);
        if (cached != null) {
            return cached.stream().limit(limit).collect(Collectors.toList());
        }

        // If not cached, calculate and cache
        List<LeaderboardEntry> entries = calculateTopPlayers(type, limit);
        cachedData.put(type, entries);
        return entries;
    }

    /**
     * Calculate top players from player data
     */
    private List<LeaderboardEntry> calculateTopPlayers(LeaderboardType type, int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        FileConfiguration config = plugin.getConfigManager().getConfig("playerdata.yml");
        
        if (config == null) {
            return entries;
        }

        ConfigurationSection playersSection = config.getConfigurationSection("players");
        if (playersSection == null) {
            return entries;
        }

        Map<UUID, PlayerStats> playerStats = new HashMap<>();

        // Collect all player stats
        for (String playerId : playersSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(playerId);
                ConfigurationSection playerSection = playersSection.getConfigurationSection(playerId);
                
                if (playerSection != null) {
                    PlayerStats stats = new PlayerStats(
                        uuid,
                        getPlayerName(uuid),
                        playerSection.getInt("total_kills", 0),
                        playerSection.getInt("total_captures", 0),
                        playerSection.getInt("level", 1),
                        playerSection.getInt("games_won", 0)
                    );
                    playerStats.put(uuid, stats);
                }
            } catch (IllegalArgumentException e) {
                // Invalid UUID, skip
                continue;
            }
        }

        // Sort and create entries based on type
        List<PlayerStats> sortedStats = new ArrayList<>(playerStats.values());
        
        switch (type) {
            case KILLS:
                sortedStats.sort((a, b) -> Integer.compare(b.totalKills, a.totalKills));
                break;
            case CAPTURES:
                sortedStats.sort((a, b) -> Integer.compare(b.totalCaptures, a.totalCaptures));
                break;
            case LEVEL:
                sortedStats.sort((a, b) -> Integer.compare(b.level, a.level));
                break;
            case GAMES_WON:
                sortedStats.sort((a, b) -> Integer.compare(b.gamesWon, a.gamesWon));
                break;
        }

        // Create leaderboard entries
        for (int i = 0; i < Math.min(sortedStats.size(), limit); i++) {
            PlayerStats stats = sortedStats.get(i);
            Object value = getStatValue(stats, type);
            
            entries.add(new LeaderboardEntry(
                stats.playerId,
                stats.playerName,
                value,
                i + 1
            ));
        }

        return entries;
    }

    /**
     * Get stat value based on leaderboard type
     */
    private Object getStatValue(PlayerStats stats, LeaderboardType type) {
        switch (type) {
            case KILLS: return stats.totalKills;
            case CAPTURES: return stats.totalCaptures;
            case LEVEL: return stats.level;
            case GAMES_WON: return stats.gamesWon;
            default: return 0;
        }
    }

    /**
     * Get player name from UUID
     */
    private String getPlayerName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return player.getName();
        }
        
        // Try to get from offline player
        return Bukkit.getOfflinePlayer(uuid).getName();
    }

    /**
     * Refresh cached data for all leaderboard types
     */
    private void refreshCachedData() {
        for (LeaderboardType type : LeaderboardType.values()) {
            cachedData.put(type, calculateTopPlayers(type, 15)); // Cache top 15 for all types
        }
    }

    /**
     * Start the automatic update task
     */
    private void startUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
        }

        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAllLeaderboards();
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 30L, 20L * 30L); // Update every 30 seconds
    }

    /**
     * Load leaderboards from configuration
     */
    public void loadConfiguration() {
        FileConfiguration config = plugin.getConfigManager().getConfig("leaderboards.yml");
        if (config == null) {
            plugin.getLogger().info("No leaderboards configuration found, creating default.");
            saveConfiguration();
            return;
        }

        ConfigurationSection leaderboardsSection = config.getConfigurationSection("leaderboards");
        if (leaderboardsSection == null) {
            return;
        }

        for (String id : leaderboardsSection.getKeys(false)) {
            try {
                ConfigurationSection section = leaderboardsSection.getConfigurationSection(id);
                if (section == null) continue;

                LeaderboardType type = LeaderboardType.fromString(section.getString("type"));
                if (type == null) continue;

                ConfigurationSection locationSection = section.getConfigurationSection("location");
                if (locationSection == null) continue;

                World world = Bukkit.getWorld(locationSection.getString("world"));
                if (world == null) continue;

                Location location = new Location(
                    world,
                    locationSection.getDouble("x"),
                    locationSection.getDouble("y"),
                    locationSection.getDouble("z")
                );

                int size = section.getInt("size", 5);
                boolean enabled = section.getBoolean("enabled", true);

                HologramLeaderboard leaderboard = new HologramLeaderboard(id, type, location, size);
                leaderboard.setEnabled(enabled);
                
                if (decentHologramsEnabled && enabled) {
                    leaderboard.createHologram();
                }
                
                leaderboards.put(id, leaderboard);
                plugin.getLogger().info("Loaded leaderboard: " + id);

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load leaderboard '" + id + "': " + e.getMessage());
            }
        }
    }

    /**
     * Save leaderboards to configuration
     */
    public void saveConfiguration() {
        FileConfiguration config = plugin.getConfigManager().getConfig("leaderboards.yml");
        if (config == null) {
            return;
        }

        // Clear existing leaderboards section
        config.set("leaderboards", null);

        // Save settings
        config.set("settings.update-interval", 30);
        config.set("settings.default-size", 5);
        config.set("settings.max-size", 15);
        config.set("settings.auto-cleanup", true);

        // Save each leaderboard
        for (Map.Entry<String, HologramLeaderboard> entry : leaderboards.entrySet()) {
            String id = entry.getKey();
            HologramLeaderboard leaderboard = entry.getValue();
            Location loc = leaderboard.getLocation();

            String path = "leaderboards." + id;
            config.set(path + ".type", leaderboard.getType().toString());
            config.set(path + ".location.world", loc.getWorld().getName());
            config.set(path + ".location.x", loc.getX());
            config.set(path + ".location.y", loc.getY());
            config.set(path + ".location.z", loc.getZ());
            config.set(path + ".size", leaderboard.getSize());
            config.set(path + ".enabled", leaderboard.isEnabled());
        }

        plugin.getConfigManager().saveConfig("leaderboards.yml");
    }

    /**
     * Cleanup method for plugin disable
     */
    public void cleanup() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        for (HologramLeaderboard leaderboard : leaderboards.values()) {
            leaderboard.deleteHologram();
        }

        leaderboards.clear();
        cachedData.clear();
        
        plugin.getLogger().info("Hologram leaderboard manager cleaned up.");
    }

    /**
     * Get all leaderboards
     */
    public Collection<HologramLeaderboard> getAllLeaderboards() {
        return leaderboards.values();
    }

    /**
     * Get leaderboard by ID
     */
    public HologramLeaderboard getLeaderboard(String id) {
        return leaderboards.get(id);
    }

    /**
     * Check if DecentHolograms is enabled
     */
    public boolean isDecentHologramsEnabled() {
        return decentHologramsEnabled;
    }

    /**
     * Inner class to hold player statistics
     */
    private static class PlayerStats {
        final UUID playerId;
        final String playerName;
        final int totalKills;
        final int totalCaptures;
        final int level;
        final int gamesWon;

        PlayerStats(UUID playerId, String playerName, int totalKills, int totalCaptures, int level, int gamesWon) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.totalKills = totalKills;
            this.totalCaptures = totalCaptures;
            this.level = level;
            this.gamesWon = gamesWon;
        }
    }
}