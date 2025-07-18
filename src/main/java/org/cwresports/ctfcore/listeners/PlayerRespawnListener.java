package org.cwresports.ctfcore.listeners;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Enhanced player respawn listener with immediate arena teleportation
 * Handles smart spawn selection and bypasses other plugin interference
 */
public class PlayerRespawnListener implements Listener {
    
    private final CTFCore plugin;
    private final Random random;
    
    public PlayerRespawnListener(CTFCore plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }
    
    @EventHandler(priority = EventPriority.HIGHEST) // Highest priority to override other plugins
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(player);
        
        if (ctfPlayer == null || !ctfPlayer.isInGame()) {
            return;
        }
        
        CTFGame game = ctfPlayer.getGame();
        if (game == null) {
            return;
        }
        
        Arena arena = game.getArena();
        
        // **ENHANCED FEATURE: Immediate arena teleportation**
        // Always set respawn location to arena to prevent main world respawning
        if (game.getState() == GameState.PLAYING && ctfPlayer.getTeam() != null) {
            // Find best spawn point for the player's team
            Arena.TeamColor team = ctfPlayer.getTeam();
            Arena.Team teamData = arena.getTeam(team);
            Location bestSpawn = findBestSpawnPoint(teamData, game);
            
            if (bestSpawn != null) {
                event.setRespawnLocation(bestSpawn);
                plugin.getLogger().info("Set respawn location for " + player.getName() + " to team spawn in arena");
            } else {
                // Fallback to lobby if no spawns available
                event.setRespawnLocation(arena.getLobbySpawn());
                plugin.getLogger().warning("No team spawn available for " + player.getName() + ", using lobby spawn");
            }
        } else {
            // Game not playing or no team, respawn at lobby
            event.setRespawnLocation(arena.getLobbySpawn());
        }
        
        // **ENHANCED FEATURE: Immediate post-respawn setup**
        // Schedule immediate setup on next tick to ensure respawn completes first
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                
                // Ensure player is in the right game mode
                if (player.getGameMode() != GameMode.SURVIVAL) {
                    player.setGameMode(GameMode.SURVIVAL);
                }
                
                // If player was in respawn countdown, handle it properly
                if (!ctfPlayer.isAlive()) {
                    // Mark player as alive again
                    ctfPlayer.respawn();
                    
                    // Apply loadout and effects immediately
                    plugin.getGameManager().applyBasicLoadoutToPlayer(player);
                    
                    // Apply team colored armor
                    if (ctfPlayer.getTeam() != null) {
                        applyTeamColoredArmor(player, ctfPlayer.getTeam());
                        
                        // Apply team kill enhancements
                        if (game.getState() == GameState.PLAYING) {
                            plugin.getGameManager().applyTeamKillEnhancements(player, game, ctfPlayer.getTeam());
                        }
                    }
                    
                    // Apply spawn protection
                    plugin.getGameManager().applySpawnProtection(player);
                    
                    plugin.getLogger().info("Applied immediate post-respawn setup for " + player.getName());
                } else {
                    // Normal respawn, just apply protection
                    plugin.getGameManager().applySpawnProtection(player);
                }
            }
        }.runTask(plugin);
    }
    
    /**
     * Find the best spawn point that's not occupied by other players
     */
    private Location findBestSpawnPoint(Arena.Team teamData, CTFGame game) {
        Location[] spawnPoints = teamData.getSpawnPoints();
        List<Location> availableSpawns = new ArrayList<>();
        
        // First, collect all valid spawn points
        for (Location spawn : spawnPoints) {
            if (spawn != null) {
                availableSpawns.add(spawn);
            }
        }
        
        if (availableSpawns.isEmpty()) {
            return null;
        }
        
        // Find spawns that aren't occupied by other players
        List<Location> unoccupiedSpawns = new ArrayList<>();
        
        for (Location spawn : availableSpawns) {
            boolean isOccupied = false;
            
            // Check if any other player is near this spawn point
            for (CTFPlayer otherPlayer : game.getPlayers()) {
                Player other = otherPlayer.getPlayer();
                if (other != null && other.isOnline() && other.getWorld().equals(spawn.getWorld())) {
                    double distance = other.getLocation().distance(spawn);
                    if (distance <= 3.0) { // Consider occupied if within 3 blocks
                        isOccupied = true;
                        break;
                    }
                }
            }
            
            if (!isOccupied) {
                unoccupiedSpawns.add(spawn);
            }
        }
        
        // If we have unoccupied spawns, use one randomly
        if (!unoccupiedSpawns.isEmpty()) {
            return unoccupiedSpawns.get(random.nextInt(unoccupiedSpawns.size()));
        }
        
        // If all spawns are occupied, pick the one with the least nearby players
        return findLeastOccupiedSpawn(availableSpawns, game);
    }
    
    /**
     * Find the spawn point with the least nearby players
     */
    private Location findLeastOccupiedSpawn(List<Location> spawns, CTFGame game) {
        Location bestSpawn = null;
        int leastPlayers = Integer.MAX_VALUE;
        
        for (Location spawn : spawns) {
            int nearbyPlayers = 0;
            
            for (CTFPlayer otherPlayer : game.getPlayers()) {
                Player other = otherPlayer.getPlayer();
                if (other != null && other.isOnline() && other.getWorld().equals(spawn.getWorld())) {
                    double distance = other.getLocation().distance(spawn);
                    if (distance <= 5.0) { // Count players within 5 blocks
                        nearbyPlayers++;
                    }
                }
            }
            
            if (nearbyPlayers < leastPlayers) {
                leastPlayers = nearbyPlayers;
                bestSpawn = spawn;
            }
        }
        
        return bestSpawn != null ? bestSpawn : spawns.get(random.nextInt(spawns.size()));
    }
    
    /**
     * Apply team colored armor to player
     */
    private void applyTeamColoredArmor(Player player, Arena.TeamColor teamColor) {
        org.bukkit.Color armorColor;
        if (teamColor == Arena.TeamColor.RED) {
            armorColor = org.bukkit.Color.RED;
        } else {
            armorColor = org.bukkit.Color.BLUE;
        }

        // Create dyed leather armor
        org.bukkit.inventory.ItemStack helmet = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_HELMET);
        org.bukkit.inventory.ItemStack chestplate = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_CHESTPLATE);
        org.bukkit.inventory.ItemStack leggings = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_LEGGINGS);
        org.bukkit.inventory.ItemStack boots = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_BOOTS);

        // Dye the armor
        dyeLeatherArmor(helmet, armorColor);
        dyeLeatherArmor(chestplate, armorColor);
        dyeLeatherArmor(leggings, armorColor);
        dyeLeatherArmor(boots, armorColor);

        // Set armor with slight delay to ensure inventory is ready
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.getInventory().setHelmet(helmet);
                    player.getInventory().setChestplate(chestplate);
                    player.getInventory().setLeggings(leggings);
                    player.getInventory().setBoots(boots);
                    player.updateInventory();
                }
            }
        }.runTaskLater(plugin, 2L); // 2 tick delay
    }
    
    /**
     * Dye leather armor with specified color
     */
    private void dyeLeatherArmor(org.bukkit.inventory.ItemStack armor, org.bukkit.Color color) {
        if (armor.getItemMeta() instanceof org.bukkit.inventory.meta.LeatherArmorMeta meta) {
            meta.setColor(color);
            armor.setItemMeta(meta);
        }
    }
}