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
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args) {

        if (!command.getName().equalsIgnoreCase("earth")) {
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage("§6EarthBound §7- Real Earth Simulation");
            sender.sendMessage("§e/earth locate §7- Show your EarthBound location");
            return true;
        }

        if (args[0].equalsIgnoreCase("locate")) {

            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used by a player.");
                return true;
            }

            double x = player.getLocation().getX();
            double y = player.getLocation().getY();
            double z = player.getLocation().getZ();

            player.sendMessage("§6§lEARTHBOUND");
            player.sendMessage("§7Minecraft Coordinates:");
            player.sendMessage("§fX: §e" + String.format("%.2f", x));
            player.sendMessage("§fY: §e" + String.format("%.2f", y));
            player.sendMessage("§fZ: §e" + String.format("%.2f", z));

            player.sendMessage("§7Earth coordinates: §cComing soon");
            player.sendMessage("§7Real-world location: §cComing soon");

            return true;
        }

        sender.sendMessage("§cUnknown EarthBound command.");
        return true;
