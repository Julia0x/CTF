package org.cwresports.ctfcore.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.cwresports.ctfcore.CTFCore;

/**
 * Handles inventory click events for GUIs and hotbar protection
 * Updated to handle admin tool GUIs
 */
public class InventoryClickListener implements Listener {

    private final CTFCore plugin;

    public InventoryClickListener(CTFCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Handle admin tool GUIs
        if (plugin.getAdminToolManager().handleGUIClick(player, event.getInventory(), event.getCurrentItem())) {
            event.setCancelled(true);
            return;
        }

        // Protect hotbar items in CTF games
        var ctfPlayer = plugin.getGameManager().getCTFPlayer(player);
        if (ctfPlayer != null && ctfPlayer.isInGame()) {
            var game = ctfPlayer.getGame();
            if (game != null && (game.getState() == org.cwresports.ctfcore.models.GameState.WAITING ||
                    game.getState() == org.cwresports.ctfcore.models.GameState.STARTING ||
                    game.getState() == org.cwresports.ctfcore.models.GameState.ENDING)) {
                // Prevent moving lobby items - only leave item at slot 8 now
                if (event.getSlot() == 8) {
                    event.setCancelled(true);
                }
            }
        }

        // Protect admin toolkit items
        if (plugin.getAdminToolManager().isInSetupMode(player)) {
            event.setCancelled(true);
        }
    }
}