package com.earthbound;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

public class EarthBound extends JavaPlugin {


    @Override
    public void onEnable() {

        getLogger().info(
                "EarthBound starting..."
        );


        /*
         * Load the real USGS Guemes Island
         * elevation raster before terrain
         * generation begins.
         */
        EarthTerrainDownloader.loadGuemesIsland();


        if (EarthTerrainDownloader.isLoaded()) {

            getLogger().info(
                    "Real USGS Guemes elevation loaded successfully!"
            );

        } else {

            getLogger().warning(
                    "USGS Guemes elevation did not load!"
            );

        }


        getLogger().info(
                "EarthBound enabled!"
        );

        getLogger().info(
                "EarthBound systems loaded!"
        );

    }


    @Override
    public ChunkGenerator getDefaultWorldGenerator(
            String worldName,
            String id
    ) {

        getLogger().info(
                "Loading EarthBound terrain generator for "
                        + worldName
        );


        return new EarthGenerator();

    }

}
