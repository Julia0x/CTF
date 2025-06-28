package org.cwresports.ctfcore.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.Arena;
import org.cwresports.ctfcore.models.CTFPlayer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles all player commands for CTF gameplay
 * Updated to handle auto team assignment and FIXED color codes
 */
public class CTFPlayerCommand implements CommandExecutor, TabCompleter {

    private final CTFCore plugin;

    public CTFPlayerCommand(CTFCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ctf.play")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "join":
                return handleJoin(player, args);
            case "leave":
                return handleLeave(player, args);
            case "stats":
                return handleStats(player, args);
            case "team":
                return handleTeam(player, args);
            case "chat":
            case "chathelp":
                plugin.getChatManager().sendChatHelp(player);
                return true;
            case "help":
                showHelp(player);
                return true;
            default:
                player.sendMessage(plugin.getConfigManager().getMessage("unknown-command"));
                return true;
        }
    }

    private boolean handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsage: /ctf join <arena>"));
            return true;
        }

        String arenaName = args[1];
        Arena arena = plugin.getArenaManager().getArena(arenaName);

        if (arena == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("arena", arenaName);
            player.sendMessage(plugin.getConfigManager().getMessage("arena-not-found", placeholders));
            return true;
        }

        if (!arena.isEnabled()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("arena", arenaName);
            player.sendMessage(plugin.getConfigManager().getMessage("arena-disabled", placeholders));
            return true;
        }

        // Check if player is already in a game
        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(player);
        if (ctfPlayer != null && ctfPlayer.isInGame()) {
            player.sendMessage(plugin.getConfigManager().getMessage("error-already-in-game"));
            return true;
        }

        // Try to join the game
        boolean success = plugin.getGameManager().addPlayerToGame(player, arena);

        if (success) {
            // Join message removed as requested
        } else {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("arena", arenaName);
            placeholders.put("current_players", String.valueOf(plugin.getGameManager().getPlayersInArena(arena).size()));
            placeholders.put("max_players", String.valueOf(plugin.getConfigManager().getGameplaySetting("max-players-per-arena", 16)));
            player.sendMessage(plugin.getConfigManager().getMessage("game-full", placeholders));
        }

        return true;
    }

    private boolean handleLeave(Player player, String[] args) {
        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(player);

        if (ctfPlayer == null || !ctfPlayer.isInGame()) {
            player.sendMessage(plugin.getConfigManager().getMessage("error-not-in-game"));
            return true;
        }

        // Leave message removed as requested

        return true;
    }


    private boolean handleStats(Player player, String[] args) {
        Player target = player;

        if (args.length > 1) {
            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cPlayer not found."));
                return true;
            }
        }

        // TODO: Implement statistics display
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eStatistics for " + target.getName() + ":"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Statistics feature coming soon!"));

        return true;
    }

    private boolean handleTeam(Player player, String[] args) {
        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(player);

        if (ctfPlayer == null || !ctfPlayer.isInGame()) {
            player.sendMessage(plugin.getConfigManager().getMessage("error-not-in-game"));
            return true;
        }

        if (args.length < 2) {
            // Show current team with proper color codes
            Arena.TeamColor currentTeam = ctfPlayer.getTeam();
            if (currentTeam != null) {
                String teamDisplay = ChatColor.translateAlternateColorCodes('&', "§eYou are on the " + currentTeam.getColorCode() + currentTeam.getName() + " §eteam.");
                player.sendMessage(teamDisplay);
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "§eYou are not on a team yet. Teams will be assigned automatically when the game starts."));
            }
            return true;
        }

        // Teams are now auto-assigned, so manual team switching is disabled
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "§cTeams are now assigned automatically when the game starts. You cannot manually switch teams."));

        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage(plugin.getConfigManager().getMessage("help-header"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-player-join"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-player-leave"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-player-stats"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "§eNote: Teams are assigned automatically when games start."));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("ctf.play") || !(sender instanceof Player)) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("join", "leave", "stats", "team", "help")
                    .stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subcommand = args[0].toLowerCase();

            switch (subcommand) {
                case "join":
                    return plugin.getArenaManager().getEnabledArenas()
                            .stream()
                            .map(Arena::getName)
                            .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());

                case "stats":
                    return plugin.getServer().getOnlinePlayers()
                            .stream()
                            .map(Player::getName)
                            .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}