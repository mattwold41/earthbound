package com.earthbound;

import org.bukkit.Material;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.aworld.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class EarthGenerator implements Listener {

    private final JavaPlugin plugin;

    public EarthGenerator(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {

        Chunk chunk = event.getChunk();

        int x = chunk.getX() * 16;
        int z = chunk.getZ() * 16;

        double latitude =
                EarthCoordinates.getLatitude(x, z);

        double longitude =
                EarthCoordinates.getLongitude(x, z);

        if (EarthLocation.isNearGuemes(latitude, longitude)) {

            generateGuemesTest(chunk);
        }
    }


    private void generateGuemesTest(Chunk chunk) {

        Location location =
                chunk.getBlock(8, 64, 8).getLocation();

        location.getBlock()
                .setType(Material.GRASS_BLOCK);
    }
}
