package org.cwresports.ctfcore.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.Arena;
import org.cwresports.ctfcore.models.CTFGame;
import org.cwresports.ctfcore.models.CTFPlayer;
import org.cwresports.ctfcore.models.GameState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced player move listener with automatic flag capture system
 * Handles movement-based events including boundary checks and flag capture
 */
public class PlayerMoveListener implements Listener {

    private final CTFCore plugin;
    private final Map<UUID, CaptureAttempt> activeCaptureAttempts;

    public PlayerMoveListener(CTFCore plugin) {
        this.plugin = plugin;
        this.activeCaptureAttempts = new ConcurrentHashMap<>();
    }

    /**
     * Represents an active capture attempt for automatic flag capture
     */
    private static class CaptureAttempt {
        private final Player player;
        private final Location capturePoint;
        private final Arena.TeamColor flagTeam;
        private final long startTime;
        private final int captureTimeSeconds;
        private final double captureRadius;

        public CaptureAttempt(Player player, Location capturePoint, Arena.TeamColor flagTeam, int captureTimeSeconds, double captureRadius) {
            this.player = player;
            this.capturePoint = capturePoint;
            this.flagTeam = flagTeam;
            this.startTime = System.currentTimeMillis();
            this.captureTimeSeconds = captureTimeSeconds;
            this.captureRadius = captureRadius;
        }

        public Player getPlayer() { return player; }
        public Location getCapturePoint() { return capturePoint; }
        public Arena.TeamColor getFlagTeam() { return flagTeam; }
        public long getStartTime() { return startTime; }
        public int getCaptureTimeSeconds() { return captureTimeSeconds; }
        public double getCaptureRadius() { return captureRadius; }

        public boolean isInArea(Location playerLocation) {
            return playerLocation.distance(capturePoint) <= captureRadius;
        }

        public int getTimeRemaining() {
            long elapsed = System.currentTimeMillis() - startTime;
            return Math.max(0, captureTimeSeconds - (int) (elapsed / 1000));
        }

        public boolean isCompleted() {
            return getTimeRemaining() <= 0;
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is in a game
        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(player);
        if (ctfPlayer == null || !ctfPlayer.isInGame()) {
            return;
        }

        CTFGame game = ctfPlayer.getGame();
        if (game == null || game.getState() != GameState.PLAYING) {
            return;
        }

        // Only process if player actually moved to a different block
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        // Check for arena boundaries
        checkArenaBoundaries(event, ctfPlayer, game);

        // Check for power-up collection
        plugin.getPowerUpManager().checkPowerUpCollection(player, game);

        // Handle automatic flag capture if enabled
        if (plugin.getConfigManager().getMainConfig().getString("flag-capture.mode", "automatic").equals("automatic")) {
            handleAutomaticFlagCapture(event, ctfPlayer, game);
        }

        // Handle active capture attempts
        handleActiveCaptureAttempts(player);
    }

    /**
     * Handle automatic flag capture when player moves into capture area
     */
    private void handleAutomaticFlagCapture(PlayerMoveEvent event, CTFPlayer ctfPlayer, CTFGame game) {
        // Only process if player has a flag
        if (!ctfPlayer.hasFlag()) {
            return;
        }

        Location playerLocation = event.getTo();
        Arena arena = game.getArena();
        Arena.Team teamData = arena.getTeam(ctfPlayer.getTeam());

        if (teamData.getCapturePoint() == null) {
            return;
        }

        double captureRadius = plugin.getConfigManager().getMainConfig().getDouble("flag-capture.automatic-radius", 2.0);
        Location capturePoint = teamData.getCapturePoint();

        // Check if player is within capture radius
        if (playerLocation.distance(capturePoint) <= captureRadius) {
            UUID playerId = ctfPlayer.getPlayer().getUniqueId();
            
            // Check if player already has an active capture attempt
            if (!activeCaptureAttempts.containsKey(playerId)) {
                // Start new capture attempt
                startAutomaticCaptureAttempt(ctfPlayer, game, capturePoint);
            }
        } else {
            // Player moved out of capture area, cancel any active attempt
            cancelCaptureAttempt(ctfPlayer.getPlayer().getUniqueId(), true);
        }
    }

    /**
     * Start automatic capture attempt
     */
    private void startAutomaticCaptureAttempt(CTFPlayer ctfPlayer, CTFGame game, Location capturePoint) {
        Player player = ctfPlayer.getPlayer();
        UUID playerId = player.getUniqueId();
        
        int captureTimeSeconds = plugin.getConfigManager().getMainConfig().getInt("flag-capture.capture-time-seconds", 3);
        double captureRadius = plugin.getConfigManager().getMainConfig().getDouble("flag-capture.automatic-radius", 2.0);
        
        CaptureAttempt attempt = new CaptureAttempt(player, capturePoint, 
                                                  ctfPlayer.getCarryingFlag().getTeam(), 
                                                  captureTimeSeconds, captureRadius);
        
        activeCaptureAttempts.put(playerId, attempt);
        
        // Schedule capture completion check
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            checkCaptureCompletion(playerId, game);
        }, captureTimeSeconds * 20L);
        
        // Start title display if enabled
        if (plugin.getConfigManager().getMainConfig().getBoolean("flag-capture.show-capture-title", true)) {
            startCaptureTitle(player, captureTimeSeconds);
        }
    }

    /**
     * Handle active capture attempts (check movement and update titles)
     */
    private void handleActiveCaptureAttempts(Player player) {
        CaptureAttempt attempt = activeCaptureAttempts.get(player.getUniqueId());
        if (attempt == null) {
            return;
        }

        // Check if player is still in capture area
        if (!attempt.isInArea(player.getLocation())) {
            cancelCaptureAttempt(player.getUniqueId(), true);
            return;
        }

        // Update title display
        if (plugin.getConfigManager().getMainConfig().getBoolean("flag-capture.show-capture-title", true)) {
            int timeRemaining = attempt.getTimeRemaining();
            if (timeRemaining > 0) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("time", String.valueOf(timeRemaining));
                
                plugin.getMessageManager().sendTitle(player, "title-capturing-flag", "subtitle-capturing-flag", placeholders);
            }
        }
    }

    /**
     * Start capture title display with countdown
     */
    private void startCaptureTitle(Player player, int captureTimeSeconds) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            int timeLeft = captureTimeSeconds;
            
            @Override
            public void run() {
                if (!player.isOnline() || !activeCaptureAttempts.containsKey(player.getUniqueId())) {
                    return;
                }
                
                if (timeLeft <= 0) {
                    return;
                }
                
                // Show countdown title
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("time", String.valueOf(timeLeft));
                
                String title = "§a§lCAPTURING FLAG";
                String subtitle = "§e§l" + timeLeft;
                
                player.sendTitle(title, subtitle, 0, 25, 0);
                
                timeLeft--;
            }
        }, 0L, 20L);
    }

    /**
     * Check if capture attempt is completed
     */
    private void checkCaptureCompletion(UUID playerId, CTFGame game) {
        CaptureAttempt attempt = activeCaptureAttempts.get(playerId);
        if (attempt == null) {
            return;
        }

        Player player = attempt.getPlayer();
        if (!player.isOnline()) {
            activeCaptureAttempts.remove(playerId);
            return;
        }

        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(player);
        if (ctfPlayer == null || !ctfPlayer.isInGame()) {
            activeCaptureAttempts.remove(playerId);
            return;
        }

        // Check if player is still in capture area and still has flag
        if (attempt.isCompleted() && attempt.isInArea(player.getLocation()) && ctfPlayer.hasFlag()) {
            // Complete the capture
            boolean success = game.captureFlag(ctfPlayer);
            if (success) {
                player.playSound(player.getLocation(), 
                               plugin.getConfigManager().getSound("flag_captured"), 1.0f, 1.0f);
                
                // Show completion title
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("team_name", ctfPlayer.getTeam().getName().toUpperCase());
                
                String title = "§a§lFLAG CAPTURED!";
                String subtitle = "§e§l" + ctfPlayer.getTeam().getColorCode() + ctfPlayer.getTeam().getName().toUpperCase() + " TEAM";
                
                player.sendTitle(title, subtitle, 10, 40, 10);
            }
        }
        
        activeCaptureAttempts.remove(playerId);
    }

    /**
     * Cancel capture attempt
     */
    private void cancelCaptureAttempt(UUID playerId, boolean showMessage) {
        CaptureAttempt attempt = activeCaptureAttempts.remove(playerId);
        if (attempt != null && showMessage) {
            Player player = attempt.getPlayer();
            if (player != null && player.isOnline()) {
                player.sendTitle("§c§lCAPTURE CANCELLED", "§7You left the capture area", 10, 30, 10);
            }
        }
    }

    /**
     * Check arena boundaries
     */
    private void checkArenaBoundaries(PlayerMoveEvent event, CTFPlayer ctfPlayer, CTFGame game) {
        Arena arena = game.getArena();
        Location playerLocation = event.getTo();

        // Check if player is outside arena boundaries
        if (arena.getBoundaryMin() != null && arena.getBoundaryMax() != null) {
            if (playerLocation.getX() < arena.getBoundaryMin().getX() || 
                playerLocation.getX() > arena.getBoundaryMax().getX() ||
                playerLocation.getZ() < arena.getBoundaryMin().getZ() || 
                playerLocation.getZ() > arena.getBoundaryMax().getZ()) {
                
                if (plugin.getConfigManager().getMainConfig().getBoolean("boundaries.teleport-back-on-exit", true)) {
                    event.setCancelled(true);
                    
                    if (plugin.getConfigManager().getMainConfig().getBoolean("boundaries.warning-message-enabled", true)) {
                        Map<String, String> placeholders = new HashMap<>();
                        ctfPlayer.getPlayer().sendMessage(plugin.getConfigManager().getMessage("boundary-warning", placeholders));
                    }
                }
            }
        }
    }

    /**
     * Clean up capture attempts on player quit
     */
    public void cleanupPlayer(Player player) {
        activeCaptureAttempts.remove(player.getUniqueId());
    }

    /**
     * Clean up all capture attempts
     */
    public void cleanup() {
        activeCaptureAttempts.clear();
    }
}