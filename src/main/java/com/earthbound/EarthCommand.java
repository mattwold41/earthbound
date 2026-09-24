package com.earthbound;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EarthCommand implements CommandExecutor {

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is for players only.");
            return true;
        }

        Player player = (Player) sender;

        int x = player.getLocation().getBlockX();
        int z = player.getLocation().getBlockZ();

        double latitude = EarthCoordinates.getLatitude(x, z);
        double longitude = EarthCoordinates.getLongitude(x, z);

        player.sendMessage(
                ChatColor.GOLD + "EarthBound Coordinates"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Latitude: " +
                ChatColor.WHITE +
                String.format("%.5f", latitude)
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Longitude: " +
                ChatColor.WHITE +
                String.format("%.5f", longitude)
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Minecraft: " +
                ChatColor.WHITE +
                "X=" + x + " Z=" + z
        );

        return true;
    }
}
