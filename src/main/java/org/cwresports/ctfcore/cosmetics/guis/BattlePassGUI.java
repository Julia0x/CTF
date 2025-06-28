package org.cwresports.ctfcore.cosmetics.guis;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.cosmetics.managers.BattlePassManager;
import org.cwresports.ctfcore.cosmetics.models.BattlePassData;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * GUI for the Battle Pass system
 */
public class BattlePassGUI {

    private final CTFCore plugin;
    private final BattlePassManager battlePassManager;

    public BattlePassGUI(CTFCore plugin) {
        this.plugin = plugin;
        this.battlePassManager = plugin.getBattlePassManager();
    }

    /**
     * Open the main battle pass GUI
     */
    public void openBattlePass(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6✦ Battle Pass ✦");

        BattlePassData data = battlePassManager.getPlayerData(player);

        // Player progress display
        gui.setItem(4, createProgressDisplay(player, data));

        // Season info
        int currentSeason = battlePassManager.getCurrentSeason();
        gui.setItem(0, createSeasonInfo(currentSeason));
        int daysLeft = battlePassManager.getDaysUntilSeasonEnd();
        gui.setItem(8, createTimeRemainingInfo(daysLeft));

        // Tier progression (showing current tier and next few)
        int maxTier = battlePassManager.getMaxTier();
        int currentTier = data.getCurrentTier();
        
        for (int i = 0; i < 7; i++) {
            int tierToShow = Math.max(1, currentTier - 3 + i);
            if (tierToShow <= maxTier) {
                Map<String, Object> tier = battlePassManager.getTier(tierToShow);
                boolean freeRewardClaimed = battlePassManager.isRewardClaimed(player, tierToShow, false);
                boolean premiumRewardClaimed = battlePassManager.isRewardClaimed(player, tierToShow, true);
                
                gui.setItem(19 + i, createTierDisplay(tier, tierToShow, currentTier, freeRewardClaimed, premiumRewardClaimed, data.hasPremium()));
            }
        }

        // Premium purchase button (if not owned)
        if (!data.hasPremium()) {
            gui.setItem(40, createPremiumPurchaseButton());
        } else {
            gui.setItem(40, createPremiumOwnedDisplay());
        }

        // Navigation buttons
        gui.setItem(45, createPreviousPageButton());
        gui.setItem(53, createNextPageButton());
        gui.setItem(49, createCloseButton());

        player.openInventory(gui);
    }

    /**
     * Open tier-specific rewards view
     */
    public void openTierRewards(Player player, int tier) {
        Inventory gui = Bukkit.createInventory(null, 27, "§6Battle Pass - Tier " + tier);

        BattlePassData data = battlePassManager.getPlayerData(player);
        Map<String, Object> tierInfo = battlePassManager.getTier(tier);

        // Tier info
        gui.setItem(4, createTierInfoDisplay(tier, tierInfo));

        // Free reward
        boolean freeRewardClaimed = battlePassManager.isRewardClaimed(player, tier, false);
        gui.setItem(11, createRewardDisplay(tierInfo, false, freeRewardClaimed, data.getCurrentTier() >= tier));

        // Premium reward
        boolean premiumRewardClaimed = battlePassManager.isRewardClaimed(player, tier, true);
        gui.setItem(15, createRewardDisplay(tierInfo, true, premiumRewardClaimed, data.getCurrentTier() >= tier && data.hasPremium()));

        // Season info
        int currentSeason = battlePassManager.getCurrentSeason();
        gui.setItem(0, createSeasonInfo(currentSeason));
        int daysLeft = battlePassManager.getDaysUntilSeasonEnd();
        gui.setItem(8, createTimeRemainingInfo(daysLeft));

        // Tier progression display
        int maxTier = battlePassManager.getMaxTier();
        for (int i = 18; i < 27; i++) {
            int tierToShow = tier - 4 + (i - 18);
            if (tierToShow >= 1 && tierToShow <= maxTier) {
                Map<String, Object> tierData = battlePassManager.getTier(tierToShow);
                boolean freeRewardClaimed2 = battlePassManager.isRewardClaimed(player, tierToShow, false);
                boolean premiumRewardClaimed2 = battlePassManager.isRewardClaimed(player, tierToShow, true);
                gui.setItem(i, createMiniTierDisplay(tierData, tierToShow, data.getCurrentTier(), freeRewardClaimed2, premiumRewardClaimed2));
            }
        }

        // Back button
        gui.setItem(22, createBackButton());

        player.openInventory(gui);
    }

    /**
     * Handle tier click (for claiming rewards)
     */
    public void handleTierClick(Player player, int tier, boolean isPremium) {
        if (tier > battlePassManager.getMaxTier()) {
            return;
        }

        player.closeInventory();

        // Try to claim reward
        if (battlePassManager.claimReward(player, tier, isPremium)) {
            String rewardType = isPremium ? "Premium" : "Free";
            player.sendMessage("§a✅ Claimed " + rewardType + " reward for Tier " + tier + "!");
        } else {
            if (isPremium && !battlePassManager.getPlayerData(player).hasPremium()) {
                player.sendMessage("§c❌ You need Premium Battle Pass to claim this reward!");
            } else {
                player.sendMessage("§c❌ Cannot claim this reward yet!");
            }
        }

        // Reopen GUI
        openBattlePass(player);
    }

    /**
     * Handle premium purchase
     */
    public void handlePremiumPurchase(Player player) {
        boolean success = battlePassManager.purchasePremium(player, 950);
        
        if (success) {
            player.sendMessage("§6✨ Premium Battle Pass purchased successfully!");
            openBattlePass(player);
        } else {
            player.sendMessage("§c❌ Failed to purchase Premium Battle Pass. Check your coin balance!");
        }
    }

    // Helper methods for creating GUI items
    private ItemStack createProgressDisplay(Player player, BattlePassData data) {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6📊 Your Progress");
            meta.setLore(Arrays.asList(
                "§7Current Tier: §e" + data.getCurrentTier() + "/" + battlePassManager.getMaxTier(),
                "§7Experience: §e" + data.getExperience() + " XP",
                "§7Premium: " + (data.hasPremium() ? "§a✅ Active" : "§c❌ Not Purchased"),
                "",
                "§7Season " + data.getSeason() + " • " + battlePassManager.getDaysUntilSeasonEnd() + " days left"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSeasonInfo(int season) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6🗓 Season " + season);
            meta.setLore(Arrays.asList("§7Current battle pass season"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createTimeRemainingInfo(int daysLeft) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e⏰ Time Remaining");
            meta.setLore(Arrays.asList("§7" + daysLeft + " days left in season"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createTierDisplay(Map<String, Object> tierInfo, int tier, int currentTier, boolean freeRewardClaimed, boolean premiumRewardClaimed, boolean hasPremium) {
        Material material;
        String displayName;
        
        if (tier <= currentTier) {
            material = Material.LIME_DYE;
            displayName = "§a✅ Tier " + tier + " §7(Unlocked)";
        } else if (tier == currentTier + 1) {
            material = Material.YELLOW_DYE;
            displayName = "§e⏳ Tier " + tier + " §7(Next)";
        } else {
            material = Material.GRAY_DYE;
            displayName = "§7🔒 Tier " + tier + " §7(Locked)";
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            
            List<String> lore = Arrays.asList(
                "§7Free Reward: " + (freeRewardClaimed ? "§a✅ Claimed" : tier <= currentTier ? "§eClick to claim!" : "§7" + tierInfo.get("freeReward")),
                "§7Premium Reward: " + (premiumRewardClaimed ? "§a✅ Claimed" : tier <= currentTier && hasPremium ? "§eClick to claim!" : "§7" + tierInfo.get("premiumReward")),
                "",
                tier <= currentTier ? "§eClick to view/claim rewards!" : "§7Reach tier " + tier + " to unlock"
            );
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createMiniTierDisplay(Map<String, Object> tierInfo, int tier, int currentTier, boolean freeRewardClaimed, boolean premiumRewardClaimed) {
        Material material = tier <= currentTier ? Material.LIME_DYE : Material.GRAY_DYE;
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§7Tier " + tier);
            meta.setLore(Arrays.asList("§7Click to view details"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createTierInfoDisplay(int tier, Map<String, Object> tierInfo) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6📖 Tier " + tier + " Information");
            meta.setLore(Arrays.asList(
                "§7Free Reward: §f" + tierInfo.get("freeReward"),
                "§7Premium Reward: §f" + tierInfo.get("premiumReward"),
                "§7XP Required: §e" + tierInfo.get("xpRequired")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createRewardDisplay(Map<String, Object> tierInfo, boolean isPremium, boolean isClaimed, boolean canClaim) {
        Material material;
        String rewardName;
        
        if (isPremium) {
            material = Material.DIAMOND;
            rewardName = (String) tierInfo.get("premiumReward");
        } else {
            material = Material.EMERALD;
            rewardName = (String) tierInfo.get("freeReward");
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String prefix = isPremium ? "§6👑 Premium: " : "§a🎁 Free: ";
            meta.setDisplayName(prefix + rewardName);
            
            if (isClaimed) {
                meta.setLore(Arrays.asList("§a✅ Already claimed!"));
            } else if (canClaim) {
                meta.setLore(Arrays.asList("§eClick to claim!"));
            } else {
                meta.setLore(Arrays.asList(isPremium ? "§cRequires Premium Battle Pass" : "§7Reach the required tier"));
            }
            
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPremiumPurchaseButton() {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6💎 Purchase Premium Battle Pass");
            meta.setLore(Arrays.asList(
                "§7Unlock premium rewards for all tiers!",
                "§7Cost: §6950 coins",
                "",
                "§eClick to purchase!"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPremiumOwnedDisplay() {
        ItemStack item = new ItemStack(Material.DIAMOND);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6✨ Premium Battle Pass");
            meta.setLore(Arrays.asList("§a✅ You own the Premium Battle Pass!"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPreviousPageButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c← Previous");
            meta.setLore(Arrays.asList("§7View earlier tiers"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNextPageButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§a→ Next");
            meta.setLore(Arrays.asList("§7View later tiers"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c← Back");
            meta.setLore(Arrays.asList("§7Return to battle pass"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c✕ Close");
            meta.setLore(Arrays.asList("§7Close the battle pass"));
            item.setItemMeta(meta);
        }
        return item;
    }
}