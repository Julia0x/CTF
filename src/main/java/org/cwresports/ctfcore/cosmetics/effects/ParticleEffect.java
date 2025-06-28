package org.cwresports.ctfcore.cosmetics.effects;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Handles particle effects for CTF cosmetics
 * Updated for Paper 1.21+ with modern particle names
 */
public class ParticleEffect {

    /**
     * Spawn critical hit particles (using CRIT instead of deprecated CRIT_MAGIC)
     */
    public static void spawnCriticalParticles(Location location, int count) {
        World world = location.getWorld();
        if (world != null) {
            // Using CRIT instead of deprecated CRIT_MAGIC
            world.spawnParticle(Particle.CRIT, location, count, 0.1, 0.1, 0.1, 0);
        }
    }

    /**
     * Spawn magic critical particles
     */
    public static void spawnMagicCriticalParticles(Location location, int count) {
        World world = location.getWorld();
        if (world != null) {
            // Using ENCHANTED_HIT as alternative to deprecated CRIT_MAGIC
            world.spawnParticle(Particle.ENCHANTED_HIT, location, count, 0.1, 0.1, 0.1, 0);
        }
    }

    /**
     * Spawn dust particles (using DUST instead of deprecated REDSTONE)
     */
    public static void spawnDustParticles(Location location, int count) {
        World world = location.getWorld();
        if (world != null) {
            // Using DUST with red color instead of deprecated REDSTONE
            Particle.DustOptions dustOptions = new Particle.DustOptions(org.bukkit.Color.RED, 1.0f);
            world.spawnParticle(Particle.DUST, location, count, 0.1, 0.1, 0.1, 0, dustOptions);
        }
    }

    /**
     * Spawn item break particles (using ITEM instead of deprecated ITEM_CRACK)
     */
    public static void spawnItemBreakParticles(Location location, int count, org.bukkit.inventory.ItemStack item) {
        World world = location.getWorld();
        if (world != null && item != null) {
            // Using ITEM instead of deprecated ITEM_CRACK
            world.spawnParticle(Particle.ITEM, location, count, 0.1, 0.1, 0.1, 0, item);
        }
    }

    /**
     * Spawn enhanced dust particles with color options
     */
    public static void spawnColoredDustParticles(Location location, int count, org.bukkit.Color color) {
        World world = location.getWorld();
        if (world != null) {
            Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1.0f);
            world.spawnParticle(Particle.DUST, location, count, 0.1, 0.1, 0.1, 0, dustOptions);
        }
    }

    /**
     * Spawn victory particles for team wins
     */
    public static void spawnVictoryParticles(Player player) {
        if (player != null && player.isOnline()) {
            Location location = player.getLocation().add(0, 2, 0);
            World world = location.getWorld();
            if (world != null) {
                world.spawnParticle(Particle.FIREWORK, location, 20, 1, 1, 1, 0.1);
                world.spawnParticle(Particle.ENCHANTED_HIT, location, 15, 0.5, 0.5, 0.5, 0);
            }
        }
    }

    /**
     * Spawn flag capture particles
     */
    public static void spawnFlagCaptureParticles(Location location) {
        World world = location.getWorld();
        if (world != null) {
            world.spawnParticle(Particle.ENCHANTED_HIT, location, 30, 1, 1, 1, 0.1);
            world.spawnParticle(Particle.HAPPY_VILLAGER, location.add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0);
        }
    }

    /**
     * Spawn death particles
     */
    public static void spawnDeathParticles(Location location) {
        World world = location.getWorld();
        if (world != null) {
            // Red dust for blood effect
            Particle.DustOptions redDust = new Particle.DustOptions(org.bukkit.Color.MAROON, 1.5f);
            world.spawnParticle(Particle.DUST, location, 25, 0.5, 0.5, 0.5, 0, redDust);
            world.spawnParticle(Particle.LARGE_SMOKE, location.add(0, 0.5, 0), 10, 0.3, 0.3, 0.3, 0.05);
        }
    }
}