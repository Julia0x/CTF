package org.cwresports.ctfcore.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.cwresports.ctfcore.CTFCore;

/**
 * Handles chat formatting with level and rank integration
 */
public class ChatListener implements Listener {

    private final CTFCore plugin;

    public ChatListener(CTFCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }

        String message = event.getMessage();

        // Process color codes if player has permission
        message = plugin.getChatManager().processColors(event.getPlayer(), message);

        // Format the chat message
        String formattedMessage = plugin.getChatManager().formatChatMessage(event.getPlayer(), message);

        if (formattedMessage != null) {
            // Cancel the original event and send our formatted message
            event.setCancelled(true);

            // Broadcast the formatted message to all players
            plugin.getServer().broadcastMessage(formattedMessage);
        }
    }
}