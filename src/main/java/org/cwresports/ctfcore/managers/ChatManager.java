package org.cwresports.ctfcore.managers;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.CTFPlayer;

/**
 * Manages chat formatting with level and rank integration
 */
public class ChatManager {

    private final CTFCore plugin;

    public ChatManager(CTFCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Format chat message with level, rank, and configurable template
     */
    public String formatChatMessage(Player player, String message) {
        if (!plugin.getConfigManager().getMainConfig().getBoolean("chat.enabled", true)) {
            return null; // Let default chat handle it
        }

        String template = plugin.getConfigManager().getMainConfig().getString("chat.format",
                "[{level}] {luckperms_prefix}{player}{luckperms_suffix}: {message}");

        // Get CTF player data
        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(player);
        int level = ctfPlayer != null ? ctfPlayer.getLevel() : 1;

        // Replace placeholders
        String formatted = template
                .replace("{player}", player.getName())
                .replace("{message}", message)
                .replace("{level}", String.valueOf(level));

        // Process PlaceholderAPI placeholders if available
        formatted = plugin.processPlaceholders(player, formatted);

        // Apply color codes
        formatted = ChatColor.translateAlternateColorCodes('&', formatted);

        return formatted;
    }

    /**
     * Check if player can use color codes in chat
     */
    public boolean canUseColors(Player player) {
        return player.hasPermission("ctf.chat.colors");
    }

    /**
     * Process color codes in message if player has permission
     */
    public String processColors(Player player, String message) {
        if (canUseColors(player)) {
            return ChatColor.translateAlternateColorCodes('&', message);
        }
        return message;
    }
}