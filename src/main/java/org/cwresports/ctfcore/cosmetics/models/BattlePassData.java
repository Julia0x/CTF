package org.cwresports.ctfcore.cosmetics.models;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents battle pass data for a player
 */
public class BattlePassData {
    
    private UUID playerId;
    private int currentTier;
    private int experience;
    private boolean hasPremium;
    private int season;
    private Map<Integer, Boolean> freeRewardsClaimed;
    private Map<Integer, Boolean> premiumRewardsClaimed;
    private long seasonStartTime;
    private long lastProgressTime;
    
    public BattlePassData(UUID playerId) {
        this.playerId = playerId;
        this.currentTier = 1;
        this.experience = 0;
        this.hasPremium = false;
        this.season = 1;
        this.freeRewardsClaimed = new HashMap<>();
        this.premiumRewardsClaimed = new HashMap<>();
        this.seasonStartTime = System.currentTimeMillis();
        this.lastProgressTime = System.currentTimeMillis();
    }
    
    // Getters
    public UUID getPlayerId() {
        return playerId;
    }
    
    public int getCurrentTier() {
        return currentTier;
    }
    
    public int getExperience() {
        return experience;
    }
    
    public boolean hasPremium() {
        return hasPremium;
    }
    
    public int getSeason() {
        return season;
    }
    
    public Map<Integer, Boolean> getFreeRewardsClaimed() {
        return freeRewardsClaimed;
    }
    
    public Map<Integer, Boolean> getPremiumRewardsClaimed() {
        return premiumRewardsClaimed;
    }
    
    public long getSeasonStartTime() {
        return seasonStartTime;
    }
    
    public long getLastProgressTime() {
        return lastProgressTime;
    }
    
    // Setters
    public void setCurrentTier(int currentTier) {
        this.currentTier = Math.max(1, currentTier);
    }
    
    public void setExperience(int experience) {
        this.experience = Math.max(0, experience);
    }
    
    public void setPremium(boolean hasPremium) {
        this.hasPremium = hasPremium;
    }
    
    public void setSeason(int season) {
        this.season = season;
    }
    
    public void setSeasonStartTime(long seasonStartTime) {
        this.seasonStartTime = seasonStartTime;
    }
    
    public void setLastProgressTime(long lastProgressTime) {
        this.lastProgressTime = lastProgressTime;
    }
    
    // Utility methods
    public void addExperience(int amount) {
        this.experience += amount;
        this.lastProgressTime = System.currentTimeMillis();
    }
    
    public boolean isRewardClaimed(int tier, boolean premium) {
        if (premium) {
            return premiumRewardsClaimed.getOrDefault(tier, false);
        } else {
            return freeRewardsClaimed.getOrDefault(tier, false);
        }
    }
    
    public void claimReward(int tier, boolean premium) {
        if (premium) {
            premiumRewardsClaimed.put(tier, true);
        } else {
            freeRewardsClaimed.put(tier, true);
        }
    }
    
    public boolean canClaimReward(int tier, boolean premium) {
        if (premium && !hasPremium) {
            return false;
        }
        return currentTier >= tier && !isRewardClaimed(tier, premium);
    }
    
    public void resetForNewSeason(int newSeason) {
        this.season = newSeason;
        this.currentTier = 1;
        this.experience = 0;
        this.freeRewardsClaimed.clear();
        this.premiumRewardsClaimed.clear();
        this.seasonStartTime = System.currentTimeMillis();
        this.lastProgressTime = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return "BattlePassData{" +
                "playerId=" + playerId +
                ", currentTier=" + currentTier +
                ", experience=" + experience +
                ", hasPremium=" + hasPremium +
                ", season=" + season +
                '}';
    }
}