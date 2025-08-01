package org.cwresports.ctfcore.listeners;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Location;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.managers.ArenaManager;
import org.cwresports.ctfcore.models.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced player interact listener with configurable flag capture system
 * Handles both right-click and automatic flag interactions based on configuration
 */
public class PlayerInteractListener implements Listener {

    private final CTFCore plugin;
    private final Map<UUID, InteractionAttempt> activeAttempts;

    public PlayerInteractListener(CTFCore plugin) {
        this.plugin = plugin;
        this.activeAttempts = new ConcurrentHashMap<>();
    }

    /**
     * Represents an active interaction attempt (flag take, capture, or return)
     */
    private static class InteractionAttempt {
        private final Player player;
        private final InteractionType type;
        private final Location targetLocation;
        private final Arena.TeamColor flagTeam; // For flag operations
        private final BukkitTask countdownTask;
        private int timeLeft;

        public InteractionAttempt(Player player, InteractionType type, Location targetLocation,
                                  Arena.TeamColor flagTeam, BukkitTask countdownTask) {
            this.player = player;
            this.type = type;
            this.targetLocation = targetLocation;
            this.flagTeam = flagTeam;
            this.countdownTask = countdownTask;
            this.timeLeft = 3;
        }

        public Player getPlayer() { return player; }
        public InteractionType getType() { return type; }
        public Location getTargetLocation() { return targetLocation; }
        public Arena.TeamColor getFlagTeam() { return flagTeam; }
        public BukkitTask getCountdownTask() { return countdownTask; }
        public int getTimeLeft() { return timeLeft; }
        public void decrementTime() { timeLeft--; }
    }

    private enum InteractionType {
        FLAG_TAKE,    // Taking enemy flag from their base
        FLAG_CAPTURE, // Capturing flag at own capture point
        FLAG_PICKUP,  // Picking up dropped flag
        FLAG_RETURN   // Returning own team's dropped flag
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Handle admin tool interactions first
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getItem() != null) {
                // Check admin tools
                if (plugin.getAdminToolManager().handleToolInteraction(player, event.getItem())) {
                    event.setCancelled(true);
                    return;
                }

                // Check if this is a lobby hotbar item
                if (plugin.getLobbyManager().handleHotbarClick(player, event.getItem())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // Only handle right-click on blocks for setup modes and flag interactions
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        // Check if player is in capture point setup mode
        if (plugin.getArenaManager().isPlayerInSetupMode(player)) {
            handleCapturePointSetup(event, player, block);
            return;
        }

        // Check if flag capture is set to right-click mode
        String captureMode = plugin.getConfigManager().getMainConfig().getString("flag-capture.mode", "automatic");
        if (captureMode.equals("right-click")) {
            // Handle right-click flag interactions
            handleFlagInteractions(event, player, block);
        }
    }

    /**
     * Handle block break events for flag setup mode
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Check if player is in flag setup mode
        if (!plugin.getArenaManager().isPlayerInSetupMode(player)) {
            return;
        }

        ArenaManager.SetupSession session = plugin.getArenaManager().getPlayerSetupSession(player);
        if (session == null || session.mode != ArenaManager.SetupMode.FLAG_PLACEMENT) {
            return;
        }

        Block block = event.getBlock();

        // Check if it's a banner block
        if (!block.getType().name().contains("BANNER")) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getMessage("setup-must-break-banner"));
            return;
        }

        // Check if banner color matches team color
        Material expectedBanner = session.teamColor == Arena.TeamColor.RED ?
                Material.RED_BANNER : Material.BLUE_BANNER;

        if (block.getType() != expectedBanner) {
            event.setCancelled(true);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("team_color", session.teamColor.getColorCode());
            placeholders.put("team_name", session.teamColor.getName());
            player.sendMessage(plugin.getConfigManager().getMessage("setup-wrong-banner-color", placeholders));
            return;
        }

        // Cancel the break event to prevent block destruction
        event.setCancelled(true);

        // Handle flag placement
        boolean success = plugin.getArenaManager().handleFlagPlacement(player, block.getLocation());

        if (success) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("team_name", session.teamColor.getName());
            placeholders.put("team_color", session.teamColor.getColorCode());
            placeholders.put("arena", session.arenaName);
            player.sendMessage(plugin.getConfigManager().getMessage("setup-flag-set", placeholders));
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("error-generic"));
        }
    }

    /**
     * Handle flag interactions with right-click system (legacy system)
     */
    private void handleFlagInteractions(PlayerInteractEvent event, Player player, Block block) {
        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(player);
        if (ctfPlayer == null || !ctfPlayer.isInGame()) {
            return;
        }

        CTFGame game = ctfPlayer.getGame();
        if (game == null || game.getState() != GameState.PLAYING) {
            return;
        }

        // Check if player is already attempting an interaction
        if (activeAttempts.containsKey(player.getUniqueId())) {
            return; // Already interacting
        }

        // Determine what type of interaction this could be
        InteractionType interactionType = null;
        Arena.TeamColor targetFlagTeam = null;
        String actionMessage = null;

        // Check if clicking on capture point (for flag capture)
        if (ctfPlayer.hasFlag() && ctfPlayer.getTeam() != null) {
            Arena arena = game.getArena();
            Arena.Team teamData = arena.getTeam(ctfPlayer.getTeam());

            if (teamData.getCapturePoint() != null &&
                    block.getLocation().equals(teamData.getCapturePoint()) &&
                    player.getLocation().distance(teamData.getCapturePoint()) <= 3.0) {

                interactionType = InteractionType.FLAG_CAPTURE;
                targetFlagTeam = ctfPlayer.getCarryingFlag().getTeam();
                actionMessage = "§a§lCapturing flag...";
            }
        }

        // Check if clicking on enemy flag at their base (for flag take)
        if (interactionType == null && !ctfPlayer.hasFlag() && ctfPlayer.getTeam() != null) {
            for (Arena.TeamColor teamColor : Arena.TeamColor.values()) {
                if (teamColor == ctfPlayer.getTeam()) continue; // Skip own team

                CTFFlag flag = game.getFlag(teamColor);
                if (flag != null && flag.isAtBase()) {
                    Arena arena = game.getArena();
                    Arena.Team enemyTeamData = arena.getTeam(teamColor);
                    Location flagLocation = enemyTeamData.getFlagLocation();

                    if (flagLocation != null &&
                            block.getLocation().equals(flagLocation) &&
                            player.getLocation().distance(flagLocation) <= 2.0) {

                        interactionType = InteractionType.FLAG_TAKE;
                        targetFlagTeam = teamColor;
                        actionMessage = "§e§lTaking " + teamColor.getColorCode() + teamColor.getName().toUpperCase() + " FLAG...";
                        break;
                    }
                }
            }
        }

        // Check if clicking on dropped flag (for pickup or return)
        if (interactionType == null && !ctfPlayer.hasFlag() && ctfPlayer.getTeam() != null) {
            for (Arena.TeamColor teamColor : Arena.TeamColor.values()) {
                CTFFlag flag = game.getFlag(teamColor);
                if (flag != null && flag.isDropped()) {
                    Location dropLocation = flag.getCurrentLocation();

                    if (dropLocation != null &&
                            block.getLocation().distance(dropLocation) <= 2.0 &&
                            player.getLocation().distance(dropLocation) <= 2.0) {

                        if (teamColor == ctfPlayer.getTeam()) {
                            // Own team flag - return it
                            interactionType = InteractionType.FLAG_RETURN;
                            targetFlagTeam = teamColor;
                            actionMessage = "§b§lReturning flag...";
                        } else {
                            // Enemy flag - pick it up
                            interactionType = InteractionType.FLAG_PICKUP;
                            targetFlagTeam = teamColor;
                            actionMessage = "§e§lPicking up " + teamColor.getColorCode() + teamColor.getName().toUpperCase() + " FLAG...";
                        }
                        break;
                    }
                }
            }
        }

        // If no valid interaction found, return
        if (interactionType == null) {
            return;
        }

        // Cancel the interaction to prevent any side effects
        event.setCancelled(true);

        // Start 3-second interaction process
        startInteractionCountdown(player, ctfPlayer, game, block, interactionType, targetFlagTeam, actionMessage);
    }

    /**
     * Start the 3-second interaction countdown with action bar display
     */
    private void startInteractionCountdown(Player player, CTFPlayer ctfPlayer, CTFGame game, Block targetBlock,
                                           InteractionType type, Arena.TeamColor flagTeam, String actionMessage) {
        int holdTime = plugin.getConfigManager().getGameplaySetting("flag-capture-hold-seconds", 3);

        BukkitTask countdownTask = new BukkitRunnable() {
            int timeLeft = holdTime;

            @Override
            public void run() {
                InteractionAttempt attempt = activeAttempts.get(player.getUniqueId());
                if (attempt == null) {
                    cancel();
                    return;
                }

                // Check if player is still online and in the game
                if (!player.isOnline() || !ctfPlayer.isInGame()) {
                    cancelInteraction(player.getUniqueId(), false);
                    cancel();
                    return;
                }

                // Check if player moved too far from target
                if (player.getLocation().distance(targetBlock.getLocation()) > 3.0) {
                    cancelInteraction(player.getUniqueId(), true);
                    cancel();
                    return;
                }

                // Type-specific validation
                boolean validationFailed = false;
                switch (type) {
                    case FLAG_CAPTURE:
                        if (!ctfPlayer.hasFlag()) {
                            validationFailed = true;
                        }
                        break;
                    case FLAG_TAKE:
                    case FLAG_PICKUP:
                    case FLAG_RETURN:
                        if (ctfPlayer.hasFlag()) {
                            validationFailed = true;
                        }
                        break;
                }

                if (validationFailed) {
                    cancelInteraction(player.getUniqueId(), false);
                    cancel();
                    return;
                }

                if (timeLeft <= 0) {
                    // Interaction completed!
                    activeAttempts.remove(player.getUniqueId());

                    // Execute the appropriate action
                    boolean success = false;
                    switch (type) {
                        case FLAG_CAPTURE:
                            success = game.captureFlag(ctfPlayer);
                            if (success) {
                                player.playSound(player.getLocation(),
                                        plugin.getConfigManager().getSound("flag_captured"), 1.0f, 1.0f);
                            }
                            break;
                        case FLAG_TAKE:
                        case FLAG_PICKUP:
                            success = game.takeFlag(ctfPlayer, flagTeam);
                            if (success) {
                                player.playSound(player.getLocation(),
                                        plugin.getConfigManager().getSound("flag_taken"), 1.0f, 1.0f);
                            }
                            break;
                        case FLAG_RETURN:
                            CTFFlag flag = game.getFlag(flagTeam);
                            if (flag != null) {
                                flag.returnToBase();
                                ctfPlayer.addFlagReturn();

                                Map<String, String> placeholders = new HashMap<>();
                                placeholders.put("player", player.getName());
                                placeholders.put("team_color", flagTeam.getColorCode());

                                game.broadcastMessage("flag-returned-clean", placeholders);
                                player.playSound(player.getLocation(),
                                        plugin.getConfigManager().getSound("flag_returned"), 1.0f, 1.0f);
                                success = true;
                            }
                            break;
                    }

                    if (!success) {
                        player.sendMessage("§c§lInteraction failed!");
                    }

                    cancel();
                    return;
                }

                // Show countdown in action bar
                String countdownText = "§e§l" + timeLeft;
                player.sendActionBar(actionMessage + " " + countdownText);

                // Play tick sound
                player.playSound(player.getLocation(), plugin.getConfigManager().getSound("countdown"), 0.5f, 1.5f);

                timeLeft--;
                attempt.decrementTime();
            }
        }.runTaskTimer(plugin, 0L, 20L); // Run every second

        // Store the interaction attempt
        InteractionAttempt attempt = new InteractionAttempt(player, type, targetBlock.getLocation(), flagTeam, countdownTask);
        activeAttempts.put(player.getUniqueId(), attempt);

        // Send initial message
        player.sendActionBar(actionMessage + " §e§l3");
    }

    /**
     * Cancel an interaction attempt
     */
    private void cancelInteraction(UUID playerId, boolean showMessage) {
        InteractionAttempt attempt = activeAttempts.remove(playerId);
        if (attempt != null) {
            attempt.getCountdownTask().cancel();

            if (showMessage && attempt.getPlayer().isOnline()) {
                attempt.getPlayer().sendActionBar("§c§lInteraction cancelled!");
            }
        }
    }

    /**
     * Handle player movement to cancel interaction if they move too far
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        InteractionAttempt attempt = activeAttempts.get(player.getUniqueId());

        if (attempt != null) {
            // Check if player moved too far from target
            if (player.getLocation().distance(attempt.getTargetLocation()) > 3.0) {
                cancelInteraction(player.getUniqueId(), true);
            }
        }
    }

    /**
     * Handle player quit to clean up interaction attempts
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cancelInteraction(event.getPlayer().getUniqueId(), false);
    }

    /**
     * Handle capture point setup when admin right-clicks a block
     */
    private void handleCapturePointSetup(PlayerInteractEvent event, Player player, Block block) {
        ArenaManager.SetupSession session = (ArenaManager.SetupSession) plugin.getArenaManager().getPlayerSetupSession(player);

        if (session == null || session.mode != ArenaManager.SetupMode.CAPTURE_POINT_PLACEMENT) {
            return;
        }

        // Cancel the interaction to prevent any side effects
        event.setCancelled(true);

        // Handle capture point placement
        boolean success = plugin.getArenaManager().handleCapturePointPlacement(player, block.getLocation());

        if (success) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("team_name", session.teamColor.getName());
            placeholders.put("team_color", session.teamColor.getColorCode());
            placeholders.put("arena", session.arenaName);
            player.sendMessage(plugin.getConfigManager().getMessage("setup-capture-set", placeholders));
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("error-generic"));
        }
    }

    /**
     * Clean up interaction attempts
     */
    public void cleanup() {
        for (InteractionAttempt attempt : activeAttempts.values()) {
            attempt.getCountdownTask().cancel();
        }
        activeAttempts.clear();
    }
}