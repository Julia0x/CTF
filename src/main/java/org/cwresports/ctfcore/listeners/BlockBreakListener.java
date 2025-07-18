package org.cwresports.ctfcore.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.managers.ArenaManager;
import org.cwresports.ctfcore.models.Arena;
import org.cwresports.ctfcore.models.CTFPlayer;
import org.cwresports.ctfcore.models.GameState;

import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced block break listener with comprehensive tracking and flag interaction
 * Handles block breaking during CTF games with proper tracking and restrictions
 */
public class BlockBreakListener implements Listener {

    private final CTFCore plugin;

    public BlockBreakListener(CTFCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Check if player is in flag setup mode (admin functionality)
        if (plugin.getArenaManager().isPlayerInSetupMode(player)) {
            handleFlagSetup(event, player, block);
            return;
        }

        // Check if player is in a CTF game
        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(player);
        if (ctfPlayer == null || !ctfPlayer.isInGame()) {
            return; // Not in CTF game, allow normal behavior
        }

        // Handle CTF game block breaking
        handleCTFGameBlockBreaking(event, player, block, ctfPlayer);
    }

    /**
     * Handle flag setup when admin breaks a banner
     */
    private void handleFlagSetup(BlockBreakEvent event, Player player, Block block) {
        ArenaManager.SetupSession session = (ArenaManager.SetupSession) plugin.getArenaManager().getPlayerSetupSession(player);

        if (session == null || session.mode != ArenaManager.SetupMode.FLAG_PLACEMENT) {
            return;
        }

        // Check if block is a banner
        if (!block.getType().name().contains("BANNER")) {
            String message = plugin.getMessageManager().processMessage("&c⛔ You must break a banner to set the flag location!");
            player.sendMessage(message);
            event.setCancelled(true);
            return;
        }

        // Check if banner color matches team color (optional validation)
        Material expectedBanner = switch (session.teamColor) {
            case RED -> Material.RED_BANNER;
            case BLUE -> Material.BLUE_BANNER;
        };

        if (block.getType() != expectedBanner) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("team_name", session.teamColor.getName());
            placeholders.put("team_color", session.teamColor.getColorCode());

            String message = plugin.getMessageManager().processMessage(
                    plugin.getConfigManager().getMessage("setup-wrong-banner-color", placeholders)
            );
            player.sendMessage(message);
            event.setCancelled(true);
            return;
        }

        // Cancel the block break (we don't want to actually destroy it)
        event.setCancelled(true);

        // Handle flag placement
        boolean success = plugin.getArenaManager().handleFlagPlacement(player, block.getLocation());

        if (success) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("team_name", session.teamColor.getName());
            placeholders.put("team_color", session.teamColor.getColorCode());
            placeholders.put("arena", session.arenaName);

            String message = plugin.getMessageManager().processMessage(
                    plugin.getConfigManager().getMessage("setup-flag-set", placeholders)
            );
            player.sendMessage(message);
        } else {
            String message = plugin.getMessageManager().processMessage("&c❌ Failed to set flag location!");
            player.sendMessage(message);
        }
    }

    /**
     * Handle block breaking during CTF games
     */
    private void handleCTFGameBlockBreaking(BlockBreakEvent event, Player player, Block block, CTFPlayer ctfPlayer) {
        var game = ctfPlayer.getGame();
        if (game == null) {
            return;
        }

        Arena arena = game.getArena();

        // Check if block is within the arena region
        if (!plugin.getWorldGuardManager().isLocationInRegion(block.getLocation(), arena.getWorldGuardRegion())) {
            return; // Outside arena, let WorldGuard handle it
        }

        // Handle banner interactions for flags
        if (block.getType().name().contains("BANNER")) {
            handleFlagInteraction(event, player, ctfPlayer, game, block);
            // Don't cancel the event yet - let flag interaction decide
            return;
        }

        // During lobby/waiting phase, prevent all non-flag block breaking
        if (game.getState() != GameState.PLAYING) {
            event.setCancelled(true);
            String message = plugin.getMessageManager().processMessage("&c⛔ You cannot break blocks while the game is not active!");
            player.sendMessage(message);
            return;
        }

        // During gameplay, allow block breaking but track it
        if (game.getState() == GameState.PLAYING) {
            // Track the block breaking for cleanup later
            plugin.getBlockTrackingManager().trackBrokenBlock(player, block, arena);

            // Log the block breaking
            plugin.getLogger().info("Player " + player.getName() + " broke " + block.getType().name() +
                    " at " + block.getLocation().getBlockX() + "," +
                    block.getLocation().getBlockY() + "," +
                    block.getLocation().getBlockZ() + " in arena " + arena.getName());
        }
    }

    /**
     * Handle flag taking when player breaks flag banner
     */
    private void handleFlagInteraction(BlockBreakEvent event, Player player, CTFPlayer ctfPlayer,
                                       org.cwresports.ctfcore.models.CTFGame game, Block block) {

        if (ctfPlayer.getTeam() == null) {
            return;
        }

        Arena arena = game.getArena();

        // Check each team's flag location
        for (Arena.TeamColor teamColor : Arena.TeamColor.values()) {
            Arena.Team teamData = arena.getTeam(teamColor);

            if (teamData.getFlagLocation() != null &&
                    teamData.getFlagLocation().getBlock().equals(block)) {

                // This is a team's flag - cancel the block break
                event.setCancelled(true);

                // Handle flag interaction
                if (teamColor == ctfPlayer.getTeam()) {
                    // Player trying to take own team's flag
                    String message = plugin.getMessageManager().processMessage("&c⛔ You cannot take your own team's flag!");
                    player.sendMessage(message);
                    return;
                }

                // Try to take enemy flag
                var flag = game.getFlag(teamColor);
                if (flag != null && flag.isAtBase()) {
                    if (game.takeFlag(ctfPlayer, teamColor)) {
                        // Successfully took flag
                        player.playSound(player.getLocation(),
                                plugin.getConfigManager().getSound("flag_taken"), 1.0f, 1.0f);

                        // Show capture hint message
                        String message = plugin.getMessageManager().processMessage("&a⚡ Flag taken! Bring it to your capture point!");
                        player.sendMessage(message);
                    } else {
                        // Could not take flag (maybe need to return own flag first)
                        String message = plugin.getMessageManager().processMessage("&c⛔ You must return your team's flag before taking the enemy flag!");
                        player.sendMessage(message);
                    }
                } else {
                    String message = plugin.getMessageManager().processMessage("&c⛔ The flag is not at its base or is already taken!");
                    player.sendMessage(message);
                }

                return;
            }
        }
    }
}