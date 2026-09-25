package com.earthbound;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EarthLocation {


    public static void showLocation(
            CommandSender sender,
            Player player) {


        Location location =
                player.getLocation();


        double latitude =
                EarthCoordinates
                        .minecraftToLatitude(
                                location.getBlockZ()
                        );


        double longitude =
                EarthCoordinates
                        .minecraftToLongitude(
                                location.getBlockX()
                        );


        sender.sendMessage(
                "§6EarthBound Coordinates"
        );


        sender.sendMessage(
                "§eLatitude: §f"
                        + latitude
        );


        sender.sendMessage(
                "§eLongitude: §f"
                        + longitude
        );


        sender.sendMessage(
                "§eMinecraft X: §f"
                        + location.getBlockX()
                        + " Z: "
                        + location.getBlockZ()
        );


        sender.sendMessage(
                "§aDownloading real Earth elevation..."
        );


        double elevation =
                EarthElevation.getElevation(
                        latitude,
                        longitude
                );


        int minecraftHeight =
                EarthElevation.getMinecraftHeight(
                        elevation
                );


        sender.sendMessage(
                "§aReal elevation: §f"
                        + elevation
                        + " meters"
        );


        sender.sendMessage(
                "§aMinecraft terrain height: §f"
                        + minecraftHeight
        );
    }
}
