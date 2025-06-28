package org.cwresports.ctfcore.cosmetics.guis;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.cosmetics.managers.CosmeticsManager;
import org.cwresports.ctfcore.cosmetics.managers.ShopManager;
import org.cwresports.ctfcore.cosmetics.models.Cosmetic;
import org.cwresports.ctfcore.cosmetics.models.CosmeticType;
import org.cwresports.ctfcore.cosmetics.utils.RarityUtil;

import java.util.Arrays;
import java.util.List;

/**
 * Shop GUI for purchasing cosmetics
 */
public class ShopGUI {

    private final CTFCore plugin;
    private final CosmeticsManager cosmeticsManager;
    private final ShopManager shopManager;

    public ShopGUI(CTFCore plugin) {
        this.plugin = plugin;
        this.cosmeticsManager = plugin.getCosmeticsManager();
        this.shopManager = new ShopManager(plugin);
    }

    /**
     * Open the main shop GUI
     */
    public void openShop(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6✦ Cosmetics Shop ✦");

        // Category buttons
        gui.setItem(10, createCategoryItem(Material.DIAMOND_SWORD, "§c⚔ Kill Effects", CosmeticType.KILL_EFFECT));
        gui.setItem(12, createCategoryItem(Material.SKELETON_SKULL, "§8☠ Death Effects", CosmeticType.DEATH_EFFECT));
        gui.setItem(14, createCategoryItem(Material.FIREWORK_ROCKET, "§e🎉 Victory Celebrations", CosmeticType.VICTORY_CELEBRATION));
        gui.setItem(16, createCategoryItem(Material.FEATHER, "§b✨ Trails", CosmeticType.TRAIL_EFFECT));
        gui.setItem(28, createCategoryItem(Material.BANNER_PATTERN, "§6🏴 Banners", CosmeticType.BANNER));
        gui.setItem(30, createCategoryItem(Material.NAME_TAG, "§d💬 Chat Colors", CosmeticType.CHAT_COLOR));

        // Featured items
        gui.setItem(22, createFeaturedItem());

        // Currency display
        int playerCoins = plugin.getCurrencyManager().getCoins(player);
        gui.setItem(49, createCurrencyDisplay(playerCoins));

        player.openInventory(gui);
    }

    /**
     * Open category-specific shop
     */
    public void openCategoryShop(Player player, CosmeticType category) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6" + category.getDisplayName() + " Shop");

        List<Cosmetic> cosmetics = shopManager.getCosmeticsByType(category);
        int slot = 0;

        for (int i = 0; i < Math.min(cosmetics.size(), 45); i++) {
            Cosmetic cosmetic = cosmetics.get(i);
            boolean owned = cosmeticsManager.ownsCosmetic(player, cosmetic.getId());
            
            ItemStack item = cosmetic.createDisplayItem(owned);
            
            if (!owned) {
                // Add purchase information
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.getLore();
                    if (lore != null) {
                        lore.add("");
                        lore.add("§7Price: §6" + cosmetic.getPrice() + " coins");
                        lore.add("§eClick to purchase!");
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                    }
                }
            }
            
            gui.setItem(slot++, item);
        }

        // Back button
        gui.setItem(49, createBackButton());

        player.openInventory(gui);
    }

    /**
     * Open all cosmetics view
     */
    public void openAllCosmetics(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6All Cosmetics");

        List<Cosmetic> allCosmetics = (List<Cosmetic>) cosmeticsManager.getAllCosmetics();
        int slot = 0;

        for (int i = 0; i < Math.min(allCosmetics.size(), 45); i++) {
            Cosmetic cosmetic = allCosmetics.get(i);
            boolean owned = cosmeticsManager.ownsCosmetic(player, cosmetic.getId());
            gui.setItem(slot++, cosmetic.createDisplayItem(owned));
        }

        player.openInventory(gui);
    }

    /**
     * Handle purchase attempt
     */
    public boolean handlePurchase(Player player, String cosmeticId) {
        return shopManager.purchaseCosmetic(player, cosmeticId);
    }

    // Helper methods
    private ItemStack createCategoryItem(Material material, String name, CosmeticType type) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList("§7Click to browse " + type.getDisplayName().toLowerCase()));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFeaturedItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6⭐ Featured Items");
            meta.setLore(Arrays.asList("§7Special cosmetics and deals"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCurrencyDisplay(int coins) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6💰 Your Coins");
            meta.setLore(Arrays.asList("§7Balance: §6" + coins + " coins"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c← Back");
            meta.setLore(Arrays.asList("§7Return to main shop"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPurchaseButton(Cosmetic cosmetic) {
        Material material = cosmetic.getType() == CosmeticType.VICTORY_CELEBRATION ? 
                            Material.FIREWORK_ROCKET : 
                            cosmetic.getType() == CosmeticType.TRAIL_EFFECT ? 
                            Material.FEATHER : 
                            cosmetic.getType() == CosmeticType.CHAT_COLOR ? 
                            Material.NAME_TAG : Material.EMERALD;
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(RarityUtil.getColorCode(cosmetic.getRarity()) + cosmetic.getName());
            item.setItemMeta(meta);
        }
        return item;
    }
}