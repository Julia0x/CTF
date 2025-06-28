package org.cwresports.ctfcore.cosmetics.managers;

import org.bukkit.entity.Player;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.cosmetics.models.BattlePassData;
import org.cwresports.ctfcore.cosmetics.models.CosmeticStorage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the battle pass system
 */
public class BattlePassManager {

    private final CTFCore plugin;
    private final CosmeticsManager cosmeticsManager;
    private final Map<UUID, BattlePassData> playerData;
    
    // Battle pass configuration
    private static final int CURRENT_SEASON = 1;
    private static final int MAX_TIER = 100;
    private static final int XP_PER_TIER = 1000;
    private static final long SEASON_DURATION_DAYS = 90; // 3 months
    private static final int PREMIUM_COST = 950; // coins
    
    // Season timing
    private final long seasonStartTime;
    private final long seasonEndTime;

    public BattlePassManager(CTFCore plugin) {
        this.plugin = plugin;
        this.cosmeticsManager = plugin.getCosmeticsManager();
        this.playerData = new ConcurrentHashMap<>();
        
        // Initialize season timing (for demo purposes, let's say season started 30 days ago)
        this.seasonStartTime = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        this.seasonEndTime = seasonStartTime + (SEASON_DURATION_DAYS * 24 * 60 * 60 * 1000);
    }

    /**
     * Get player battle pass data
     */
    public BattlePassData getPlayerData(Player player) {
        UUID playerId = player.getUniqueId();
        return playerData.computeIfAbsent(playerId, uuid -> {
            BattlePassData data = new BattlePassData(uuid);
            loadPlayerData(player); // Load from storage if exists
            return data;
        });
    }

    /**
     * Load player battle pass data
     */
    public void loadPlayerData(Player player) {
        if (player != null) {
            UUID playerId = player.getUniqueId();
            CosmeticStorage storage = cosmeticsManager.getStorage();
            Map<String, Object> data = storage.loadPlayerCosmetics(playerId);
            
            // Load battle pass specific data from storage
            if (data.containsKey("battlepass")) {
                Map<String, Object> bpData = (Map<String, Object>) data.get("battlepass");
                BattlePassData battlePassData = new BattlePassData(playerId);
                
                if (bpData.containsKey("tier")) {
                    battlePassData.setCurrentTier((Integer) bpData.get("tier"));
                }
                if (bpData.containsKey("experience")) {
                    battlePassData.setExperience((Integer) bpData.get("experience"));
                }
                if (bpData.containsKey("premium")) {
                    battlePassData.setPremium((Boolean) bpData.get("premium"));
                }
                if (bpData.containsKey("season")) {
                    battlePassData.setSeason((Integer) bpData.get("season"));
                }
                
                playerData.put(playerId, battlePassData);
            }
        }
    }

    /**
     * Save player battle pass data
     */
    public void savePlayerData(Player player) {
        if (player != null) {
            UUID playerId = player.getUniqueId();
            BattlePassData bpData = playerData.get(playerId);
            if (bpData != null) {
                CosmeticStorage storage = cosmeticsManager.getStorage();
                Map<String, Object> data = storage.loadPlayerCosmetics(playerId);
                
                Map<String, Object> battlePassMap = new HashMap<>();
                battlePassMap.put("tier", bpData.getCurrentTier());
                battlePassMap.put("experience", bpData.getExperience());
                battlePassMap.put("premium", bpData.hasPremium());
                battlePassMap.put("season", bpData.getSeason());
                
                data.put("battlepass", battlePassMap);
                storage.savePlayerCosmetics(playerId, data);
            }
        }
    }

    /**
     * Get current season number
     */
    public int getCurrentSeason() {
        return CURRENT_SEASON;
    }

    /**
     * Get days until season ends
     */
    public int getDaysUntilSeasonEnd() {
        long currentTime = System.currentTimeMillis();
        long timeRemaining = seasonEndTime - currentTime;
        return Math.max(0, (int) (timeRemaining / (24 * 60 * 60 * 1000)));
    }

    /**
     * Get maximum tier for battle pass
     */
    public int getMaxTier() {
        return MAX_TIER;
    }

    /**
     * Get tier information (XP required, rewards, etc.)
     */
    public Map<String, Object> getTier(int tier) {
        Map<String, Object> tierInfo = new HashMap<>();
        tierInfo.put("tier", tier);
        tierInfo.put("xpRequired", tier * XP_PER_TIER);
        tierInfo.put("freeReward", getFreeReward(tier));
        tierInfo.put("premiumReward", getPremiumReward(tier));
        return tierInfo;
    }

    /**
     * Check if reward is claimed
     */
    public boolean isRewardClaimed(Player player, int tier, boolean premium) {
        BattlePassData data = getPlayerData(player);
        return data.isRewardClaimed(tier, premium);
    }

    /**
     * Claim reward for specific tier
     */
    public boolean claimReward(Player player, int tier, boolean premium) {
        BattlePassData data = getPlayerData(player);
        
        if (!data.canClaimReward(tier, premium)) {
            return false;
        }
        
        data.claimReward(tier, premium);
        savePlayerData(player);
        
        // Award the actual reward
        awardTierReward(player, tier, premium);
        
        return true;
    }

    /**
     * Purchase premium battle pass
     */
    public boolean purchasePremium(Player player, int cost) {
        if (plugin.getCurrencyManager().hasEnough(player, cost)) {
            if (plugin.getCurrencyManager().removeCoins(player, cost)) {
                BattlePassData data = getPlayerData(player);
                data.setPremium(true);
                savePlayerData(player);
                
                player.sendMessage("§6✨ Premium Battle Pass activated! You now have access to premium rewards!");
                return true;
            }
        }
        
        player.sendMessage("§cInsufficient coins! You need " + cost + " coins to purchase Premium Battle Pass.");
        return false;
    }

    /**
     * Get player progress
     */
    public int getPlayerProgress(Player player) {
        BattlePassData data = getPlayerData(player);
        return data.getCurrentTier();
    }

    /**
     * Award experience to player
     */
    public void awardExperience(Player player, int amount) {
        BattlePassData data = getPlayerData(player);
        int oldTier = data.getCurrentTier();
        
        data.addExperience(amount);
        
        // Calculate new tier
        int newTier = Math.min(MAX_TIER, (data.getExperience() / XP_PER_TIER) + 1);
        data.setCurrentTier(newTier);
        
        // Check for tier ups
        if (newTier > oldTier) {
            player.sendMessage("§6🎉 Battle Pass Tier Up! You reached tier " + newTier + "!");
            // Auto-claim available rewards if configured
        }
        
        savePlayerData(player);
    }

    // Helper methods
    private String getFreeReward(int tier) {
        // Define free rewards for each tier
        if (tier % 10 == 0) {
            return "Special Cosmetic";
        } else if (tier % 5 == 0) {
            return "100 Coins";
        } else {
            return "50 Coins";
        }
    }

    private String getPremiumReward(int tier) {
        // Define premium rewards for each tier
        if (tier % 10 == 0) {
            return "Exclusive Premium Cosmetic";
        } else if (tier % 5 == 0) {
            return "200 Coins";
        } else {
            return "100 Coins";
        }
    }

    private void awardTierReward(Player player, int tier, boolean premium) {
        // Award the actual rewards (coins, cosmetics, etc.)
        if (premium) {
            // Award premium rewards
            if (tier % 10 == 0) {
                // Award special premium cosmetic
                player.sendMessage("§6✨ Unlocked exclusive premium cosmetic!");
            } else if (tier % 5 == 0) {
                plugin.getCurrencyManager().addCoins(player, 200);
            } else {
                plugin.getCurrencyManager().addCoins(player, 100);
            }
        } else {
            // Award free rewards
            if (tier % 10 == 0) {
                // Award special cosmetic
                player.sendMessage("§a✨ Unlocked special cosmetic!");
            } else if (tier % 5 == 0) {
                plugin.getCurrencyManager().addCoins(player, 100);
            } else {
                plugin.getCurrencyManager().addCoins(player, 50);
            }
        }
    }
}