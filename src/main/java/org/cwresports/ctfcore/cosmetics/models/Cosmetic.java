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
}