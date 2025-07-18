package org.cwresports.ctfcore.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.cwresports.ctfcore.CTFCore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles configuration file migration between plugin versions
 * Automatically detects legacy configs and updates them to new format
 */
public class ConfigMigrationManager {

    private final CTFCore plugin;
    private final String CURRENT_VERSION = "1.1.0";
    private final DateTimeFormatter BACKUP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public ConfigMigrationManager(CTFCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Check and migrate all configuration files
     */
    public void checkAndMigrateConfigs() {
        plugin.getLogger().info("Checking configuration versions...");

        List<String> migratedConfigs = new ArrayList<>();

        // Check each config file
        if (migrateConfig("config.yml")) {
            migratedConfigs.add("config.yml");
        }
        if (migrateConfig("messages.yml")) {
            migratedConfigs.add("messages.yml");
        }
        if (migrateConfig("scoreboards.yml")) {
            migratedConfigs.add("scoreboards.yml");
        }
        if (migrateConfig("leaderboards.yml")) {
            migratedConfigs.add("leaderboards.yml");
        }

        if (!migratedConfigs.isEmpty()) {
            plugin.getLogger().info("Successfully migrated " + migratedConfigs.size() + " configuration files to version " + CURRENT_VERSION);
            plugin.getLogger().info("Migrated files: " + String.join(", ", migratedConfigs));
            plugin.getLogger().info("Backup files created in /backups/ folder");
        } else {
            plugin.getLogger().info("All configuration files are up to date (version " + CURRENT_VERSION + ")");
        }
    }

    /**
     * Migrate a specific configuration file
     */
    private boolean migrateConfig(String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName);
        
        if (!configFile.exists()) {
            return false; // File doesn't exist, skip migration
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        String currentVersion = config.getString("config-version", "legacy");

        // Check if migration is needed
        if (CURRENT_VERSION.equals(currentVersion)) {
            return false; // Already up to date
        }

        try {
            // Create backup
            createBackup(configFile);

            // Perform migration based on current version
            if ("legacy".equals(currentVersion)) {
                migrateFromLegacy(fileName, config);
            } else {
                // Handle other version migrations in the future
                migrateFromVersion(fileName, config, currentVersion);
            }

            // Set new version and save
            config.set("config-version", CURRENT_VERSION);
            config.save(configFile);

            plugin.getLogger().info("Migrated " + fileName + " from " + currentVersion + " to " + CURRENT_VERSION);
            return true;

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to migrate " + fileName + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Create backup of configuration file
     */
    private void createBackup(File configFile) throws IOException {
        File backupDir = new File(plugin.getDataFolder(), "backups");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        String timestamp = LocalDateTime.now().format(BACKUP_DATE_FORMAT);
        String backupName = configFile.getName().replace(".yml", "_" + timestamp + ".yml");
        File backupFile = new File(backupDir, backupName);

        Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Migrate from legacy version (no version number)
     */
    private void migrateFromLegacy(String fileName, FileConfiguration config) {
        switch (fileName) {
            case "config.yml":
                migrateLegacyConfig(config);
                break;
            case "messages.yml":
                migrateLegacyMessages(config);
                break;
            case "scoreboards.yml":
                migrateLegacyScoreboards(config);
                break;
            case "leaderboards.yml":
                migrateLegacyLeaderboards(config);
                break;
        }
    }

    /**
     * Migrate from specific version (for future versions)
     */
    private void migrateFromVersion(String fileName, FileConfiguration config, String fromVersion) {
        // Handle future version migrations here
        plugin.getLogger().info("Migration from version " + fromVersion + " to " + CURRENT_VERSION + " for " + fileName);
    }

    /**
     * Migrate legacy config.yml
     */
    private void migrateLegacyConfig(FileConfiguration config) {
        // Add new autojoin section if it doesn't exist
        if (!config.contains("autojoin")) {
            config.createSection("autojoin");
            config.set("autojoin.enabled", true);
            config.set("autojoin.prefer-populated-games", true);
            config.set("autojoin.min-players-threshold", 2);
            config.set("autojoin.item-name", "&a&lAuto Join Game");
            config.set("autojoin.item-lore", List.of(
                "&7Click to automatically join",
                "&7a game with other players!",
                "",
                "&ePrefers games with more players"
            ));
        }

        // Add new server lobby section if it doesn't exist
        if (!config.contains("server-lobby")) {
            config.createSection("server-lobby");
            config.set("server-lobby.give-items", true);
            config.set("server-lobby.clear-inventory", true);
        }

        // Ensure all other sections exist with defaults
        ensureConfigDefaults(config);
    }

    /**
     * Migrate legacy messages.yml
     */
    private void migrateLegacyMessages(FileConfiguration config) {
        // Add new autojoin messages
        if (!config.contains("autojoin-searching")) {
            config.set("autojoin-searching", "&e⏳ Searching for available games...");
        }
        if (!config.contains("autojoin-found")) {
            config.set("autojoin-found", "&a✅ Found game! Joining arena &e{arena}&a...");
        }
        if (!config.contains("autojoin-no-games")) {
            config.set("autojoin-no-games", "&c❌ No games available right now. Try again later!");
        }
        if (!config.contains("autojoin-joined-populated")) {
            config.set("autojoin-joined-populated", "&a🎮 Joined game with {player_count} other players!");
        }
        if (!config.contains("autojoin-joined-random")) {
            config.set("autojoin-joined-random", "&a🎲 Started a new game in arena &e{arena}&a!");
        }
    }

    /**
     * Migrate legacy scoreboards.yml
     */
    private void migrateLegacyScoreboards(FileConfiguration config) {
        // Add any new scoreboard configurations
        if (!config.contains("placeholders.autojoin-status")) {
            config.set("placeholders.autojoin-status.searching", "&e⏳ Searching...");
            config.set("placeholders.autojoin-status.found", "&a✅ Found game!");
            config.set("placeholders.autojoin-status.none", "&7No games");
        }
    }

    /**
     * Migrate legacy leaderboards.yml  
     */
    private void migrateLegacyLeaderboards(FileConfiguration config) {
        // Add any new leaderboard configurations
        if (!config.contains("settings.migration-notes")) {
            config.set("settings.migration-notes", "Migrated from legacy version");
        }
    }

    /**
     * Ensure all default config values exist
     */
    private void ensureConfigDefaults(FileConfiguration config) {
        // Gameplay defaults
        if (!config.contains("gameplay.min-players-to-start")) {
            config.set("gameplay.min-players-to-start", 8);
        }
        if (!config.contains("gameplay.max-players-per-arena")) {
            config.set("gameplay.max-players-per-arena", 8);
        }
        
        // Server defaults
        if (!config.contains("server.lobby-spawn")) {
            config.set("server.lobby-spawn", null);
        }
        
        // Sound defaults
        if (!config.contains("sounds.autojoin_found")) {
            config.set("sounds.autojoin_found", "ENTITY_PLAYER_LEVELUP");
        }
        if (!config.contains("sounds.autojoin_searching")) {
            config.set("sounds.autojoin_searching", "BLOCK_NOTE_BLOCK_PLING");
        }
    }

    /**
     * Get current version
     */
    public String getCurrentVersion() {
        return CURRENT_VERSION;
    }

    /**
     * Check if config file needs migration
     */
    public boolean needsMigration(String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName);
        if (!configFile.exists()) {
            return false;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        String currentVersion = config.getString("config-version", "legacy");
        return !CURRENT_VERSION.equals(currentVersion);
    }
}