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
 * Enhanced power-up manager with improved spawning and collection system
 * Manages power-ups that spawn randomly during CTF games
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
                new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 200, 1)), // 10 seconds Strength II
        REGENERATION("§a❤ Regeneration", Material.GHAST_TEAR,
                new PotionEffect(PotionEffectType.REGENERATION, 100, 2)), // 5 seconds Regen III
        JUMP_BOOST("§b↑ Jump Boost", Material.RABBIT_FOOT,
                new PotionEffect(PotionEffectType.JUMP, 400, 2)), // 20 seconds Jump III
        INVISIBILITY("§8⚹ Invisibility", Material.FERMENTED_SPIDER_EYE,
                new PotionEffect(PotionEffectType.INVISIBILITY, 60, 0)), // 3 seconds Invisibility
        RESISTANCE("§7⛨ Resistance", Material.IRON_INGOT,
                new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 200, 1)); // 10 seconds Resistance II

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
            if (meta != null) {
                meta.setDisplayName(type.getDisplayName());
                item.setItemMeta(meta);
            }
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

                        center.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, particleLoc, 5, 0.1, 0.1, 0.1, 0);
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
            player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation().add(0, 1, 0),
                    30, 1, 1, 1, 0.1);

            // Send message via action bar
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("powerup", type.getDisplayName());
            String message = plugin.getConfigManager().getMessage("powerup-collected", placeholders);
            
            // Send action bar message
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                new net.md_5.bungee.api.chat.TextComponent("§a§l✓ " + message + " §a§l✓"));

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
        plugin.getLogger().info("Added powerup spawn point for arena " + arena.getName() + " at " + 
                               location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
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
     * Enhanced start power-up spawning for a game
     */
    public void startPowerUpSpawning(CTFGame game) {
        if (spawnTasks.containsKey(game)) {
            return; // Already started
        }

        activePowerUps.put(game, new ArrayList<>());
        
        plugin.getLogger().info("Starting power-up spawning for arena: " + game.getArena().getName());

        // Spawn first power-up after 30 seconds
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (game.getState() == org.cwresports.ctfcore.models.GameState.PLAYING) {
                spawnRandomPowerUp(game);
            }
        }, 600L); // 30 seconds

        // Then spawn power-ups every 45-75 seconds randomly
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (game.getState() != org.cwresports.ctfcore.models.GameState.PLAYING) {
                    plugin.getLogger().info("Stopping power-up spawning for arena: " + game.getArena().getName() + " (game not playing)");
                    cancel();
                    spawnTasks.remove(game);
                    return;
                }

                spawnRandomPowerUp(game);
            }
        }.runTaskTimer(plugin, 900L, 900L + new Random().nextInt(600)); // 45-75 seconds

        spawnTasks.put(game, task);
    }

    /**
     * Stop power-up spawning and clean up for a game
     */
    public void stopPowerUpSpawning(CTFGame game) {
        plugin.getLogger().info("Stopping power-up spawning for arena: " + game.getArena().getName());
        
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
     * Enhanced spawn random power-up method
     */
    private void spawnRandomPowerUp(CTFGame game) {
        Arena arena = game.getArena();
        List<PowerUp> powerUps = activePowerUps.get(game);

        if (powerUps == null) {
            plugin.getLogger().warning("No power-up list found for arena: " + arena.getName());
            return;
        }

        if (powerUps.size() >= 3) {
            plugin.getLogger().info("Max power-ups reached for arena: " + arena.getName());
            return; // Max 3 power-ups at once
        }

        // Get random spawn location
        Location spawnLoc = getRandomPowerUpLocation(arena);
        if (spawnLoc == null) {
            plugin.getLogger().warning("No valid spawn location found for power-up in arena: " + arena.getName());
            return;
        }

        // Random power-up type
        PowerUpType[] types = PowerUpType.values();
        PowerUpType randomType = types[new Random().nextInt(types.length)];

        // Create power-up
        PowerUp powerUp = new PowerUp(randomType, spawnLoc, plugin);
        powerUps.add(powerUp);

        plugin.getLogger().info("Spawned power-up " + randomType.getDisplayName() + " at " + 
                               spawnLoc.getBlockX() + "," + spawnLoc.getBlockY() + "," + spawnLoc.getBlockZ());

        // Announce to all players via action bar
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("powerup", randomType.getDisplayName());
        String message = plugin.getConfigManager().getMessage("powerup-spawned", placeholders);
        
        for (org.cwresports.ctfcore.models.CTFPlayer ctfPlayer : game.getPlayers()) {
            Player player = ctfPlayer.getPlayer();
            if (player != null && player.isOnline()) {
                // Send action bar message
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                    new net.md_5.bungee.api.chat.TextComponent("§e§l⚡ " + message + " §e§l⚡"));
            }
        }

        // Auto-remove after 60 seconds if not collected
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (powerUps.contains(powerUp)) {
                powerUp.remove();
                powerUps.remove(powerUp);
                plugin.getLogger().info("Auto-removed uncollected power-up " + randomType.getDisplayName());
            }
        }, 1200L); // 60 seconds
    }

    /**
     * Enhanced get random location method with better fallback
     */
    private Location getRandomPowerUpLocation(Arena arena) {
        List<Location> spawnPoints = powerupSpawnPoints.get(arena);
        
        if (spawnPoints != null && !spawnPoints.isEmpty()) {
            // Use configured spawn points
            Random random = new Random();
            Location chosen = spawnPoints.get(random.nextInt(spawnPoints.size())).clone();
            plugin.getLogger().info("Using configured spawn point for arena: " + arena.getName());
            return chosen;
        }

        // Fallback to calculated location
        plugin.getLogger().info("Using fallback spawn calculation for arena: " + arena.getName());
        return getRandomPowerUpLocationFallback(arena);
    }

    /**
     * Enhanced fallback method for power-up spawning
     */
    private Location getRandomPowerUpLocationFallback(Arena arena) {
        // Try to use arena center if available
        if (arena.getBoundaryMin() != null && arena.getBoundaryMax() != null) {
            Location min = arena.getBoundaryMin();
            Location max = arena.getBoundaryMax();
            
            Random random = new Random();
            double x = min.getX() + random.nextDouble() * (max.getX() - min.getX());
            double z = min.getZ() + random.nextDouble() * (max.getZ() - min.getZ());
            double y = Math.max(min.getY(), max.getY()) + 1;
            
            Location spawnLoc = new Location(min.getWorld(), x, y, z);
            
            // Find safe Y coordinate
            while (spawnLoc.getBlock().getType().isSolid() && spawnLoc.getY() < 255) {
                spawnLoc.add(0, 1, 0);
            }
            
            plugin.getLogger().info("Generated fallback spawn location using arena boundaries");
            return spawnLoc;
        }

        // Get center point between red and blue flag locations
        Arena.Team redTeam = arena.getTeam(Arena.TeamColor.RED);
        Arena.Team blueTeam = arena.getTeam(Arena.TeamColor.BLUE);

        if (redTeam.getFlagLocation() == null || blueTeam.getFlagLocation() == null) {
            plugin.getLogger().warning("Cannot generate fallback spawn location - missing flag locations");
            return null;
        }

        Location redFlag = redTeam.getFlagLocation();
        Location blueFlag = blueTeam.getFlagLocation();

        // Calculate center point
        double centerX = (redFlag.getX() + blueFlag.getX()) / 2;
        double centerZ = (redFlag.getZ() + blueFlag.getZ()) / 2;
        double centerY = Math.max(redFlag.getY(), blueFlag.getY()) + 1;

        // Add some randomness around the center
        Random random = new Random();
        centerX += (random.nextDouble() - 0.5) * 20; // ±10 blocks
        centerZ += (random.nextDouble() - 0.5) * 20; // ±10 blocks

        Location spawnLoc = new Location(redFlag.getWorld(), centerX, centerY, centerZ);

        // Ensure the location is safe
        while (spawnLoc.getBlock().getType().isSolid() && spawnLoc.getY() < 255) {
            spawnLoc.add(0, 1, 0);
        }

        plugin.getLogger().info("Generated fallback spawn location using flag positions");
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
                plugin.getLogger().info("Player " + player.getName() + " collected power-up " + powerUp.getType().getDisplayName());
                break; // Only collect one at a time
            }
        }
    }

    /**
     * Get debug information about power-up spawning
     */
    public Map<String, Object> getDebugInfo(Arena arena) {
        Map<String, Object> info = new HashMap<>();
        info.put("configured_spawn_points", getPowerupSpawnCount(arena));
        info.put("has_boundaries", arena.getBoundaryMin() != null && arena.getBoundaryMax() != null);
        info.put("has_flag_locations", 
                arena.getTeam(Arena.TeamColor.RED).getFlagLocation() != null &&
                arena.getTeam(Arena.TeamColor.BLUE).getFlagLocation() != null);
        
        CTFGame game = plugin.getGameManager().getGame(arena);
        if (game != null) {
            info.put("active_powerups", activePowerUps.getOrDefault(game, new ArrayList<>()).size());
            info.put("spawn_task_active", spawnTasks.containsKey(game));
        }
        
        return info;
    }

    /**
     * Enhanced cleanup method
     */
    public void cleanup() {
        plugin.getLogger().info("Cleaning up PowerUpManager...");
        
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
        
        plugin.getLogger().info("PowerUpManager cleanup complete");
    }

    /**
     * Shutdown the power-up manager (alias for cleanup)
     */
    public void shutdown() {
        cleanup();
    }
}