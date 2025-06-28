package org.cwresports.ctfcore.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.Arena;
import org.cwresports.ctfcore.models.CTFGame;
import org.cwresports.ctfcore.models.CTFPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages chat formatting with level, rank integration, and arena isolation
 */
public class ChatManager {

    private final CTFCore plugin;

    public ChatManager(CTFCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Format and route chat message with arena isolation support
     */
    public boolean handleChatMessage(Player player, String message) {
        if (!plugin.getConfigManager().getScoreboards().getBoolean("chat.enabled", true)) {
            return false; // Let default chat handle it
        }

        boolean arenaIsolation = plugin.getConfigManager().getScoreboards().getBoolean("chat.arena-isolation", true);
        String globalPrefix = plugin.getConfigManager().getScoreboards().getString("chat.global-chat-command", "!");
        String teamPrefix = plugin.getConfigManager().getScoreboards().getString("chat.team-chat-command", "@");

        // Check for global chat prefix
        if (message.startsWith(globalPrefix) && arenaIsolation) {
            String globalMessage = message.substring(globalPrefix.length()).trim();
            sendGlobalMessage(player, globalMessage);
            return true;
        }

        // Check for team chat prefix
        if (message.startsWith(teamPrefix)) {
            String teamMessage = message.substring(teamPrefix.length()).trim();
            sendTeamMessage(player, teamMessage);
            return true;
        }

        // Regular arena chat
        if (arenaIsolation) {
            sendArenaMessage(player, message);
            return true;
        }

        // Format for global broadcast
        String formattedMessage = formatChatMessage(player, message);
        if (formattedMessage != null) {
            Bukkit.broadcastMessage(formattedMessage);
            return true;
        }

        return false;
    }

    /**
     * Send message to all players (global chat)
     */
    private void sendGlobalMessage(Player player, String message) {
        String template = "&7[GLOBAL] " + plugin.getConfigManager().getScoreboards().getString("chat.format",
                "&7[{level}] {team_prefix}{player}: &f{message}");
        
        String formatted = formatMessage(player, message, template);
        Bukkit.broadcastMessage(formatted);
    }

    /**
     * Send message only to team members
     */
    private void sendTeamMessage(Player player, String message) {
        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(player);
        if (ctfPlayer == null || !ctfPlayer.isInGame() || ctfPlayer.getTeam() == null) {
            player.sendMessage("§c❌ You must be in a game with a team to use team chat!");
            return;
        }

        String teamColor = ctfPlayer.getTeam().getColorCode();
        String template = teamColor + "[TEAM] " + plugin.getConfigManager().getScoreboards().getString("chat.format",
                "&7[{level}] {team_prefix}{player}: &f{message}");

        String formatted = formatMessage(player, message, template);

        // Send to team members only
        CTFGame game = ctfPlayer.getGame();
        for (CTFPlayer teamPlayer : game.getPlayersOnTeam(ctfPlayer.getTeam())) {
            if (teamPlayer.getPlayer() != null && teamPlayer.getPlayer().isOnline()) {
                teamPlayer.getPlayer().sendMessage(formatted);
            }
        }
    }

    /**
     * Send message only to players in the same arena
     */
    private void sendArenaMessage(Player player, String message) {
        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(player);
        List<Player> recipients = new ArrayList<>();

        if (ctfPlayer != null && ctfPlayer.isInGame()) {
            // Send to players in the same game
            CTFGame game = ctfPlayer.getGame();
            for (CTFPlayer gamePlayer : game.getPlayers()) {
                if (gamePlayer.getPlayer() != null && gamePlayer.getPlayer().isOnline()) {
                    recipients.add(gamePlayer.getPlayer());
                }
            }
        } else {
            // Send to lobby players (not in games)
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                CTFPlayer onlineCTFPlayer = plugin.getGameManager().getCTFPlayer(onlinePlayer);
                if (onlineCTFPlayer == null || !onlineCTFPlayer.isInGame()) {
                    recipients.add(onlinePlayer);
                }
            }
        }

        String template = plugin.getConfigManager().getScoreboards().getString("chat.format",
                "&7[{level}] {team_prefix}{player}: &f{message}");
        String formatted = formatMessage(player, message, template);

        // Send to recipients
        for (Player recipient : recipients) {
            recipient.sendMessage(formatted);
        }
    }

    /**
     * Format chat message with template
     */
    private String formatMessage(Player player, String message, String template) {
        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(player);
        int level = ctfPlayer != null ? ctfPlayer.getLevel() : 1;

        // Team prefix
        String teamPrefix = "";
        if (ctfPlayer != null && ctfPlayer.getTeam() != null) {
            Arena.TeamColor teamColor = ctfPlayer.getTeam();
            teamPrefix = teamColor.getColorCode() + "● ";
        }

        // Process color codes if player has permission
        message = processColors(player, message);

        // Replace placeholders
        String formatted = template
                .replace("{player}", player.getName())
                .replace("{message}", message)
                .replace("{level}", String.valueOf(level))
                .replace("{team_prefix}", teamPrefix);

        // Process PlaceholderAPI placeholders if available
        formatted = plugin.processPlaceholders(player, formatted);

        // Apply color codes
        formatted = ChatColor.translateAlternateColorCodes('&', formatted);

        return formatted;
    }

    /**
     * Format chat message with level, rank, and configurable template (legacy method)
     */
    public String formatChatMessage(Player player, String message) {
        String template = plugin.getConfigManager().getMainConfig().getString("chat.format",
                "[{level}] {luckperms_prefix}{player}{luckperms_suffix}: {message}");

        return formatMessage(player, message, template);
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

    /**
     * Send system message to player about chat commands
     */
    public void sendChatHelp(Player player) {
        boolean arenaIsolation = plugin.getConfigManager().getScoreboards().getBoolean("chat.arena-isolation", true);
        
        if (!arenaIsolation) {
            player.sendMessage("§7Chat isolation is disabled. All messages are global.");
            return;
        }

        String globalPrefix = plugin.getConfigManager().getScoreboards().getString("chat.global-chat-command", "!");
        String teamPrefix = plugin.getConfigManager().getScoreboards().getString("chat.team-chat-command", "@");

        player.sendMessage("§e§l=== CHAT COMMANDS ===");
        player.sendMessage("§7Regular chat: §fArena-only chat");
        player.sendMessage("§7" + globalPrefix + " <message>: §fGlobal chat");
        player.sendMessage("§7" + teamPrefix + " <message>: §fTeam chat (in-game only)");
    }
}