package org.cwresports.ctfcore.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.Arena;
import org.cwresports.ctfcore.models.CTFGame;
import org.cwresports.ctfcore.models.CTFPlayer;
import org.cwresports.ctfcore.models.GameState;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages scoreboards for CTF games, lobby, and admin setup with FULL customization from scoreboards.yml
 */
public class ScoreboardManager {

    private final CTFCore plugin;
    private final Map<UUID, Scoreboard> playerScoreboards;
    private final Map<UUID, String> adminViewingArena;
    private final UpdateTask updateTask;

    public ScoreboardManager(CTFCore plugin) {
        this.plugin = plugin;
        this.playerScoreboards = new HashMap<>();
        this.adminViewingArena = new ConcurrentHashMap<>();

        // Start update task if scoreboards are enabled
        boolean scoreboardEnabled = true;
        int updateInterval = 60;

        try {
            if (plugin.getConfigManager() != null && plugin.getConfigManager().getScoreboards() != null) {
                scoreboardEnabled = plugin.getConfigManager().getScoreboards().getBoolean("global.enabled", true);
                updateInterval = plugin.getConfigManager().getScoreboards().getInt("global.update-interval-ticks", 60);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not load scoreboard configuration, using defaults: " + e.getMessage());
        }

        if (scoreboardEnabled) {
            this.updateTask = new UpdateTask();
            this.updateTask.runTaskTimer(plugin, 0L, updateInterval);
        } else {
            this.updateTask = null;
        }
    }

    /**
     * Update scoreboard for a specific player
     */
    public void updatePlayerScoreboard(Player player) {
        if (!plugin.getConfigManager().getScoreboards().getBoolean("global.enabled", true)) {
            return;
        }

        // Check if player is in admin setup mode
        if (plugin.getAdminToolManager().isInSetupMode(player)) {
            String arenaName = plugin.getAdminToolManager().getSetupArena(player);
            Arena arena = plugin.getArenaManager().getArena(arenaName);
            if (arena != null) {
                updateAdminScoreboard(player, arena);
                return;
            }
        }

        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(player);
        if (ctfPlayer == null || !ctfPlayer.isInGame()) {
            // Show lobby scoreboard
            updateLobbyScoreboard(player);
            return;
        }

        CTFGame game = ctfPlayer.getGame();
        if (game == null) {
            updateLobbyScoreboard(player);
            return;
        }

        // Show appropriate game scoreboard
        if (game.getState() == GameState.WAITING || game.getState() == GameState.STARTING) {
            updateGameLobbyScoreboard(player, ctfPlayer, game);
        } else {
            updateGamePlayingScoreboard(player, ctfPlayer, game);
        }
    }

    /**
     * Update lobby scoreboard for players not in games - BEDWARS STYLE from scoreboards.yml
     */
    private void updateLobbyScoreboard(Player player) {
        if (!plugin.getConfigManager().getScoreboards().getBoolean("lobby.enabled", true)) {
            clearPlayerScoreboard(player);
            return;
        }

        Scoreboard scoreboard = getOrCreateScoreboard(player);

        String title = plugin.getConfigManager().getScoreboards().getString("lobby.title", "&6&lBEDWARS");
        title = processPlaceholders(player, title);

        Objective objective = scoreboard.getObjective("ctf_lobby");
        if (objective == null) {
            objective = scoreboard.registerNewObjective("ctf_lobby", Criteria.DUMMY, title);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            objective.setDisplayName(title);
        }

        // Clear existing scores
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        // Get CTF player data for level info - always get fresh data
        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(player);
        if (ctfPlayer == null) {
            // Load fresh player data
            Map<String, Object> playerData = plugin.getPlayerDataManager().loadPlayerData(player.getUniqueId());
            ctfPlayer = new org.cwresports.ctfcore.models.CTFPlayer(player, playerData);
        }

        // Get lines from config and process them
        var linesSection = plugin.getConfigManager().getScoreboards().getConfigurationSection("lobby.lines");
        if (linesSection != null) {
            for (String lineKey : linesSection.getKeys(false)) {
                int lineNumber = Integer.parseInt(lineKey);
                String lineText = linesSection.getString(lineKey, "");

                // Process placeholders for this line
                lineText = processLobbyPlaceholders(player, ctfPlayer, lineText);

                objective.getScore(lineText).setScore(lineNumber);
            }
        }

        player.setScoreboard(scoreboard);
    }

    /**
     * Update game lobby scoreboard (waiting for game to start)
     */
    private void updateGameLobbyScoreboard(Player player, CTFPlayer ctfPlayer, CTFGame game) {
        if (!plugin.getConfigManager().getScoreboards().getBoolean("game-lobby.enabled", true)) {
            return;
        }

        Scoreboard scoreboard = getOrCreateScoreboard(player);

        String title = plugin.getConfigManager().getScoreboards().getString("game-lobby.title", "&6&l*** LOBBY ***");
        title = processPlaceholders(player, title);

        Objective objective = scoreboard.getObjective("ctf_game_lobby");
        if (objective == null) {
            objective = scoreboard.registerNewObjective("ctf_game_lobby", Criteria.DUMMY, title);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            objective.setDisplayName(title);
        }

        // Clear existing scores
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        // Get lines from config and process them
        var linesSection = plugin.getConfigManager().getScoreboards().getConfigurationSection("game-lobby.lines");
        if (linesSection != null) {
            for (String lineKey : linesSection.getKeys(false)) {
                int lineNumber = Integer.parseInt(lineKey);
                String lineText = linesSection.getString(lineKey, "");

                // Process placeholders for this line
                lineText = processGameLobbyPlaceholders(player, ctfPlayer, game, lineText);

                objective.getScore(lineText).setScore(lineNumber);
            }
        }

        player.setScoreboard(scoreboard);
    }

    /**
     * Update game playing scoreboard (during active gameplay)
     */
    private void updateGamePlayingScoreboard(Player player, CTFPlayer ctfPlayer, CTFGame game) {
        if (!plugin.getConfigManager().getScoreboards().getBoolean("game-playing.enabled", true)) {
            return;
        }

        Scoreboard scoreboard = getOrCreateScoreboard(player);

        String title = plugin.getConfigManager().getScoreboards().getString("game-playing.title", "&e&lCAPTURE THE FLAG");
        title = processPlaceholders(player, title);

        Objective objective = scoreboard.getObjective("ctf_game_playing");
        if (objective == null) {
            objective = scoreboard.registerNewObjective("ctf_game_playing", Criteria.DUMMY, title);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            objective.setDisplayName(title);
        }

        // Clear existing scores
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        // Get lines from config and process them
        var linesSection = plugin.getConfigManager().getScoreboards().getConfigurationSection("game-playing.lines");
        if (linesSection != null) {
            for (String lineKey : linesSection.getKeys(false)) {
                int lineNumber = Integer.parseInt(lineKey);
                String lineText = linesSection.getString(lineKey, "");

                // Process placeholders for this line
                lineText = processGamePlayingPlaceholders(player, ctfPlayer, game, lineText);

                objective.getScore(lineText).setScore(lineNumber);
            }
        }

        player.setScoreboard(scoreboard);
    }

    /**
     * Process lobby-specific placeholders (enhanced with small symbols)
     */
    private String processLobbyPlaceholders(Player player, CTFPlayer ctfPlayer, String text) {
        // Date - FIXED: Added date processing
        String dateFormat = plugin.getConfigManager().getMainConfig().getString("date-format", "MM/dd/yy");
        text = text.replace("{date}", LocalDate.now().format(DateTimeFormatter.ofPattern(dateFormat)));

        // Level and XP
        text = text.replace("{level}", String.valueOf(ctfPlayer.getLevel()));
        text = text.replace("{current_xp}", String.valueOf(ctfPlayer.getExperience()));
        text = text.replace("{required_xp}", String.valueOf(ctfPlayer.getXPForNextLevel()));

        // Small level progress indicator instead of big progress bar
        text = text.replace("{level_progress}", createLevelProgress(ctfPlayer.getXPProgress()));
        text = text.replace("{progress_bar}", createProgressBar(ctfPlayer.getXPProgress()));

        // Currency
        double balance = plugin.getCurrencyManager().getBalance(player);
        text = text.replace("{coins}", String.valueOf((int) balance));

        // Stats with K/D ratio
        text = text.replace("{total_wins}", String.valueOf(ctfPlayer.getGamesWon()));
        text = text.replace("{total_kills}", String.valueOf(ctfPlayer.getTotalKills()));
        text = text.replace("{total_deaths}", String.valueOf(ctfPlayer.getTotalDeaths()));

        // Calculate K/D ratio
        double kdRatio = ctfPlayer.getTotalDeaths() > 0 ?
                (double) ctfPlayer.getTotalKills() / ctfPlayer.getTotalDeaths() :
                ctfPlayer.getTotalKills();
        text = text.replace("{kd_ratio}", String.format("%.2f", kdRatio));

        // Online players count
        text = text.replace("{online_players}", String.valueOf(Bukkit.getOnlinePlayers().size()));

        // Flag carrying status (even in lobby, for consistency)
        if (ctfPlayer.hasFlag()) {
            Arena.TeamColor flagTeam = ctfPlayer.getCarryingFlag().getTeam();
            String flagDisplay = plugin.getConfigManager().getScoreboards().getString(
                    "placeholders.carrying-flag." + flagTeam.getName(),
                    flagTeam.getColorCode() + "🚩 " + flagTeam.getName().toUpperCase() + " FLAG");
            text = text.replace("{carrying_flag}", flagDisplay);
            text = text.replace("{has_flag}", "true");
        } else {
            text = text.replace("{carrying_flag}", plugin.getConfigManager().getScoreboards().getString(
                    "placeholders.carrying-flag.none", ""));
            text = text.replace("{has_flag}", "false");
        }

        return processPlaceholders(player, text);
    }

    /**
     * Process game lobby placeholders (enhanced)
     */
    private String processGameLobbyPlaceholders(Player player, CTFPlayer ctfPlayer, CTFGame game, String text) {
        // Date - FIXED: Added date processing
        String dateFormat = plugin.getConfigManager().getMainConfig().getString("date-format", "MM/dd/yy");
        text = text.replace("{date}", LocalDate.now().format(DateTimeFormatter.ofPattern(dateFormat)));

        // Arena info
        text = text.replace("{arena}", game.getArena().getName());

        // Player info
        text = text.replace("{level}", String.valueOf(ctfPlayer.getLevel()));
        text = text.replace("{current_xp}", String.valueOf(ctfPlayer.getExperience()));
        text = text.replace("{required_xp}", String.valueOf(ctfPlayer.getXPForNextLevel()));

        // Player counts
        text = text.replace("{current_players}", String.valueOf(game.getPlayers().size()));
        text = text.replace("{max_players}", String.valueOf(plugin.getConfigManager().getGameplaySetting("max-players-per-arena", 8)));

        // Team info
        if (ctfPlayer.getTeam() != null) {
            String teamDisplay = plugin.getConfigManager().getScoreboards().getString(
                    "placeholders.team-display." + ctfPlayer.getTeam().getName(), ctfPlayer.getTeam().getColorCode() + ctfPlayer.getTeam().getName().toUpperCase());
            text = text.replace("{team_display}", teamDisplay);
        } else {
            text = text.replace("{team_display}", plugin.getConfigManager().getScoreboards().getString(
                    "placeholders.team-display.none", "&7None"));
        }

        // Team counts
        int maxPerTeam = plugin.getConfigManager().getGameplaySetting("max-players-per-team", 4);
        text = text.replace("{red_count}", String.valueOf(game.getPlayersOnTeam(Arena.TeamColor.RED).size()));
        text = text.replace("{blue_count}", String.valueOf(game.getPlayersOnTeam(Arena.TeamColor.BLUE).size()));
        text = text.replace("{max_per_team}", String.valueOf(maxPerTeam));

        // Stats
        text = text.replace("{total_kills}", String.valueOf(ctfPlayer.getTotalKills()));
        text = text.replace("{total_deaths}", String.valueOf(ctfPlayer.getTotalDeaths()));
        text = text.replace("{win_rate}", String.format("%.1f", ctfPlayer.getWinRate() * 100));

        // Flag carrying status
        if (ctfPlayer.hasFlag()) {
            Arena.TeamColor flagTeam = ctfPlayer.getCarryingFlag().getTeam();
            String flagDisplay = plugin.getConfigManager().getScoreboards().getString(
                    "placeholders.carrying-flag." + flagTeam.getName(),
                    flagTeam.getColorCode() + "🚩 " + flagTeam.getName().toUpperCase() + " FLAG");
            text = text.replace("{carrying_flag}", flagDisplay);
            text = text.replace("{has_flag}", "true");
        } else {
            text = text.replace("{carrying_flag}", plugin.getConfigManager().getScoreboards().getString(
                    "placeholders.carrying-flag.none", ""));
            text = text.replace("{has_flag}", "false");
        }

        return processPlaceholders(player, text);
    }

    /**
     * Process game playing placeholders (enhanced with time and team info)
     */
    private String processGamePlayingPlaceholders(Player player, CTFPlayer ctfPlayer, CTFGame game, String text) {
        // Date
        String dateFormat = plugin.getConfigManager().getMainConfig().getString("date-format", "MM/dd/yy");
        text = text.replace("{date}", LocalDate.now().format(DateTimeFormatter.ofPattern(dateFormat)));

        // Arena info
        text = text.replace("{arena}", game.getArena().getName());

        // Time remaining
        long timeLeft = game.getTimeLeft();
        String timeFormat = String.format("%02d:%02d", timeLeft / 60, timeLeft % 60);
        text = text.replace("{time_remaining}", timeFormat);

        // Player info
        text = text.replace("{level}", String.valueOf(ctfPlayer.getLevel()));
        text = text.replace("{current_xp}", String.valueOf(ctfPlayer.getExperience()));
        text = text.replace("{required_xp}", String.valueOf(ctfPlayer.getXPForNextLevel()));

        // Team info
        if (ctfPlayer.getTeam() != null) {
            String teamDisplay = plugin.getConfigManager().getScoreboards().getString(
                    "placeholders.team-display." + ctfPlayer.getTeam().getName(), ctfPlayer.getTeam().getColorCode() + ctfPlayer.getTeam().getName().toUpperCase());
            text = text.replace("{team_display}", teamDisplay);
            text = text.replace("{your_team_score}", String.valueOf(game.getScore(ctfPlayer.getTeam())));
        } else {
            text = text.replace("{team_display}", plugin.getConfigManager().getScoreboards().getString(
                    "placeholders.team-display.none", "&7None"));
            text = text.replace("{your_team_score}", "0");
        }

        // Game scores
        int flagsToWin = plugin.getConfigManager().getGameplaySetting("flags-to-win", 3);
        text = text.replace("{red_score}", String.valueOf(game.getScore(Arena.TeamColor.RED)));
        text = text.replace("{blue_score}", String.valueOf(game.getScore(Arena.TeamColor.BLUE)));
        text = text.replace("{flags_to_win}", String.valueOf(flagsToWin));

        // Session stats
        text = text.replace("{session_kills}", String.valueOf(ctfPlayer.getKills()));
        text = text.replace("{session_deaths}", String.valueOf(ctfPlayer.getDeaths()));
        text = text.replace("{session_captures}", String.valueOf(ctfPlayer.getCaptures()));

        // Flag carrying status
        if (ctfPlayer.hasFlag()) {
            Arena.TeamColor flagTeam = ctfPlayer.getCarryingFlag().getTeam();
            String flagDisplay = plugin.getConfigManager().getScoreboards().getString(
                    "placeholders.carrying-flag." + flagTeam.getName(),
                    flagTeam.getColorCode() + "🚩 " + flagTeam.getName().toUpperCase() + " FLAG");
            text = text.replace("{carrying_flag}", flagDisplay);
            text = text.replace("{has_flag}", "true");
        } else {
            text = text.replace("{carrying_flag}", plugin.getConfigManager().getScoreboards().getString(
                    "placeholders.carrying-flag.none", ""));
            text = text.replace("{has_flag}", "false");
        }

        return processPlaceholders(player, text);
    }

    /**
     * Create progress bar with small symbols (BedWars1058 style)
     */
    private String createProgressBar(double progress) {
        int length = plugin.getConfigManager().getScoreboards().getInt("placeholders.progress-bar.length", 8);
        String filledChar = plugin.getConfigManager().getScoreboards().getString("placeholders.progress-bar.filled-char", "●");
        String emptyChar = plugin.getConfigManager().getScoreboards().getString("placeholders.progress-bar.empty-char", "○");
        String filledColor = plugin.getConfigManager().getScoreboards().getString("placeholders.progress-bar.filled-color", "&a");
        String emptyColor = plugin.getConfigManager().getScoreboards().getString("placeholders.progress-bar.empty-color", "&7");

        int filled = (int) (progress * length);
        StringBuilder bar = new StringBuilder();

        for (int i = 0; i < length; i++) {
            if (i < filled) {
                bar.append(filledColor).append(filledChar);
            } else {
                bar.append(emptyColor).append(emptyChar);
            }
        }

        return ChatColor.translateAlternateColorCodes('&', bar.toString());
    }

    /**
     * Create small level progress indicator (dots style)
     */
    private String createLevelProgress(double progress) {
        String style = plugin.getConfigManager().getScoreboards().getString("placeholders.level-progress.style", "dots");
        int length = plugin.getConfigManager().getScoreboards().getInt("placeholders.level-progress.length", 5);
        String filled = plugin.getConfigManager().getScoreboards().getString("placeholders.level-progress.filled", "&a●");
        String empty = plugin.getConfigManager().getScoreboards().getString("placeholders.level-progress.empty", "&8●");

        int filledCount = (int) (progress * length);
        StringBuilder progressBar = new StringBuilder();

        for (int i = 0; i < length; i++) {
            if (i < filledCount) {
                progressBar.append(filled);
            } else {
                progressBar.append(empty);
            }
        }

        return ChatColor.translateAlternateColorCodes('&', progressBar.toString());
    }

    /**
     * Update admin scoreboard with real-time arena data - FIXED team colors
     */
    public void updateAdminScoreboard(Player admin, Arena arena) {
        if (admin == null || !admin.isOnline() || arena == null) {
            return;
        }

        if (!plugin.getConfigManager().getScoreboards().getBoolean("admin.enabled", true)) {
            return;
        }

        // Create a temporary scoreboard for admin
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        String title = plugin.getConfigManager().getScoreboards().getString("admin.title", "&e&lArena: {arena}");
        title = title.replace("{arena}", arena.getName());
        title = processPlaceholders(admin, title);

        Objective objective = scoreboard.registerNewObjective("arena_setup", Criteria.DUMMY, title);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Get lines from config and process them
        var linesSection = plugin.getConfigManager().getScoreboards().getConfigurationSection("admin.lines");
        if (linesSection != null) {
            for (String lineKey : linesSection.getKeys(false)) {
                int lineNumber = Integer.parseInt(lineKey);
                String lineText = linesSection.getString(lineKey, "");

                // Process placeholders for this line
                lineText = processAdminPlaceholders(admin, arena, lineText);

                objective.getScore(lineText).setScore(lineNumber);
            }
        }

        admin.setScoreboard(scoreboard);
        playerScoreboards.put(admin.getUniqueId(), scoreboard);

        // Track that this admin is viewing this arena
        setAdminViewingArena(admin, arena.getName());
    }

    /**
     * Process admin-specific placeholders with FIXED team colors
     */
    private String processAdminPlaceholders(Player admin, Arena arena, String text) {
        // Date - FIXED: Added date processing for admin scoreboards too
        String dateFormat = plugin.getConfigManager().getMainConfig().getString("date-format", "MM/dd/yy");
        text = text.replace("{date}", LocalDate.now().format(DateTimeFormatter.ofPattern(dateFormat)));

        Map<String, Object> status = arena.getConfigurationStatus();

        // Basic arena info
        text = text.replace("{arena}", arena.getName());

        // Game status
        CTFGame game = plugin.getGameManager().getGame(arena);
        if (game != null) {
            text = text.replace("{game_status}", getGameStateDisplay(game.getState()));
            text = text.replace("{player_count}", String.valueOf(game.getPlayers().size()));
            text = text.replace("{red_players}", String.valueOf(game.getPlayersOnTeam(Arena.TeamColor.RED).size()));
            text = text.replace("{blue_players}", String.valueOf(game.getPlayersOnTeam(Arena.TeamColor.BLUE).size()));
            text = text.replace("{red_score}", String.valueOf(game.getScore(Arena.TeamColor.RED)));
            text = text.replace("{blue_score}", String.valueOf(game.getScore(Arena.TeamColor.BLUE)));
        } else {
            text = text.replace("{game_status}", plugin.getConfigManager().getScoreboards().getString("placeholders.game-state.waiting", "&7Waiting"));
            text = text.replace("{player_count}", "0");
            text = text.replace("{red_players}", "0");
            text = text.replace("{blue_players}", "0");
            text = text.replace("{red_score}", "0");
            text = text.replace("{blue_score}", "0");
        }

        // Setup status
        boolean lobbyComplete = (Boolean) status.get("lobby_complete");
        text = text.replace("{lobby_status}", lobbyComplete ?
                plugin.getConfigManager().getScoreboards().getString("placeholders.status.complete", "&a✓") :
                plugin.getConfigManager().getScoreboards().getString("placeholders.status.incomplete", "&c✗"));

        // Team setup status with FIXED colors
        for (Arena.TeamColor teamColor : Arena.TeamColor.values()) {
            String teamKey = teamColor.getName();

            boolean spawnsComplete = (Boolean) status.get(teamKey + "_spawns_complete");
            boolean flagComplete = (Boolean) status.get(teamKey + "_flag_complete");
            boolean captureComplete = (Boolean) status.get(teamKey + "_capture_complete");
            int spawnCount = (Integer) status.get(teamKey + "_spawns_count");

            String spawnsStatus;
            if (spawnsComplete) {
                spawnsStatus = plugin.getConfigManager().getScoreboards().getString("placeholders.status.complete", "&a✓");
            } else {
                spawnsStatus = "&c" + spawnCount + "/4 &7(" + getSpawnProgress(spawnCount) + ")";
            }

            String flagStatus = flagComplete ?
                    plugin.getConfigManager().getScoreboards().getString("placeholders.status.complete", "&a✓") :
                    plugin.getConfigManager().getScoreboards().getString("placeholders.status.incomplete", "&c✗ &7(Break banner)");

            String captureStatus = captureComplete ?
                    plugin.getConfigManager().getScoreboards().getString("placeholders.status.complete", "&a✓") :
                    plugin.getConfigManager().getScoreboards().getString("placeholders.status.incomplete", "&c✗ &7(Right-click)");

            text = text.replace("{" + teamKey + "_spawns_status}", spawnsStatus);
            text = text.replace("{" + teamKey + "_flag_status}", flagStatus);
            text = text.replace("{" + teamKey + "_capture_status}", captureStatus);
        }

        // Ready status
        boolean fullyConfigured = arena.isFullyConfigured();
        text = text.replace("{ready_status}", fullyConfigured ? "&a&lREADY TO SAVE" : "&c&lINCOMPLETE");

        return processPlaceholders(admin, text);
    }

    /**
     * Get spawn progress indicator
     */
    private String getSpawnProgress(int spawnCount) {
        StringBuilder progress = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (i < spawnCount) {
                progress.append("&a●");
            } else {
                progress.append("&7●");
            }
        }
        return progress.toString();
    }

    /**
     * Get display string for game state
     */
    private String getGameStateDisplay(GameState state) {
        String configKey = "placeholders.game-state." + state.toString().toLowerCase();
        return plugin.getConfigManager().getScoreboards().getString(configKey, "&7Unknown");
    }

    /**
     * Process general placeholders (PlaceholderAPI, etc.)
     */
    private String processPlaceholders(Player player, String text) {
        text = plugin.processPlaceholders(player, text);
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Update scoreboard for all players in a game
     */
    public void updateGameScoreboard(CTFGame game) {
        if (!plugin.getConfigManager().getScoreboards().getBoolean("global.enabled", true)) {
            return;
        }

        for (CTFPlayer ctfPlayer : game.getPlayers()) {
            Player player = ctfPlayer.getPlayer();
            if (player != null && player.isOnline()) {
                updatePlayerScoreboard(player);
            }
        }
    }

    /**
     * Clear scoreboard for a player
     */
    public void clearPlayerScoreboard(Player player) {
        playerScoreboards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    /**
     * Get or create scoreboard for a player
     */
    private Scoreboard getOrCreateScoreboard(Player player) {
        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            playerScoreboards.put(player.getUniqueId(), scoreboard);
        }
        return scoreboard;
    }

    /**
     * Set admin as viewing a specific arena for real-time updates
     */
    public void setAdminViewingArena(Player admin, String arenaName) {
        if (admin != null && admin.isOnline()) {
            adminViewingArena.put(admin.getUniqueId(), arenaName);
        }
    }

    /**
     * Remove admin from viewing arena
     */
    public void removeAdminViewingArena(Player admin) {
        if (admin != null) {
            adminViewingArena.remove(admin.getUniqueId());
        }
    }

    /**
     * Shutdown scoreboard manager
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }

        // Clear all player scoreboards
        for (Player player : Bukkit.getOnlinePlayers()) {
            clearPlayerScoreboard(player);
        }

        playerScoreboards.clear();
        adminViewingArena.clear();
    }

    /**
     * Update task for periodic scoreboard updates
     */
    private class UpdateTask extends BukkitRunnable {
        @Override
        public void run() {
            // Update game scoreboards
            for (Player player : Bukkit.getOnlinePlayers()) {
                updatePlayerScoreboard(player);
            }

            // Update admin scoreboards for real-time arena viewing
            for (Map.Entry<UUID, String> entry : adminViewingArena.entrySet()) {
                Player admin = Bukkit.getPlayer(entry.getKey());
                if (admin != null && admin.isOnline()) {
                    Arena arena = plugin.getArenaManager().getArena(entry.getValue());
                    if (arena != null) {
                        updateAdminScoreboard(admin, arena);
                    } else {
                        // Arena doesn't exist anymore, remove from tracking
                        removeAdminViewingArena(admin);
                    }
                }
            }
        }
    }
}