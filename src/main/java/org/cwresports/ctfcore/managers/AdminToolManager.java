package org.cwresports.ctfcore.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.Arena;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages admin tools for arena creation and setup with modern dashboard and FIXED team colors
 */
public class AdminToolManager {

    private final CTFCore plugin;
    private final Map<UUID, String> playerArenaSetup; // Track which arena player is setting up
    private final Map<UUID, Arena.TeamColor> playerTeamSetup; // Track which team player is setting up
    private final Map<UUID, String> openGUITitles; // Track GUI titles for players

    public AdminToolManager(CTFCore plugin) {
        this.plugin = plugin;
        this.playerArenaSetup = new HashMap<>();
        this.playerTeamSetup = new HashMap<>();
        this.openGUITitles = new HashMap<>();
    }

    /**
     * Give admin toolkit to player with modern layout
     */
    public void giveAdminToolkit(Player player, String arenaName) {
        player.getInventory().clear();

        // Give 4 red wool and 4 blue wool for spawn points
        ItemStack redWool = new ItemStack(Material.RED_WOOL, 4);
        ItemMeta redMeta = redWool.getItemMeta();
        redMeta.setDisplayName("§c§lRed Team Spawn Tool");
        redMeta.setLore(Arrays.asList(
                "§7Right-click to set red team spawn points",
                "§7Arena: §e" + arenaName,
                "§7§lYou have 4 spawn points to set",
                "§7Each use sets one spawn point"
        ));
        redWool.setItemMeta(redMeta);

        ItemStack blueWool = new ItemStack(Material.BLUE_WOOL, 4);
        ItemMeta blueMeta = blueWool.getItemMeta();
        blueMeta.setDisplayName("§9§lBlue Team Spawn Tool");
        blueMeta.setLore(Arrays.asList(
                "§7Right-click to set blue team spawn points",
                "§7Arena: §e" + arenaName,
                "§7§lYou have 4 spawn points to set",
                "§7Each use sets one spawn point"
        ));
        blueWool.setItemMeta(blueMeta);

        // Flag setup tool
        ItemStack flagTool = new ItemStack(Material.WHITE_BANNER);
        ItemMeta flagMeta = flagTool.getItemMeta();
        flagMeta.setDisplayName("§e§lFlag Setup Tool");
        flagMeta.setLore(Arrays.asList(
                "§7Right-click to open flag setup menu",
                "§7Arena: §e" + arenaName,
                "§7Set flag locations for both teams"
        ));
        flagTool.setItemMeta(flagMeta);

        // Capture point tool
        ItemStack captureTool = new ItemStack(Material.BEACON);
        ItemMeta captureMeta = captureTool.getItemMeta();
        captureMeta.setDisplayName("§6§lCapture Point Tool");
        captureMeta.setLore(Arrays.asList(
                "§7Right-click to open capture point menu",
                "§7Arena: §e" + arenaName,
                "§7Set capture points for both teams"
        ));
        captureTool.setItemMeta(captureMeta);

        // Powerup spawn tool
        ItemStack powerupTool = new ItemStack(Material.NETHER_STAR);
        ItemMeta powerupMeta = powerupTool.getItemMeta();
        powerupMeta.setDisplayName("§d§lPowerup Spawn Tool");
        powerupMeta.setLore(Arrays.asList(
                "§7Right-click to set powerup spawn points",
                "§7Arena: §e" + arenaName,
                "§7Set locations where powerups will spawn"
        ));
        powerupTool.setItemMeta(powerupMeta);

        // Lobby spawn tool
        ItemStack lobbyTool = new ItemStack(Material.EMERALD);
        ItemMeta lobbyMeta = lobbyTool.getItemMeta();
        lobbyMeta.setDisplayName("§a§lLobby Spawn Tool");
        lobbyMeta.setLore(Arrays.asList(
                "§7Right-click to set lobby spawn",
                "§7Arena: §e" + arenaName,
                "§7Where players wait before game starts"
        ));
        lobbyTool.setItemMeta(lobbyMeta);

        // Dashboard tool - NEW
        ItemStack dashboardTool = new ItemStack(Material.COMPASS);
        ItemMeta dashboardMeta = dashboardTool.getItemMeta();
        dashboardMeta.setDisplayName("§b§lArena Dashboard");
        dashboardMeta.setLore(Arrays.asList(
                "§7Right-click to open arena dashboard",
                "§7Arena: §e" + arenaName,
                "§7View setup progress and manage arena"
        ));
        dashboardTool.setItemMeta(dashboardMeta);

        // Save arena tool
        ItemStack saveTool = new ItemStack(Material.DIAMOND);
        ItemMeta saveMeta = saveTool.getItemMeta();
        saveMeta.setDisplayName("§b§lSave Arena Tool");
        saveMeta.setLore(Arrays.asList(
                "§7Right-click to validate and save arena",
                "§7Arena: §e" + arenaName,
                "§7Enables the arena for gameplay"
        ));
        saveTool.setItemMeta(saveMeta);

        // Set items in hotbar
        player.getInventory().setItem(0, redWool);
        player.getInventory().setItem(1, blueWool);
        player.getInventory().setItem(2, flagTool);
        player.getInventory().setItem(3, captureTool);
        player.getInventory().setItem(4, powerupTool);
        player.getInventory().setItem(5, lobbyTool);
        player.getInventory().setItem(7, dashboardTool);
        player.getInventory().setItem(8, saveTool);

        // Track arena setup
        playerArenaSetup.put(player.getUniqueId(), arenaName);

        player.sendMessage("§a§l⚡ ADMIN TOOLKIT ACTIVATED!");
        player.sendMessage("§eUse the tools in your hotbar to set up arena: §b" + arenaName);
        player.sendMessage("§7Use the §bCompass §7to open the arena dashboard");
    }

    /**
     * Handle admin tool interaction
     */
    public boolean handleToolInteraction(Player player, ItemStack item) {
        if (!player.hasPermission("ctf.admin")) {
            return false;
        }

        String arenaName = playerArenaSetup.get(player.getUniqueId());
        if (arenaName == null) {
            return false;
        }

        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            return false;
        }

        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }

        String displayName = item.getItemMeta().getDisplayName();

        switch (displayName) {
            case "§c§lRed Team Spawn Tool":
                handleSpawnTool(player, arena, Arena.TeamColor.RED, item);
                return true;
            case "§9§lBlue Team Spawn Tool":
                handleSpawnTool(player, arena, Arena.TeamColor.BLUE, item);
                return true;
            case "§e§lFlag Setup Tool":
                openFlagSetupMenu(player, arena);
                return true;
            case "§6§lCapture Point Tool":
                openCaptureSetupMenu(player, arena);
                return true;
            case "§d§lPowerup Spawn Tool":
                handlePowerupTool(player, arena);
                return true;
            case "§a§lLobby Spawn Tool":
                handleLobbyTool(player, arena);
                return true;
            case "§b§lArena Dashboard":
                openArenaDashboard(player, arena);
                return true;
            case "§b§lSave Arena Tool":
                handleSaveTool(player, arena);
                return true;
        }

        return false;
    }

    /**
     * Open modern arena dashboard - COMPLETELY REDESIGNED with FIXED team colors (BedWars1058 style)
     */
    private void openArenaDashboard(Player player, Arena arena) {
        String title = "§7Arena: §e" + arena.getName();
        Inventory gui = Bukkit.createInventory(null, 45, title); // Smaller 45-slot GUI
        openGUITitles.put(player.getUniqueId(), title);

        Map<String, Object> status = arena.getConfigurationStatus();

        // Header decoration (reduced)
        ItemStack headerItem = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta headerMeta = headerItem.getItemMeta();
        headerMeta.setDisplayName("§b§l" + arena.getName());
        headerItem.setItemMeta(headerMeta);

        for (int i = 0; i < 9; i++) {
            gui.setItem(i, headerItem);
        }

        // Arena info (compact)
        ItemStack arenaInfo = new ItemStack(Material.PAPER);
        ItemMeta arenaInfoMeta = arenaInfo.getItemMeta();
        arenaInfoMeta.setDisplayName("§e§lArena Info");
        arenaInfoMeta.setLore(Arrays.asList(
                "§7Name: §f" + arena.getName(),
                "§7World: §f" + status.get("world"),
                "§7Region: §f" + status.get("region"),
                "§7Status: " + ((Boolean) status.get("enabled") ? "§aEnabled" : "§cDisabled"),
                "",
                "§7Progress: " + getCompactProgressBar(arena)
        ));
        arenaInfo.setItemMeta(arenaInfoMeta);
        gui.setItem(13, arenaInfo);

        // Lobby setup (compact)
        boolean lobbyComplete = (Boolean) status.get("lobby_complete");
        ItemStack lobbyItem = new ItemStack(lobbyComplete ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK);
        ItemMeta lobbyMeta = lobbyItem.getItemMeta();
        lobbyMeta.setDisplayName("§a§lLobby " + (lobbyComplete ? "§a✓" : "§c✗"));
        lobbyMeta.setLore(Arrays.asList(
                "§7Where players wait",
                lobbyComplete ? "§a§lREADY" : "§c§lNEEDS SETUP"
        ));
        lobbyItem.setItemMeta(lobbyMeta);
        gui.setItem(18, lobbyItem);

        // Team setups - FIXED COLORS (more compact)
        int[] teamSlots = {20, 24}; // Red and Blue team slots
        Arena.TeamColor[] teams = {Arena.TeamColor.RED, Arena.TeamColor.BLUE};

        for (int i = 0; i < teams.length; i++) {
            Arena.TeamColor team = teams[i];
            String teamKey = team.getName();

            boolean spawnsComplete = (Boolean) status.get(teamKey + "_spawns_complete");
            boolean flagComplete = (Boolean) status.get(teamKey + "_flag_complete");
            boolean captureComplete = (Boolean) status.get(teamKey + "_capture_complete");
            int spawnCount = (Integer) status.get(teamKey + "_spawns_count");

            boolean teamFullySetup = spawnsComplete && flagComplete && captureComplete;

            ItemStack teamItem = new ItemStack(teamFullySetup ? Material.LIME_CONCRETE :
                    (team == Arena.TeamColor.RED ? Material.RED_CONCRETE : Material.BLUE_CONCRETE));
            ItemMeta teamMeta = teamItem.getItemMeta();

            // FIXED: Properly apply team colors
            String teamDisplayName = ChatColor.translateAlternateColorCodes('&',
                    team.getColorCode() + "§l" + team.getName().toUpperCase() + " TEAM " +
                            (teamFullySetup ? "§a✓" : "§c✗"));
            teamMeta.setDisplayName(teamDisplayName);

            teamMeta.setLore(Arrays.asList(
                    "§7Spawns: " + (spawnsComplete ? "§a✓" : "§c" + spawnCount + "/4"),
                    "§7Flag: " + (flagComplete ? "§a✓" : "§c✗"),
                    "§7Capture: " + (captureComplete ? "§a✓" : "§c✗"),
                    "",
                    teamFullySetup ? "§a§lREADY" : "§c§lINCOMPLETE"
            ));
            teamItem.setItemMeta(teamMeta);
            gui.setItem(teamSlots[i], teamItem);
        }

        // Powerup spawns (compact)
        int powerupCount = arena.getPowerupSpawnPoints().size();
        ItemStack powerupItem = new ItemStack(powerupCount > 0 ? Material.NETHER_STAR : Material.BARRIER);
        ItemMeta powerupMeta = powerupItem.getItemMeta();
        powerupMeta.setDisplayName("§d§lPowerups " + (powerupCount > 0 ? "§a✓" : "§7(Optional)"));
        powerupMeta.setLore(Arrays.asList(
                "§7Count: §f" + powerupCount,
                powerupCount > 0 ? "§a§lCONFIGURED" : "§7§lOPTIONAL"
        ));
        powerupItem.setItemMeta(powerupMeta);
        gui.setItem(22, powerupItem);

        // Action buttons (compact)
        ItemStack saveButton = new ItemStack(arena.isFullyConfigured() ? Material.DIAMOND : Material.BARRIER);
        ItemMeta saveMeta = saveButton.getItemMeta();
        saveMeta.setDisplayName(arena.isFullyConfigured() ? "§a§lSAVE ARENA" : "§c§lCANNOT SAVE");
        saveMeta.setLore(Arrays.asList(
                arena.isFullyConfigured() ?
                        "§7Click to enable arena" :
                        "§7Complete setup first"
        ));
        saveButton.setItemMeta(saveMeta);
        gui.setItem(31, saveButton);

        // Close button
        ItemStack closeButton = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta closeMeta = closeButton.getItemMeta();
        closeMeta.setDisplayName("§c§lClose");
        closeButton.setItemMeta(closeMeta);
        gui.setItem(35, closeButton);

        // Bottom decoration (minimal)
        ItemStack bottomItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bottomMeta = bottomItem.getItemMeta();
        bottomMeta.setDisplayName("§7");
        bottomItem.setItemMeta(bottomMeta);

        for (int i = 36; i < 45; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, bottomItem);
            }
        }

        player.openInventory(gui);
    }

    /**
     * Get compact progress bar for arena setup (BedWars1058 style)
     */
    private String getCompactProgressBar(Arena arena) {
        Map<String, Object> status = arena.getConfigurationStatus();
        int totalTasks = 7; // 1 lobby + 2 teams (spawns + flag + capture each)
        int completedTasks = 0;

        // Count completed tasks (simplified)
        if ((Boolean) status.get("lobby_complete")) completedTasks++;
        if ((Boolean) status.get("red_spawns_complete")) completedTasks++;
        if ((Boolean) status.get("red_flag_complete")) completedTasks++;
        if ((Boolean) status.get("red_capture_complete")) completedTasks++;
        if ((Boolean) status.get("blue_spawns_complete")) completedTasks++;
        if ((Boolean) status.get("blue_flag_complete")) completedTasks++;
        if ((Boolean) status.get("blue_capture_complete")) completedTasks++;

        double progress = (double) completedTasks / totalTasks;
        int bars = (int) (progress * 10);

        StringBuilder progressBar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i < bars) {
                progressBar.append("§a●");
            } else {
                progressBar.append("§7●");
            }
        }

        return progressBar + " §f" + completedTasks + "/" + totalTasks;
    }

    /**
     * Handle spawn point tool with wool count limit
     */
    private void handleSpawnTool(Player player, Arena arena, Arena.TeamColor team, ItemStack item) {
        Arena.Team teamData = arena.getTeam(team);

        // Find next available spawn slot
        int nextSpawn = -1;
        for (int i = 0; i < 4; i++) {
            if (teamData.getSpawnPoint(i) == null) {
                nextSpawn = i + 1;
                break;
            }
        }

        if (nextSpawn == -1) {
            player.sendMessage("§c❌ All spawn points for " + team.getColorCode() + team.getName() + " §cteam are already set!");
            return;
        }

        // Set spawn point
        teamData.setSpawnPoint(nextSpawn - 1, player.getLocation());
        plugin.getArenaManager().saveAllArenas();

        // Remove one wool from the stack
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().removeItem(item);
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("arena", arena.getName());
        placeholders.put("team_name", team.getName());
        placeholders.put("team_color", team.getColorCode());
        placeholders.put("spawn_number", String.valueOf(nextSpawn));
        player.sendMessage(plugin.getConfigManager().getMessage("setup-spawn-set", placeholders));

        // Update admin scoreboard
        plugin.getScoreboardManager().updateAdminScoreboard(player, arena);
    }

    /**
     * Handle powerup spawn tool
     */
    private void handlePowerupTool(Player player, Arena arena) {
        arena.addPowerupSpawnPoint(player.getLocation());
        plugin.getArenaManager().saveArenas();
        player.sendMessage("§d✅ Powerup spawn point added at your location!");
        player.sendMessage("§7Total powerup spawns: §e" + arena.getPowerupSpawnPoints().size());

        // Update admin scoreboard
        plugin.getScoreboardManager().updateAdminScoreboard(player, arena);
    }

    /**
     * Open flag setup menu with simple title
     */
    private void openFlagSetupMenu(Player player, Arena arena) {
        String title = "TEAM";
        Inventory gui = Bukkit.createInventory(null, 9, title);

        // Track the GUI title for this player
        openGUITitles.put(player.getUniqueId(), title);

        // Red flag
        ItemStack redFlag = new ItemStack(Material.RED_BANNER);
        ItemMeta redMeta = redFlag.getItemMeta();
        redMeta.setDisplayName("§c§lSet Red Flag");
        redMeta.setLore(Arrays.asList(
                "§7Click to enter flag setup mode",
                "§7Then break a red banner to set flag location",
                "",
                "§e💡 Place a red banner first, then break it!"
        ));
        redFlag.setItemMeta(redMeta);

        // Blue flag
        ItemStack blueFlag = new ItemStack(Material.BLUE_BANNER);
        ItemMeta blueMeta = blueFlag.getItemMeta();
        blueMeta.setDisplayName("§9§lSet Blue Flag");
        blueMeta.setLore(Arrays.asList(
                "§7Click to enter flag setup mode",
                "§7Then break a blue banner to set flag location",
                "",
                "§e💡 Place a blue banner first, then break it!"
        ));
        blueFlag.setItemMeta(blueMeta);

        gui.setItem(3, redFlag);
        gui.setItem(5, blueFlag);

        player.openInventory(gui);
    }

    /**
     * Open capture point setup menu with simple title
     */
    private void openCaptureSetupMenu(Player player, Arena arena) {
        String title = "TEAM";
        Inventory gui = Bukkit.createInventory(null, 9, title);

        // Track the GUI title for this player
        openGUITitles.put(player.getUniqueId(), title);

        // Red capture
        ItemStack redCapture = new ItemStack(Material.RED_CONCRETE);
        ItemMeta redMeta = redCapture.getItemMeta();
        redMeta.setDisplayName("§c§lSet Red Capture Point");
        redMeta.setLore(Arrays.asList(
                "§7Click to enter capture setup mode",
                "§7Then right-click a block to set capture point",
                "",
                "§e💡 Right-click any solid block!"
        ));
        redCapture.setItemMeta(redMeta);

        // Blue capture
        ItemStack blueCapture = new ItemStack(Material.BLUE_CONCRETE);
        ItemMeta blueMeta = blueCapture.getItemMeta();
        blueMeta.setDisplayName("§9§lSet Blue Capture Point");
        blueMeta.setLore(Arrays.asList(
                "§7Click to enter capture setup mode",
                "§7Then right-click a block to set capture point",
                "",
                "§e💡 Right-click any solid block!"
        ));
        blueCapture.setItemMeta(blueMeta);

        gui.setItem(3, redCapture);
        gui.setItem(5, blueCapture);

        player.openInventory(gui);
    }

    /**
     * Handle lobby spawn tool
     */
    private void handleLobbyTool(Player player, Arena arena) {
        arena.setLobbySpawn(player.getLocation());
        plugin.getArenaManager().saveAllArenas();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("arena", arena.getName());
        player.sendMessage(plugin.getConfigManager().getMessage("setup-lobby-set", placeholders));

        // Update admin scoreboard
        plugin.getScoreboardManager().updateAdminScoreboard(player, arena);
    }

    /**
     * Handle save arena tool
     */
    private void handleSaveTool(Player player, Arena arena) {
        ArenaManager.ValidationResult result = plugin.getArenaManager().validateAndEnableArena(arena.getName());
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("arena", arena.getName());

        if (result.isSuccess()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aSaved " + arena.getName()));

            // Send title message
            Map<String, String> titlePlaceholders = new HashMap<>();
            titlePlaceholders.put("arena", arena.getName());
            plugin.getMessageManager().sendTitle(player, "arena-enabled", null, titlePlaceholders);

            // Clear toolkit and admin mode
            clearAdminSetup(player);
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("validation-failed", placeholders));
            player.sendMessage("§cError: " + result.getMessage());
        }
    }

    /**
     * Handle GUI click for flag/capture setup and dashboard
     */
    public boolean handleGUIClick(Player player, Inventory inventory, ItemStack clickedItem) {
        String title = openGUITitles.get(player.getUniqueId());

        if (title == null) {
            return false;
        }

        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return false;
        }

        String displayName = clickedItem.getItemMeta().getDisplayName();
        String arenaName = playerArenaSetup.get(player.getUniqueId());

        if (arenaName == null) {
            return false;
        }

        // Handle dashboard clicks
        if (title.startsWith("§7Arena: §e")) {
            if (displayName.equals("§c§lClose")) {
                player.closeInventory();
                return true;
            } else if (displayName.contains("SAVE ARENA")) {
                player.closeInventory();
                Arena arena = plugin.getArenaManager().getArena(arenaName);
                if (arena != null) {
                    handleSaveTool(player, arena);
                }
                return true;
            }
            return true; // Prevent other clicks in dashboard
        }

        // Handle team selection menus
        if (title.equals("TEAM")) {
            Arena.TeamColor team = null;
            if (displayName.equals("§c§lSet Red Flag") || displayName.equals("§c§lSet Red Capture Point")) {
                team = Arena.TeamColor.RED;
            } else if (displayName.equals("§9§lSet Blue Flag") || displayName.equals("§9§lSet Blue Capture Point")) {
                team = Arena.TeamColor.BLUE;
            }

            if (team != null) {
                player.closeInventory();
                openGUITitles.remove(player.getUniqueId()); // Clear GUI tracking

                if (displayName.contains("Flag")) {
                    plugin.getArenaManager().startFlagSetup(player, arenaName, team);

                    // Send additional instruction
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("team_color", team.getColorCode());
                    placeholders.put("team_name", team.getName());
                    player.sendMessage(plugin.getConfigManager().getMessage("setup-flag-instruction", placeholders));

                } else if (displayName.contains("Capture")) {
                    plugin.getArenaManager().startCaptureSetup(player, arenaName, team);

                    // Send additional instruction
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("team_color", team.getColorCode());
                    placeholders.put("team_name", team.getName());
                    player.sendMessage(plugin.getConfigManager().getMessage("setup-capture-instruction", placeholders));
                }
                return true;
            }
        }

        return false;
    }

    /**
     * Clear admin setup mode for player
     */
    public void clearAdminSetup(Player player) {
        playerArenaSetup.remove(player.getUniqueId());
        playerTeamSetup.remove(player.getUniqueId());
        openGUITitles.remove(player.getUniqueId());

        // Clear inventory
        player.getInventory().clear();

        // Clear admin scoreboard
        plugin.getScoreboardManager().clearPlayerScoreboard(player);
        plugin.getScoreboardManager().removeAdminViewingArena(player);

        player.sendMessage("§e⚠ Admin setup mode deactivated.");
    }

    /**
     * Check if player is in admin setup mode
     */
    public boolean isInSetupMode(Player player) {
        return playerArenaSetup.containsKey(player.getUniqueId());
    }

    /**
     * Get arena player is setting up
     */
    public String getSetupArena(Player player) {
        return playerArenaSetup.get(player.getUniqueId());
    }
}