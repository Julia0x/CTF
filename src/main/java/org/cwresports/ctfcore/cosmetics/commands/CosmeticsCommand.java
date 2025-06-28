package org.cwresports.ctfcore.cosmetics.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.cosmetics.guis.BattlePassGUI;
import org.cwresports.ctfcore.cosmetics.guis.CosmeticsGUI;

/**
 * Simple command handler for cosmetics and battle pass
 */
public class CosmeticsCommand implements CommandExecutor {

    private final CTFCore plugin;
    private final CosmeticsGUI cosmeticsGUI;
    private final BattlePassGUI battlePassGUI;

    public CosmeticsCommand(CTFCore plugin) {
        this.plugin = plugin;
        this.cosmeticsGUI = new CosmeticsGUI(plugin);
        this.battlePassGUI = new BattlePassGUI(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        // Check if it's a battle pass command
        if (label.equalsIgnoreCase("battlepass") || label.equalsIgnoreCase("bp")) {
            battlePassGUI.openBattlePass(player);
            return true;
        }

        // Default: open cosmetics menu
        cosmeticsGUI.openCosmeticsMenu(player);
        return true;
    }
}