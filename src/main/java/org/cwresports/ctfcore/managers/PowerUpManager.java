package org.cwresports.ctfcore.managers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.CTFGame;
import org.cwresports.ctfcore.models.Arena;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages power-ups that spawn randomly during CTF games
 * Power-ups provide temporary advantages to players
 */
public class PowerUpManager {

    private final CTFCore plugin;
    private final Map<CTFGame, List<PowerUp>> activePowerUps;
    private final Map<CTFGame, BukkitTask> spawnTasks;
    private final Map<Arena, List<Location>> powerupSpawnPoints;

    public enum PowerUpType {
        SPEED_BOOST("§e⚡ Speed Boost", Material.SUGAR,
                new PotionEffect(PotionEffectType.SPEED, 300, 2)), // 15 seconds Speed III
        STRENGTH("§c⚔ Strength", Material.BLAZE_POWDER,
                new PotionEffect(PotionEffectType.STRENGTH, 200, 1)), // 10 seconds Strength II
        REGENERATION("§a❤ Regeneration", Material.GHAST_TEAR,
                new PotionEffect(PotionEffectType.REGENERATION, 100, 2)), // 5 seconds Regen III
        JUMP_BOOST("§b↑ Jump Boost", Material.RABBIT_FOOT,
                new PotionEffect(PotionEffectType.JUMP_BOOST, 400, 2)), // 20 seconds Jump III
        INVISIBILITY("§8⚹ Invisibility", Material.FERMENTED_SPIDER_EYE,
                new PotionEffect(PotionEffectType.INVISIBILITY, 60, 0)), // 3 seconds Invisibility
        RESISTANCE("§7⛨ Resistance", Material.IRON_INGOT,
                new PotionEffect(PotionEffectType.RESISTANCE, 200, 1)); // 10 seconds Resistance II

        private final String displayName;
        private final Material material;
        private final PotionEffect effect;

        PowerUpType(String displayName, Material material, PotionEffect effect) {
            this.displayName = displayName;
            this.material = material;
            this.effect = effect;
        }

        public String getDisplayName() { return displayName; }
        public Material getMaterial() { return material; }
        public PotionEffect getEffect() { return effect; }
    }

    public static class PowerUp {
        private final PowerUpType type;
        private final Location location;
        private final ArmorStand armorStand;
        private final BukkitTask particleTask;
        private long spawnTime;

        public PowerUp(PowerUpType type, Location location, CTFCore plugin) {
            this.type = type;
            this.location = location;
            this.spawnTime = System.currentTimeMillis();

            // Create visual armor stand
            this.armorStand = (ArmorStand) location.getWorld().spawnEntity(
                    location.clone().add(0, 0.5, 0), EntityType.ARMOR_STAND);
            armorStand.setVisible(false);
            armorStand.setGravity(false);
            armorStand.setCustomName(type.getDisplayName());
            armorStand.setCustomNameVisible(true);

            // Set item in hand
            ItemStack item = new ItemStack(type.getMaterial());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(type.getDisplayName());
            item.setItemMeta(meta);
            armorStand.setItemInHand(item);

            // Start particle effect
            this.particleTask = new BukkitRunnable() {
                double angle = 0;

                @Override
                public void run() {
                    if (armorStand.isDead()) {
                        cancel();
                        return;
                    }

                    // Rotating particle effect
                    Location center = armorStand.getLocation().add(0, 1, 0);
                    for (int i = 0; i < 3; i++) {
                        double x = Math.cos(angle + i * 2 * Math.PI / 3) * 1.5;
                        double z = Math.sin(angle + i * 2 * Math.PI / 3) * 1.5;
                        Location particleLoc = center.clone().add(x, 0, z);

                        center.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 5, 0.1, 0.1, 0.1, 0);
                    }

                    // Floating effect
                    double y = Math.sin(angle * 2) * 0.2;
                    armorStand.teleport(location.clone().add(0, 0.5 + y, 0));

                    angle += 0.1;
                }
            }.runTaskTimer(plugin, 0L, 2L);
        }

        public PowerUpType getType() { return type; }
        public Location getLocation() { return location; }
        public long getSpawnTime() { return spawnTime; }

        public boolean isNear(Player player, double distance) {
            return player.getLocation().distance(location) <= distance;
        }

        public void remove() {
            if (particleTask != null) {
                particleTask.cancel();
            }
            if (armorStand != null && !armorStand.isDead()) {
                armorStand.remove();
            }
        }

        public void collect(Player player, CTFCore plugin) {
            // Apply effect
            player.addPotionEffect(type.getEffect());

            // Play effects
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation().add(0, 1, 0),
                    30, 1, 1, 1, 0.1);

            // Send message via boss bar instead of chat
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("powerup", type.getDisplayName());
            plugin.getMessageManager().updateBossBar(player, "powerup-collected", placeholders, 1.0);

            // Auto-clear boss bar after 3 seconds
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getMessageManager().clearBossBar(player);
            }, 60L);

            // Remove the power-up
            remove();
        }
    }

    public PowerUpManager(CTFCore plugin) {
        this.plugin = plugin;
        this.activePowerUps = new ConcurrentHashMap<>();
        this.spawnTasks = new ConcurrentHashMap<>();
        this.powerupSpawnPoints = new ConcurrentHashMap<>();
    }

    /**
     * Add a powerup spawn point to an arena
     */
    public void addPowerupSpawnPoint(Arena arena, Location location) {
        powerupSpawnPoints.computeIfAbsent(arena, k -> new ArrayList<>()).add(location.clone());
    }

    /**
     * Get the number of powerup spawn points for an arena
     */
    public int getPowerupSpawnCount(Arena arena) {
        List<Location> spawns = powerupSpawnPoints.get(arena);
        return spawns != null ? spawns.size() : 0;
    }

    /**
     * Get all powerup spawn points for an arena
     */
    public List<Location> getPowerupSpawnPoints(Arena arena) {
        return powerupSpawnPoints.getOrDefault(arena, new ArrayList<>());
    }

    /**
     * Clear all powerup spawn points for an arena
     */
    public void clearPowerupSpawnPoints(Arena arena) {
        powerupSpawnPoints.remove(arena);
    }

    /**
     * Start power-up spawning for a game
     */
    public void startPowerUpSpawning(CTFGame game) {
        if (spawnTasks.containsKey(game)) {
            return; // Already started
        }

        activePowerUps.put(game, new ArrayList<>());

        // Spawn power-ups every 30-60 seconds randomly
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (game.getState() != org.cwresports.ctfcore.models.GameState.PLAYING) {
                    cancel();
                    spawnTasks.remove(game);
                    return;
                }

                spawnRandomPowerUp(game);
            }
        }.runTaskTimer(plugin, 600L, 600L + new Random().nextInt(600)); // 30-60 seconds

        spawnTasks.put(game, task);
    }

    /**
     * Stop power-up spawning and clean up for a game
     */
    public void stopPowerUpSpawning(CTFGame game) {
        BukkitTask task = spawnTasks.remove(game);
        if (task != null) {
            task.cancel();
        }

        List<PowerUp> powerUps = activePowerUps.remove(game);
        if (powerUps != null) {
            for (PowerUp powerUp : powerUps) {
                powerUp.remove();
            }
        }
    }

    /**
     * Spawn a random power-up at a configured spawn point
     */
    private void spawnRandomPowerUp(CTFGame game) {
        Arena arena = game.getArena();
        List<PowerUp> powerUps = activePowerUps.get(game);

        if (powerUps == null || powerUps.size() >= 3) {
            return; // Max 3 power-ups at once
        }

        // Get random spawn location from configured spawn points
        Location spawnLoc = getRandomPowerUpLocation(arena);
        if (spawnLoc == null) {
            return;
        }

        // Random power-up type
        PowerUpType[] types = PowerUpType.values();
        PowerUpType randomType = types[new Random().nextInt(types.length)];

        // Create power-up
        PowerUp powerUp = new PowerUp(randomType, spawnLoc, plugin);
        powerUps.add(powerUp);

        // Announce to all players via boss bar instead of chat
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("powerup", randomType.getDisplayName());
        
        for (org.cwresports.ctfcore.models.CTFPlayer ctfPlayer : game.getPlayers()) {
            Player player = ctfPlayer.getPlayer();
            if (player != null && player.isOnline()) {
                plugin.getMessageManager().updateBossBar(player, "powerup-spawned", placeholders, 1.0);
                
                // Auto-clear boss bar after 5 seconds
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    plugin.getMessageManager().clearBossBar(player);
                }, 100L);
            }
        }

        // Auto-remove after 30 seconds if not collected
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (powerUps.contains(powerUp)) {
                powerUp.remove();
                powerUps.remove(powerUp);
            }
        }, 600L);
    }

    /**
     * Get a random location for power-up spawning from configured spawn points
     */
    private Location getRandomPowerUpLocation(Arena arena) {
        List<Location> spawnPoints = powerupSpawnPoints.get(arena);
        
        if (spawnPoints == null || spawnPoints.isEmpty()) {
            // Fallback to old method if no spawn points configured
            return getRandomPowerUpLocationFallback(arena);
        }

        // Return random spawn point
        Random random = new Random();
        return spawnPoints.get(random.nextInt(spawnPoints.size())).clone();
    }

    /**
     * Fallback method for power-up spawning when no spawn points are configured
     */
    private Location getRandomPowerUpLocationFallback(Arena arena) {
        // Get center point between red and blue flag locations
        Arena.Team redTeam = arena.getTeam(Arena.TeamColor.RED);
        Arena.Team blueTeam = arena.getTeam(Arena.TeamColor.BLUE);

        if (redTeam.getFlagLocation() == null || blueTeam.getFlagLocation() == null) {
            return null;
        }

        Location redFlag = redTeam.getFlagLocation();
        Location blueFlag = blueTeam.getFlagLocation();

        // Calculate center point
        double centerX = (redFlag.getX() + blueFlag.getX()) / 2;
        double centerZ = (redFlag.getZ() + blueFlag.getZ()) / 2;
        double centerY = Math.max(redFlag.getY(), blueFlag.getY());

        // Add some randomness around the center
        Random random = new Random();
        centerX += (random.nextDouble() - 0.5) * 20; // ±10 blocks
        centerZ += (random.nextDouble() - 0.5) * 20; // ±10 blocks

        Location spawnLoc = new Location(redFlag.getWorld(), centerX, centerY, centerZ);

        // Ensure the location is safe
        while (spawnLoc.getBlock().getType().isSolid() && spawnLoc.getY() < 255) {
            spawnLoc.add(0, 1, 0);
        }

        return spawnLoc;
    }

    /**
     * Check if a player is near any power-up and collect it
     */
    public void checkPowerUpCollection(Player player, CTFGame game) {
        List<PowerUp> powerUps = activePowerUps.get(game);
        if (powerUps == null) {
            return;
        }

        Iterator<PowerUp> iterator = powerUps.iterator();
        while (iterator.hasNext()) {
            PowerUp powerUp = iterator.next();
            if (powerUp.isNear(player, 2.0)) { // 2 block radius
                powerUp.collect(player, plugin);
                iterator.remove();
                break; // Only collect one at a time
            }
        }
    }

    /**
     * Cleanup method for plugin disable
     */
    public void cleanup() {
        for (BukkitTask task : spawnTasks.values()) {
            task.cancel();
        }
        spawnTasks.clear();

        for (List<PowerUp> powerUps : activePowerUps.values()) {
            for (PowerUp powerUp : powerUps) {
                powerUp.remove();
            }
        }
        activePowerUps.clear();
        powerupSpawnPoints.clear();
    }

    /**
     * Shutdown the power-up manager (alias for cleanup)
     */
    public void shutdown() {
        cleanup();
    }
}