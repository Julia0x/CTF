package org.cwresports.ctfcore.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.managers.ArenaManager;
import org.cwresports.ctfcore.models.Arena;
import org.cwresports.ctfcore.models.CTFGame;
import org.cwresports.ctfcore.models.GameState;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles all administrative commands for CTF arena management
 * Updated with admin toolkit integration and FIXED color codes
 */
public class CTFAdminCommand implements CommandExecutor, TabCompleter {

    private final CTFCore plugin;

    public CTFAdminCommand(CTFCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ctf.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "create":
                return handleCreate(sender, args);
            case "delete":
                return handleDelete(sender, args);
            case "setup":
                return handleSetup(sender, args);
            case "setlobby":
                return handleSetLobby(sender, args);
            case "setspawn":
                return handleSetSpawn(sender, args);
            case "setflag":
                return handleSetFlag(sender, args);
            case "setcapture":
                return handleSetCapture(sender, args);
            case "status":
                return handleStatus(sender, args);
            case "save":
                return handleSave(sender, args);
            case "list":
                return handleList(sender, args);
            case "reload":
                return handleReload(sender, args);
            case "setserverlobby":
                return handleSetServerLobby(sender, args);
            case "forcestart":
                return handleForceStart(sender, args);
            case "help":
                showHelp(sender);
                return true;
            default:
                sender.sendMessage(plugin.getConfigManager().getMessage("unknown-command"));
                return true;
        }
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsage: /ctfadmin create <arenaName> <worldGuardRegion>"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        String arenaName = args[1];
        String regionName = args[2];
        String worldName = player.getWorld().getName();

        ArenaManager arenaManager = plugin.getArenaManager();

        if (arenaManager.getArena(arenaName) != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("arena", arenaName);
            sender.sendMessage(plugin.getConfigManager().getMessage("arena-already-exists", placeholders));
            return true;
        }

        if (!plugin.getWorldGuardManager().regionExists(player.getWorld(), regionName)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("region", regionName);
            placeholders.put("world", worldName);
            sender.sendMessage(plugin.getConfigManager().getMessage("worldguard-region-not-found", placeholders));
            return true;
        }

        if (arenaManager.createArena(arenaName, regionName, worldName)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("arena", arenaName);
            sender.sendMessage(plugin.getConfigManager().getMessage("arena-created", placeholders));

            // Automatically give admin toolkit
            plugin.getAdminToolManager().giveAdminToolkit(player, arenaName);
        } else {
            sender.sendMessage(plugin.getConfigManager().getMessage("error-generic"));
        }

        return true;
    }

    private boolean handleSetup(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsage: /ctfadmin setup <arenaName>"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        String arenaName = args[1];
        Arena arena = plugin.getArenaManager().getArena(arenaName);

        if (arena == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("arena", arenaName);
            sender.sendMessage(plugin.getConfigManager().getMessage("arena-not-found", placeholders));
            return true;
        }

        // Give admin toolkit
        plugin.getAdminToolManager().giveAdminToolkit(player, arenaName);

        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsage: /ctfadmin delete <arenaName>"));
            return true;
        }

        String arenaName = args[1];
        ArenaManager arenaManager = plugin.getArenaManager();

        if (arenaManager.getArena(arenaName) == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("arena", arenaName);
            sender.sendMessage(plugin.getConfigManager().getMessage("arena-not-found", placeholders));
            return true;
        }

        // Check if arena is in use
        if (plugin.getGameManager().isArenaInUse(arenaName)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("arena", arenaName);
            sender.sendMessage(plugin.getConfigManager().getMessage("error-arena-in-use", placeholders));
            return true;
        }

        if (arenaManager.deleteArena(arenaName)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("arena", arenaName);
            sender.sendMessage(plugin.getConfigManager().getMessage("arena-deleted", placeholders));
        } else {
            sender.sendMessage(plugin.getConfigManager().getMessage("error-generic"));
        }

        return true;
    }

    private boolean handleSetLobby(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsage: /ctfadmin setlobby <arenaName>"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        String arenaName = args[1];
        Arena arena = plugin.getArenaManager().getArena(arenaName);

        if (arena == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("arena", arenaName);
            sender.sendMessage(plugin.getConfigManager().getMessage("arena-not-found", placeholders));
            return true;
        }

        arena.setLobbySpawn(player.getLocation());
        plugin.getArenaManager().saveAllArenas();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("arena", arenaName);
        sender.sendMessage(plugin.getConfigManager().getMessage("setup-lobby-set", placeholders));

        // Update setup boss bar
        showArenaSetupBossBar(player, arena);

        return true;
    }

    private boolean handleSetSpawn(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsage: /ctfadmin setspawn <arenaName> <team> <1-4>"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        String arenaName = args[1];
        String teamName = args[2];
        String spawnNumberStr = args[3];

        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("arena", arenaName);
            sender.sendMessage(plugin.getConfigManager().getMessage("arena-not-found", placeholders));
            return true;
        }

        Arena.TeamColor teamColor = Arena.TeamColor.fromString(teamName);
        if (teamColor == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error-invalid-team"));
            return true;
        }

        int spawnNumber;
        try {
            spawnNumber = Integer.parseInt(spawnNumberStr);
            if (spawnNumber < 1 || spawnNumber > 4) {
                sender.sendMessage(plugin.getConfigManager().getMessage("error-invalid-spawn-number"));
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error-invalid-spawn-number"));
            return true;
        }

        arena.getTeam(teamColor).setSpawnPoint(spawnNumber - 1, player.getLocation());
        plugin.getArenaManager().saveAllArenas();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("arena", arenaName);
        placeholders.put("team_name", teamColor.getName());
        placeholders.put("team_color", ChatColor.translateAlternateColorCodes('&', teamColor.getColorCode()));
        placeholders.put("spawn_number", String.valueOf(spawnNumber));
        sender.sendMessage(plugin.getConfigManager().getMessage("setup-spawn-set", placeholders));

        // Update setup boss bar
        showArenaSetupBossBar(player, arena);

        return true;
    }

    private boolean handleSetFlag(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsage: /ctfadmin setflag <arenaName> <team>"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        String arenaName = args[1];
        String teamName = args[2];

        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("arena", arenaName);
            sender.sendMessage(plugin.getConfigManager().getMessage("arena-not-found", placeholders));
            return true;
        }

        Arena.TeamColor teamColor = Arena.TeamColor.fromString(teamName);
        if (teamColor == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error-invalid-team"));
            return true;
        }

        // Start flag setup mode
        if (plugin.getArenaManager().startFlagSetup(player, arenaName, teamColor)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("team_name", teamColor.getName());
            placeholders.put("team_color", ChatColor.translateAlternateColorCodes('&', teamColor.getColorCode()));
            sender.sendMessage(plugin.getConfigManager().getMessage("setup-flag-mode", placeholders));
        } else {
            sender.sendMessage(plugin.getConfigManager().getMessage("error-setup-mode-active"));
        }

        return true;
    }

    private boolean handleSetCapture(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsage: /ctfadmin setcapture <arenaName> <team>"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        String arenaName = args[1];
        String teamName = args[2];

        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("arena", arenaName);
            sender.sendMessage(plugin.getConfigManager().getMessage("arena-not-found", placeholders));
            return true;
        }

        Arena.TeamColor teamColor = Arena.TeamColor.fromString(teamName);
        if (teamColor == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error-invalid-team"));
            return true;
        }

        // Start capture point setup mode
        if (plugin.getArenaManager().startCaptureSetup(player, arenaName, teamColor)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("team_name", teamColor.getName());
            placeholders.put("team_color", ChatColor.translateAlternateColorCodes('&', teamColor.getColorCode()));
            sender.sendMessage(plugin.getConfigManager().getMessage("setup-capture-mode", placeholders));
        } else {
            sender.sendMessage(plugin.getConfigManager().getMessage("error-setup-mode-active"));
        }

        return true;
    }

    private boolean handleStatus(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsage: /ctfadmin status <arenaName>"));
            return true;
        }

        String arenaName = args[1];
        Arena arena = plugin.getArenaManager().getArena(arenaName);

        if (arena == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("arena", arenaName);
            sender.sendMessage(plugin.getConfigManager().getMessage("arena-not-found", placeholders));
            return true;
        }

        showArenaStatus(sender, arena);

        // Also show admin scoreboard and setup boss bar if sender is a player
        if (sender instanceof Player) {
            Player player = (Player) sender;
            plugin.getScoreboardManager().updateAdminScoreboard(player, arena);
            showArenaSetupBossBar(player, arena);
        }
        return true;
    }

    private boolean handleSave(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsage: /ctfadmin save <arenaName>"));
            return true;
        }

        String arenaName = args[1];
        ArenaManager arenaManager = plugin.getArenaManager();
        Arena arena = arenaManager.getArena(arenaName);

        if (arena == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("arena", arenaName);
            sender.sendMessage(plugin.getConfigManager().getMessage("arena-not-found", placeholders));
            return true;
        }

        ArenaManager.ValidationResult result = arenaManager.validateAndEnableArena(arenaName);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("arena", arenaName);

        if (result.isSuccess()) {
            // Changed message to "Saved {arenaname}"
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aSaved " + arenaName));

            // Send title message to indicate successful save
            if (sender instanceof Player) {
                Player player = (Player) sender;
                Map<String, String> titlePlaceholders = new HashMap<>();
                titlePlaceholders.put("arena", arenaName);
                plugin.getMessageManager().sendTitle(player, "arena-enabled", null, titlePlaceholders);

                // Clear admin setup mode
                plugin.getAdminToolManager().clearAdminSetup(player);
            }
        } else {
            sender.sendMessage(plugin.getConfigManager().getMessage("validation-failed", placeholders));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError: " + result.getMessage()));

            // Keep showing admin scoreboard and setup boss bar if validation failed
            if (sender instanceof Player) {
                Player player = (Player) sender;
                Arena failedArena = arenaManager.getArena(arenaName);
                if (failedArena != null) {
                    plugin.getScoreboardManager().updateAdminScoreboard(player, failedArena);
                    showArenaSetupBossBar(player, failedArena);
                }
            }
        }

        return true;
    }

    private boolean handleList(CommandSender sender, String[] args) {
        Collection<Arena> arenas = plugin.getArenaManager().getAllArenas();

        if (arenas.isEmpty()) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eNo arenas found."));
            return true;
        }

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l=== CTF Arenas ==="));
        for (Arena arena : arenas) {
            String status = arena.isEnabled() ? "&aEnabled" : "&cDisabled";
            String configured = arena.isFullyConfigured() ? "&a✔" : "&c✖";
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e" + arena.getName() + " &7- " + status + " " + configured));
        }

        return true;
    }

    private boolean handleReload(CommandSender sender, String[] args) {
        plugin.reload();
        sender.sendMessage(plugin.getConfigManager().getMessage("plugin-reloaded"));
        return true;
    }


    private boolean handleForceStart(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsage: /ctfadmin forcestart <arenaName>"));
            return true;
        }

        String arenaName = args[1];
        Arena arena = plugin.getArenaManager().getArena(arenaName);

        if (arena == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("arena", arenaName);
            sender.sendMessage(plugin.getConfigManager().getMessage("arena-not-found", placeholders));
            return true;
        }

        if (!arena.isEnabled()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("arena", arenaName);
            sender.sendMessage(plugin.getConfigManager().getMessage("arena-disabled", placeholders));
            return true;
        }

        CTFGame game = plugin.getGameManager().getGame(arena);
        if (game == null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNo active game found for arena " + arenaName));
            return true;
        }

        if (game.getState() != GameState.WAITING && game.getState() != GameState.STARTING) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cGame in arena " + arenaName + " is already running or ending!"));
            return true;
        }

        if (game.getPlayers().size() < 2) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNeed at least 2 players to force start! Current: " + game.getPlayers().size()));
            return true;
        }

        // Force start the game with 2 minimum players
        plugin.getGameManager().forceStartGame(game);

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aForce started game in arena " + arenaName + " with " + game.getPlayers().size() + " players!"));

        return true;
    }

    private boolean handleSetServerLobby(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (plugin.getServerLobbyManager().setServerLobbySpawn(player.getLocation())) {
            sender.sendMessage(plugin.getConfigManager().getMessage("server-lobby-set"));
        } else {
            sender.sendMessage(plugin.getConfigManager().getMessage("error-generic"));
        }

        return true;
    }

    /**
     * Show arena setup boss bar when arena is being configured
     */
    private void showArenaSetupBossBar(Player player, Arena arena) {
        if (arena.isFullyConfigured()) {
            // Arena is fully configured, show "Save Arena" boss bar
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("arena", arena.getName());
            plugin.getMessageManager().updateBossBar(player, "bossbar-arena-save", placeholders, 1.0);
        } else {
            // Arena is incomplete, show setup progress
            Map<String, Object> status = arena.getConfigurationStatus();
            int totalTasks = 15; // 1 lobby + 8 spawns + 2 flags + 2 captures + 2 teams
            int completedTasks = 0;

            // Count completed tasks
            if ((Boolean) status.get("lobby_complete")) completedTasks++;
            if ((Boolean) status.get("red_spawns_complete")) completedTasks += 4;
            if ((Boolean) status.get("blue_spawns_complete")) completedTasks += 4;
            if ((Boolean) status.get("red_flag_complete")) completedTasks++;
            if ((Boolean) status.get("blue_flag_complete")) completedTasks++;
            if ((Boolean) status.get("red_capture_complete")) completedTasks++;
            if ((Boolean) status.get("blue_capture_complete")) completedTasks++;

            double progress = (double) completedTasks / totalTasks;

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("arena", arena.getName());
            placeholders.put("completed", String.valueOf(completedTasks));
            placeholders.put("total", String.valueOf(totalTasks));

            plugin.getMessageManager().updateBossBar(player, "bossbar-arena-setup", placeholders, progress);
        }
    }

    private void showArenaStatus(CommandSender sender, Arena arena) {
        Map<String, Object> status = arena.getConfigurationStatus();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("arena", arena.getName());

        // Header
        sender.sendMessage(plugin.getConfigManager().getMessage("status-header", placeholders));

        // Basic info
        placeholders.put("enabled", status.get("enabled").toString());
        sender.sendMessage(plugin.getConfigManager().getMessage("status-enabled", placeholders));

        placeholders.put("world", (String) status.get("world"));
        sender.sendMessage(plugin.getConfigManager().getMessage("status-world", placeholders));

        placeholders.put("region", (String) status.get("region"));
        sender.sendMessage(plugin.getConfigManager().getMessage("status-region", placeholders));

        // Lobby
        boolean lobbyComplete = (Boolean) status.get("lobby_complete");
        placeholders.put("status", lobbyComplete ?
                plugin.getConfigManager().getMessage("status-complete") :
                plugin.getConfigManager().getMessage("status-incomplete"));
        sender.sendMessage(plugin.getConfigManager().getMessage("status-lobby", placeholders));

        // Teams
        for (Arena.TeamColor teamColor : Arena.TeamColor.values()) {
            String teamKey = teamColor.getName();

            placeholders.put("team_name", teamColor.getName().toUpperCase());
            placeholders.put("team_color", ChatColor.translateAlternateColorCodes('&', teamColor.getColorCode()));
            sender.sendMessage(plugin.getConfigManager().getMessage("status-team-header", placeholders));

            // Spawns
            int spawnCount = (Integer) status.get(teamKey + "_spawns_count");
            boolean spawnsComplete = (Boolean) status.get(teamKey + "_spawns_complete");
            placeholders.put("status", spawnsComplete ?
                    plugin.getConfigManager().getMessage("status-complete") :
                    plugin.getConfigManager().getMessage("status-incomplete"));
            placeholders.put("count", String.valueOf(spawnCount));
            sender.sendMessage(plugin.getConfigManager().getMessage("status-spawns", placeholders));

            // Flag
            boolean flagComplete = (Boolean) status.get(teamKey + "_flag_complete");
            placeholders.put("status", flagComplete ?
                    plugin.getConfigManager().getMessage("status-complete") :
                    plugin.getConfigManager().getMessage("status-incomplete"));
            sender.sendMessage(plugin.getConfigManager().getMessage("status-flag", placeholders));

            // Capture point
            boolean captureComplete = (Boolean) status.get(teamKey + "_capture_complete");
            placeholders.put("status", captureComplete ?
                    plugin.getConfigManager().getMessage("status-complete") :
                    plugin.getConfigManager().getMessage("status-incomplete"));
            sender.sendMessage(plugin.getConfigManager().getMessage("status-capture", placeholders));
        }

        // Footer
        sender.sendMessage(plugin.getConfigManager().getMessage("status-footer"));
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().getMessage("help-header"));
        sender.sendMessage(plugin.getConfigManager().getMessage("help-admin-create"));
        sender.sendMessage(plugin.getConfigManager().getMessage("help-admin-setup"));
        sender.sendMessage(plugin.getConfigManager().getMessage("help-admin-delete"));
        sender.sendMessage(plugin.getConfigManager().getMessage("help-admin-setlobby"));
        sender.sendMessage(plugin.getConfigManager().getMessage("help-admin-setspawn"));
        sender.sendMessage(plugin.getConfigManager().getMessage("help-admin-setflag"));
        sender.sendMessage(plugin.getConfigManager().getMessage("help-admin-setcapture"));
        sender.sendMessage(plugin.getConfigManager().getMessage("help-admin-status"));
        sender.sendMessage(plugin.getConfigManager().getMessage("help-admin-save"));
        sender.sendMessage(plugin.getConfigManager().getMessage("help-admin-list"));
        sender.sendMessage(plugin.getConfigManager().getMessage("help-admin-reload"));
        sender.sendMessage(plugin.getConfigManager().getMessage("help-admin-setserverlobby"));
        sender.sendMessage(plugin.getConfigManager().getMessage("help-admin-forcestart"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("ctf.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("create", "setup", "delete", "setlobby", "setspawn", "setflag",
                            "setcapture", "status", "save", "list", "reload", "setserverlobby", "forcestart", "help")
                    .stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subcommand = args[0].toLowerCase();
            if (Arrays.asList("setup", "delete", "setlobby", "setspawn", "setflag", "setcapture", "status", "save", "forcestart")
                    .contains(subcommand)) {
                return plugin.getArenaManager().getArenaNames()
                        .stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            String subcommand = args[0].toLowerCase();
            if (Arrays.asList("setspawn", "setflag", "setcapture").contains(subcommand)) {
                return Arrays.asList("red", "blue")
                        .stream()
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("setspawn")) {
            return Arrays.asList("1", "2", "3", "4")
                    .stream()
                    .filter(s -> s.startsWith(args[3]))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}