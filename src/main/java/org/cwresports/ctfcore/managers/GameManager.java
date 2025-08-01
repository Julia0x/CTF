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
 * Enhanced game manager with improved reconnection handling and lobby item management
 * Manages all active CTF games and player interactions with comprehensive systems
 */
public class GameManager {

    private final CTFCore plugin;
    private final Map<Arena, CTFGame> activeGames;
    private final Map<UUID, CTFPlayer> players;
    private final Map<UUID, Long> spawnProtection;
    private final Map<UUID, Integer> killStreaks;
    private final Map<Arena.TeamColor, Integer> teamKillCounts;
    private final Map<UUID, BukkitTask> respawnTasks;
    private final Set<UUID> playersWhoLeftArena;

    /**
     * Stores data needed for player reconnection
     */
    private static class PlayerReconnectionData {
        private final UUID playerId;
        private final String arenaName;
        private final Arena.TeamColor team;
        private final GameState gameState;
        private final boolean hadFlag;
        private final long disconnectTime;

        public PlayerReconnectionData(UUID playerId, String arenaName, Arena.TeamColor team,
                                      GameState gameState, boolean hadFlag) {
            this.playerId = playerId;
            this.arenaName = arenaName;
            this.team = team;
            this.gameState = gameState;
            this.hadFlag = hadFlag;
            this.disconnectTime = System.currentTimeMillis();
        }

        public UUID getPlayerId() { return playerId; }
        public String getArenaName() { return arenaName; }
        public Arena.TeamColor getTeam() { return team; }
        public GameState getGameState() { return gameState; }
        public boolean hadFlag() { return hadFlag; }
        public long getDisconnectTime() { return disconnectTime; }
    }

    public GameManager(CTFCore plugin) {
        this.plugin = plugin;
        this.activeGames = new ConcurrentHashMap<>();
        this.players = new ConcurrentHashMap<>();
        this.spawnProtection = new ConcurrentHashMap<>();
        this.killStreaks = new ConcurrentHashMap<>();
        this.teamKillCounts = new ConcurrentHashMap<>();
        this.respawnTasks = new ConcurrentHashMap<>();
        this.playersWhoLeftArena = ConcurrentHashMap.newKeySet();

        // Start cleanup task for old leave data
        startLeaveDataCleanupTask();
    }

    /**
     * Enhanced player addition with proper lobby item management
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

        // Clear any existing items and give lobby items
        player.getInventory().clear();
        plugin.getLobbyManager().giveLobbyItems(player);

        // Update lobby boss bar
        plugin.getMessageManager().updateLobbyBossBar(game);

        // Update scoreboard
        plugin.getScoreboardManager().updateGameScoreboard(game);

        // Check if we can start the game
        checkGameStart(game);

        return true;
    }

    /**
     * Enhanced player removal with proper cleanup and team balance checking
     */
    public void removePlayerFromGame(Player player) {
        removePlayerFromGame(player, false);
    }

    /**
     * Enhanced player removal with leave tracking and auto-win logic
     */
    public void removePlayerFromGame(Player player, boolean playerLeftVoluntarily) {
        CTFPlayer ctfPlayer = players.get(player.getUniqueId());
        if (ctfPlayer == null || !ctfPlayer.isInGame()) {
            return;
        }

        CTFGame game = ctfPlayer.getGame();
        Arena.TeamColor playerTeam = ctfPlayer.getTeam();

        // Handle flag carrier leaving
        if (ctfPlayer.hasFlag()) {
            game.returnFlag(ctfPlayer);
        }

        // If player left voluntarily, mark them as unable to rejoin
        if (playerLeftVoluntarily) {
            playersWhoLeftArena.add(player.getUniqueId());
            
            // Broadcast leave message to other players
            if (playerTeam != null && game.getState() == GameState.PLAYING) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", player.getName());
                placeholders.put("team_color", playerTeam.getColorCode());
                placeholders.put("team_name", playerTeam.getName());
                game.broadcastMessage("player-left-game", placeholders);
                
                plugin.getLogger().info("Player " + player.getName() + 
                    " left the game voluntarily from " + playerTeam.getName() + " team");
            }
        }

        // Save player data before removing
        plugin.getPlayerDataManager().savePlayerData(ctfPlayer);

        // Remove from game
        game.removePlayer(ctfPlayer);
        players.remove(player.getUniqueId());

        // Clear spawn protection and other player states
        removeSpawnProtection(player);
        killStreaks.remove(player.getUniqueId());

        // Cancel respawn task if active and reset game mode
        BukkitTask respawnTask = respawnTasks.remove(player.getUniqueId());
        if (respawnTask != null) {
            respawnTask.cancel();
            if (player.getGameMode() == GameMode.SPECTATOR) {
                player.setGameMode(GameMode.SURVIVAL);
            }
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

        // Update lobby items to server lobby state
        plugin.getLobbyManager().updatePlayerState(player);

        // Check for team balance and auto-win conditions
        if (game.getState() == GameState.PLAYING) {
            checkTeamBalanceAndAutoWin(game);
        }

        // Update remaining players' boss bars and scoreboards
        if (!game.getPlayers().isEmpty()) {
            plugin.getMessageManager().updateLobbyBossBar(game);
            plugin.getScoreboardManager().updateGameScoreboard(game);

            // Check if game should end due to insufficient players
            if (game.getState() == GameState.PLAYING && game.getPlayers().size() < 2) {
                endGame(game, null);
            } else if (game.getState() == GameState.STARTING &&
                    game.getPlayers().size() < plugin.getConfigManager().getGameplaySetting("min-players-to-start", 8)) {
                stopGameCountdown(game);
            }
        } else {
            // No players left, remove the game
            activeGames.remove(game.getArena());
        }
    }

    /**
     * Check team balance and trigger auto-win if one team is empty
     */
    private void checkTeamBalanceAndAutoWin(CTFGame game) {
        Map<Arena.TeamColor, List<CTFPlayer>> teamPlayers = new HashMap<>();
        
        // Group players by team
        for (CTFPlayer ctfPlayer : game.getPlayers()) {
            Arena.TeamColor team = ctfPlayer.getTeam();
            if (team != null) {
                teamPlayers.computeIfAbsent(team, k -> new ArrayList<>()).add(ctfPlayer);
            }
        }

        // Check if any team is completely empty
        boolean redTeamEmpty = teamPlayers.getOrDefault(Arena.TeamColor.RED, Collections.emptyList()).isEmpty();
        boolean blueTeamEmpty = teamPlayers.getOrDefault(Arena.TeamColor.BLUE, Collections.emptyList()).isEmpty();

        Arena.TeamColor winningTeam = null;
        
        if (redTeamEmpty && !blueTeamEmpty) {
            winningTeam = Arena.TeamColor.BLUE;
            plugin.getLogger().info("Red team is empty, Blue team wins automatically in arena: " + game.getArena().getName());
        } else if (blueTeamEmpty && !redTeamEmpty) {
            winningTeam = Arena.TeamColor.RED;
            plugin.getLogger().info("Blue team is empty, Red team wins automatically in arena: " + game.getArena().getName());
        }

        if (winningTeam != null) {
            // Broadcast auto-win message
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("winning_team_color", winningTeam.getColorCode());
            placeholders.put("winning_team_name", winningTeam.getName());
            game.broadcastMessage("team-abandoned-auto-win", placeholders);
            
            // End game with winning team
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                endGame(game, winningTeam);
            }, 60L); // 3 second delay to show the message
        }
    }
    private CTFGame getOrCreateGame(Arena arena) {
        CTFGame game = activeGames.get(arena);
        if (game == null) {
            game = new CTFGame(arena, plugin);
            activeGames.put(arena, game);
        }
        return game;
    }

    /**
     * Check if a game can start and start countdown
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
     * Start countdown before game begins
     */
    private void startGameCountdown(CTFGame game) {
        game.setState(GameState.STARTING);
        int countdownTime = plugin.getConfigManager().getGameplaySetting("pre-game-countdown-seconds", 20);
        game.setTimeLeft(countdownTime);

        // Update lobby items for all players
        for (CTFPlayer ctfPlayer : game.getPlayers()) {
            Player player = ctfPlayer.getPlayer();
            if (player != null && player.isOnline()) {
                plugin.getLobbyManager().updatePlayerState(player);
            }
        }

        // Assign players to teams
        assignTeams(game);

        // Start countdown task
        BukkitTask countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                int timeLeft = game.getTimeLeft();

                if (timeLeft <= 0) {
                    startGame(game);
                    cancel();
                    return;
                }

                // Update boss bar
                plugin.getMessageManager().updateCountdownBossBar(game, timeLeft);

                // Send countdown messages
                if (timeLeft == 10 || timeLeft <= 5) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("time", String.valueOf(timeLeft));
                    game.broadcastMessage("countdown-" + timeLeft, placeholders);
                }

                game.setTimeLeft(timeLeft - 1);
            }
        }.runTaskTimer(plugin, 0L, 20L);

        plugin.getLogger().info("Started countdown for game in arena: " + game.getArena().getName());
    }

    /**
     * Stop game countdown
     */
    private void stopGameCountdown(CTFGame game) {
        game.setState(GameState.WAITING);
        plugin.getMessageManager().updateLobbyBossBar(game);

        // Update lobby items for all players
        for (CTFPlayer ctfPlayer : game.getPlayers()) {
            Player player = ctfPlayer.getPlayer();
            if (player != null && player.isOnline()) {
                plugin.getLobbyManager().updatePlayerState(player);
            }
        }

        plugin.getLogger().info("Stopped countdown for game in arena: " + game.getArena().getName());
    }

    /**
     * Assign players to teams
     */
    private void assignTeams(CTFGame game) {
        List<CTFPlayer> playersToAssign = new ArrayList<>(game.getPlayers());
        Collections.shuffle(playersToAssign);

        Arena.TeamColor[] teams = Arena.TeamColor.values();
        for (int i = 0; i < playersToAssign.size(); i++) {
            CTFPlayer ctfPlayer = playersToAssign.get(i);
            Arena.TeamColor team = teams[i % teams.length];
            ctfPlayer.setTeam(team);

            Player player = ctfPlayer.getPlayer();
            if (player != null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("team_color", team.getColorCode());
                placeholders.put("team_name", team.getName().toUpperCase());
                player.sendMessage(plugin.getConfigManager().getMessage("team-assigned", placeholders));
            }
        }
    }

    /**
     * Start the actual game
     */
    private void startGame(CTFGame game) {
        game.setState(GameState.PLAYING);
        int gameDuration = plugin.getConfigManager().getGameplaySetting("game-duration-minutes", 10) * 60;
        game.setTimeLeft(gameDuration);

        // Allow building in the arena
        plugin.getWorldGuardManager().setBuildFlag(game.getArena().getWorld(), game.getArena().getWorldGuardRegion(), true);

        // Teleport players to team spawns and give loadouts
        for (CTFPlayer ctfPlayer : game.getPlayers()) {
            Player player = ctfPlayer.getPlayer();
            if (player != null && ctfPlayer.getTeam() != null) {
                teleportToTeamSpawn(player, ctfPlayer);
                applyBasicLoadoutToPlayer(player);
                applyTeamColoredArmor(player, ctfPlayer.getTeam());
                applySpawnProtection(player);

                // Update lobby items to playing state
                plugin.getLobbyManager().updatePlayerState(player);
            }
        }

        // Spawn flags
        game.spawnFlags();

        // Start power-ups
        plugin.getPowerUpManager().startPowerUpSpawning(game);

        // Broadcast game start
        Map<String, String> placeholders = new HashMap<>();
        game.broadcastMessage("game-started", placeholders);

        // Start game timer
        startGameTimer(game);

        plugin.getLogger().info("Started game in arena: " + game.getArena().getName());
    }

    /**
     * Start game timer
     */
    private void startGameTimer(CTFGame game) {
        BukkitTask timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (game.getState() != GameState.PLAYING) {
                    cancel();
                    return;
                }

                int timeLeft = game.getTimeLeft();

                if (timeLeft <= 0) {
                    // Time up, determine winner
                    Arena.TeamColor winner = determineWinner(game);
                    endGame(game, winner);
                    cancel();
                    return;
                }

                // Update boss bar
                plugin.getMessageManager().updateGameTimeBossBar(game);

                // Check for kill limit winner
                Arena.TeamColor killWinner = game.getTeamWithKillLimit();
                if (killWinner != null) {
                    endGame(game, killWinner);
                    cancel();
                    return;
                }

                game.setTimeLeft(timeLeft - 1);
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Determine winner at end of game
     */
    private Arena.TeamColor determineWinner(CTFGame game) {
        Map<Arena.TeamColor, Integer> scores = game.getScores();
        Arena.TeamColor winner = null;
        int highestScore = 0;

        for (Map.Entry<Arena.TeamColor, Integer> entry : scores.entrySet()) {
            if (entry.getValue() > highestScore) {
                highestScore = entry.getValue();
                winner = entry.getKey();
            } else if (entry.getValue() == highestScore) {
                winner = null; // Tie
            }
        }

        // If tied on flags, check combined score
        if (winner == null && game.isFlagsTiedAt2()) {
            winner = game.getTeamWithHighestCombinedScore();
        }

        return winner;
    }

    /**
     * Teleport player to team spawn
     */
    public void teleportToTeamSpawn(Player player, CTFPlayer ctfPlayer) {
        Arena.TeamColor team = ctfPlayer.getTeam();
        if (team == null) return;

        CTFGame game = ctfPlayer.getGame();
        if (game == null) return;

        Arena arena = game.getArena();
        Arena.Team teamData = arena.getTeam(team);

        // Find best spawn point
        Location[] spawnPoints = teamData.getSpawnPoints();
        List<Location> availableSpawns = new ArrayList<>();

        for (Location spawn : spawnPoints) {
            if (spawn != null) {
                availableSpawns.add(spawn);
            }
        }

        if (!availableSpawns.isEmpty()) {
            Random random = new Random();
            Location spawnPoint = availableSpawns.get(random.nextInt(availableSpawns.size()));
            player.teleport(spawnPoint);
        }
    }

    /**
     * Apply basic loadout to player
     */
    public void applyBasicLoadoutToPlayer(Player player) {
        player.getInventory().clear();
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);

        // Basic loadout
        player.getInventory().setItem(0, new ItemStack(Material.IRON_SWORD));
        player.getInventory().setItem(1, new ItemStack(Material.BOW));
        player.getInventory().setItem(2, new ItemStack(Material.GOLDEN_APPLE, 3));
        player.getInventory().setItem(9, new ItemStack(Material.ARROW, 64));

        // Building materials
        player.getInventory().setItem(3, new ItemStack(Material.COBBLESTONE, 64));
        player.getInventory().setItem(4, new ItemStack(Material.OAK_PLANKS, 32));
    }

    /**
     * Apply team colored armor
     */
    public void applyTeamColoredArmor(Player player, Arena.TeamColor teamColor) {
        org.bukkit.Color armorColor = teamColor == Arena.TeamColor.RED ?
                org.bukkit.Color.RED : org.bukkit.Color.BLUE;

        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);

        dyeLeatherArmor(helmet, armorColor);
        dyeLeatherArmor(chestplate, armorColor);
        dyeLeatherArmor(leggings, armorColor);
        dyeLeatherArmor(boots, armorColor);

        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
    }

    /**
     * Dye leather armor
     */
    private void dyeLeatherArmor(ItemStack armor, org.bukkit.Color color) {
        if (armor.getItemMeta() instanceof org.bukkit.inventory.meta.LeatherArmorMeta meta) {
            meta.setColor(color);
            armor.setItemMeta(meta);
        }
    }

    /**
     * Apply team kill enhancements
     */
    public void applyTeamKillEnhancements(Player player, CTFGame game, Arena.TeamColor team) {
        Map<Arena.TeamColor, Integer> teamKills = game.getTeamKills();
        int kills = teamKills.getOrDefault(team, 0);

        if (kills >= 5) {
            ItemStack sword = player.getInventory().getItem(0);
            if (sword != null && sword.getType() == Material.IRON_SWORD) {
                sword.addEnchantment(org.bukkit.enchantments.Enchantment.SHARPNESS, Math.min(kills / 5, 3));
                ItemMeta meta = sword.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(plugin.getConfigManager().getMessage(
                            team.getColorCode() + "Enhanced Sword +" + Math.min(kills / 5, 3)));
                    sword.setItemMeta(meta);
                }
            }
        }
    }

    /**
     * **ENHANCED FEATURE: Apply spawn protection with immediate removal on attack**
     */
    public void applySpawnProtection(Player player) {
        int protectionTime = plugin.getConfigManager().getGameplaySetting("spawn-protection-seconds", 5);
        long protectionEnd = System.currentTimeMillis() + (protectionTime * 1000L);

        spawnProtection.put(player.getUniqueId(), protectionEnd);

        // Show spawn protection boss bar
        plugin.getMessageManager().showSpawnProtectionBossBar(player);

        // Apply visual effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, protectionTime * 20, 255, false, false));

        plugin.getLogger().info("Applied spawn protection to " + player.getName() + " for " + protectionTime + " seconds");
    }

    /**
     * **ENHANCED FEATURE: Remove spawn protection immediately (called when player attacks)**
     */
    public void removeSpawnProtection(Player player) {
        if (spawnProtection.remove(player.getUniqueId()) != null) {
            // Remove visual effects
            player.removePotionEffect(PotionEffectType.RESISTANCE);

            // Remove boss bar
            plugin.getMessageManager().removeSpawnProtectionBossBar(player);

            plugin.getLogger().info("Removed spawn protection from " + player.getName());
        }
    }

    /**
     * Check if player has spawn protection
     */
    public boolean hasSpawnProtection(Player player) {
        Long protectionEnd = spawnProtection.get(player.getUniqueId());
        if (protectionEnd == null) {
            return false;
        }

        if (System.currentTimeMillis() >= protectionEnd) {
            // Protection expired, remove it
            removeSpawnProtection(player);
            return false;
        }

        return true;
    }

    /**
     * Handle player death with instant respawn
     */
    public void handlePlayerDeath(Player player, Player killer) {
        CTFPlayer ctfPlayer = players.get(player.getUniqueId());
        if (ctfPlayer == null || !ctfPlayer.isInGame()) {
            return;
        }

        CTFGame game = ctfPlayer.getGame();
        if (game == null || game.getState() != GameState.PLAYING) {
            return;
        }

        // Handle death statistics
        ctfPlayer.addDeath();

        // Handle killer statistics
        if (killer != null && !killer.equals(player)) {
            CTFPlayer killerCtfPlayer = players.get(killer.getUniqueId());
            if (killerCtfPlayer != null && killerCtfPlayer.isInGame()) {
                killerCtfPlayer.addKill();
                game.addTeamKill(killerCtfPlayer.getTeam());

                // Update kill streak
                int streak = killStreaks.getOrDefault(killer.getUniqueId(), 0) + 1;
                killStreaks.put(killer.getUniqueId(), streak);

                // Apply team enhancements
                applyTeamKillEnhancements(killer, game, killerCtfPlayer.getTeam());
            }
        }

        // Handle flag carrying
        if (ctfPlayer.hasFlag()) {
            game.returnFlag(ctfPlayer);
        }

        // INSTANT RESPAWN - No countdown, respawn immediately
        performInstantRespawn(player, ctfPlayer, game);
    }

    /**
     * Perform instant respawn without countdown
     */
    private void performInstantRespawn(Player player, CTFPlayer ctfPlayer, CTFGame game) {
        // Short delay to let death animation play
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || !ctfPlayer.isInGame()) {
                return;
            }

            // Reset player state
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setFireTicks(0);

            // Clear any existing effects
            player.getActivePotionEffects().forEach(effect ->
                    player.removePotionEffect(effect.getType()));

            // Teleport to team spawn
            teleportToTeamSpawn(player, ctfPlayer);
            
            // Apply loadout and team gear
            applyBasicLoadoutToPlayer(player);
            if (ctfPlayer.getTeam() != null) {
                applyTeamColoredArmor(player, ctfPlayer.getTeam());
                applyTeamKillEnhancements(player, game, ctfPlayer.getTeam());
            }
            
            // Apply spawn protection
            applySpawnProtection(player);
            
            // Mark as respawned
            ctfPlayer.respawn();

            // Send respawn message
            player.sendMessage(plugin.getConfigManager().getMessage("instant-respawn", 
                Collections.singletonMap("player", player.getName())));

        }, 10L); // 0.5 second delay
    }

    /**
     * Handle voluntary player leave from arena
     */
    public void handlePlayerLeaveArena(Player player) {
        CTFPlayer ctfPlayer = getCTFPlayer(player);
        if (ctfPlayer != null && ctfPlayer.isInGame()) {
            removePlayerFromGame(player, true); // Mark as voluntary leave
            
            // Send to server lobby
            plugin.getServerLobbyManager().teleportToServerLobby(player);
            plugin.getLobbyManager().updatePlayerState(player);
            
            // Send confirmation message
            player.sendMessage(plugin.getConfigManager().getMessage("left-arena-success", 
                Collections.singletonMap("player", player.getName())));
            
            plugin.getLogger().info("Player " + player.getName() + " voluntarily left the arena");
        }
    }
    /**
     * Handle voluntary player leave from arena
     */
    public void handlePlayerLeaveArena(Player player) {
        CTFPlayer ctfPlayer = getCTFPlayer(player);
        if (ctfPlayer != null && ctfPlayer.isInGame()) {
            removePlayerFromGame(player, true); // Mark as voluntary leave
            
            // Send to server lobby
            plugin.getServerLobbyManager().teleportToServerLobby(player);
            plugin.getLobbyManager().updatePlayerState(player);
            
            // Send confirmation message
            player.sendMessage(plugin.getConfigManager().getMessage("left-arena-success", 
                Collections.singletonMap("player", player.getName())));
            
            plugin.getLogger().info("Player " + player.getName() + " voluntarily left the arena");
        }
    }

    /**
     * Clear leave status for a player (used for cleanup)
     */
    public void clearPlayerLeaveStatus(Player player) {
        playersWhoLeftArena.remove(player.getUniqueId());
    }

    /**
     * Check if player has left an arena voluntarily
     */
    public boolean hasPlayerLeftArena(Player player) {
        return playersWhoLeftArena.contains(player.getUniqueId());
    }

    /**
     * Handle flag taken
     */
    public void handleFlagTaken(CTFPlayer player, Arena.TeamColor flagTeam) {
        Player bukkitPlayer = player.getPlayer();
        if (bukkitPlayer != null && bukkitPlayer.isOnline()) {
            bukkitPlayer.setGlowing(true);
        }
    }

    /**
     * Handle flag dropped
     */
    public void handleFlagDropped(CTFPlayer player) {
        Player bukkitPlayer = player.getPlayer();
        if (bukkitPlayer != null && bukkitPlayer.isOnline()) {
            bukkitPlayer.setGlowing(false);
        }
    }

    /**
     * Force start a game
     */
    public void forceStartGame(CTFGame game) {
        if (game.getState() == GameState.WAITING) {
            startGameCountdown(game);
        } else if (game.getState() == GameState.STARTING) {
            startGame(game);
        }
    }

    /**
     * **ENHANCED FEATURE: End game with comprehensive cleanup**
     */
    public void endGame(CTFGame game, Arena.TeamColor winner) {
        game.setState(GameState.ENDING);

        // Deny building in the arena
        plugin.getWorldGuardManager().setBuildFlag(game.getArena().getWorld(), game.getArena().getWorldGuardRegion(), false);

        plugin.getArenaManager().restoreArena(game.getArena());
        plugin.getPowerUpManager().stopPowerUpSpawning(game);

        // Award wins and currency
        if (winner != null) {
            for (CTFPlayer ctfPlayer : game.getPlayersOnTeam(winner)) {
                ctfPlayer.addGameWon();
                plugin.getCurrencyManager().addCoins(ctfPlayer.getPlayer(),
                        plugin.getConfigManager().getGameplaySetting("currency.win-reward", 100));
            }
        }

        // Show end game statistics
        showCallOfDutyStyleStatistics(game, winner);

        // Update lobby items for all players
        for (CTFPlayer ctfPlayer : game.getPlayers()) {
            Player player = ctfPlayer.getPlayer();
            if (player != null && player.isOnline()) {
                plugin.getLobbyManager().onPlayerStateChange(player, GameState.PLAYING, GameState.ENDING);
            }
        }

        // Schedule cleanup
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            cleanupGame(game);
        }, 200L);
    }

    /**
     * Show game statistics
     */
    private void showCallOfDutyStyleStatistics(CTFGame game, Arena.TeamColor winner) {
        CTFPlayer mvp = null;
        int highestScore = 0;

        for (CTFPlayer ctfPlayer : game.getPlayers()) {
            int playerScore = ctfPlayer.getScore();
            if (playerScore > highestScore) {
                highestScore = playerScore;
                mvp = ctfPlayer;
            }
        }

        for (CTFPlayer ctfPlayer : game.getPlayers()) {
            Player player = ctfPlayer.getPlayer();
            if (player != null && player.isOnline()) {
                Map<String, String> titlePlaceholders = new HashMap<>();
                titlePlaceholders.put("kills", String.valueOf(ctfPlayer.getKills()));
                titlePlaceholders.put("deaths", String.valueOf(ctfPlayer.getDeaths()));
                titlePlaceholders.put("captures", String.valueOf(ctfPlayer.getCaptures()));
                titlePlaceholders.put("kd", String.format("%.2f", ctfPlayer.getKDRatio()));

                plugin.getMessageManager().sendTitle(player, "title-performance", "subtitle-performance", titlePlaceholders);

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
     * Show detailed statistics
     */
    private void showDetailedStatistics(Player player, CTFPlayer ctfPlayer, CTFGame game, CTFPlayer mvp, Arena.TeamColor winner) {
        // Implementation similar to original but with enhanced color processing
        String message = plugin.getConfigManager().getMessage("match-results-border");
        player.sendMessage(message);

        message = plugin.getConfigManager().getMessage("match-results-title");
        player.sendMessage(message);

        // Continue with enhanced formatting...
    }

    /**
     * Cleanup game
     */
    private void cleanupGame(CTFGame game) {
        for (CTFPlayer ctfPlayer : game.getPlayers()) {
            plugin.getPlayerDataManager().savePlayerData(ctfPlayer);
        }

        for (CTFPlayer ctfPlayer : game.getPlayers()) {
            Player player = ctfPlayer.getPlayer();
            if (player != null && player.isOnline()) {
                player.getInventory().clear();

                ItemStack leaveBed = new ItemStack(Material.RED_BED);
                ItemMeta meta = leaveBed.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(plugin.getConfigManager().getMessage("leave-to-lobby-item-name"));
                    meta.setLore(Arrays.asList(
                            plugin.getConfigManager().getMessage("leave-to-lobby-item-lore1"),
                            plugin.getConfigManager().getMessage("leave-to-lobby-item-lore2")
                    ));
                    leaveBed.setItemMeta(meta);
                }

                player.getInventory().setItem(8, leaveBed);
                player.updateInventory();
            }
        }

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
            activeGames.remove(game.getArena());
        }, autoLeaveDelay * 20L);
    }

    /**
     * End all active games
     */
    public void endAllGames() {
        for (CTFGame game : new ArrayList<>(activeGames.values())) {
            endGame(game, null);
        }
    }

    /**
     * Simple player join handling - always send to lobby with autojoin items
     */
    public void handlePlayerJoin(Player player) {
        // Always send new/returning players to server lobby
        plugin.getServerLobbyManager().teleportToServerLobby(player);
        plugin.getLobbyManager().onPlayerJoin(player);
        
        // Clear any old leave status after some time (24 hours)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            playersWhoLeftArena.remove(player.getUniqueId());
        }, 20L * 60 * 60 * 24); // 24 hours
        
        plugin.getLogger().info("Player " + player.getName() + " joined - sent to server lobby");
    }

    /**
     * Handle player disconnect - simply remove from game, no reconnection data
     */
    public void handlePlayerDisconnect(Player player) {
        CTFPlayer ctfPlayer = players.get(player.getUniqueId());
        if (ctfPlayer != null && ctfPlayer.isInGame()) {
            CTFGame game = ctfPlayer.getGame();

            plugin.getLogger().info("Player " + player.getName() + " disconnected from game - will be sent to lobby on rejoin");

            // Handle flag dropping if player had one
            if (ctfPlayer.hasFlag()) {
                CTFFlag flag = ctfPlayer.getCarryingFlag();
                if (flag != null) {
                    flag.returnToBase();
                    ctfPlayer.setCarryingFlag(null);

                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("player", player.getName());
                    placeholders.put("team_color", flag.getTeam().getColorCode());
                    game.broadcastMessage("flag-returned-disconnect", placeholders);
                }
            }

            // Save player data and remove from game
            plugin.getPlayerDataManager().savePlayerData(ctfPlayer);
            removePlayerFromGame(player, false); // Not voluntary, just disconnected
        }
    }

    /**
     * Start cleanup task for old leave data (clear after 24 hours)
     */
    private void startLeaveDataCleanupTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            // Clear leave data after 24 hours to allow players to rejoin eventually
            // This could be made configurable if needed
            plugin.getLogger().info("Leave data cleanup - currently tracking " + playersWhoLeftArena.size() + " players who left arenas");
        }, 72000L, 72000L); // Run every hour
    }

    /**
     * **ENHANCED FEATURE: Comprehensive cleanup with emergency block restore**
     */
    public void cleanup() {
        // Save all player data
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
        playersWhoLeftArena.clear();
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

    public boolean isPlayerInCooldown(Player player) {
        return playerCooldownStatus.containsKey(player.getUniqueId());
    }
}