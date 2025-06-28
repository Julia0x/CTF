package org.cwresports.ctfcore.models;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.World;
import org.cwresports.ctfcore.CTFCore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an active CTF game instance
 * Enhanced with world-specific messaging and XP rewards
 */
public class CTFGame {

    private final Arena arena;
    private final CTFCore plugin;
    private final Set<CTFPlayer> players;
    private final Map<Arena.TeamColor, CTFFlag> flags;
    private final Map<Arena.TeamColor, Integer> scores;
    private final Map<Arena.TeamColor, Integer> teamKills; // Track kills per team
    private GameState state;
    private int timeLeft;

    public CTFGame(Arena arena, CTFCore plugin) {
        this.arena = arena;
        this.plugin = plugin;
        this.players = ConcurrentHashMap.newKeySet();
        this.flags = new HashMap<>();
        this.scores = new HashMap<>();
        this.teamKills = new HashMap<>();
        this.state = GameState.WAITING;
        this.timeLeft = 0;

        // Initialize team scores and kills
        for (Arena.TeamColor team : Arena.TeamColor.values()) {
            scores.put(team, 0);
            teamKills.put(team, 0);
        }

        // Initialize flags
        initializeFlags();
    }

    /**
     * Initialize flags for each team
     */
    private void initializeFlags() {
        for (Arena.TeamColor team : Arena.TeamColor.values()) {
            Arena.Team teamData = arena.getTeam(team);
            if (teamData.getFlagLocation() != null) {
                CTFFlag flag = new CTFFlag(team, teamData.getFlagLocation());
                flags.put(team, flag);
            }
        }
    }

    /**
     * Spawn flags at their locations
     */
    public void spawnFlags() {
        for (CTFFlag flag : flags.values()) {
            flag.spawn();
        }
    }

    /**
     * Add a player to the game - FIXED DUPLICATE MESSAGE
     */
    public void addPlayer(CTFPlayer player) {
        players.add(player);
        player.setGame(this);

        // Check if arena is full (max 8 players)
        if (players.size() > 8) {
            players.remove(player);
            player.setGame(null);

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("current_players", "8");
            placeholders.put("max_players", "8");

            String message = plugin.getConfigManager().getMessage("game-full", placeholders);
            player.getPlayer().sendMessage(message);
            return;
        }

        // NO JOIN MESSAGES HERE - handled in GameManager to prevent duplicates
    }

    /**
     * Remove a player from the game
     */
    public void removePlayer(CTFPlayer player) {
        players.remove(player);
        player.setGame(null);

        // Send leave message to OTHER players in the arena
        if (state == GameState.WAITING || state == GameState.STARTING || state == GameState.PLAYING) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", player.getPlayer() != null ? player.getPlayer().getName() : "Unknown");
            placeholders.put("current_players", String.valueOf(players.size()));
            placeholders.put("max_players", "8");

            // Broadcast to OTHER players in the arena only
            String message = plugin.getConfigManager().getMessage("player-left-arena", placeholders);
            World arenaWorld = arena.getWorld();

            for (CTFPlayer otherPlayer : players) {
                if (otherPlayer.getPlayer() != null && otherPlayer.getPlayer().isOnline() &&
                        otherPlayer.getPlayer().getWorld().equals(arenaWorld)) {
                    otherPlayer.getPlayer().sendMessage(message);
                }
            }
        }

        // Send confirmation message to the leaving player
        String leaveConfirmation = plugin.getConfigManager().getMessage("left-game", new HashMap<>());
        if (player.getPlayer() != null && player.getPlayer().isOnline()) {
            player.getPlayer().sendMessage(leaveConfirmation);
        }
    }

    /**
     * Handle flag taking with glowing effect
     */
    public boolean takeFlag(CTFPlayer player, Arena.TeamColor flagTeam) {
        if (player.getTeam() == flagTeam) {
            // Can't take own team's flag
            return false;
        }

        CTFFlag flag = flags.get(flagTeam);
        if (flag == null || flag.isCarried() || !flag.isAtBase()) {
            return false;
        }

        // Check if player's team flag is at base (some game modes require this)
        CTFFlag ownFlag = flags.get(player.getTeam());
        if (ownFlag != null && !ownFlag.isAtBase()) {
            Map<String, String> placeholders = new HashMap<>();
            player.getPlayer().sendMessage(plugin.getConfigManager().getMessage("flag-must-return", placeholders));
            return false;
        }

        // Take the flag
        flag.setCarrier(player);
        player.setCarryingFlag(flag);

        // Apply glowing effect through GameManager
        plugin.getGameManager().handleFlagTaken(player, flagTeam);

        // Broadcast flag taken ONLY to players in the same world
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getPlayer().getName());
        placeholders.put("team_color", ChatColor.translateAlternateColorCodes('&', player.getTeam().getColorCode()));
        placeholders.put("enemy_team", flagTeam.getName());
        placeholders.put("enemy_color", ChatColor.translateAlternateColorCodes('&', flagTeam.getColorCode()));

        broadcastMessageInWorld("flag-taken", placeholders);
        playSoundInWorld(plugin.getConfigManager().getSound("flag_taken"));

        return true;
    }

    /**
     * Handle flag capture with XP rewards
     */
    public boolean captureFlag(CTFPlayer player) {
        CTFFlag flag = player.getCarryingFlag();
        if (flag == null) {
            return false;
        }

        Arena.Team teamData = arena.getTeam(player.getTeam());
        if (teamData.getCapturePoint() == null) {
            return false;
        }

        // Check if player is at capture point
        if (player.getPlayer().getLocation().distance(teamData.getCapturePoint()) > 3.0) {
            return false;
        }

        // Remove glowing effect
        plugin.getGameManager().handleFlagDropped(player);

        // Capture the flag
        flag.returnToBase();
        player.setCarryingFlag(null);

        // Update score
        int newScore = scores.get(player.getTeam()) + 1;
        scores.put(player.getTeam(), newScore);

        // Update player stats and grant XP (addCapture now grants XP automatically)
        player.addCapture();

        // Broadcast capture ONLY to players in the same world
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getPlayer().getName());
        placeholders.put("team_color", ChatColor.translateAlternateColorCodes('&', player.getTeam().getColorCode()));
        placeholders.put("enemy_team", flag.getTeam().getName());
        placeholders.put("enemy_color", ChatColor.translateAlternateColorCodes('&', flag.getTeam().getColorCode()));
        placeholders.put("team_score", String.valueOf(newScore));
        placeholders.put("flags_to_win", String.valueOf(plugin.getConfigManager().getGameplaySetting("flags-to-win", 3)));

        broadcastMessageInWorld("flag-captured", placeholders);
        playSoundInWorld(plugin.getConfigManager().getSound("flag_captured"));

        // Check for win condition
        int flagsToWin = plugin.getConfigManager().getGameplaySetting("flags-to-win", 3);
        if (newScore >= flagsToWin) {
            plugin.getGameManager().endGame(this, player.getTeam());
        } else if (isFlagsTiedAt2()) {
            // Switch to combined scoring mode when flags are tied at 2-2
            broadcastMessage("flags-tied-at-2", new HashMap<>());
            plugin.getMessageManager().updateCombinedScoringBossBar(this);
        }

        return true;
    }

    /**
     * Handle flag return when carrier dies with XP rewards
     */
    public void returnFlag(CTFPlayer player) {
        CTFFlag flag = player.getCarryingFlag();
        if (flag == null) {
            return;
        }

        // Remove glowing effect
        plugin.getGameManager().handleFlagDropped(player);

        // Return flag to base immediately
        flag.returnToBase();
        player.setCarryingFlag(null);

        // Grant XP for flag return (addFlagReturn now grants XP automatically)
        player.addFlagReturn();

        // Only broadcast important flag returns (not death returns) to same world
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getPlayer().getName());
        placeholders.put("team_color", ChatColor.translateAlternateColorCodes('&', flag.getTeam().getColorCode()));

        broadcastMessageInWorld("flag-returned-clean", placeholders);
        playSoundInWorld(plugin.getConfigManager().getSound("flag_returned"));
    }

    /**
     * Handle flag dropping
     */
    public void dropFlag(CTFPlayer player) {
        CTFFlag flag = player.getCarryingFlag();
        if (flag == null) {
            return;
        }

        // Remove glowing effect
        plugin.getGameManager().handleFlagDropped(player);

        // Drop flag at player's location
        flag.dropAt(player.getPlayer().getLocation());
        player.setCarryingFlag(null);

        // Only show drop message to same world players
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getPlayer().getName());
        placeholders.put("team_color", ChatColor.translateAlternateColorCodes('&', player.getTeam().getColorCode()));
        placeholders.put("enemy_team", flag.getTeam().getName());
        placeholders.put("enemy_color", ChatColor.translateAlternateColorCodes('&', flag.getTeam().getColorCode()));

        broadcastMessageInWorld("flag-dropped", placeholders);
        playSoundInWorld(plugin.getConfigManager().getSound("flag_dropped"));

        // Start return timer
        startFlagReturnTimer(flag);
    }

    /**
     * Start flag return timer
     */
    private void startFlagReturnTimer(CTFFlag flag) {
        int returnTime = plugin.getConfigManager().getGameplaySetting("flag-drop-return-timer-seconds", 5);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!flag.isCarried() && !flag.isAtBase()) {
                flag.returnToBase();

                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("team_name", flag.getTeam().getName());
                placeholders.put("team_color", ChatColor.translateAlternateColorCodes('&', flag.getTeam().getColorCode()));

                broadcastMessageInWorld("flag-returned-clean", placeholders);
                playSoundInWorld(plugin.getConfigManager().getSound("flag_returned"));
            }
        }, returnTime * 20L);
    }

    /**
     * Broadcast a message to all players in the game IN THE SAME WORLD ONLY
     */
    public void broadcastMessage(String messageKey, Map<String, String> placeholders) {
        broadcastMessageInWorld(messageKey, placeholders);
    }

    /**
     * Broadcast message only to players in the same world as the arena
     */
    private void broadcastMessageInWorld(String messageKey, Map<String, String> placeholders) {
        String message = plugin.getConfigManager().getMessage(messageKey, placeholders);
        World arenaWorld = arena.getWorld();

        for (CTFPlayer ctfPlayer : players) {
            Player player = ctfPlayer.getPlayer();
            if (player != null && player.isOnline() &&
                    player.getWorld().equals(arenaWorld)) {
                player.sendMessage(message);
            }
        }
    }

    /**
     * Play a sound to all players in the game IN THE SAME WORLD ONLY
     */
    public void playSound(Sound sound) {
        playSoundInWorld(sound);
    }

    /**
     * Play sound only to players in the same world as the arena
     */
    private void playSoundInWorld(Sound sound) {
        World arenaWorld = arena.getWorld();

        for (CTFPlayer ctfPlayer : players) {
            Player player = ctfPlayer.getPlayer();
            if (player != null && player.isOnline() &&
                    player.getWorld().equals(arenaWorld)) {
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            }
        }
    }

    /**
     * Get players on a specific team
     */
    public List<CTFPlayer> getPlayersOnTeam(Arena.TeamColor team) {
        return players.stream()
                .filter(player -> player.getTeam() == team)
                .toList();
    }

    /**
     * Get team scores formatted for display
     */
    public Map<Arena.TeamColor, Integer> getScores() {
        return new HashMap<>(scores);
    }

    /**
     * Add a kill to a team's count
     */
    public void addTeamKill(Arena.TeamColor team) {
        teamKills.put(team, teamKills.get(team) + 1);
    }

    /**
     * Get team kill counts
     */
    public Map<Arena.TeamColor, Integer> getTeamKills() {
        return new HashMap<>(teamKills);
    }

    /**
     * Get team with most kills (null if tied)
     */
    public Arena.TeamColor getTeamWithMostKills() {
        Map.Entry<Arena.TeamColor, Integer> maxEntry = teamKills.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        if (maxEntry == null) return null;

        // Check for ties
        long maxKillsCount = teamKills.values().stream()
                .filter(kills -> kills.equals(maxEntry.getValue()))
                .count();

        // If more than one team has the max kills, it's a tie
        return maxKillsCount > 1 ? null : maxEntry.getKey();
    }

    /**
     * Check if any team has reached the configured kill limit (win condition)
     */
    public Arena.TeamColor getTeamWithKillLimit() {
        int killLimit = plugin.getConfigManager().getGameplaySetting("kills-to-win", 25);
        for (Map.Entry<Arena.TeamColor, Integer> entry : teamKills.entrySet()) {
            if (entry.getValue() >= killLimit) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Check if flags are tied at 2-2
     */
    public boolean isFlagsTiedAt2() {
        int redScore = scores.getOrDefault(Arena.TeamColor.RED, 0);
        int blueScore = scores.getOrDefault(Arena.TeamColor.BLUE, 0);
        return redScore == 2 && blueScore == 2;
    }

    /**
     * Get combined score for 2-2 flag mode (flags worth 10 points, kills worth 1 point)
     */
    public Map<Arena.TeamColor, Integer> getCombinedScores() {
        Map<Arena.TeamColor, Integer> combinedScores = new HashMap<>();

        for (Arena.TeamColor team : Arena.TeamColor.values()) {
            int flagScore = scores.getOrDefault(team, 0) * 10;  // 10 points per flag
            int killScore = teamKills.getOrDefault(team, 0);    // 1 point per kill
            combinedScores.put(team, flagScore + killScore);
        }

        return combinedScores;
    }

    /**
     * Get team with highest combined score (for 2-2 flag mode)
     */
    public Arena.TeamColor getTeamWithHighestCombinedScore() {
        Map<Arena.TeamColor, Integer> combinedScores = getCombinedScores();

        Map.Entry<Arena.TeamColor, Integer> maxEntry = combinedScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        if (maxEntry == null) return null;

        // Check for ties
        long maxScoreCount = combinedScores.values().stream()
                .filter(score -> score.equals(maxEntry.getValue()))
                .count();

        // If more than one team has the max score, it's a tie
        return maxScoreCount > 1 ? null : maxEntry.getKey();
    }

    /**
     * Get formatted time left string
     */
    public String getFormattedTimeLeft() {
        int minutes = timeLeft / 60;
        int seconds = timeLeft % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // Getters and setters

    public Arena getArena() {
        return arena;
    }

    public Set<CTFPlayer> getPlayers() {
        return new HashSet<>(players);
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public void setTimeLeft(int timeLeft) {
        this.timeLeft = timeLeft;
    }

    public CTFFlag getFlag(Arena.TeamColor team) {
        return flags.get(team);
    }

    public int getScore(Arena.TeamColor team) {
        return scores.getOrDefault(team, 0);
    }
}