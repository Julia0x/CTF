package org.cwresports.ctfcore.managers;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all active CTF games and player interactions
 * Enhanced with spawn protection, kill streaks, team enhancements, and level system
 */
public class GameManager {

    private final CTFCore plugin;
    private final Map<Arena, CTFGame> activeGames;
    private final Map<UUID, CTFPlayer> players;
    private final Map<UUID, Long> spawnProtection;
    private final Map<UUID, Integer> killStreaks;
    private final Map<Arena.TeamColor, Integer> teamKillCounts;
    private final Map<UUID, BukkitTask> respawnTasks;

    public GameManager(CTFCore plugin) {
        this.plugin = plugin;
        this.activeGames = new ConcurrentHashMap<>();
        this.players = new ConcurrentHashMap<>();
        this.spawnProtection = new ConcurrentHashMap<>();
        this.killStreaks = new ConcurrentHashMap<>();
        this.teamKillCounts = new ConcurrentHashMap<>();
        this.respawnTasks = new ConcurrentHashMap<>();
    }

    /**
     * Add a player to a game with automatic team assignment and level loading
     */
    public boolean addPlayerToGame(Player player, Arena arena) {
        if (!arena.isEnabled()) {
            return false;
        }

        // Check if player is already in a game
        CTFPlayer existingPlayer = players.get(player.getUniqueId());
        if (existingPlayer != null && existingPlayer.isInGame()) {
            return false;
        }

        // Get or create game for arena
        CTFGame game = getOrCreateGame(arena);

        // Check if game is full
        if (game.getPlayers().size() >= plugin.getConfigManager().getGameplaySetting("max-players-per-arena", 8)) {
            return false;
        }

        // Load player data from persistent storage
        Map<String, Object> playerData = plugin.getPlayerDataManager().loadPlayerData(player.getUniqueId());

        // Create CTF player with loaded data
        CTFPlayer ctfPlayer = new CTFPlayer(player, playerData);
        players.put(player.getUniqueId(), ctfPlayer);

        // Add to game
        game.addPlayer(ctfPlayer);

        // Teleport to lobby
        if (arena.getLobbySpawn() != null) {
            player.teleport(arena.getLobbySpawn());
        }

        // Give lobby items
        plugin.getLobbyManager().giveLobbyItems(player);

        // Join message removed as requested
        // Update lobby boss bar
        plugin.getMessageManager().updateLobbyBossBar(game);

        // Update scoreboard
        plugin.getScoreboardManager().updateGameScoreboard(game);

        // Check if we can start the game
        checkGameStart(game);

        return true;
    }

    /**
     * Remove a player from their current game and save their data
     */
    public void removePlayerFromGame(Player player) {
        CTFPlayer ctfPlayer = players.get(player.getUniqueId());
        if (ctfPlayer == null || !ctfPlayer.isInGame()) {
            return;
        }

        CTFGame game = ctfPlayer.getGame();

        // Handle flag carrier leaving
        if (ctfPlayer.hasFlag()) {
            game.returnFlag(ctfPlayer);
        }

        // Save player data before removing
        plugin.getPlayerDataManager().savePlayerData(ctfPlayer);

        // Remove from game
        game.removePlayer(ctfPlayer);
        players.remove(player.getUniqueId());

        // Clear spawn protection
        spawnProtection.remove(player.getUniqueId());
        killStreaks.remove(player.getUniqueId());

        // Cancel respawn task if active
        BukkitTask respawnTask = respawnTasks.remove(player.getUniqueId());
        if (respawnTask != null) {
            respawnTask.cancel();
        }

        // Clear inventory and effects
        player.getInventory().clear();
        player.getActivePotionEffects().forEach(effect ->
                player.removePotionEffect(effect.getType()));

        // Remove glowing effect
        player.setGlowing(false);

        // Reset health and hunger
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);

        // Clear boss bar and scoreboard
        plugin.getMessageManager().clearBossBar(player);
        plugin.getScoreboardManager().clearPlayerScoreboard(player);

        // Update remaining players' boss bars and scoreboards
        if (!game.getPlayers().isEmpty()) {
            plugin.getMessageManager().updateLobbyBossBar(game);
            plugin.getScoreboardManager().updateGameScoreboard(game);

            // Check if game should end due to insufficient players
            if (game.getState() == GameState.PLAYING && game.getPlayers().size() < 2) {
                endGame(game, null); // End with no winner
            } else if (game.getState() == GameState.STARTING &&
                    game.getPlayers().size() < plugin.getConfigManager().getGameplaySetting("min-players-to-start", 8)) {
                // Stop countdown if not enough players
                stopGameCountdown(game);
            }
        } else {
            // No players left, remove the game
            activeGames.remove(game.getArena());
        }
    }

    /**
     * Get or create a game for an arena
     */
    private CTFGame getOrCreateGame(Arena arena) {
        CTFGame game = activeGames.get(arena);
        if (game == null) {
            game = new CTFGame(arena, plugin);
            activeGames.put(arena, game);
        }
        return game;
    }

    /**
     * Check if a game can start and begin countdown
     */
    private void checkGameStart(CTFGame game) {
        if (game.getState() != GameState.WAITING) {
            return;
        }

        int minPlayers = plugin.getConfigManager().getGameplaySetting("min-players-to-start", 8);
        if (game.getPlayers().size() >= minPlayers) {
            startGameCountdown(game);
        }
    }

    /**
     * Start game countdown
     */
    private void startGameCountdown(CTFGame game) {
        game.setState(GameState.STARTING);

        int countdownTime = plugin.getConfigManager().getGameplaySetting("pre-game-countdown-seconds", 20);

        new BukkitRunnable() {
            int timeLeft = countdownTime;

            @Override
            public void run() {
                if (game.getState() != GameState.STARTING) {
                    cancel();
                    return;
                }

                // Check if we still have enough players
                int minPlayers = plugin.getConfigManager().getGameplaySetting("min-players-to-start", 8);
                if (game.getPlayers().size() < minPlayers) {
                    stopGameCountdown(game);
                    cancel();
                    return;
                }

                if (timeLeft <= 0) {
                    startGame(game);
                    cancel();
                    return;
                }

                // Update boss bar
                plugin.getMessageManager().updateCountdownBossBar(game, timeLeft);

                // Send countdown messages at specific intervals
                if (timeLeft <= 10 || timeLeft % 10 == 0) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("time", String.valueOf(timeLeft));
                    game.broadcastMessage("game-starting", placeholders);
                    game.playSound(plugin.getConfigManager().getSound("countdown"));
                }

                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Stop game countdown
     */
    private void stopGameCountdown(CTFGame game) {
        game.setState(GameState.WAITING);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("min_players", String.valueOf(plugin.getConfigManager().getGameplaySetting("min-players-to-start", 8)));
        game.broadcastMessage("countdown-stopped", placeholders);

        // Update boss bar back to waiting
        plugin.getMessageManager().updateLobbyBossBar(game);
    }

    /**
     * Start the actual game
     */
    private void startGame(CTFGame game) {
        game.setState(GameState.PLAYING);

        // Set game duration
        int gameDuration = plugin.getConfigManager().getGameplaySetting("game-duration-minutes", 10) * 60;
        game.setTimeLeft(gameDuration);

        // Assign teams automatically
        assignTeamsAutomatically(game);

        // Spawn flags
        game.spawnFlags();

        // Teleport players to spawn points and give loadouts
        for (CTFPlayer ctfPlayer : game.getPlayers()) {
            Player player = ctfPlayer.getPlayer();
            if (player != null && player.isOnline()) {
                teleportToTeamSpawn(player, ctfPlayer);
                applyBasicLoadoutToPlayer(player);
                applyTeamColoredArmor(player, ctfPlayer.getTeam());
                applySpawnProtection(player);

                // Increment games played
                ctfPlayer.addGamePlayed();
            }
        }

        // Send game start messages
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("flags_to_win", String.valueOf(plugin.getConfigManager().getGameplaySetting("flags-to-win", 3)));
        placeholders.put("kills_to_win", String.valueOf(plugin.getConfigManager().getGameplaySetting("kills-to-win", 25)));

        plugin.getMessageManager().sendGameTitle(game, "title-game-start", null, placeholders);
        game.broadcastMessage("game-started", placeholders);
        game.playSound(plugin.getConfigManager().getSound("game_start"));

        // Start power-up spawning
        plugin.getPowerUpManager().startPowerUpSpawning(game);

        // Start game timer
        startGameTimer(game);

        // Update scoreboards and boss bars
        plugin.getScoreboardManager().updateGameScoreboard(game);
        plugin.getMessageManager().updateGameTimeBossBar(game);
    }

    /**
     * Assign teams automatically with balance
     */
    private void assignTeamsAutomatically(CTFGame game) {
        List<CTFPlayer> unassignedPlayers = new ArrayList<>(game.getPlayers());
        Collections.shuffle(unassignedPlayers); // Randomize for fairness

        int redCount = 0;
        int blueCount = 0;
        int maxPerTeam = plugin.getConfigManager().getGameplaySetting("max-players-per-team", 4);

        for (CTFPlayer ctfPlayer : unassignedPlayers) {
            Arena.TeamColor assignedTeam;

            if (redCount < maxPerTeam && (blueCount >= maxPerTeam || redCount <= blueCount)) {
                assignedTeam = Arena.TeamColor.RED;
                redCount++;
            } else {
                assignedTeam = Arena.TeamColor.BLUE;
                blueCount++;
            }

            ctfPlayer.setTeam(assignedTeam);

            // Send team assignment message
            if (ctfPlayer.getPlayer() != null && ctfPlayer.getPlayer().isOnline()) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("team_color", assignedTeam.getColorCode());
                placeholders.put("team_name", assignedTeam.getName().toUpperCase());
                ctfPlayer.getPlayer().sendMessage(plugin.getConfigManager().getMessage("assigned-to-team", placeholders));
            }
        }
    }

    /**
     * Teleport player to their team spawn point
     */
    private void teleportToTeamSpawn(Player player, CTFPlayer ctfPlayer) {
        if (ctfPlayer.getTeam() == null) {
            return;
        }

        Arena arena = ctfPlayer.getGame().getArena();
        Arena.Team teamData = arena.getTeam(ctfPlayer.getTeam());

        // Find an available spawn point
        Location[] spawnPoints = teamData.getSpawnPoints();
        List<Location> availableSpawns = new ArrayList<>();

        for (Location spawn : spawnPoints) {
            if (spawn != null) {
                availableSpawns.add(spawn);
            }
        }

        if (!availableSpawns.isEmpty()) {
            Location spawn = availableSpawns.get(new Random().nextInt(availableSpawns.size()));
            player.teleport(spawn);
        }
    }

    /**
     * Apply basic loadout to player
     */
    public void applyBasicLoadoutToPlayer(Player player) {
        player.getInventory().clear();

        // Basic loadout
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        ItemStack bow = new ItemStack(Material.BOW);
        ItemStack arrows = new ItemStack(Material.ARROW, 32);
        ItemStack food = new ItemStack(Material.COOKED_BEEF, 16);
        ItemStack blocks = new ItemStack(Material.COBBLESTONE, 64);

        player.getInventory().setItem(0, sword);
        player.getInventory().setItem(1, bow);
        player.getInventory().setItem(2, blocks);
        player.getInventory().setItem(3, food);
        player.getInventory().setItem(9, arrows);

        // Reset health and hunger
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);

        player.updateInventory();
    }

    /**
     * Apply team colored armor
     */
    private void applyTeamColoredArmor(Player player, Arena.TeamColor teamColor) {
        if (teamColor == null) return;

        Color armorColor = teamColor == Arena.TeamColor.RED ? Color.RED : Color.BLUE;

        // Create dyed leather armor
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);

        // Dye the armor
        dyeLeatherArmor(helmet, armorColor);
        dyeLeatherArmor(chestplate, armorColor);
        dyeLeatherArmor(leggings, armorColor);
        dyeLeatherArmor(boots, armorColor);

        // Set armor
        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
    }

    /**
     * Dye leather armor with specified color
     */
    private void dyeLeatherArmor(ItemStack armor, Color color) {
        if (armor.getItemMeta() instanceof org.bukkit.inventory.meta.LeatherArmorMeta meta) {
            meta.setColor(color);
            armor.setItemMeta(meta);
        }
    }

    /**
     * Apply spawn protection to player
     */
    public void applySpawnProtection(Player player) {
        int protectionTime = plugin.getConfigManager().getGameplaySetting("spawn-protection-seconds", 5);
        spawnProtection.put(player.getUniqueId(), System.currentTimeMillis() + (protectionTime * 1000L));

        // Show spawn protection boss bar
        plugin.getMessageManager().showSpawnProtectionBossBar(player);

        // Apply visual effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, protectionTime * 20, 4, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, protectionTime * 20, 1, true, false));
    }

    /**
     * Check if player has spawn protection
     */
    public boolean hasSpawnProtection(Player player) {
        Long protectionEnd = spawnProtection.get(player.getUniqueId());
        if (protectionEnd == null) {
            return false;
        }

        if (System.currentTimeMillis() > protectionEnd) {
            spawnProtection.remove(player.getUniqueId());
            return false;
        }

        return true;
    }

    /**
     * Start game timer
     */
    private void startGameTimer(CTFGame game) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (game.getState() != GameState.PLAYING) {
                    cancel();
                    return;
                }

                int timeLeft = game.getTimeLeft();
                if (timeLeft <= 0) {
                    // Time's up - determine winner
                    Arena.TeamColor winner = determineWinner(game);
                    endGame(game, winner);
                    cancel();
                    return;
                }

                game.setTimeLeft(timeLeft - 1);

                // Update boss bar every second
                plugin.getMessageManager().updateGameTimeBossBar(game);

                // Update scoreboards every 3 seconds for performance
                if (timeLeft % 3 == 0) {
                    plugin.getScoreboardManager().updateGameScoreboard(game);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * Determine winner based on current game state
     */
    private Arena.TeamColor determineWinner(CTFGame game) {
        Map<Arena.TeamColor, Integer> scores = game.getScores();

        // Check flag scores first
        int redFlags = scores.getOrDefault(Arena.TeamColor.RED, 0);
        int blueFlags = scores.getOrDefault(Arena.TeamColor.BLUE, 0);

        if (redFlags > blueFlags) {
            return Arena.TeamColor.RED;
        } else if (blueFlags > redFlags) {
            return Arena.TeamColor.BLUE;
        }

        // Flags are tied, check if it's 2-2 (combined scoring)
        if (game.isFlagsTiedAt2()) {
            return game.getTeamWithHighestCombinedScore();
        }

        // Regular tie, check kills
        return game.getTeamWithMostKills();
    }

    /**
     * Handle player death in CTF game with XP rewards
     */
    public void handlePlayerDeath(Player player, Player killer) {
        CTFPlayer ctfPlayer = players.get(player.getUniqueId());
        if (ctfPlayer == null || !ctfPlayer.isInGame()) {
            return;
        }

        CTFGame game = ctfPlayer.getGame();

        // Handle flag carrier death
        if (ctfPlayer.hasFlag()) {
            game.returnFlag(ctfPlayer);
        }

        // Update stats
        ctfPlayer.addDeath();

        // Handle killer stats and effects
        if (killer != null && !killer.equals(player)) {
            CTFPlayer killerCtfPlayer = players.get(killer.getUniqueId());
            if (killerCtfPlayer != null && killerCtfPlayer.isInGame() && killerCtfPlayer.getGame().equals(game)) {
                killerCtfPlayer.addKill(); // This now grants XP automatically

                // Add team kill
                if (killerCtfPlayer.getTeam() != null) {
                    game.addTeamKill(killerCtfPlayer.getTeam());

                    // Check for team enhancements
                    checkTeamEnhancements(game, killerCtfPlayer.getTeam());

                    // Check kill limit win condition
                    Arena.TeamColor killWinner = game.getTeamWithKillLimit();
                    if (killWinner != null) {
                        endGame(game, killWinner);
                        return;
                    }
                }

                // Handle kill streaks
                handleKillStreak(killer);

                // Send kill message
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("killer", killer.getName());
                placeholders.put("victim", player.getName());
                placeholders.put("killer_color", killerCtfPlayer.getTeam() != null ? killerCtfPlayer.getTeam().getColorCode() : "§f");
                placeholders.put("victim_color", ctfPlayer.getTeam() != null ? ctfPlayer.getTeam().getColorCode() : "§f");

                if (ctfPlayer.hasFlag()) {
                    game.broadcastMessage("killed-flag-carrier", placeholders);
                } else {
                    game.broadcastMessage("player-killed", placeholders);
                }
            }
        }

        // Start respawn countdown
        startRespawnCountdown(player, ctfPlayer);
    }

    /**
     * Handle kill streaks
     */
    private void handleKillStreak(Player killer) {
        int currentStreak = killStreaks.getOrDefault(killer.getUniqueId(), 0) + 1;
        killStreaks.put(killer.getUniqueId(), currentStreak);

        // Announce kill streaks
        String message = null;
        switch (currentStreak) {
            case 3 -> message = "killstreak-spree";
            case 5 -> message = "killstreak-rampage";
            case 7 -> message = "killstreak-dominating";
            case 10 -> message = "killstreak-unstoppable";
            case 15 -> message = "killstreak-legendary";
        }

        if (message != null) {
            CTFPlayer killerCtfPlayer = players.get(killer.getUniqueId());
            if (killerCtfPlayer != null && killerCtfPlayer.isInGame()) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", killer.getName());
                placeholders.put("streak", String.valueOf(currentStreak));
                killerCtfPlayer.getGame().broadcastMessage(message, placeholders);
            }
        }
    }

    /**
     * Check and apply team enhancements based on kill count
     */
    private void checkTeamEnhancements(CTFGame game, Arena.TeamColor team) {
        Map<Arena.TeamColor, Integer> teamKills = game.getTeamKills();
        int kills = teamKills.getOrDefault(team, 0);

        // Apply team enhancements at 5, 10, 15, 20 kills
        int enhancementLevel = 0;
        if (kills >= 20) enhancementLevel = 4;
        else if (kills >= 15) enhancementLevel = 3;
        else if (kills >= 10) enhancementLevel = 2;
        else if (kills >= 5) enhancementLevel = 1;

        if (enhancementLevel > 0 && kills % 5 == 0) {
            // Apply enhancement to all team members
            for (CTFPlayer ctfPlayer : game.getPlayersOnTeam(team)) {
                Player player = ctfPlayer.getPlayer();
                if (player != null && player.isOnline()) {
                    applyTeamKillEnhancements(player, game, team);
                }
            }

            // Announce enhancement
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("team", team.getName().toUpperCase());
            placeholders.put("kills", String.valueOf(kills));
            placeholders.put("level", String.valueOf(enhancementLevel));
            game.broadcastMessage("team-enhancement", placeholders);
        }
    }

    /**
     * Apply team kill enhancements to player's sword
     */
    public void applyTeamKillEnhancements(Player player, CTFGame game, Arena.TeamColor team) {
        ItemStack sword = player.getInventory().getItem(0);
        if (sword != null && sword.getType() == Material.IRON_SWORD) {
            Map<Arena.TeamColor, Integer> teamKills = game.getTeamKills();
            int kills = teamKills.getOrDefault(team, 0);

            int sharpnessLevel = Math.min(kills / 5, 4); // Max Sharpness IV

            if (sharpnessLevel > 0) {
                sword.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.SHARPNESS, sharpnessLevel);

                // Update item name
                ItemMeta meta = sword.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                            team.getColorCode() + "Enhanced Sword +" + sharpnessLevel));
                    sword.setItemMeta(meta);
                }
            }
        }
    }

    /**
     * Start respawn countdown for dead player
     */
    private void startRespawnCountdown(Player player, CTFPlayer ctfPlayer) {
        int respawnDelay = plugin.getConfigManager().getGameplaySetting("respawn-delay-seconds", 3);

        BukkitTask respawnTask = new BukkitRunnable() {
            int timeLeft = respawnDelay;

            @Override
            public void run() {
                if (!player.isOnline() || !ctfPlayer.isInGame()) {
                    cancel();
                    respawnTasks.remove(player.getUniqueId());
                    return;
                }

                if (timeLeft <= 0) {
                    // Respawn player
                    player.spigot().respawn();
                    respawnTasks.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                // Send countdown message
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("time", String.valueOf(timeLeft));
                player.sendMessage(plugin.getConfigManager().getMessage("respawning", placeholders));

                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        respawnTasks.put(player.getUniqueId(), respawnTask);
    }

    /**
     * Handle flag taken (apply glowing effect)
     */
    public void handleFlagTaken(CTFPlayer player, Arena.TeamColor flagTeam) {
        Player bukkitPlayer = player.getPlayer();
        if (bukkitPlayer != null && bukkitPlayer.isOnline()) {
            bukkitPlayer.setGlowing(true);
        }
    }

    /**
     * Handle flag dropped (remove glowing effect)
     */
    public void handleFlagDropped(CTFPlayer player) {
        Player bukkitPlayer = player.getPlayer();
        if (bukkitPlayer != null && bukkitPlayer.isOnline()) {
            bukkitPlayer.setGlowing(false);
        }
    }

    /**
     * Force start a game with minimum players
     */
    public void forceStartGame(CTFGame game) {
        if (game.getState() == GameState.WAITING) {
            startGameCountdown(game);
        } else if (game.getState() == GameState.STARTING) {
            startGame(game);
        }
    }

    /**
     * End a game and award wins to winning team with Call of Duty style statistics
     */
    public void endGame(CTFGame game, Arena.TeamColor winner) {
        game.setState(GameState.ENDING);

        // Stop power-up spawning
        plugin.getPowerUpManager().stopPowerUpSpawning(game);

        // Award wins and currency to winning team players
        if (winner != null) {
            for (CTFPlayer ctfPlayer : game.getPlayersOnTeam(winner)) {
                ctfPlayer.addGameWon();
                // Award currency for winning
                plugin.getCurrencyManager().addCoins(ctfPlayer.getPlayer(),
                        plugin.getConfigManager().getGameplaySetting("currency.win-reward", 100));
            }
        }

        // Award participation currency to all players - FIXED LAMBDA ISSUE
        final int participationReward = plugin.getConfigManager().getGameplaySetting("currency.participation-reward", 25);
        final int killReward = plugin.getConfigManager().getGameplaySetting("currency.per-kill", 5);
        final int captureReward = plugin.getConfigManager().getGameplaySetting("currency.per-capture", 20);
        final int returnReward = plugin.getConfigManager().getGameplaySetting("currency.per-return", 10);

        for (CTFPlayer ctfPlayer : game.getPlayers()) {
            Player player = ctfPlayer.getPlayer();
            if (player != null && player.isOnline()) {
                // Base participation reward
                plugin.getCurrencyManager().addCoins(player, participationReward);

                // Performance bonuses
                int killBonus = ctfPlayer.getKills() * killReward;
                int captureBonus = ctfPlayer.getCaptures() * captureReward;
                int returnBonus = ctfPlayer.getFlagReturns() * returnReward;

                plugin.getCurrencyManager().addCoins(player, killBonus + captureBonus + returnBonus);
            }
        }

        // Send end messages and titles
        Map<String, String> placeholders = new HashMap<>();

        if (winner != null) {
            placeholders.put("team_color", winner.getColorCode());
            placeholders.put("team_name", winner.getName().toUpperCase());

            // Determine win type
            String winMessage;
            if (game.getScore(winner) >= plugin.getConfigManager().getGameplaySetting("flags-to-win", 3)) {
                winMessage = "game-won";
            } else if (game.getTeamKills().getOrDefault(winner, 0) >= plugin.getConfigManager().getGameplaySetting("kills-to-win", 25)) {
                winMessage = "game-won-kills";
            } else if (game.isFlagsTiedAt2()) {
                winMessage = "game-won-combined";
            } else {
                winMessage = "game-won";
            }

            game.broadcastMessage(winMessage, placeholders);
            plugin.getMessageManager().updateVictoryBossBar(game, winner);

            // Send titles to players
            for (CTFPlayer ctfPlayer : game.getPlayers()) {
                Player player = ctfPlayer.getPlayer();
                if (player != null && player.isOnline()) {
                    if (ctfPlayer.getTeam() == winner) {
                        plugin.getMessageManager().sendTitle(player, "title-game-won", null, placeholders);
                        player.playSound(player.getLocation(), plugin.getConfigManager().getSound("game_win"), 1.0f, 1.0f);
                    } else {
                        plugin.getMessageManager().sendTitle(player, "title-game-lost", null, placeholders);
                        player.playSound(player.getLocation(), plugin.getConfigManager().getSound("game_lose"), 1.0f, 1.0f);
                    }
                }
            }
        } else {
            // Draw
            game.broadcastMessage("game-draw", placeholders);
            plugin.getMessageManager().updateVictoryBossBar(game, null);
        }

        // Show Call of Duty style game statistics
        showCallOfDutyStyleStatistics(game, winner);

        // Schedule cleanup
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            cleanupGame(game);
        }, 200L); // 10 seconds
    }

    /**
     * Show Call of Duty style game statistics with titles and detailed breakdown
     */
    private void showCallOfDutyStyleStatistics(CTFGame game, Arena.TeamColor winner) {
        // Find MVP (Most Valuable Player)
        CTFPlayer mvp = null;
        int highestScore = 0;

        for (CTFPlayer ctfPlayer : game.getPlayers()) {
            int playerScore = ctfPlayer.getScore();
            if (playerScore > highestScore) {
                highestScore = playerScore;
                mvp = ctfPlayer;
            }
        }

        // Send statistics to all players with Call of Duty style presentation
        for (CTFPlayer ctfPlayer : game.getPlayers()) {
            Player player = ctfPlayer.getPlayer();
            if (player != null && player.isOnline()) {

                // Show personal performance title
                Map<String, String> titlePlaceholders = new HashMap<>();
                titlePlaceholders.put("kills", String.valueOf(ctfPlayer.getKills()));
                titlePlaceholders.put("deaths", String.valueOf(ctfPlayer.getDeaths()));
                titlePlaceholders.put("captures", String.valueOf(ctfPlayer.getCaptures()));
                titlePlaceholders.put("kd", String.format("%.2f", ctfPlayer.getKDRatio()));

                // Send performance title
                plugin.getMessageManager().sendTitle(player, "title-performance", "subtitle-performance", titlePlaceholders);

                // Wait 3 seconds then show detailed stats
                final CTFPlayer finalCtfPlayer = ctfPlayer;
                final CTFPlayer finalMvp = mvp;
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        showDetailedStatistics(player, finalCtfPlayer, game, finalMvp, winner);
                    }
                }, 60L);
            }
        }
    }

    /**
     * Show detailed Call of Duty style statistics breakdown
     */
    private void showDetailedStatistics(Player player, CTFPlayer ctfPlayer, CTFGame game, CTFPlayer mvp, Arena.TeamColor winner) {
        player.sendMessage("§8§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("§6§l                        MATCH RESULTS");
        player.sendMessage("§8§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        // Match outcome
        if (winner != null) {
            if (ctfPlayer.getTeam() == winner) {
                player.sendMessage("§a§l                          VICTORY");
            } else {
                player.sendMessage("§c§l                          DEFEAT");
            }
        } else {
            player.sendMessage("§e§l                           DRAW");
        }

        player.sendMessage("");

        // Personal performance
        player.sendMessage("§e§l► YOUR PERFORMANCE:");
        player.sendMessage("§7  Kills: §f" + ctfPlayer.getKills() + "  §7Deaths: §f" + ctfPlayer.getDeaths() + "  §7K/D: §f" + String.format("%.2f", ctfPlayer.getKDRatio()));
        player.sendMessage("§7  Flag Captures: §f" + ctfPlayer.getCaptures() + "  §7Flag Returns: §f" + ctfPlayer.getFlagReturns());
        player.sendMessage("§7  Score: §f" + ctfPlayer.getScore() + "  §7Level: §f" + ctfPlayer.getLevel());

        // Currency earned
        int totalEarned = plugin.getConfigManager().getGameplaySetting("currency.participation-reward", 25);
        if (ctfPlayer.getTeam() == winner) {
            totalEarned += plugin.getConfigManager().getGameplaySetting("currency.win-reward", 100);
        }
        totalEarned += ctfPlayer.getKills() * plugin.getConfigManager().getGameplaySetting("currency.per-kill", 5);
        totalEarned += ctfPlayer.getCaptures() * plugin.getConfigManager().getGameplaySetting("currency.per-capture", 20);
        totalEarned += ctfPlayer.getFlagReturns() * plugin.getConfigManager().getGameplaySetting("currency.per-return", 10);

        player.sendMessage("§7  Coins Earned: §6" + totalEarned);

        player.sendMessage("");

        // MVP
        if (mvp != null && mvp.getPlayer() != null) {
            player.sendMessage("§6§l► MATCH MVP: §f" + mvp.getPlayer().getName() + " §7(Score: " + mvp.getScore() + ")");
            player.sendMessage("");
        }

        // Team scores
        player.sendMessage("§e§l► TEAM SCORES:");
        player.sendMessage("§c  Red Team: §f" + game.getScore(Arena.TeamColor.RED) + " flags, " +
                game.getTeamKills().getOrDefault(Arena.TeamColor.RED, 0) + " kills");
        player.sendMessage("§9  Blue Team: §f" + game.getScore(Arena.TeamColor.BLUE) + " flags, " +
                game.getTeamKills().getOrDefault(Arena.TeamColor.BLUE, 0) + " kills");

        player.sendMessage("");
        player.sendMessage("§8§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }

    /**
     * Cleanup game after it ends
     */
    private void cleanupGame(CTFGame game) {
        // Save all player data before cleanup
        for (CTFPlayer ctfPlayer : game.getPlayers()) {
            plugin.getPlayerDataManager().savePlayerData(ctfPlayer);
        }

        // Give end game items
        for (CTFPlayer ctfPlayer : game.getPlayers()) {
            Player player = ctfPlayer.getPlayer();
            if (player != null && player.isOnline()) {
                plugin.getLobbyManager().giveGameEndItems(player);
            }
        }

        // Auto-leave players after delay
        int autoLeaveDelay = plugin.getConfigManager().getGameplaySetting("auto-leave-delay-seconds", 10);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            List<CTFPlayer> playersToRemove = new ArrayList<>(game.getPlayers());
            for (CTFPlayer ctfPlayer : playersToRemove) {
                Player player = ctfPlayer.getPlayer();
                if (player != null && player.isOnline()) {
                    removePlayerFromGame(player);
                    plugin.getServerLobbyManager().teleportToServerLobby(player);
                }
            }

            // Remove game
            activeGames.remove(game.getArena());
        }, autoLeaveDelay * 20L);
    }

    /**
     * End all active games (called on plugin disable)
     */
    public void endAllGames() {
        for (CTFGame game : new ArrayList<>(activeGames.values())) {
            endGame(game, null);
        }
    }

    /**
     * Handle player reconnection
     */
    public void handlePlayerReconnection(Player player) {
        CTFPlayer ctfPlayer = players.get(player.getUniqueId());
        if (ctfPlayer != null && ctfPlayer.isInGame()) {
            // Player reconnected to an active game
            CTFGame game = ctfPlayer.getGame();

            if (game.getState() == GameState.PLAYING) {
                // Teleport back to team spawn
                teleportToTeamSpawn(player, ctfPlayer);
                applyBasicLoadoutToPlayer(player);
                if (ctfPlayer.getTeam() != null) {
                    applyTeamColoredArmor(player, ctfPlayer.getTeam());
                    applyTeamKillEnhancements(player, game, ctfPlayer.getTeam());
                }
                applySpawnProtection(player);
            } else {
                // Game is in lobby, give lobby items
                if (game.getArena().getLobbySpawn() != null) {
                    player.teleport(game.getArena().getLobbySpawn());
                }
                plugin.getLobbyManager().giveLobbyItems(player);
            }

            // Update UI
            plugin.getScoreboardManager().updatePlayerScoreboard(player);
            plugin.getMessageManager().updateGameTimeBossBar(game);
        }
    }

    /**
     * Handle player disconnect
     */
    public void handlePlayerDisconnect(Player player) {
        CTFPlayer ctfPlayer = players.get(player.getUniqueId());
        if (ctfPlayer != null && ctfPlayer.isInGame()) {
            CTFGame game = ctfPlayer.getGame();

            // Handle flag carrier disconnect
            if (ctfPlayer.hasFlag()) {
                CTFFlag flag = ctfPlayer.getCarryingFlag();
                flag.returnToBase();
                ctfPlayer.setCarryingFlag(null);

                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", player.getName());
                placeholders.put("team_color", flag.getTeam().getColorCode());
                game.broadcastMessage("flag-returned-disconnect", placeholders);
            }

            // Save player data on disconnect
            plugin.getPlayerDataManager().savePlayerData(ctfPlayer);

            // Don't remove player immediately - they might reconnect
            // The cleanup will happen in the quit listener if they don't return
        }
    }

    /**
     * Cleanup method for plugin disable
     */
    public void cleanup() {
        // Save all player data before cleanup
        for (CTFPlayer ctfPlayer : players.values()) {
            plugin.getPlayerDataManager().savePlayerData(ctfPlayer);
        }

        // Cancel all respawn tasks
        for (BukkitTask task : respawnTasks.values()) {
            task.cancel();
        }
        respawnTasks.clear();

        // Clear all data
        spawnProtection.clear();
        killStreaks.clear();
        teamKillCounts.clear();
        players.clear();
        activeGames.clear();
    }

    // Getters

    public CTFPlayer getCTFPlayer(Player player) {
        return players.get(player.getUniqueId());
    }

    public CTFGame getGame(Arena arena) {
        return activeGames.get(arena);
    }

    public boolean isArenaInUse(String arenaName) {
        return activeGames.keySet().stream()
                .anyMatch(arena -> arena.getName().equals(arenaName));
    }

    public List<CTFPlayer> getPlayersInArena(Arena arena) {
        CTFGame game = activeGames.get(arena);
        return game != null ? new ArrayList<>(game.getPlayers()) : new ArrayList<>();
    }

    public Collection<CTFGame> getActiveGames() {
        return activeGames.values();
    }
}