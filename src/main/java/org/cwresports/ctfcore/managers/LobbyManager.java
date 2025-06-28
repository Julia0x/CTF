package org.cwresports.ctfcore.managers;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.Arena;
import org.cwresports.ctfcore.models.CTFGame;
import org.cwresports.ctfcore.models.CTFPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages lobby functionality including hotbar items and GUI interactions
 * Updated to remove team selection and compass
 */
public class LobbyManager {
    
    private final CTFCore plugin;
    
    // Hotbar slot assignments
    private static final int LEAVE_SLOT = 8;
    
    public LobbyManager(CTFCore plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Give lobby items to a player - simplified for no kit selection
     */
    public void giveLobbyItems(Player player) {
        player.getInventory().clear();
        
        // Leave item
        ItemStack leaveItem = new ItemStack(Material.RED_BED);
        ItemMeta leaveMeta = leaveItem.getItemMeta();
        leaveMeta.setDisplayName("§c§lLeave Game");
        leaveMeta.setLore(Arrays.asList(
            "§7Right-click to leave the game",
            "§7Return to server lobby"
        ));
        leaveItem.setItemMeta(leaveMeta);
        
        // Set items in hotbar
        player.getInventory().setItem(LEAVE_SLOT, leaveItem);
        
        player.updateInventory();
    }
    
    /**
     * Handle hotbar item interaction - removed team selection
     */
    public boolean handleHotbarClick(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }
        
        String displayName = item.getItemMeta().getDisplayName();
        
        switch (displayName) {
            case "§c§lLeave Game":
            case "§c§lLeave to Lobby":
                handleLeaveGame(player);
                return true;
        }
        
        return false;
    }
    
    /**
     * Handle leaving game
     */
    private void handleLeaveGame(Player player) {
        plugin.getGameManager().removePlayerFromGame(player);
        
        // Teleport to server lobby if configured
        plugin.getServerLobbyManager().teleportToServerLobby(player);
    }
}