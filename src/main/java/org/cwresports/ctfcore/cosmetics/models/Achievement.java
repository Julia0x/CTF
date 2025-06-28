package org.cwresports.ctfcore.cosmetics.models;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Represents a player achievement in the CTF system
 */
public class Achievement {
    
    public enum AchievementCategory {
        KILLS("Combat"),
        COMBAT("Combat"),
        FLAGS("Flag Objectives"),
        TEAMWORK("Teamwork"),
        GAMES("Games Played"),
        SPECIAL("Special"),
        SEASONAL("Seasonal"),
        PRESTIGE("Prestige");

        private final String displayName;

        AchievementCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final String id;
    private final String name;
    private final String description;
    private final AchievementCategory category;
    private final int maxProgress;
    private final List<String> rewards;
    private final boolean hidden;

    public Achievement(String id, String name, String description, AchievementCategory category, int maxProgress, List<String> rewards) {
        this(id, name, description, category, maxProgress, rewards, false);
    }

    public Achievement(String id, String name, String description, AchievementCategory category, int maxProgress, List<String> rewards, boolean hidden) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.maxProgress = maxProgress;
        this.rewards = rewards;
        this.hidden = hidden;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public AchievementCategory getCategory() {
        return category;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public List<String> getRewards() {
        return rewards;
    }

    public boolean isHidden() {
        return hidden;
    }

    public ItemStack createDisplayItem(int progress, boolean unlocked) {
        Material material = unlocked ? Material.EMERALD : Material.GRAY_DYE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§e" + name);
            meta.setLore(List.of(
                "§7" + description,
                "§7Progress: §e" + progress + "/" + maxProgress,
                unlocked ? "§a✅ Unlocked" : "§c❌ Locked"
            ));
            item.setItemMeta(meta);
        }
        
        return item;
    }
}