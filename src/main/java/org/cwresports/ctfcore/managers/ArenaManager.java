package org.cwresports.ctfcore.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.Arena;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all CTF arenas including creation, validation, and setup modes
 * Updated to remove boss bar functionality from admin interface
 */
public class ArenaManager {

    private final CTFCore plugin;
    private final Map<String, Arena> arenas;
    private final Map<Player, SetupSession> activeSetupSessions;

    public enum SetupMode {
        FLAG_PLACEMENT,
        CAPTURE_POINT_PLACEMENT
    }

    public static class SetupSession {
        public final Player player;
        public final String arenaName;
        public final Arena.TeamColor teamColor;
        public final SetupMode mode;

        public SetupSession(Player player, String arenaName, Arena.TeamColor teamColor, SetupMode mode) {
            this.player = player;
            this.arenaName = arenaName;
            this.teamColor = teamColor;
            this.mode = mode;
        }
    }

    public ArenaManager(CTFCore plugin) {
        this.plugin = plugin;
        this.arenas = new ConcurrentHashMap<>();
        this.activeSetupSessions = new ConcurrentHashMap<>();
    }

    /**
     * Load all arenas from configuration
     * Only loads arenas that are marked as enabled
     */
    public void loadArenas() {
        arenas.clear();

        FileConfiguration config = plugin.getConfigManager().getArenas();
        ConfigurationSection arenasSection = config.getConfigurationSection("arenas");

        if (arenasSection == null) {
            plugin.getLogger().info("No arenas section found in arenas.yml");
            return;
        }

        int totalLoaded = 0;
        int enabledLoaded = 0;

        for (String arenaName : arenasSection.getKeys(false)) {
            ConfigurationSection arenaSection = arenasSection.getConfigurationSection(arenaName);
            if (arenaSection != null) {
                Arena arena = Arena.fromConfig(arenaName, arenaSection);
                arenas.put(arenaName, arena);
                totalLoaded++;

                if (arena.isEnabled()) {
                    enabledLoaded++;
                    plugin.getLogger().info("Loaded enabled arena: " + arenaName);
                } else {
                    plugin.getLogger().info("Loaded disabled arena: " + arenaName);
                }
            }
        }

        plugin.getLogger().info("Loaded " + totalLoaded + " arenas (" + enabledLoaded + " enabled)");
    }

    /**
     * Save all arenas to configuration
     */
    public void saveAllArenas() {
        FileConfiguration config = plugin.getConfigManager().getArenas();

        if (config == null) {
            plugin.getLogger().warning("Cannot save arenas - configuration not available");
            return;
        }

        // Clear existing arenas section
        config.set("arenas", null);

        // Save all arenas
        for (Arena arena : arenas.values()) {
            ConfigurationSection arenaSection = config.createSection("arenas." + arena.getName());
            arena.saveToConfig(arenaSection);
        }

        plugin.getConfigManager().saveConfig("arenas.yml");
        plugin.getLogger().info("Saved " + arenas.size() + " arenas to configuration");
    }

    /**
     * Save arenas (alias for saveAllArenas for compatibility)
     */
    public void saveArenas() {
        saveAllArenas();
    }

    /**
     * Create a new arena
     */
    public boolean createArena(String name, String worldGuardRegion, String worldName) {
        if (arenas.containsKey(name)) {
            return false; // Arena already exists
        }

        // Validate WorldGuard region exists
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            return false; // World doesn't exist
        }

        if (!plugin.getWorldGuardManager().regionExists(world, worldGuardRegion)) {
            return false; // WorldGuard region doesn't exist
        }

        Arena arena = new Arena(name);
        arena.setWorldGuardRegion(worldGuardRegion);
        arena.setWorldName(worldName);

        arenas.put(name, arena);

        // Save immediately to prevent loss
        saveArena(arena);

        plugin.getLogger().info("Created new arena: " + name);
        return true;
    }

    /**
     * Delete an arena and clean up any setup modes
     */
    public boolean deleteArena(String name) {
        Arena arena = arenas.get(name);
        if (arena == null) {
            return false;
        }

        // Clear any active setup sessions for this arena
        clearSetupMode(name);

        // Remove from memory
        arenas.remove(name);

        // Remove from configuration
        FileConfiguration config = plugin.getConfigManager().getArenas();
        config.set("arenas." + name, null);
        plugin.getConfigManager().saveConfig("arenas.yml");

        plugin.getLogger().info("Deleted arena: " + name);
        return true;
    }

    /**
     * Delete incomplete arena (called when admin quits without completing setup)
     */
    public void deleteIncompleteArena(String name) {
        Arena arena = arenas.get(name);
        if (arena != null && !arena.isFullyConfigured()) {
            deleteArena(name);
            plugin.getLogger().info("Deleted incomplete arena: " + name);
        }
    }

    /**
     * Validate and enable an arena
     */
    public ValidationResult validateAndEnableArena(String name) {
        Arena arena = arenas.get(name);
        if (arena == null) {
            return new ValidationResult(false, "Arena not found");
        }

        if (!arena.isFullyConfigured()) {
            return new ValidationResult(false, "Arena is not fully configured");
        }

        // Validate WorldGuard region still exists
        World world = arena.getWorld();
        if (world == null) {
            return new ValidationResult(false, "Arena world is not loaded");
        }

        if (!plugin.getWorldGuardManager().regionExists(world, arena.getWorldGuardRegion())) {
            return new ValidationResult(false, "WorldGuard region no longer exists");
        }

        // Enable the arena
        arena.setEnabled(true);
        saveArena(arena);

        plugin.getLogger().info("Enabled arena: " + name);
        return new ValidationResult(true, "Arena enabled successfully");
    }

    /**
     * Start flag setup mode for a player
     */
    public boolean startFlagSetup(Player player, String arenaName, Arena.TeamColor teamColor) {
        Arena arena = arenas.get(arenaName);
        if (arena == null) {
            return false;
        }

        // Clear any existing setup session for this player
        clearPlayerSetupSession(player);

        // Create new setup session
        SetupSession session = new SetupSession(player, arenaName, teamColor, SetupMode.FLAG_PLACEMENT);
        activeSetupSessions.put(player, session);

        return true;
    }

    /**
     * Start capture point setup mode for a player
     */
    public boolean startCaptureSetup(Player player, String arenaName, Arena.TeamColor teamColor) {
        Arena arena = arenas.get(arenaName);
        if (arena == null) {
            return false;
        }

        // Clear any existing setup session for this player
        clearPlayerSetupSession(player);

        // Create new setup session
        SetupSession session = new SetupSession(player, arenaName, teamColor, SetupMode.CAPTURE_POINT_PLACEMENT);
        activeSetupSessions.put(player, session);

        return true;
    }

    /**
     * Handle flag placement from block break event - NO BOSS BAR
     */
    public boolean handleFlagPlacement(Player player, Location location) {
        SetupSession session = activeSetupSessions.get(player);
        if (session == null || session.mode != SetupMode.FLAG_PLACEMENT) {
            return false;
        }

        Arena arena = arenas.get(session.arenaName);
        if (arena == null) {
            clearPlayerSetupSession(player);
            return false;
        }

        // Set flag location
        arena.getTeam(session.teamColor).setFlagLocation(location);

        // Save arena
        saveArena(arena);

        // Clear setup session
        clearPlayerSetupSession(player);

        // Update admin scoreboard only (no boss bar)
        plugin.getScoreboardManager().updateAdminScoreboard(player, arena);

        return true;
    }

    /**
     * Handle capture point placement from player interact event - NO BOSS BAR
     */
    public boolean handleCapturePointPlacement(Player player, Location location) {
        SetupSession session = activeSetupSessions.get(player);
        if (session == null || session.mode != SetupMode.CAPTURE_POINT_PLACEMENT) {
            return false;
        }

        Arena arena = arenas.get(session.arenaName);
        if (arena == null) {
            clearPlayerSetupSession(player);
            return false;
        }

        // Set capture point location
        arena.getTeam(session.teamColor).setCapturePoint(location);

        // Save arena
        saveArena(arena);

        // Clear setup session
        clearPlayerSetupSession(player);

        // Update admin scoreboard only (no boss bar)
        plugin.getScoreboardManager().updateAdminScoreboard(player, arena);

        return true;
    }

    /**
     * Check if player is in any setup mode
     */
    public boolean isPlayerInSetupMode(Player player) {
        return activeSetupSessions.containsKey(player);
    }

    /**
     * Get player's current setup session
     */
    public SetupSession getPlayerSetupSession(Player player) {
        return activeSetupSessions.get(player);
    }

    /**
     * Clear setup mode for a specific arena
     */
    public void clearSetupMode(String arenaName) {
        // Remove all setup sessions for this arena
        activeSetupSessions.entrySet().removeIf(entry ->
                entry.getValue().arenaName.equals(arenaName));
    }

    /**
     * Clear setup session for a specific player
     */
    public void clearPlayerSetupSession(Player player) {
        activeSetupSessions.remove(player);
    }

    /**
     * Clear all setup modes (called on plugin disable)
     */
    public void clearAllSetupModes() {
        activeSetupSessions.clear();
    }

    /**
     * Handle player quit - clean up incomplete arenas
     */
    public void handlePlayerQuit(Player player) {
        SetupSession session = activeSetupSessions.get(player);
        if (session != null) {
            // Clear the setup session
            clearPlayerSetupSession(player);

            // Check if arena is incomplete and delete it
            Arena arena = arenas.get(session.arenaName);
            if (arena != null && !arena.isFullyConfigured()) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("arena", session.arenaName);

                // Notify other online admins
                String message = plugin.getConfigManager().getMessage("arena-incomplete-deleted", placeholders);
                plugin.getServer().getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission("ctf.admin"))
                        .forEach(p -> p.sendMessage(message));

                deleteArena(session.arenaName);
            }
        }
    }

    /**
     * Save a specific arena
     */
    private void saveArena(Arena arena) {
        FileConfiguration config = plugin.getConfigManager().getArenas();

        if (config == null) {
            plugin.getLogger().warning("Cannot save arena " + arena.getName() + " - configuration not available");
            return;
        }

        ConfigurationSection arenaSection = config.createSection("arenas." + arena.getName());
        arena.saveToConfig(arenaSection);
        plugin.getConfigManager().saveConfig("arenas.yml");
    }

    // Getters
    public Arena getArena(String name) {
        return arenas.get(name);
    }

    public Collection<Arena> getAllArenas() {
        return arenas.values();
    }

    public Collection<Arena> getEnabledArenas() {
        return arenas.values().stream()
                .filter(Arena::isEnabled)
                .toList();
    }

    public Set<String> getArenaNames() {
        return arenas.keySet();
    }

    /**
     * Clear admin scoreboard and remove from tracking
     */
    public void clearAdminScoreboard(Player admin) {
        if (admin != null && admin.isOnline()) {
            admin.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            plugin.getScoreboardManager().removeAdminViewingArena(admin);
        }
    }

    public static class ValidationResult {
        private final boolean success;
        private final String message;

        public ValidationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}