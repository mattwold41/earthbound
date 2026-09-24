package com.earthbound;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class EarthBound extends JavaPlugin implements CommandExecutor {

    @Override
    public void onEnable() {

        getLogger().info("EarthBound is now online!");

        if (getCommand("earth") != null) {
            getCommand("earth").setExecutor(this);
        }

        getServer()
                .getPluginManager()
                .registerEvents(
                        new EarthGenerator(this),
                        this
                );

        getLogger().info("Earth generator enabled!");
    }


    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args) {


        if (!command.getName()
                .equalsIgnoreCase("earth")) {

            return false;
        }


        if (args.length == 0) {

            sender.sendMessage("§6§lEarthBound");
            sender.sendMessage("§7Real Earth Simulation");
            sender.sendMessage("§e/earth locate");

            return true;
        }


        if (args[0]
                .equalsIgnoreCase("locate")) {


            if (!(sender instanceof Player player)) {

                sender.sendMessage("Players only.");
                return true;
            }


            double x =
                    player.getLocation().getX();

            double y =
                    player.getLocation().getY();

            double z =
                    player.getLocation().getZ();


            double latitude =
                    EarthCoordinates.getLatitude(x, z);

            double longitude =
                    EarthCoordinates.getLongitude(x, z);


            player.sendMessage("§6§lEARTHBOUND");

            player.sendMessage("§7Minecraft Coordinates:");

            player.sendMessage(
                    "§fX: §e" +
                    String.format("%.2f", x)
            );

            player.sendMessage(
                    "§fY: §e" +
                    String.format("%.2f", y)
            );

            player.sendMessage(
                    "§fZ: §e" +
                    String.format("%.2f", z)
            );


            player.sendMessage("§7Earth Coordinates:");

            player.sendMessage(
                    "§fLatitude: §a" +
                    String.format("%.5f", latitude)
            );

            player.sendMessage(
                    "§fLongitude: §a" +
                    String.format("%.5f", longitude)
            );


            if (EarthLocation.isNearGuemes(latitude, longitude)) {

                player.sendMessage(
                        "§7Region: §aGuemes Island Test Area"
                );

            } else {

                player.sendMessage(
                        "§7Region: §7Outside test area"
                );
            }


            return true;
        }


        sender.sendMessage(
                "§cUnknown EarthBound command."
        );

        return true;
    }
}
