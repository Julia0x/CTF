package org.cwresports.ctfcore.cosmetics.managers;

import org.bukkit.entity.Player;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.cosmetics.models.Achievement;

import java.util.*;

/**
 * Manages player achievements and rewards
 */
public class AchievementManager {

    private final CTFCore plugin;
    private final Map<String, Achievement> achievements;
    private final Map<UUID, Map<String, Integer>> playerProgress;
    private final Map<UUID, Set<String>> playerUnlocked;

    public AchievementManager(CTFCore plugin) {
        this.plugin = plugin;
        this.achievements = new HashMap<>();
        this.playerProgress = new HashMap<>();
        this.playerUnlocked = new HashMap<>();
        initializeAchievements();
    }

    private void initializeAchievements() {
        // Initialize default achievements
        achievements.put("first_kill", new Achievement("first_kill", "First Blood", "Get your first kill", Achievement.AchievementCategory.KILLS, 1, Arrays.asList("fire_kill")));
        achievements.put("kill_streak_5", new Achievement("kill_streak_5", "Killing Spree", "Get 5 kills in a row", Achievement.AchievementCategory.KILLS, 5, Arrays.asList("explosion_death")));
        achievements.put("first_flag", new Achievement("first_flag", "Flag Runner", "Capture your first flag", Achievement.AchievementCategory.FLAGS, 1, Arrays.asList("firework_victory")));
        achievements.put("flag_master", new Achievement("flag_master", "Flag Master", "Capture 100 flags", Achievement.AchievementCategory.FLAGS, 100, Arrays.asList("legendary_trail")));
        achievements.put("first_game", new Achievement("first_game", "Welcome to CTF", "Play your first game", Achievement.AchievementCategory.GAMES, 1, Arrays.asList("default_kill")));
        achievements.put("game_veteran", new Achievement("game_veteran", "CTF Veteran", "Play 500 games", Achievement.AchievementCategory.GAMES, 500, Arrays.asList("veteran_banner")));
    }

    /**
     * Load player achievement data
     */
    public void loadPlayerData(Player player) {
        if (player != null) {
            UUID playerId = player.getUniqueId();
            playerProgress.computeIfAbsent(playerId, k -> new HashMap<>());
            playerUnlocked.computeIfAbsent(playerId, k -> new HashSet<>());
        }
    }

    /**
     * Save player achievement data
     */
    public void savePlayerData(Player player) {
        // Data is already saved in memory - could implement file/database saving here
    }

    /**
     * Handle kill event for achievements
     */
    public void handleKill(Player player) {
        if (player != null) {
            incrementProgress(player, "first_kill", 1);
            incrementProgress(player, "kill_streak_5", 1);
        }
    }

    /**
     * Handle death event for achievements
     */
    public void handleDeath(Player player) {
        // Could track death-related achievements here if needed
    }

    /**
     * Handle flag capture for achievements
     */
    public void handleFlagCapture(Player player) {
        if (player != null) {
            incrementProgress(player, "first_flag", 1);
            incrementProgress(player, "flag_master", 1);
        }
    }

    /**
     * Handle flag return for achievements
     */
    public void handleFlagReturn(Player player) {
        // Could track flag return achievements here if needed
    }

    /**
     * Handle game win for achievements
     */
    public void handleGameWin(Player player) {
        if (player != null) {
            incrementProgress(player, "first_game", 1);
            incrementProgress(player, "game_veteran", 1);
        }
    }

    /**
     * Handle game play for achievements
     */
    public void handleGamePlay(Player player) {
        if (player != null) {
            incrementProgress(player, "first_game", 1);
            incrementProgress(player, "game_veteran", 1);
        }
    }

    /**
     * Get completion percentage for player
     */
    public double getCompletionPercentage(Player player) {
        if (player == null) return 0.0;
        
        Set<String> unlocked = playerUnlocked.getOrDefault(player.getUniqueId(), Collections.emptySet());
        return achievements.isEmpty() ? 0.0 : (double) unlocked.size() / achievements.size() * 100.0;
    }

    /**
     * Get unlocked achievements for player
     */
    public Set<String> getUnlockedAchievements(Player player) {
        if (player != null) {
            return playerUnlocked.getOrDefault(player.getUniqueId(), Collections.emptySet());
        }
        return Collections.emptySet();
    }

    /**
     * Get all achievements
     */
    public Collection<Achievement> getAllAchievements() {
        return achievements.values();
    }

    /**
     * Get achievements by category
     */
    public List<Achievement> getAchievementsByCategory(Achievement.AchievementCategory category) {
        return achievements.values().stream()
                .filter(achievement -> achievement.getCategory() == category)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Check if player has unlocked achievement
     */
    public boolean hasUnlocked(Player player, String achievementId) {
        if (player != null) {
            return playerUnlocked.getOrDefault(player.getUniqueId(), Collections.emptySet()).contains(achievementId);
        }
        return false;
    }

    /**
     * Get progress for specific achievement
     */
    public int getProgress(Player player, String achievementId) {
        if (player != null) {
            return playerProgress.getOrDefault(player.getUniqueId(), Collections.emptyMap()).getOrDefault(achievementId, 0);
        }
        return 0;
    }

    /**
     * Award achievement to player by UUID
     */
    public void awardAchievement(UUID playerUuid, String achievementType) {
        playerUnlocked.computeIfAbsent(playerUuid, k -> new HashSet<>()).add(achievementType);
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
        return playerUnlocked.getOrDefault(playerUuid, Collections.emptySet()).contains(achievementType);
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
        return playerUnlocked.getOrDefault(playerUuid, Collections.emptySet()).size();
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
            savePlayerData(player); // Save before removing
            // Keep data persistent - don't actually remove from memory
        }
    }

    /**
     * Increment progress for an achievement
     */
    private void incrementProgress(Player player, String achievementId, int amount) {
        if (player == null || !achievements.containsKey(achievementId)) return;

        UUID playerId = player.getUniqueId();
        Map<String, Integer> progress = playerProgress.computeIfAbsent(playerId, k -> new HashMap<>());
        
        int currentProgress = progress.getOrDefault(achievementId, 0) + amount;
        progress.put(achievementId, currentProgress);

        Achievement achievement = achievements.get(achievementId);
        if (currentProgress >= achievement.getMaxProgress() && !hasUnlocked(player, achievementId)) {
            unlockAchievement(player, achievementId);
        }
    }

    /**
     * Unlock an achievement for a player
     */
    private void unlockAchievement(Player player, String achievementId) {
        if (player == null) return;

        UUID playerId = player.getUniqueId();
        playerUnlocked.computeIfAbsent(playerId, k -> new HashSet<>()).add(achievementId);

        Achievement achievement = achievements.get(achievementId);
        if (achievement != null) {
            // Give rewards
            for (String reward : achievement.getRewards()) {
                plugin.getCosmeticsManager().giveCosmetic(player, reward);
            }
            
            // Notify player
            player.sendMessage("§a✅ Achievement Unlocked: §e" + achievement.getName());
        }
    }
}