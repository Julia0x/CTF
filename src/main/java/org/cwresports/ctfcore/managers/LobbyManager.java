package org.cwresports.ctfcore.managers;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.CTFGame;
import org.cwresports.ctfcore.models.CTFPlayer;
import org.cwresports.ctfcore.models.GameState;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced lobby manager with improved item management and state tracking
 * Handles lobby items for both in-game and server lobby with proper cleanup
 */
public class LobbyManager {
    
    private final CTFCore plugin;
    private final Map<UUID, LobbyState> playerStates;
    
    // Hotbar slot assignments
    private static final int LEAVE_SLOT = 8;
    
    public enum LobbyState {
        IN_GAME_LOBBY,  // Player is in game lobby (waiting/starting)
        IN_GAME_PLAYING, // Player is in game and playing
        SERVER_LOBBY,   // Player is in server lobby (not in any game)
        RECONNECTING    // Player is reconnecting
    }
    
    public LobbyManager(CTFCore plugin) {
        this.plugin = plugin;
        this.playerStates = new ConcurrentHashMap<>();
        
        // Start periodic cleanup task
        startCleanupTask();
    }
    
    /**
     * Give lobby items to a player based on their current state
     */
    public void giveLobbyItems(Player player) {
        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(player);
        LobbyState state = determineLobbyState(ctfPlayer);
        
        // Update player state
        playerStates.put(player.getUniqueId(), state);
        
        // Clear inventory first
        player.getInventory().clear();
        
        switch (state) {
            case IN_GAME_LOBBY:
                giveInGameLobbyItems(player);
                break;
            case SERVER_LOBBY:
                giveServerLobbyItems(player);
                break;
            case IN_GAME_PLAYING:
                // Don't give lobby items during gameplay
                break;
            case RECONNECTING:
                // Handle reconnection items separately
                handleReconnectionItems(player, ctfPlayer);
                break;
        }
        
        player.updateInventory();
    }
    
    /**
     * Give in-game lobby items (waiting for game to start)
     */
    private void giveInGameLobbyItems(Player player) {
        // Leave game item
        ItemStack leaveItem = new ItemStack(Material.RED_BED);
        ItemMeta leaveMeta = leaveItem.getItemMeta();
        if (leaveMeta != null) {
            leaveMeta.setDisplayName("§c§lLeave Game");
            leaveMeta.setLore(Arrays.asList(
                "§7Right-click to leave the game",
                "§7Return to server lobby"
            ));
            leaveItem.setItemMeta(leaveMeta);
        }
        
        // Set items in hotbar
        player.getInventory().setItem(LEAVE_SLOT, leaveItem);
    }
    
    /**
     * Give server lobby items (not in any game)
     */
    private void giveServerLobbyItems(Player player) {
        // Could add server lobby specific items here
        // For now, we'll clear the inventory and let the player choose games
        player.getInventory().clear();
    }
    
    /**
     * Handle reconnection items based on game state
     */
    private void handleReconnectionItems(Player player, CTFPlayer ctfPlayer) {
        if (ctfPlayer == null || !ctfPlayer.isInGame()) {
            giveServerLobbyItems(player);
            return;
        }
        
        CTFGame game = ctfPlayer.getGame();
        if (game == null) {
            giveServerLobbyItems(player);
            return;
        }
        
        // Give items based on game state
        if (game.getState() == GameState.PLAYING) {
            // Player is reconnecting to an active game
            // GameManager will handle giving proper game items
        } else {
            // Player is reconnecting to game lobby
            giveInGameLobbyItems(player);
        }
    }
    
    /**
     * Determine lobby state for a player
     */
    private LobbyState determineLobbyState(CTFPlayer ctfPlayer) {
        if (ctfPlayer == null || !ctfPlayer.isInGame()) {
            return LobbyState.SERVER_LOBBY;
        }
        
        CTFGame game = ctfPlayer.getGame();
        if (game == null) {
            return LobbyState.SERVER_LOBBY;
        }
        
        switch (game.getState()) {
            case WAITING:
            case STARTING:
                return LobbyState.IN_GAME_LOBBY;
            case PLAYING:
                return LobbyState.IN_GAME_PLAYING;
            case ENDING:
                return LobbyState.IN_GAME_LOBBY;
            default:
                return LobbyState.SERVER_LOBBY;
        }
    }
    
    /**
     * Handle hotbar item interaction with improved state validation
     */
    public boolean handleHotbarClick(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }
        
        String displayName = item.getItemMeta().getDisplayName();
        
        switch (displayName) {
            case "§c§lLeave Game":
            case "§c§lLeave to Lobby":
                handleLeaveGame(player);
                return true;
        }
        
        return false;
    }
    
    /**
     * Handle leaving game with proper cleanup
     */
    private void handleLeaveGame(Player player) {
        // Remove player from current game
        plugin.getGameManager().removePlayerFromGame(player);
        
        // Update player state
        playerStates.put(player.getUniqueId(), LobbyState.SERVER_LOBBY);
        
        // Clear inventory and give server lobby items
        giveServerLobbyItems(player);
        
        // Teleport to server lobby if configured
        if (plugin.getServerLobbyManager().isServerLobbyConfigured()) {
            plugin.getServerLobbyManager().teleportToServerLobby(player);
        }
    }
    
    /**
     * Clean up lobby items when player changes state
     */
    public void cleanupLobbyItems(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        // Remove lobby items from inventory
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                String displayName = item.getItemMeta().getDisplayName();
                if (displayName.equals("§c§lLeave Game") || displayName.equals("§c§lLeave to Lobby")) {
                    player.getInventory().setItem(i, null);
                }
            }
        }
        
        player.updateInventory();
    }
    
    /**
     * Update player's lobby state and items
     */
    public void updatePlayerState(Player player) {
        // Schedule on next tick to avoid timing issues
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            giveLobbyItems(player);
        });
    }
    
    /**
     * Handle player state change (called when player joins/leaves games)
     */
    public void onPlayerStateChange(Player player, GameState oldState, GameState newState) {
        // Clean up old items
        cleanupLobbyItems(player);
        
        // Give new items after a small delay to ensure state is updated
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            giveLobbyItems(player);
        }, 5L); // 5 tick delay
    }
    
    /**
     * Handle player quit - cleanup state
     */
    public void onPlayerQuit(Player player) {
        playerStates.remove(player.getUniqueId());
    }
    
    /**
     * Handle player reconnection
     */
    public void onPlayerReconnect(Player player) {
        // Mark as reconnecting temporarily
        playerStates.put(player.getUniqueId(), LobbyState.RECONNECTING);
        
        // Update items after reconnection logic is complete
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            updatePlayerState(player);
        }, 10L); // 10 tick delay to allow GameManager to process reconnection
    }
    
    /**
     * Get current lobby state for a player
     */
    public LobbyState getPlayerState(Player player) {
        return playerStates.getOrDefault(player.getUniqueId(), LobbyState.SERVER_LOBBY);
    }
    
    /**
     * Force update all players' lobby states
     */
    public void refreshAllPlayers() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            updatePlayerState(player);
        }
    }
    
    /**
     * Start periodic cleanup task to fix any inconsistencies
     */
    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Clean up disconnected players
                playerStates.entrySet().removeIf(entry -> {
                    Player player = plugin.getServer().getPlayer(entry.getKey());
                    return player == null || !player.isOnline();
                });
                
                // Validate and fix player states
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    LobbyState currentState = playerStates.get(player.getUniqueId());
                    CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(player);
                    LobbyState expectedState = determineLobbyState(ctfPlayer);
                    
                    // If state doesn't match expected, update it
                    if (currentState != expectedState) {
                        playerStates.put(player.getUniqueId(), expectedState);
                        
                        // Only update items if the change is significant
                        if (shouldUpdateItems(currentState, expectedState)) {
                            updatePlayerState(player);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 100L); // Run every 5 seconds
    }
    
    /**
     * Check if items should be updated based on state change
     */
    private boolean shouldUpdateItems(LobbyState oldState, LobbyState newState) {
        if (oldState == null || newState == null) {
            return true;
        }
        
        // Update items when transitioning between different functional states
        switch (oldState) {
            case IN_GAME_LOBBY:
                return newState == LobbyState.SERVER_LOBBY || newState == LobbyState.IN_GAME_PLAYING;
            case IN_GAME_PLAYING:
                return newState != LobbyState.IN_GAME_PLAYING;
            case SERVER_LOBBY:
                return newState != LobbyState.SERVER_LOBBY;
            case RECONNECTING:
                return true; // Always update after reconnection
            default:
                return true;
        }
    }
    
    /**
     * Shutdown cleanup
     */
    public void shutdown() {
        playerStates.clear();
    }
}