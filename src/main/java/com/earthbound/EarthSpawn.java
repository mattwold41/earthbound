package com.earthbound;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class EarthSpawn implements Listener {

    private final JavaPlugin plugin;

    public EarthSpawn(JavaPlugin plugin) {
        this.plugin = plugin;
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        World world = player.getWorld();


        // Guemes Island Ferry Terminal spawn
        Location guemesSpawn = new Location(
                world,
                -624,
                80,
                -544
        );


        player.teleport(guemesSpawn);


        player.sendMessage(
                "§6EarthBound §7spawned at Guemes Island Ferry Terminal."
        );
    }
}
