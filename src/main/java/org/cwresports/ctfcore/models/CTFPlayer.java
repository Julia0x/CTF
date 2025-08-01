package org.cwresports.ctfcore.models;

import org.bukkit.entity.Player;
import org.cwresports.ctfcore.CTFCore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a player in a CTF game with game-specific data and persistent progression
 */
public class CTFPlayer {

    private final UUID playerId;
    private CTFGame game;
    private Arena.TeamColor team;
    private CTFFlag carryingFlag;

    // Game session statistics
    private int kills;
    private int deaths;
    private int captures;
    private int flagReturns;

    // Persistent progression data
    private int level;
    private int experience;
    private int totalKills;
    private int totalDeaths;
    private int totalCaptures;
    private int totalFlagReturns;
    private int gamesPlayed;
    private int gamesWon;

    // State
    private boolean alive;
    private long lastRespawnTime;

    public CTFPlayer(Player player) {
        this(player, 1, 0);
    }

    public CTFPlayer(Player player, int level, int experience) {
        this.playerId = player.getUniqueId();
        this.level = level;
        this.experience = experience;
        this.alive = true;
        this.kills = 0;
        this.deaths = 0;
        this.captures = 0;
        this.flagReturns = 0;
        this.totalKills = 0;
        this.totalDeaths = 0;
        this.totalCaptures = 0;
        this.totalFlagReturns = 0;
        this.gamesPlayed = 0;
        this.gamesWon = 0;
        this.lastRespawnTime = System.currentTimeMillis();
    }

    public CTFPlayer(Player player, Map<String, Object> data) {
        this.playerId = player.getUniqueId();
        this.level = (Integer) data.getOrDefault("level", 1);
        this.experience = (Integer) data.getOrDefault("experience", 0);
        this.totalKills = (Integer) data.getOrDefault("total_kills", 0);
        this.totalDeaths = (Integer) data.getOrDefault("total_deaths", 0);
        this.totalCaptures = (Integer) data.getOrDefault("total_captures", 0);
        this.totalFlagReturns = (Integer) data.getOrDefault("total_flag_returns", 0);
        this.gamesPlayed = (Integer) data.getOrDefault("games_played", 0);
        this.gamesWon = (Integer) data.getOrDefault("games_won", 0);
        this.alive = true;
        this.kills = 0;
        this.deaths = 0;
        this.captures = 0;
        this.flagReturns = 0;
        this.lastRespawnTime = System.currentTimeMillis();
    }

    /**
     * Get the Bukkit Player object
     */
    public Player getPlayer() {
        return org.bukkit.Bukkit.getPlayer(playerId);
    }

    /**
     * Check if player is currently in a game
     */
    public boolean isInGame() {
        return game != null;
    }

    /**
     * Check if player is carrying a flag
     */
    public boolean hasFlag() {
        return carryingFlag != null;
    }

    /**
     * Check if player is alive
     */
    public boolean isAlive() {
        return alive;
    }

    /**
     * Set player as dead (for respawn countdown)
     */
    public void setDead() {
        this.alive = false;
    }

    /**
     * Respawn the player
     */
    public void respawn() {
        this.alive = true;
        this.lastRespawnTime = System.currentTimeMillis();
    }

    /**
     * Add a kill to player stats and grant XP
     */
    public void addKill() {
        kills++;
        totalKills++;

        int xpPerKill = CTFCore.getInstance().getConfigManager().getGameplaySetting("experience.per-kill", 10);
        addExperience(xpPerKill);
    }

    /**
     * Add a death to player stats
     */
    public void addDeath() {
        deaths++;
        totalDeaths++;
        setDead();
    }

    /**
     * Add a capture to player stats and grant XP
     */
    public void addCapture() {
        captures++;
        totalCaptures++;

        int xpPerCapture = CTFCore.getInstance().getConfigManager().getGameplaySetting("experience.per-capture", 50);
        addExperience(xpPerCapture);
    }

    /**
     * Add a flag return to player stats and grant XP
     */
    public void addFlagReturn() {
        flagReturns++;
        totalFlagReturns++;

        int xpPerReturn = CTFCore.getInstance().getConfigManager().getGameplaySetting("experience.per-flag-return", 25);
        addExperience(xpPerReturn);
    }

    /**
     * Add experience and check for level up
     */
    public void addExperience(int amount) {
        if (amount <= 0) return;

        experience += amount;

        Player player = getPlayer();
        if (player != null && player.isOnline()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("amount", String.valueOf(amount));
            player.sendMessage(CTFCore.getInstance().getConfigManager().getMessage("xp-gained", placeholders));
        }

        checkLevelUp();
    }

    /**
     * Check if player should level up
     */
    private void checkLevelUp() {
        int xpRequired = CTFCore.getInstance().getPlayerDataManager().getXPRequiredForLevel(level + 1);

        if (experience >= xpRequired) {
            level++;
            experience -= xpRequired;

            Player player = getPlayer();
            if (player != null && player.isOnline()) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("level", String.valueOf(level));

                player.sendMessage(CTFCore.getInstance().getConfigManager().getMessage("level-up", placeholders));
                CTFCore.getInstance().getMessageManager().sendTitle(player, "title-level-up", null, placeholders);
                player.playSound(player.getLocation(),
                        CTFCore.getInstance().getConfigManager().getSound("level_up"), 1.0f, 1.0f);
            }

            // Check for multiple level ups
            checkLevelUp();
        }
    }

    /**
     * Add a game played
     */
    public void addGamePlayed() {
        gamesPlayed++;
    }

    /**
     * Add a game won
     */
    public void addGameWon() {
        gamesWon++;
    }

    /**
     * Get XP needed for next level
     */
    public int getXPForNextLevel() {
        return CTFCore.getInstance().getPlayerDataManager().getXPRequiredForLevel(level + 1);
    }

    /**
     * Get XP progress to next level as percentage
     */
    public double getXPProgress() {
        int xpRequired = getXPForNextLevel();
        return xpRequired > 0 ? (double) experience / xpRequired : 1.0;
    }

    /**
     * Get player's kill/death ratio
     */
    public double getKDRatio() {
        if (totalDeaths == 0) {
            return totalKills > 0 ? totalKills : 0.0;
        }
        return (double) totalKills / totalDeaths;
    }

    /**
     * Get player's win rate
     */
    public double getWinRate() {
        if (gamesPlayed == 0) {
            return 0.0;
        }
        return (double) gamesWon / gamesPlayed;
    }

    /**
     * Get player's score (kills + captures * 5 + returns * 2)
     */
    public int getScore() {
        return kills + (captures * 5) + (flagReturns * 2);
    }

    /**
     * Reset session statistics (not persistent data)
     */
    public void resetStats() {
        kills = 0;
        deaths = 0;
        captures = 0;
        flagReturns = 0;
    }

    /**
     * Check if player can respawn (respawn delay check)
     */
    public boolean canRespawn(int respawnDelaySeconds) {
        long timeSinceDeath = System.currentTimeMillis() - lastRespawnTime;
        return timeSinceDeath >= (respawnDelaySeconds * 1000L);
    }

    // Getters and setters

    public UUID getPlayerId() {
        return playerId;
    }

    public CTFGame getGame() {
        return game;
    }

    public void setGame(CTFGame game) {
        this.game = game;
    }

    public Arena.TeamColor getTeam() {
        return team;
    }

    public void setTeam(Arena.TeamColor team) {
        this.team = team;
    }

    public CTFFlag getCarryingFlag() {
        return carryingFlag;
    }

    public void setCarryingFlag(CTFFlag carryingFlag) {
        this.carryingFlag = carryingFlag;
    }

    // Session stats
    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getCaptures() {
        return captures;
    }

    public int getFlagReturns() {
        return flagReturns;
    }

    // Persistent progression
    public int getLevel() {
        return level;
    }

    public int getExperience() {
        return experience;
    }

    public int getTotalKills() {
        return totalKills;
    }

    public int getTotalDeaths() {
        return totalDeaths;
    }

    public int getTotalCaptures() {
        return totalCaptures;
    }

    public int getTotalFlagReturns() {
        return totalFlagReturns;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public int getGamesWon() {
        return gamesWon;
    }

    public long getLastRespawnTime() {
        return lastRespawnTime;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        CTFPlayer ctfPlayer = (CTFPlayer) obj;
        return playerId.equals(ctfPlayer.playerId);
    }

    @Override
    public int hashCode() {
        return playerId.hashCode();
    }

    @Override
    public String toString() {
        Player player = getPlayer();
        String playerName = player != null ? player.getName() : "Unknown";
        String teamName = team != null ? team.getName() : "None";

        return String.format("CTFPlayer{name=%s, level=%d, team=%s, score=%d, k/d=%d/%d}",
                playerName, level, teamName, getScore(), kills, deaths);
    }
}