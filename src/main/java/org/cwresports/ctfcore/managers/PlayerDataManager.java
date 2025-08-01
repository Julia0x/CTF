package org.cwresports.ctfcore.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.cwresports.ctfcore.CTFCore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages persistent player data including levels and experience
 */
public class PlayerDataManager {

    private final CTFCore plugin;

    public PlayerDataManager(CTFCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Load player data from configuration
     */
    public Map<String, Object> loadPlayerData(UUID playerId) {
        FileConfiguration config = plugin.getConfigManager().getConfig("playerdata.yml");

        if (config == null) {
            return getDefaultPlayerData();
        }

        ConfigurationSection playerSection = config.getConfigurationSection("players." + playerId.toString());

        if (playerSection == null) {
            return getDefaultPlayerData();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("level", playerSection.getInt("level", 1));
        data.put("experience", playerSection.getInt("experience", 0));
        data.put("total_kills", playerSection.getInt("total_kills", 0));
        data.put("total_deaths", playerSection.getInt("total_deaths", 0));
        data.put("total_captures", playerSection.getInt("total_captures", 0));
        data.put("total_flag_returns", playerSection.getInt("total_flag_returns", 0));
        data.put("games_played", playerSection.getInt("games_played", 0));
        data.put("games_won", playerSection.getInt("games_won", 0));

        return data;
    }

    /**
     * Save player data to configuration
     */
    public void savePlayerData(UUID playerId, Map<String, Object> data) {
        FileConfiguration config = plugin.getConfigManager().getConfig("playerdata.yml");

        if (config == null) {
            plugin.getLogger().warning("Could not save player data - playerdata.yml not available");
            return;
        }

        String playerPath = "players." + playerId.toString();

        config.set(playerPath + ".level", data.get("level"));
        config.set(playerPath + ".experience", data.get("experience"));
        config.set(playerPath + ".total_kills", data.get("total_kills"));
        config.set(playerPath + ".total_deaths", data.get("total_deaths"));
        config.set(playerPath + ".total_captures", data.get("total_captures"));
        config.set(playerPath + ".total_flag_returns", data.get("total_flag_returns"));
        config.set(playerPath + ".games_played", data.get("games_played"));
        config.set(playerPath + ".games_won", data.get("games_won"));

        plugin.getConfigManager().saveConfig("playerdata.yml");
    }

    /**
     * Save CTFPlayer data
     */
    public void savePlayerData(org.cwresports.ctfcore.models.CTFPlayer ctfPlayer) {
        if (ctfPlayer.getPlayer() == null) {
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("level", ctfPlayer.getLevel());
        data.put("experience", ctfPlayer.getExperience());
        data.put("total_kills", ctfPlayer.getTotalKills());
        data.put("total_deaths", ctfPlayer.getTotalDeaths());
        data.put("total_captures", ctfPlayer.getTotalCaptures());
        data.put("total_flag_returns", ctfPlayer.getTotalFlagReturns());
        data.put("games_played", ctfPlayer.getGamesPlayed());
        data.put("games_won", ctfPlayer.getGamesWon());

        savePlayerData(ctfPlayer.getPlayerId(), data);
    }

    /**
     * Get default player data for new players
     */
    private Map<String, Object> getDefaultPlayerData() {
        Map<String, Object> data = new HashMap<>();
        data.put("level", 1);
        data.put("experience", 0);
        data.put("total_kills", 0);
        data.put("total_deaths", 0);
        data.put("total_captures", 0);
        data.put("total_flag_returns", 0);
        data.put("games_played", 0);
        data.put("games_won", 0);
        return data;
    }

    /**
     * Calculate XP required for a specific level
     */
    public int getXPRequiredForLevel(int level) {
        int baseXP = plugin.getConfigManager().getGameplaySetting("experience.level-up-base-xp", 100);
        int multiplier = plugin.getConfigManager().getGameplaySetting("experience.level-up-multiplier", 50);

        return baseXP + ((level - 1) * multiplier);
    }

    /**
     * Get total XP required to reach a level (cumulative)
     */
    public int getTotalXPForLevel(int level) {
        int totalXP = 0;
        for (int i = 2; i <= level; i++) {
            totalXP += getXPRequiredForLevel(i);
        }
        return totalXP;
    }
}