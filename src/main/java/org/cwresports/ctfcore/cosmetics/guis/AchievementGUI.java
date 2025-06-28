package org.cwresports.ctfcore.cosmetics.guis;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.cosmetics.managers.AchievementManager;
import org.cwresports.ctfcore.cosmetics.models.Achievement;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * GUI for viewing achievements
 */
public class AchievementGUI {

    private final CTFCore plugin;
    private final AchievementManager achievementManager;

    public AchievementGUI(CTFCore plugin) {
        this.plugin = plugin;
        this.achievementManager = plugin.getAchievementManager();
    }

    /**
     * Open the main achievements GUI
     */
    public void openAchievementsMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6✦ Achievements ✦");

        // Player stats
        double completionPercentage = achievementManager.getCompletionPercentage(player);
        Set<String> unlockedAchievements = achievementManager.getUnlockedAchievements(player);
        List<Achievement> allAchievements = (List<Achievement>) achievementManager.getAllAchievements();

        // Progress display
        gui.setItem(4, createProgressDisplay(player, completionPercentage, unlockedAchievements.size(), allAchievements.size()));

        // Category buttons
        gui.setItem(19, createCategoryButton(Material.DIAMOND_SWORD, "§c⚔ Combat", Achievement.AchievementCategory.COMBAT));
        gui.setItem(21, createCategoryButton(Material.CYAN_BANNER, "§9🏴 Flags", Achievement.AchievementCategory.FLAGS));
        gui.setItem(23, createCategoryButton(Material.PLAYER_HEAD, "§e🤝 Teamwork", Achievement.AchievementCategory.TEAMWORK));
        gui.setItem(25, createCategoryButton(Material.CLOCK, "§7⏰ Games", Achievement.AchievementCategory.GAMES));
        gui.setItem(37, createCategoryButton(Material.NETHER_STAR, "§d⭐ Special", Achievement.AchievementCategory.SPECIAL));
        gui.setItem(39, createCategoryButton(Material.GOLDEN_APPLE, "§6👑 Prestige", Achievement.AchievementCategory.PRESTIGE));
        gui.setItem(41, createCategoryButton(Material.CLOCK, "§b🗓 Seasonal", Achievement.AchievementCategory.SEASONAL));

        player.openInventory(gui);
    }

    /**
     * Open category-specific achievements
     */
    public void openCategoryAchievements(Player player, Achievement.AchievementCategory category) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6" + category.getDisplayName() + " Achievements");

        List<Achievement> categoryAchievements = achievementManager.getAchievementsByCategory(category);
        int slot = 0;

        for (Achievement achievement : categoryAchievements) {
            if (!achievement.isHidden() || achievementManager.hasUnlocked(player, achievement.getId())) {
                boolean unlocked = achievementManager.hasUnlocked(player, achievement.getId());
                int progress = achievementManager.getProgress(player, achievement.getId());
                
                ItemStack item = achievement.createDisplayItem(progress, unlocked);
                gui.setItem(slot++, item);
            }
        }

        // Back button
        gui.setItem(49, createBackButton());

        player.openInventory(gui);
    }

    // Helper methods
    private ItemStack createProgressDisplay(Player player, double percentage, int unlocked, int total) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6📊 Your Progress");
            meta.setLore(Arrays.asList(
                "§7Completion: §e" + String.format("%.1f", percentage) + "%",
                "§7Unlocked: §e" + unlocked + "/" + total + " achievements",
                "",
                "§7Keep playing to unlock more!"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCategoryButton(Material material, String name, Achievement.AchievementCategory category) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList("§7" + category.getDescription(), "§eClick to view achievements"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c← Back");
            meta.setLore(Arrays.asList("§7Return to achievements menu"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isAchievementCategory(Achievement.AchievementCategory category) {
        return category == Achievement.AchievementCategory.COMBAT || 
               category == Achievement.AchievementCategory.TEAMWORK || 
               category == Achievement.AchievementCategory.PRESTIGE;
    }
}