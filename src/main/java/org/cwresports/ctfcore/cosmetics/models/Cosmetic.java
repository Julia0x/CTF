package org.cwresports.ctfcore.cosmetics.models;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;

/**
 * Represents a cosmetic item in the CTF system
 */
public class Cosmetic {
    
    private final String id;
    private final String name;
    private final String description;
    private final CosmeticType type;
    private final int cost;
    private final String rarity;
    private final boolean purchasable;

    public Cosmetic(String id, String name, String description, CosmeticType type, int cost, String rarity) {
        this(id, name, description, type, cost, rarity, true);
    }

    public Cosmetic(String id, String name, String description, CosmeticType type, int cost, String rarity, boolean purchasable) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.cost = cost;
        this.rarity = rarity;
        this.purchasable = purchasable;
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

    public CosmeticType getType() {
        return type;
    }

    public int getCost() {
        return cost;
    }

    public String getRarity() {
        return rarity;
    }

    public int getPrice() {
        return cost;
    }

    public boolean isPurchasable() {
        return purchasable;
    }

    public ItemStack createDisplayItem(boolean owned) {
        Material material = owned ? Material.EMERALD : (cost == 0 ? Material.GRAY_DYE : Material.GOLD_INGOT);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§e" + name);
            meta.setLore(List.of(
                "§7" + description,
                "§7Type: §e" + type.getDisplayName(),
                "§7Rarity: §e" + rarity,
                cost > 0 ? "§7Price: §6" + cost + " coins" : "§aFree",
                owned ? "§a✅ Owned" : "§c❌ Not Owned"
            ));
            item.setItemMeta(meta);
        }
        
        return item;
    }
}