package org.cwresports.ctfcore;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.plugin.java.JavaPlugin;
import org.cwresports.ctfcore.commands.CTFAdminCommand;
import org.cwresports.ctfcore.commands.CTFPlayerCommand;
import org.cwresports.ctfcore.listeners.*;
import org.cwresports.ctfcore.listeners.PlayerDamageListener;
import org.cwresports.ctfcore.managers.*;
import org.cwresports.ctfcore.placeholders.CTFPlaceholderExpansion;

/**
 * Main plugin class for CTF-Core
 * Complete Capture the Flag gamemode for Paper 1.21+
 *
 * @author CWReSports
 * @version 1.0.0
 */
public class CTFCore extends JavaPlugin {

    private static CTFCore instance;

    // Managers
    private ConfigManager configManager;
    private ArenaManager arenaManager;
    private GameManager gameManager;
    private PlayerDataManager playerDataManager;

    private WorldGuardManager worldGuardManager;
    private ScoreboardManager scoreboardManager;
    private LobbyManager lobbyManager;
    private ServerLobbyManager serverLobbyManager;
    private MessageManager messageManager;
    private PowerUpManager powerUpManager;
    private SpectatorManager spectatorManager;
    private ChatManager chatManager;
    private AdminToolManager adminToolManager;
    private CurrencyManager currencyManager;
    private TabListManager tabListManager;
    private HologramLeaderboardManager hologramLeaderboardManager;

    // PlaceholderAPI integration
    private boolean placeholderAPIEnabled = false;

    @Override
    public void onEnable() {
        instance = this;

        // Check dependencies
        if (!checkDependencies()) {
            getLogger().severe("Required dependencies not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Check for PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderAPIEnabled = true;
            getLogger().info("PlaceholderAPI found! Enabling placeholder support.");
        }

        // Initialize managers
        initializeManagers();

        // Register commands
        registerCommands();

        // Register event listeners
        registerListeners();

        // Register PlaceholderAPI expansion
        if (placeholderAPIEnabled) {
            new CTFPlaceholderExpansion(this).register();
            getLogger().info("CTF PlaceholderAPI expansion registered!");
        }

        getLogger().info("CTF-Core has been enabled successfully!");
        getLogger().info("Version: " + getDescription().getVersion());
        getLogger().info("Loaded " + arenaManager.getEnabledArenas().size() + " enabled arenas.");

        // Log currency system status
        if (currencyManager.isEnabled()) {
            getLogger().info("Currency system enabled with Vault integration.");
        } else {
            getLogger().warning("Currency system disabled - Vault not found or no economy plugin.");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down CTF-Core...");

        // ENHANCED: End all games gracefully to prevent issues on restart
        if (gameManager != null) {
            try {
                getLogger().info("Ending all active games...");
                gameManager.endAllGames();
                gameManager.cleanup(); // Add cleanup method
            } catch (Exception e) {
                getLogger().warning("Could not end games properly: " + e.getMessage());
            }
        }

        // Save all player data before shutdown
        if (playerDataManager != null && gameManager != null) {
            try {
                getLogger().info("Saving player data...");
                for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                    org.cwresports.ctfcore.models.CTFPlayer ctfPlayer = gameManager.getCTFPlayer(player);
                    if (ctfPlayer != null) {
                        playerDataManager.savePlayerData(ctfPlayer);
                    }
                }
            } catch (Exception e) {
                getLogger().warning("Could not save player data: " + e.getMessage());
            }
        }

        // ENHANCED: Save arenas and clean up properly
        if (arenaManager != null) {
            try {
                getLogger().info("Saving arena configurations...");
                arenaManager.saveArenas();
            } catch (Exception e) {
                getLogger().warning("Could not save arenas on disable: " + e.getMessage());
            }
        }

        // Clear temporary setup modes
        if (arenaManager != null) {
            arenaManager.clearAllSetupModes();
        }

        // Shutdown scoreboard manager
        if (scoreboardManager != null) {
            try {
                scoreboardManager.shutdown();
            } catch (Exception e) {
                getLogger().warning("Could not shutdown scoreboard manager: " + e.getMessage());
            }
        }

        // Shutdown message manager
        if (messageManager != null) {
            try {
                messageManager.shutdown();
            } catch (Exception e) {
                getLogger().warning("Could not shutdown message manager: " + e.getMessage());
            }
        }

        // ENHANCED: Shutdown power-up manager
        if (powerUpManager != null) {
            try {
                powerUpManager.cleanup();
            } catch (Exception e) {
                getLogger().warning("Could not shutdown power-up manager: " + e.getMessage());
            }
        }

        // ENHANCED: Shutdown tab list manager
        if (tabListManager != null) {
            try {
                tabListManager.shutdown();
            } catch (Exception e) {
                getLogger().warning("Could not shutdown tab list manager: " + e.getMessage());
            }
        }

        // ENHANCED: Clear all player data to prevent memory leaks
        try {
            getLogger().info("Cleaning up player data...");
            for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                // Reset player inventories and states
                player.getInventory().clear();
                player.setHealth(20.0);
                player.setFoodLevel(20);
                player.setFireTicks(0);
                player.getActivePotionEffects().forEach(effect ->
                        player.removePotionEffect(effect.getType()));
            }
        } catch (Exception e) {
            getLogger().warning("Could not clean up player data: " + e.getMessage());
        }

        getLogger().info("CTF-Core has been disabled successfully.");
    }

    /**
     * Check if required dependencies are present
     */
    private boolean checkDependencies() {
        if (getServer().getPluginManager().getPlugin("WorldGuard") == null) {
            getLogger().severe("WorldGuard not found! This plugin requires WorldGuard to function.");
            return false;
        }

        if (getServer().getPluginManager().getPlugin("WorldEdit") == null) {
            getLogger().severe("WorldEdit not found! This plugin requires WorldEdit to function.");
            return false;
        }

        return true;
    }

    /**
     * Initialize all managers in proper order
     */
    private void initializeManagers() {
        // First, initialize config manager and load configurations
        configManager = new ConfigManager(this);
        configManager.loadAll();

        // Then initialize other managers that don't depend on config
        worldGuardManager = new WorldGuardManager();
        playerDataManager = new PlayerDataManager(this);
        currencyManager = new CurrencyManager(this);

        // Initialize managers that depend on config
        arenaManager = new ArenaManager(this);
        gameManager = new GameManager(this);
        scoreboardManager = new ScoreboardManager(this);
        lobbyManager = new LobbyManager(this);
        serverLobbyManager = new ServerLobbyManager(this);
        messageManager = new MessageManager(this);
        powerUpManager = new PowerUpManager(this);
        spectatorManager = new SpectatorManager(this);
        chatManager = new ChatManager(this);
        adminToolManager = new AdminToolManager(this);
        tabListManager = new TabListManager(this);
        hologramLeaderboardManager = new HologramLeaderboardManager(this);

        // Load data that depends on config
        arenaManager.loadArenas();

        getLogger().info("All managers initialized successfully.");
    }

    /**
     * Register plugin commands
     */
    private void registerCommands() {
        getCommand("ctfadmin").setExecutor(new CTFAdminCommand(this));
        getCommand("ctf").setExecutor(new CTFPlayerCommand(this));

        getLogger().info("Commands registered successfully.");
    }

    /**
     * Register event listeners
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerRespawnListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        getLogger().info("Event listeners registered successfully.");
    }

    /**
     * Reload plugin configuration and data
     */
    public void reload() {
        // End all active games before reloading
        gameManager.endAllGames();

        // Reload configurations
        configManager.loadAll();

        // Clear and reload arenas
        arenaManager.clearAllSetupModes();
        arenaManager.loadArenas();

        getLogger().info("Plugin reloaded successfully.");
    }

    /**
     * Process placeholders if PlaceholderAPI is available
     * Converts {placeholder} format to %placeholder% format for PlaceholderAPI compatibility
     */
    public String processPlaceholders(org.bukkit.entity.Player player, String text) {
        if (placeholderAPIEnabled && player != null && text != null) {
            // Convert {placeholder} format to %placeholder% format for PlaceholderAPI
            String convertedText = convertPlaceholderFormat(text);
            return PlaceholderAPI.setPlaceholders(player, convertedText);
        }
        return text;
    }

    /**
     * Convert {placeholder} format to %placeholder% format for PlaceholderAPI compatibility
     */
    private String convertPlaceholderFormat(String text) {
        if (text == null) return "";

        // Convert common PlaceholderAPI placeholders from {format} to %format%
        text = text.replaceAll("\\{(luckperms_[^}]+)\\}", "%$1%");
        text = text.replaceAll("\\{(vault_[^}]+)\\}", "%$1%");
        text = text.replaceAll("\\{(player_[^}]+)\\}", "%$1%");
        text = text.replaceAll("\\{(server_[^}]+)\\}", "%$1%");

        // CTF placeholders should remain in {format} for our own expansion
        // text = text.replaceAll("\\{(ctf_[^}]+)\\}", "%$1%");

        return text;
    }

    // Getters for managers
    public static CTFCore getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public WorldGuardManager getWorldGuardManager() {
        return worldGuardManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public LobbyManager getLobbyManager() {
        return lobbyManager;
    }

    public ServerLobbyManager getServerLobbyManager() {
        return serverLobbyManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public PowerUpManager getPowerUpManager() {
        return powerUpManager;
    }

    public SpectatorManager getSpectatorManager() {
        return spectatorManager;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public AdminToolManager getAdminToolManager() {
        return adminToolManager;
    }

    public CurrencyManager getCurrencyManager() {
        return currencyManager;
    }

    public TabListManager getTabListManager() {
        return tabListManager;
    }

    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }
}