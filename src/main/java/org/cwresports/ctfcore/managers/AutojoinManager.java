package org.cwresports.ctfcore.managers;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.Arena;
import org.cwresports.ctfcore.models.CTFGame;
import org.cwresports.ctfcore.models.GameState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages automatic game joining functionality
 * Prioritizes games with most players waiting, fallback to random if all empty
 */
public class AutojoinManager {

    private final CTFCore plugin;
    private final Map<UUID, Long> lastAutojoinAttempt;
    private final long AUTOJOIN_COOLDOWN = 3000; // 3 seconds cooldown

    public AutojoinManager(CTFCore plugin) {
        this.plugin = plugin;
        this.lastAutojoinAttempt = new ConcurrentHashMap<>();
        
        // Start periodic cleanup of old cooldown entries
        startCleanupTask();
    }

    /**
     * Create autojoin item for server lobby
     */
    public ItemStack createAutojoinItem() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String itemName = plugin.getConfigManager().getMainConfig().getString("autojoin.item-name", "&a&lAuto Join Game");
            List<String> itemLore = plugin.getConfigManager().getMainConfig().getStringList("autojoin.item-lore");
            
            if (itemLore.isEmpty()) {
                itemLore = Arrays.asList(
                    "&7Click to automatically join",
                    "&7a game with other players!",
                    "",
                    "&ePrefers games with more players"
                );
            }
            
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', itemName));
            List<String> coloredLore = new ArrayList<>();
            for (String line : itemLore) {
                coloredLore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * Handle autojoin attempt by player
     */
    public void handleAutojoin(Player player) {
        // Check if autojoin is enabled
        if (!plugin.getConfigManager().getMainConfig().getBoolean("autojoin.enabled", true)) {
            player.sendMessage(plugin.getConfigManager().getMessage("autojoin-disabled"));
            return;
        }

        // Check if player is already in a game
        if (plugin.getGameManager().getCTFPlayer(player) != null) {
            player.sendMessage(plugin.getConfigManager().getMessage("already-in-game"));
            return;
        }

        // Check cooldown
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        if (lastAutojoinAttempt.containsKey(playerId)) {
            long lastAttempt = lastAutojoinAttempt.get(playerId);
            if (currentTime - lastAttempt < AUTOJOIN_COOLDOWN) {
                long remainingCooldown = (AUTOJOIN_COOLDOWN - (currentTime - lastAttempt)) / 1000;
                player.sendMessage(plugin.getConfigManager().getMessage("autojoin-cooldown", 
                    Map.of("seconds", String.valueOf(remainingCooldown))));
                return;
            }
        }

        // Update cooldown
        lastAutojoinAttempt.put(playerId, currentTime);

        // Start autojoin process
        player.sendMessage(plugin.getConfigManager().getMessage("autojoin-searching"));
        
        // Play searching sound
        player.playSound(player.getLocation(), 
            plugin.getConfigManager().getSound("autojoin_searching"), 1.0f, 1.0f);

        // Run autojoin logic asynchronously to avoid blocking
        new BukkitRunnable() {
            @Override
            public void run() {
                Arena targetArena = findBestArena(player);
                
                // Switch back to main thread for game operations
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (targetArena != null) {
                        joinArena(player, targetArena);
                    } else {
                        handleNoGamesAvailable(player);
                    }
                });
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Find the best arena to join based on player count and preferences
     */
    private Arena findBestArena(Player player) {
        List<Arena> availableArenas = new ArrayList<>();
        
        // Get all enabled arenas
        for (Arena arena : plugin.getArenaManager().getEnabledArenas()) {
            if (arena.isEnabled()) {
                CTFGame game = plugin.getGameManager().getGame(arena);
                
                // Check if arena can accept players
                if (game == null || canJoinGame(game)) {
                    availableArenas.add(arena);
                }
            }
        }

        if (availableArenas.isEmpty()) {
            return null;
        }

        // Sort arenas by preference
        availableArenas.sort((arena1, arena2) -> {
            int players1 = getWaitingPlayerCount(arena1);
            int players2 = getWaitingPlayerCount(arena2);
            
            // Prefer arenas with more waiting players
            if (players1 != players2) {
                return Integer.compare(players2, players1); // Descending order
            }
            
            // If same player count, prefer arenas that meet minimum threshold
            int minThreshold = plugin.getConfigManager().getMainConfig().getInt("autojoin.min-players-threshold", 2);
            boolean arena1MeetsThreshold = players1 >= minThreshold;
            boolean arena2MeetsThreshold = players2 >= minThreshold;
            
            if (arena1MeetsThreshold != arena2MeetsThreshold) {
                return arena1MeetsThreshold ? -1 : 1;
            }
            
            // Random selection if all else equal
            return 0;
        });

        return availableArenas.get(0);
    }

    /**
     * Check if player can join a game
     */
    private boolean canJoinGame(CTFGame game) {
        if (game == null) {
            return true; // No game running, can start new one
        }

        return (game.getState() == GameState.WAITING || game.getState() == GameState.STARTING) 
               && game.getPlayers().size() < game.getArena().getMaxPlayers();
    }

    /**
     * Get count of players waiting in arena
     */
    private int getWaitingPlayerCount(Arena arena) {
        CTFGame game = plugin.getGameManager().getGame(arena);
        if (game == null) {
            return 0;
        }

        if (game.getState() == GameState.WAITING || game.getState() == GameState.STARTING) {
            return game.getPlayers().size();
        }

        return 0;
    }

    /**
     * Join player to selected arena
     */
    private void joinArena(Player player, Arena arena) {
        CTFGame game = plugin.getGameManager().getGame(arena.getName());
        int waitingPlayers = getWaitingPlayerCount(arena);

        try {
            // Attempt to join the game
            boolean success = plugin.getGameManager().addPlayerToGame(player, arena);
            
            if (success) {
                // Send appropriate message based on game state
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("arena", arena.getName());
                placeholders.put("player_count", String.valueOf(waitingPlayers));

                if (waitingPlayers > 0) {
                    player.sendMessage(plugin.getConfigManager().getMessage("autojoin-joined-populated", placeholders));
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("autojoin-joined-random", placeholders));
                }

                // Play success sound
                player.playSound(player.getLocation(), 
                    plugin.getConfigManager().getSound("autojoin_found"), 1.0f, 1.0f);

                plugin.getLogger().info("Player " + player.getName() + " auto-joined arena " + arena.getName() + 
                    " (had " + waitingPlayers + " waiting players)");
            } else {
                handleJoinFailed(player, arena);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to auto-join player " + player.getName() + " to arena " + arena.getName() + ": " + e.getMessage());
            handleJoinFailed(player, arena);
        }
    }

    /**
     * Handle failed join attempt
     */
    private void handleJoinFailed(Player player, Arena arena) {
        player.sendMessage(plugin.getConfigManager().getMessage("autojoin-failed", 
            Map.of("arena", arena.getName())));
        
        // Try to find another arena
        new BukkitRunnable() {
            @Override
            public void run() {
                Arena nextArena = findBestArena(player);
                if (nextArena != null && !nextArena.getName().equals(arena.getName())) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        joinArena(player, nextArena);
                    });
                } else {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        handleNoGamesAvailable(player);
                    });
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Handle case when no games are available
     */
    private void handleNoGamesAvailable(Player player) {
        player.sendMessage(plugin.getConfigManager().getMessage("autojoin-no-games"));
    }

    /**
     * Check if item is autojoin item
     */
    public boolean isAutojoinItem(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }

        String expectedName = plugin.getConfigManager().getMainConfig().getString("autojoin.item-name", "&a&lAuto Join Game");
        String actualName = meta.getDisplayName();
        String expectedNameColored = org.bukkit.ChatColor.translateAlternateColorCodes('&', expectedName);

        return expectedNameColored.equals(actualName);
    }

    /**
     * Get autojoin statistics for admin commands
     */
    public Map<String, Object> getAutojoinStats() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalArenas = plugin.getArenaManager().getEnabledArenas().size();
        int waitingGames = 0;
        int totalWaitingPlayers = 0;

        for (Arena arena : plugin.getArenaManager().getEnabledArenas()) {
            if (arena.isEnabled()) {
                CTFGame game = plugin.getGameManager().getGame(arena.getName());
                if (game != null && (game.getState() == GameState.WAITING || game.getState() == GameState.STARTING)) {
                    waitingGames++;
                    totalWaitingPlayers += game.getPlayers().size();
                }
            }
        }

        stats.put("total_arenas", totalArenas);
        stats.put("waiting_games", waitingGames);
        stats.put("total_waiting_players", totalWaitingPlayers);
        stats.put("recent_autojoin_attempts", lastAutojoinAttempt.size());

        return stats;
    }

    /**
     * Start periodic cleanup task
     */
    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                lastAutojoinAttempt.entrySet().removeIf(entry -> 
                    currentTime - entry.getValue() > AUTOJOIN_COOLDOWN * 5); // Clean up after 5x cooldown
            }
        }.runTaskTimer(plugin, 1200L, 1200L); // Run every minute
    }

    /**
     * Shutdown cleanup
     */
    public void shutdown() {
        lastAutojoinAttempt.clear();
    }
}