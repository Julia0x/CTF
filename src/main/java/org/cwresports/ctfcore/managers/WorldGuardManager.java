package org.cwresports.ctfcore.managers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Enhanced WorldGuard integration manager with passthrough control
 */
public class WorldGuardManager {
    
    /**
     * Check if a location is within a WorldGuard region
     */
    public boolean isLocationInRegion(Location location, String regionName) {
        if (location == null || regionName == null) {
            return false;
        }
        
        try {
            World world = location.getWorld();
            if (world == null) {
                return false;
            }
            
            RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                    .get(BukkitAdapter.adapt(world));
            
            if (regionManager == null) {
                return false;
            }
            
            ProtectedRegion region = regionManager.getRegion(regionName);
            if (region == null) {
                return false;
            }
            
            return region.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get the current passthrough state of a region
     */
    public boolean getRegionPassthroughState(String regionName) {
        try {
            // Find the region in any world (assuming region names are unique)
            for (World world : org.bukkit.Bukkit.getWorlds()) {
                RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                        .get(BukkitAdapter.adapt(world));
                
                if (regionManager != null) {
                    ProtectedRegion region = regionManager.getRegion(regionName);
                    if (region != null) {
                        StateFlag.State state = region.getFlag(Flags.PASSTHROUGH);
                        if (state == StateFlag.State.ALLOW) {
                            return true;
                        } else if (state == StateFlag.State.DENY) {
                            return false;
                        }
                        // If null, return default (false)
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            // Log error but don't crash
            System.err.println("Error getting passthrough state for region " + regionName + ": " + e.getMessage());
        }
        return false; // Default to false if region not found or error
    }
    
    /**
     * Set the passthrough state of a region
     */
    public void setRegionPassthrough(String regionName, boolean enabled) {
        try {
            // Find the region in any world (assuming region names are unique)
            for (World world : org.bukkit.Bukkit.getWorlds()) {
                RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                        .get(BukkitAdapter.adapt(world));
                
                if (regionManager != null) {
                    ProtectedRegion region = regionManager.getRegion(regionName);
                    if (region != null) {
                        StateFlag.State state = enabled ? StateFlag.State.ALLOW : StateFlag.State.DENY;
                        region.setFlag(Flags.PASSTHROUGH, state);
                        
                        // Save changes
                        try {
                            regionManager.saveChanges();
                        } catch (Exception e) {
                            System.err.println("Error saving region changes: " + e.getMessage());
                        }
                        
                        return; // Found and updated the region
                    }
                }
            }
            
            System.err.println("Region not found: " + regionName);
        } catch (Exception e) {
            System.err.println("Error setting passthrough for region " + regionName + ": " + e.getMessage());
        }
    }
    
    /**
     * Check if a region exists
     */
    public boolean regionExists(String regionName) {
        try {
            for (World world : org.bukkit.Bukkit.getWorlds()) {
                RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                        .get(BukkitAdapter.adapt(world));
                
                if (regionManager != null) {
                    ProtectedRegion region = regionManager.getRegion(regionName);
                    if (region != null) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking region existence: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Get a region by name
     */
    public ProtectedRegion getRegion(String regionName) {
        try {
            for (World world : org.bukkit.Bukkit.getWorlds()) {
                RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                        .get(BukkitAdapter.adapt(world));
                
                if (regionManager != null) {
                    ProtectedRegion region = regionManager.getRegion(regionName);
                    if (region != null) {
                        return region;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting region: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Check if a player can build in a region
     */
    public boolean canBuildInRegion(org.bukkit.entity.Player player, String regionName) {
        try {
            World world = player.getWorld();
            RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                    .get(BukkitAdapter.adapt(world));
            
            if (regionManager == null) {
                return false;
            }
            
            ProtectedRegion region = regionManager.getRegion(regionName);
            if (region == null) {
                return false;
            }
            
            // Check if player has build permission
            return region.isMember(WorldGuard.getInstance().getPlatform().getSessionManager()
                    .get(BukkitAdapter.adapt(player)).getUuid()) || 
                   region.isOwner(WorldGuard.getInstance().getPlatform().getSessionManager()
                    .get(BukkitAdapter.adapt(player)).getUuid());
        } catch (Exception e) {
            return false;
        }
    }
}