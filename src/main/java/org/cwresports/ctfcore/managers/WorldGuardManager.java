package org.cwresports.ctfcore.managers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Manages WorldGuard integration for arena boundaries and region validation
 * Updated to use modern WorldGuard API without deprecated methods
 */
public class WorldGuardManager {

    private final WorldGuard worldGuard;
    private final WorldGuardPlugin worldGuardPlugin;

    public WorldGuardManager() {
        this.worldGuard = WorldGuard.getInstance();
        this.worldGuardPlugin = WorldGuardPlugin.inst();
    }

    /**
     * Check if a WorldGuard region exists in the specified world - ENHANCED ERROR HANDLING
     */
    public boolean regionExists(World world, String regionName) {
        if (world == null || regionName == null || regionName.trim().isEmpty()) {
            return false;
        }

        try {
            RegionManager regionManager = worldGuard.getPlatform().getRegionContainer()
                    .get(BukkitAdapter.adapt(world));

            if (regionManager == null) {
                // Log warning for debugging
                System.out.println("[CTF-Core] Warning: RegionManager is null for world: " + world.getName());
                return false;
            }

            return regionManager.hasRegion(regionName.toLowerCase());
        } catch (Exception e) {
            // Enhanced error logging
            System.out.println("[CTF-Core] Error checking region '" + regionName + "' in world '" + world.getName() + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if a location is within a specific WorldGuard region - ENHANCED ERROR HANDLING
     */
    public boolean isLocationInRegion(Location location, String regionName) {
        if (location == null || regionName == null || regionName.trim().isEmpty()) {
            return false;
        }

        try {
            World world = location.getWorld();
            if (world == null) {
                System.out.println("[CTF-Core] Warning: Location has null world");
                return false;
            }

            RegionManager regionManager = worldGuard.getPlatform().getRegionContainer()
                    .get(BukkitAdapter.adapt(world));

            if (regionManager == null) {
                System.out.println("[CTF-Core] Warning: RegionManager is null for world: " + world.getName());
                return false;
            }

            ProtectedRegion region = regionManager.getRegion(regionName.toLowerCase());
            if (region == null) {
                return false;
            }

            return region.contains(BukkitAdapter.asBlockVector(location));
        } catch (Exception e) {
            System.out.println("[CTF-Core] Error checking if location is in region '" + regionName + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if a player is within a specific WorldGuard region
     */
    public boolean isPlayerInRegion(Player player, String regionName) {
        return isLocationInRegion(player.getLocation(), regionName);
    }

    /**
     * Get the WorldGuard region at a specific location
     */
    public ProtectedRegion getRegionAt(Location location) {
        try {
            World world = location.getWorld();
            if (world == null) {
                return null;
            }

            RegionManager regionManager = worldGuard.getPlatform().getRegionContainer()
                    .get(BukkitAdapter.adapt(world));

            if (regionManager == null) {
                return null;
            }

            // Get the highest priority region at this location
            var regions = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(location));

            ProtectedRegion highestPriorityRegion = null;
            int highestPriority = Integer.MIN_VALUE;

            for (ProtectedRegion region : regions) {
                if (region.getPriority() > highestPriority) {
                    highestPriority = region.getPriority();
                    highestPriorityRegion = region;
                }
            }

            return highestPriorityRegion;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if WorldGuard is available and functioning
     */
    public boolean isWorldGuardAvailable() {
        return worldGuard != null && worldGuardPlugin != null;
    }

    /**
     * Get a safe location within a region (for teleporting players back)
     */
    public Location getSafeLocationInRegion(World world, String regionName) {
        try {
            RegionManager regionManager = worldGuard.getPlatform().getRegionContainer()
                    .get(BukkitAdapter.adapt(world));

            if (regionManager == null) {
                return null;
            }

            ProtectedRegion region = regionManager.getRegion(regionName);
            if (region == null) {
                return null;
            }

            // Get the minimum point of the region and add some height for safety
            var minPoint = region.getMinimumPoint();
            Location safeLocation = new Location(world,
                    minPoint.x() + 1,  // Use x() instead of getX()
                    minPoint.y() + 2,  // Use y() instead of getY()
                    minPoint.z() + 1); // Use z() instead of getZ()

            // Make sure the location is safe (not in a block)
            if (safeLocation.getBlock().getType().isSolid()) {
                safeLocation.setY(world.getHighestBlockYAt(safeLocation) + 1);
            }

            return safeLocation;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Calculate if a player is near the edge of a region (for boundary warnings)
     */
    public boolean isNearRegionBoundary(Player player, String regionName, double distance) {
        try {
            World world = player.getWorld();
            RegionManager regionManager = worldGuard.getPlatform().getRegionContainer()
                    .get(BukkitAdapter.adapt(world));

            if (regionManager == null) {
                return false;
            }

            ProtectedRegion region = regionManager.getRegion(regionName);
            if (region == null) {
                return false;
            }

            Location playerLoc = player.getLocation();

            // Check distance to each boundary using modern API
            var minPoint = region.getMinimumPoint();
            var maxPoint = region.getMaximumPoint();

            double distanceToMinX = Math.abs(playerLoc.getX() - minPoint.x()); // Use x() instead of getX()
            double distanceToMaxX = Math.abs(playerLoc.getX() - maxPoint.x()); // Use x() instead of getX()
            double distanceToMinZ = Math.abs(playerLoc.getZ() - minPoint.z()); // Use z() instead of getZ()
            double distanceToMaxZ = Math.abs(playerLoc.getZ() - maxPoint.z()); // Use z() instead of getZ()

            double minDistance = Math.min(Math.min(distanceToMinX, distanceToMaxX),
                    Math.min(distanceToMinZ, distanceToMaxZ));

            return minDistance <= distance;
        } catch (Exception e) {
            return false;
        }
    }
}