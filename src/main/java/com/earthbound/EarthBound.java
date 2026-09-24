package com.earthbound;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class EarthBound extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("EarthBound is now online!");

        registerCommand("earth", (sender, args) -> {

            if (args.length == 0) {
                sender.sendMessage("EarthBound - Real Earth Simulation");
                sender.sendMessage("/earth locate - Show your EarthBound location");
                return;
            }

            if (args[0].equalsIgnoreCase("locate")) {

                if (!(sender instanceof Player player)) {
                    sender.sendMessage("This command can only be used by a player.");
                    return;
                }

                double x = player.getLocation().getX();
                double y = player.getLocation().getY();
                double z = player.getLocation().getZ();

                player.sendMessage("EARTHBOUND");
                player.sendMessage("Minecraft Coordinates:");
                player.sendMessage("X: " + String.format("%.2f", x));
                player.sendMessage("Y: " + String.format("%.2f", y));
                player.sendMessage("Z: " + String.format("%.2f", z));
                player.sendMessage("Earth coordinates: Coming soon");
                player.sendMessage("Real-world location: Coming soon");

                return;
            }

            sender.sendMessage("Unknown EarthBound command.");
        });
    }
}
