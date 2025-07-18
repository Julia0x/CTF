package org.cwresports.ctfcore.managers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.Arena;
import org.cwresports.ctfcore.models.CTFGame;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages block tracking and restoration for CTF games
 * Ensures clean arena state after matches by tracking and restoring player-placed blocks
 */
public class BlockTrackingManager {

    private final CTFCore plugin;
    private final Map<String, Set<TrackedBlock>> arenaBlocks; // Arena name -> Set of tracked blocks
    private final Map<String, Set<Location>> playerPlacedBlocks; // Arena name -> Set of player-placed block locations
    private final Map<String, Boolean> arenaPassthroughStates; // Arena name -> Original passthrough state

    // Blocks that should be tracked when placed by players
    private final Set<Material> TRACKABLE_BLOCKS = Set.of(
            Material.COBBLESTONE, Material.STONE, Material.DIRT, Material.SAND,
            Material.SANDSTONE, Material.OAK_WOOD, Material.OAK_PLANKS, Material.WHITE_WOOL,
            Material.GLASS, Material.OBSIDIAN, Material.BRICK, Material.MOSSY_COBBLESTONE,
            Material.NETHERRACK, Material.GLOWSTONE, Material.JACK_O_LANTERN,
            Material.STONE_BRICKS, Material.NETHER_BRICKS, Material.END_STONE,
            Material.EMERALD_BLOCK, Material.DIAMOND_BLOCK, Material.GOLD_BLOCK,
            Material.IRON_BLOCK, Material.REDSTONE_BLOCK, Material.LAPIS_BLOCK,
            Material.COAL_BLOCK, Material.QUARTZ_BLOCK, Material.PRISMARINE,
            Material.SEA_LANTERN, Material.HAY_BLOCK, Material.PACKED_ICE,
            Material.RED_TERRACOTTA, Material.BLUE_TERRACOTTA, Material.WHITE_TERRACOTTA,
            Material.WHITE_CONCRETE, Material.WHITE_GLAZED_TERRACOTTA, Material.SHULKER_BOX,
            Material.LADDER, Material.VINE, Material.TORCH, Material.REDSTONE_TORCH,
            Material.LEVER, Material.STONE_BUTTON, Material.STONE_PRESSURE_PLATE,
            Material.OAK_FENCE, Material.OAK_FENCE_GATE, Material.OAK_DOOR, Material.OAK_TRAPDOOR,
            Material.CHEST, Material.FURNACE, Material.CRAFTING_TABLE, Material.ANVIL,
            Material.ENCHANTING_TABLE, Material.BREWING_STAND, Material.CAULDRON
    );

    public BlockTrackingManager(CTFCore plugin) {
        this.plugin = plugin;
        this.arenaBlocks = new ConcurrentHashMap<>();
        this.playerPlacedBlocks = new ConcurrentHashMap<>();
        this.arenaPassthroughStates = new ConcurrentHashMap<>();
    }

    /**
     * Track a block that was placed by a player during a CTF game
     */
    public void trackPlacedBlock(Player player, Block block, Arena arena) {
        if (!isTrackableBlock(block.getType())) {
            return;
        }

        String arenaName = arena.getName();
        Location blockLocation = block.getLocation();

        // Check if the block is within the arena region
        if (!plugin.getWorldGuardManager().isLocationInRegion(blockLocation, arena.getWorldGuardRegion())) {
            return;
        }

        // Store the original block state before it was placed
        BlockState originalState = block.getState();
        originalState.setType(Material.AIR); // The block was air before placement

        // Track the block
        TrackedBlock trackedBlock = new TrackedBlock(blockLocation, originalState, player.getUniqueId());

        arenaBlocks.computeIfAbsent(arenaName, k -> ConcurrentHashMap.newKeySet()).add(trackedBlock);
        playerPlacedBlocks.computeIfAbsent(arenaName, k -> ConcurrentHashMap.newKeySet()).add(blockLocation);

        plugin.getLogger().info("Tracking block placed by " + player.getName() + " at " +
                locationToString(blockLocation) + " in arena " + arenaName);
    }

    /**
     * Track a block that was broken by a player during a CTF game
     */
    public void trackBrokenBlock(Player player, Block block, Arena arena) {
        String arenaName = arena.getName();
        Location blockLocation = block.getLocation();

        // Check if the block is within the arena region
        if (!plugin.getWorldGuardManager().isLocationInRegion(blockLocation, arena.getWorldGuardRegion())) {
            return;
        }

        // Only track if it's not a player-placed block
        Set<Location> playerPlaced = playerPlacedBlocks.get(arenaName);
        if (playerPlaced != null && playerPlaced.contains(blockLocation)) {
            // This is a player-placed block being broken, remove it from tracking
            playerPlaced.remove(blockLocation);
            arenaBlocks.get(arenaName).removeIf(tb -> tb.getLocation().equals(blockLocation));
            plugin.getLogger().info("Removed player-placed block from tracking: " + locationToString(blockLocation));
            return;
        }

        // Store the original block state before it was broken
        BlockState originalState = block.getState();

        // Track the block that was broken
        TrackedBlock trackedBlock = new TrackedBlock(blockLocation, originalState, player.getUniqueId());

        arenaBlocks.computeIfAbsent(arenaName, k -> ConcurrentHashMap.newKeySet()).add(trackedBlock);

        plugin.getLogger().info("Tracking block broken by " + player.getName() + " at " +
                locationToString(blockLocation) + " in arena " + arenaName);
    }

    /**
     * Start tracking blocks for a CTF game
     */
    public void startTrackingForGame(CTFGame game) {
        String arenaName = game.getArena().getName();

        // Clear any existing tracking data for this arena
        arenaBlocks.remove(arenaName);
        playerPlacedBlocks.remove(arenaName);

        // Enable passthrough for the arena region to allow block placement
        enableArenaPassthrough(game.getArena());

        plugin.getLogger().info("Started block tracking for arena: " + arenaName);
    }

    /**
     * Stop tracking and restore all blocks for a CTF game
     */
    public void stopTrackingAndRestore(CTFGame game) {
        String arenaName = game.getArena().getName();

        // Restore all tracked blocks
        restoreArenaBlocks(arenaName);

        // Restore original passthrough state
        restoreArenaPassthrough(game.getArena());

        // Clear tracking data
        arenaBlocks.remove(arenaName);
        playerPlacedBlocks.remove(arenaName);

        plugin.getLogger().info("Stopped block tracking and restored arena: " + arenaName);
    }

    /**
     * Enable passthrough for arena region during games
     */
    private void enableArenaPassthrough(Arena arena) {
        try {
            String regionName = arena.getWorldGuardRegion();
            if (regionName == null) return;

            // Store original passthrough state
            boolean originalState = plugin.getWorldGuardManager().getRegionPassthroughState(regionName);
            arenaPassthroughStates.put(arena.getName(), originalState);

            // Enable passthrough
            plugin.getWorldGuardManager().setRegionPassthrough(regionName, true);

            plugin.getLogger().info("Enabled passthrough for arena region: " + regionName);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to enable passthrough for arena " + arena.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Restore original passthrough state for arena region
     */
    private void restoreArenaPassthrough(Arena arena) {
        try {
            String regionName = arena.getWorldGuardRegion();
            if (regionName == null) return;

            // Restore original passthrough state
            Boolean originalState = arenaPassthroughStates.get(arena.getName());
            if (originalState != null) {
                plugin.getWorldGuardManager().setRegionPassthrough(regionName, originalState);
                arenaPassthroughStates.remove(arena.getName());

                plugin.getLogger().info("Restored passthrough state for arena region: " + regionName + " to " + originalState);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to restore passthrough for arena " + arena.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Restore all tracked blocks for an arena
     */
    private void restoreArenaBlocks(String arenaName) {
        Set<TrackedBlock> trackedBlocks = arenaBlocks.get(arenaName);
        if (trackedBlocks == null || trackedBlocks.isEmpty()) {
            return;
        }

        int restoredCount = 0;

        for (TrackedBlock trackedBlock : trackedBlocks) {
            try {
                Location location = trackedBlock.getLocation();
                BlockState originalState = trackedBlock.getOriginalState();

                // Restore the block to its original state
                Block block = location.getBlock();

                // If the original state was AIR, remove the block
                if (originalState.getType() == Material.AIR) {
                    block.setType(Material.AIR);
                } else {
                    // Restore the original block
                    block.setType(originalState.getType());
                    block.setBlockData(originalState.getBlockData());
                }

                restoredCount++;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to restore block at " +
                        locationToString(trackedBlock.getLocation()) + ": " + e.getMessage());
            }
        }

        plugin.getLogger().info("Restored " + restoredCount + " blocks for arena: " + arenaName);
    }

    /**
     * Check if a block type should be tracked
     */
    private boolean isTrackableBlock(Material material) {
        return TRACKABLE_BLOCKS.contains(material) ||
                material.name().contains("WOOL") ||
                material.name().contains("CONCRETE") ||
                material.name().contains("TERRACOTTA") ||
                material.name().contains("GLASS") ||
                material.name().contains("STONE") ||
                material.name().contains("BRICK") ||
                material.name().contains("PLANKS") ||
                material.name().contains("LOG") ||
                material.name().contains("WOOD");
    }

    /**
     * Get the number of tracked blocks for an arena
     */
    public int getTrackedBlockCount(String arenaName) {
        Set<TrackedBlock> blocks = arenaBlocks.get(arenaName);
        return blocks != null ? blocks.size() : 0;
    }

    /**
     * Check if block tracking is active for an arena
     */
    public boolean isTrackingActive(String arenaName) {
        return arenaBlocks.containsKey(arenaName);
    }

    /**
     * Emergency cleanup - force restore all arenas
     */
    public void emergencyCleanup() {
        plugin.getLogger().warning("Performing emergency block tracking cleanup...");

        for (String arenaName : new HashSet<>(arenaBlocks.keySet())) {
            try {
                restoreArenaBlocks(arenaName);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to restore blocks for arena " + arenaName + ": " + e.getMessage());
            }
        }

        // Restore all passthrough states
        for (Map.Entry<String, Boolean> entry : arenaPassthroughStates.entrySet()) {
            try {
                Arena arena = plugin.getArenaManager().getArena(entry.getKey());
                if (arena != null) {
                    restoreArenaPassthrough(arena);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to restore passthrough for arena " + entry.getKey() + ": " + e.getMessage());
            }
        }

        // Clear all data
        arenaBlocks.clear();
        playerPlacedBlocks.clear();
        arenaPassthroughStates.clear();

        plugin.getLogger().info("Emergency cleanup completed");
    }

    /**
     * Convert location to string for logging
     */
    private String locationToString(Location location) {
        return String.format("(%d, %d, %d)",
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }

    /**
     * Inner class to represent a tracked block
     */
    private static class TrackedBlock {
        private final Location location;
        private final BlockState originalState;
        private final UUID playerUUID;
        private final long timestamp;

        public TrackedBlock(Location location, BlockState originalState, UUID playerUUID) {
            this.location = location.clone();
            this.originalState = originalState;
            this.playerUUID = playerUUID;
            this.timestamp = System.currentTimeMillis();
        }

        public Location getLocation() {
            return location;
        }

        public BlockState getOriginalState() {
            return originalState;
        }

        public UUID getPlayerUUID() {
            return playerUUID;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TrackedBlock that = (TrackedBlock) obj;
            return location.equals(that.location);
        }

        @Override
        public int hashCode() {
            return location.hashCode();
        }
    }
}