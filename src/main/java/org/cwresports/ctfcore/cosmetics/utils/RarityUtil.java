package org.cwresports.ctfcore.cosmetics.utils;

/**
 * Utility class for cosmetic-related functions
 */
public class RarityUtil {
    
    /**
     * Get color code for rarity
     */
    public static String getColorCode(String rarity) {
        if (rarity == null) return "§7";
        
        switch (rarity.toLowerCase()) {
            case "common": return "§f";
            case "uncommon": return "§a";
            case "rare": return "§9";
            case "epic": return "§5";
            case "legendary": return "§6";
            case "mythic": return "§c";
            default: return "§7";
        }
    }
    
    /**
     * Get display name with color
     */
    public static String getColoredName(String name, String rarity) {
        return getColorCode(rarity) + name;
    }
}