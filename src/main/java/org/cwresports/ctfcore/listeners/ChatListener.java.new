package org.cwresports.ctfcore.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.cwresports.ctfcore.CTFCore;

/**
 * Handles chat formatting with level, rank integration, and arena isolation
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

        // Always cancel the event first to prevent default chat
        event.setCancelled(true);

        // Handle the message through our chat manager
        boolean handled = plugin.getChatManager().handleChatMessage(event.getPlayer(), message);
        
        if (!handled) {
            // Fallback to original formatting if not handled
            String formattedMessage = plugin.getChatManager().formatChatMessage(event.getPlayer(), message);
            if (formattedMessage != null) {
                plugin.getServer().broadcastMessage(formattedMessage);
            }
        }
    }
}