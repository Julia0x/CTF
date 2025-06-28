package org.cwresports.ctfcore.cosmetics.managers;

import org.bukkit.entity.Player;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.cosmetics.models.*;
import org.cwresports.ctfcore.cosmetics.utils.RarityUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages the cosmetic shop system
 */
public class ShopManager {

    private final CTFCore plugin;
    private final CosmeticsManager cosmeticsManager;

    public ShopManager(CTFCore plugin) {
        this.plugin = plugin;
        this.cosmeticsManager = plugin.getCosmeticsManager();
    }

    /**
     * Get available cosmetics for purchase
     */
    public List<Cosmetic> getAvailableCosmetics() {
        return cosmeticsManager.getAllCosmetics().stream()
                .filter(Cosmetic::isPurchasable)
                .collect(Collectors.toList());
    }

    /**
     * Purchase a cosmetic for a player
     */
    public boolean purchaseCosmetic(Player player, String cosmeticId) {
        if (player == null) return false;

        Cosmetic cosmetic = cosmeticsManager.getCosmetic(cosmeticId);
        if (cosmetic == null || !cosmetic.isPurchasable()) {
            return false;
        }

        if (cosmeticsManager.ownsCosmetic(player, cosmeticId)) {
            player.sendMessage("§cYou already own this cosmetic!");
            return false;
        }

        // Check if player has enough currency
        int playerBalance = plugin.getCurrencyManager().getCoins(player);
        if (playerBalance < cosmetic.getPrice()) {
            player.sendMessage("§cYou don't have enough coins! Need " + cosmetic.getPrice() + " coins.");
            return false;
        }

        // Purchase the cosmetic
        plugin.getCurrencyManager().removeCoins(player, cosmetic.getPrice());
        boolean success = cosmeticsManager.giveCosmetic(player, cosmeticId);

        if (success) {
            player.sendMessage(RarityUtil.getColorCode(cosmetic.getRarity()) + "✅ Purchased " + cosmetic.getName() + " for " + cosmetic.getPrice() + " coins!");
        }

        return success;
    }

    /**
     * Get cosmetics by type for shop display
     */
    public List<Cosmetic> getCosmeticsByType(CosmeticType type) {
        return cosmeticsManager.getCosmeticsByType(type).stream()
                .filter(Cosmetic::isPurchasable)
                .collect(Collectors.toList());
    }

    /**
     * Get featured cosmetics
     */
    public List<Cosmetic> getFeaturedCosmetics() {
        return getAvailableCosmetics().stream()
                .filter(cosmetic -> "Epic".equals(cosmetic.getRarity()) || "Legendary".equals(cosmetic.getRarity()))
                .limit(5)
                .collect(Collectors.toList());
    }
}