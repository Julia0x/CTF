package org.cwresports.ctfcore.cosmetics.managers;

import org.bukkit.entity.Player;
import org.cwresports.ctfcore.CTFCore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages player achievements and rewards
 */
public class AchievementManager {

    private final CTFCore plugin;
    private final Map<UUID, Integer> playerAchievements;

    public AchievementManager(CTFCore plugin) {
        this.plugin = plugin;
        this.playerAchievements = new HashMap<>();
    }

    /**
     * Award achievement to player by UUID
     */
    public void awardAchievement(UUID playerUuid, String achievementType) {
        // Achievement logic here
        int currentCount = playerAchievements.getOrDefault(playerUuid, 0);
        playerAchievements.put(playerUuid, currentCount + 1);
    }

    /**
     * Award achievement to player (converts Player to UUID)
     */
    public void awardAchievement(Player player, String achievementType) {
        if (player != null) {
            awardAchievement(player.getUniqueId(), achievementType);
        }
    }

    /**
     * Check if player has achievement
     */
    public boolean hasAchievement(UUID playerUuid, String achievementType) {
        return playerAchievements.containsKey(playerUuid);
    }

    /**
     * Check if player has achievement (converts Player to UUID)
     */
    public boolean hasAchievement(Player player, String achievementType) {
        if (player != null) {
            return hasAchievement(player.getUniqueId(), achievementType);
        }
        return false;
    }

    /**
     * Get achievement count for player
     */
    public int getAchievementCount(UUID playerUuid) {
        return playerAchievements.getOrDefault(playerUuid, 0);
    }

    /**
     * Get achievement count for player (converts Player to UUID)
     */
    public int getAchievementCount(Player player) {
        if (player != null) {
            return getAchievementCount(player.getUniqueId());
        }
        return 0;
    }

    /**
     * Remove player data on quit
     */
    public void removePlayerData(Player player) {
        if (player != null) {
            playerAchievements.remove(player.getUniqueId());
        }
    }
}