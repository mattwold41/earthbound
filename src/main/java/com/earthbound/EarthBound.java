package com.earthbound;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class EarthBound extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("EarthBound is now online!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!command.getName().equalsIgnoreCase("earth")) {
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage("EarthBound - Real Earth Simulation");
            sender.sendMessage("/earth locate");
            return true;
        }

        if (args[0].equalsIgnoreCase("locate")) {

            if (!(sender instanceof Player player)) {
                sender.sendMessage("Players only.");
                return true;
            }

            player.sendMessage("EARTHBOUND");
            player.sendMessage("X: " + player.getLocation().getX());
            player.sendMessage("Y: " + player.getLocation().getY());
            player.sendMessage("Z: " + player.getLocation().getZ());
            player.sendMessage("Real-world location: Coming soon");

            return true;
        }

        sender.sendMessage("Unknown EarthBound command.");
        return true;
    }
}
