package org.cwresports.ctfcore.cosmetics.guis;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.cosmetics.managers.CosmeticsManager;
import org.cwresports.ctfcore.cosmetics.models.Cosmetic;
import org.cwresports.ctfcore.cosmetics.models.CosmeticType;
import org.cwresports.ctfcore.cosmetics.utils.RarityUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * GUI for managing equipped cosmetics
 */
public class CosmeticsGUI {

    private final CTFCore plugin;
    private final CosmeticsManager cosmeticsManager;

    public CosmeticsGUI(CTFCore plugin) {
        this.plugin = plugin;
        this.cosmeticsManager = plugin.getCosmeticsManager();
    }

    /**
     * Open the main cosmetics management GUI
     */
    public void openCosmeticsMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6✦ Your Cosmetics ✦");

        // Category slots with equipped items
        gui.setItem(10, createEquippedSlot(player, CosmeticType.KILL_EFFECT, Material.DIAMOND_SWORD, "§c⚔ Kill Effect"));
        gui.setItem(12, createEquippedSlot(player, CosmeticType.DEATH_EFFECT, Material.SKELETON_SKULL, "§8☠ Death Effect"));
        gui.setItem(14, createEquippedSlot(player, CosmeticType.VICTORY_CELEBRATION, Material.FIREWORK_ROCKET, "§e🎉 Victory Dance"));
        gui.setItem(16, createEquippedSlot(player, CosmeticType.TRAIL_EFFECT, Material.FEATHER, "§b✨ Trail"));
        gui.setItem(28, createEquippedSlot(player, CosmeticType.BANNER, Material.WHITE_BANNER, "§6🏴 Banner"));
        gui.setItem(30, createEquippedSlot(player, CosmeticType.CHAT_COLOR, Material.NAME_TAG, "§d💬 Chat Color"));

        // Browse buttons
        gui.setItem(37, createBrowseButton("§aBrowse All Cosmetics", "View all your cosmetics"));
        gui.setItem(43, createShopButton());

        player.openInventory(gui);
    }

    /**
     * Open category browser
     */
    public void openCategoryBrowser(Player player, CosmeticType category) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6" + category.getDisplayName());

        Set<String> ownedCosmetics = cosmeticsManager.getOwnedCosmetics(player);
        List<Cosmetic> categoryCosmetics = cosmeticsManager.getCosmeticsByType(category);
        
        String currentEquipped = cosmeticsManager.getEquippedCosmetic(player, category);

        int slot = 0;
        for (Cosmetic cosmetic : categoryCosmetics) {
            if (ownedCosmetics.contains(cosmetic.getId())) {
                boolean isEquipped = cosmetic.getId().equals(currentEquipped);
                ItemStack item = cosmetic.createDisplayItem(true);
                
                // Add equip status to lore
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.getLore();
                    if (lore != null) {
                        lore.add("");
                        if (isEquipped) {
                            lore.add("§a✅ Currently Equipped");
                            lore.add("§cClick to unequip");
                        } else {
                            lore.add("§eClick to equip");
                        }
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                    }
                }
                
                gui.setItem(slot++, item);
            }
        }

        // Back button
        gui.setItem(49, createBackButton());

        player.openInventory(gui);
    }

    /**
     * Open all cosmetics browser
     */
    public void openAllCosmeticsBrowser(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6All Your Cosmetics");

        Set<String> ownedCosmetics = cosmeticsManager.getOwnedCosmetics(player);
        List<Cosmetic> allCosmetics = cosmeticsManager.getCosmeticsByType(CosmeticType.KILL_EFFECT); // Get from all types

        int slot = 0;
        for (CosmeticType type : CosmeticType.values()) {
            String equipped = cosmeticsManager.getEquippedCosmetic(player, type);
            if (equipped != null) {
                Cosmetic cosmetic = cosmeticsManager.getCosmetic(equipped);
                if (cosmetic != null) {
                    ItemStack item = cosmetic.createDisplayItem(true);
                    gui.setItem(slot++, item);
                }
            }
        }

        player.openInventory(gui);
    }

    /**
     * Handle cosmetic equip/unequip
     */
    public void handleCosmeticClick(Player player, String cosmeticId) {
        Cosmetic cosmetic = cosmeticsManager.getCosmetic(cosmeticId);
        if (cosmetic == null) return;

        String currentEquipped = cosmeticsManager.getEquippedCosmetic(player, cosmetic.getType());
        
        if (cosmeticId.equals(currentEquipped)) {
            // Unequip
            cosmeticsManager.unequipCosmetic(player, cosmetic.getType());
            player.sendMessage("§c❌ Unequipped " + cosmetic.getName());
        } else {
            // Equip
            if (currentEquipped != null) {
                Cosmetic oldCosmetic = cosmeticsManager.getCosmetic(currentEquipped);
                if (oldCosmetic != null) {
                    cosmeticsManager.unequipCosmetic(player, cosmetic.getType());
                }
            }
            cosmeticsManager.equipCosmetic(player, cosmeticId);
            player.sendMessage(RarityUtil.getColorCode(cosmetic.getRarity()) + "✅ Equipped " + cosmetic.getName());
        }
    }

    // Helper methods
    private ItemStack createEquippedSlot(Player player, CosmeticType type, Material defaultMaterial, String displayName) {
        String equippedId = cosmeticsManager.getEquippedCosmetic(player, type);
        
        if (equippedId != null) {
            Cosmetic equipped = cosmeticsManager.getCosmetic(equippedId);
            if (equipped != null) {
                ItemStack item = equipped.createDisplayItem(true);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(RarityUtil.getColorCode(equipped.getRarity()) + equipped.getName() + " §7(Equipped)");
                    item.setItemMeta(meta);
                }
                return item;
            }
        }

        // No item equipped
        ItemStack item = new ItemStack(Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(Arrays.asList("§7No " + type.getDisplayName().toLowerCase() + " equipped", "§eClick to browse"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBrowseButton(String name, String description) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList("§7" + description));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createShopButton() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6🛒 Visit Shop");
            meta.setLore(Arrays.asList("§7Purchase new cosmetics"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c← Back");
            meta.setLore(Arrays.asList("§7Return to cosmetics menu"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isEquippedCosmetic(Player player, String cosmeticId, CosmeticType type) {
        if (type == CosmeticType.VICTORY_CELEBRATION || 
            type == CosmeticType.TRAIL_EFFECT || 
            type == CosmeticType.CHAT_COLOR) {
            return cosmeticId.equals(cosmeticsManager.getEquippedCosmetic(player, type));
        }
        return false;
    }
}