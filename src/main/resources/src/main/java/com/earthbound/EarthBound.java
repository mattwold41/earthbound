package com.earthbound;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class EarthBound extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("EarthBound is now online!");

        registerCommand("earth", (CommandSourceStack source, String[] args) -> {

            if (args.length == 0) {
                source.getSender().sendPlainMessage("EarthBound - Real Earth Simulation");
                source.getSender().sendPlainMessage("/earth locate - Show your EarthBound location");
                return;
            }

            if (args[0].equalsIgnoreCase("locate")) {

                if (!(source.getSender() instanceof Player player)) {
                    source.getSender().sendPlainMessage(
                            "This command can only be used by a player."
                    );
                    return;
                }

                double x = player.getLocation().getX();
                double y = player.getLocation().getY();
                double z = player.getLocation().getZ();

                player.sendPlainMessage("EARTHBOUND");
                player.sendPlainMessage("Minecraft Coordinates:");
                player.sendPlainMessage("X: " + String.format("%.2f", x));
                player.sendPlainMessage("Y: " + String.format("%.2f", y));
                player.sendPlainMessage("Z: " + String.format("%.2f", z));
                player.sendPlainMessage("Earth coordinates: Coming soon");
                player.sendPlainMessage("Real-world location: Coming soon");

                return;
            }

            source.getSender().sendPlainMessage("Unknown EarthBound command.");
        });
    }
}
