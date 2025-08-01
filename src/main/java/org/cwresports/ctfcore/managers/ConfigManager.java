package org.cwresports.ctfcore.managers;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.cwresports.ctfcore.CTFCore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages all configuration files for the plugin including level system settings
 * Updated to support scoreboards.yml
 */
public class ConfigManager {

    private final CTFCore plugin;
    private final Map<String, FileConfiguration> configs;
    private final Map<String, File> configFiles;

    public ConfigManager(CTFCore plugin) {
        this.plugin = plugin;
        this.configs = new HashMap<>();
        this.configFiles = new HashMap<>();
    }

    /**
     * Load all configuration files
     */
    public void loadAll() {
        loadConfig("config.yml");
        loadConfig("messages.yml");
        loadConfig("scoreboards.yml");  // NEW: Load scoreboards config
        createConfig("arenas.yml"); // Create if doesn't exist
        loadConfig("arenas.yml");
        createConfig("playerdata.yml"); // Create player data file
        loadConfig("playerdata.yml");
    }

    /**
     * Load a specific configuration file
     */
    public void loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);

        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Load defaults from jar if available
        InputStream defConfigStream = plugin.getResource(fileName);
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            config.setDefaults(defConfig);
        }

        configs.put(fileName, config);
        configFiles.put(fileName, file);

        plugin.getLogger().info("Loaded configuration: " + fileName);
    }

    /**
     * Create a configuration file if it doesn't exist
     */
    public void createConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);

        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();

                // Create empty YAML structure for specific files
                if (fileName.equals("arenas.yml")) {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    config.createSection("arenas");
                    config.save(file);
                } else if (fileName.equals("playerdata.yml")) {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    config.createSection("players");
                    config.save(file);
                }

                plugin.getLogger().info("Created configuration file: " + fileName);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create " + fileName + ": " + e.getMessage());
            }
        }
    }

    /**
     * Save a specific configuration file
     */
    public void saveConfig(String fileName) {
        FileConfiguration config = configs.get(fileName);
        File file = configFiles.get(fileName);

        if (config != null && file != null) {
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save " + fileName + ": " + e.getMessage());
            }
        }
    }

    /**
     * Get a configuration by filename
     */
    public FileConfiguration getConfig(String fileName) {
        return configs.get(fileName);
    }

    /**
     * Get the main plugin configuration
     */
    public FileConfiguration getMainConfig() {
        return getConfig("config.yml");
    }

    /**
     * Get the messages configuration
     */
    public FileConfiguration getMessages() {
        return getConfig("messages.yml");
    }

    /**
     * Get the scoreboards configuration
     */
    public FileConfiguration getScoreboards() {
        return getConfig("scoreboards.yml");
    }

    /**
     * Get the arenas configuration
     */
    public FileConfiguration getArenas() {
        return getConfig("arenas.yml");
    }

    /**
     * Get a formatted message with color codes and placeholders
     */
    public String getMessage(String key, Map<String, String> placeholders) {
        String message = getMessages().getString(key, "&cMessage not found: " + key);

        // Apply placeholders
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        // Apply color codes
        message = ChatColor.translateAlternateColorCodes('&', message);

        // Add prefix if not a help message or status display
        if (!key.startsWith("help-") && !key.startsWith("status-") && !key.equals("plugin-prefix")) {
            String prefix = ChatColor.translateAlternateColorCodes('&', getMessages().getString("plugin-prefix", ""));
            message = prefix + message;
        }

        return message;
    }

    /**
     * Get a formatted message without placeholders
     */
    public String getMessage(String key) {
        return getMessage(key, null);
    }

    /**
     * Get a sound by configuration key
     */
    public Sound getSound(String key) {
        String soundName = getMainConfig().getString("sounds." + key, "BLOCK_NOTE_BLOCK_PLING");
        try {
            return Sound.valueOf(soundName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound name in config: " + soundName + ". Using default.");
            return Sound.BLOCK_NOTE_BLOCK_PLING;
        }
    }

    /**
     * Get gameplay setting as integer
     */
    public int getGameplaySetting(String key, int defaultValue) {
        return getMainConfig().getInt("gameplay." + key, defaultValue);
    }

    /**
     * Get gameplay setting as boolean
     */
    public boolean getGameplaySetting(String key, boolean defaultValue) {
        return getMainConfig().getBoolean("gameplay." + key, defaultValue);
    }

    /**
     * Get gameplay setting as string
     */
    public String getGameplaySetting(String key, String defaultValue) {
        return getMainConfig().getString("gameplay." + key, defaultValue);
    }
}