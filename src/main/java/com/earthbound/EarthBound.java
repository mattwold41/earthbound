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
         * Load real USGS elevation data.
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


        /*
         * Load real vector water polygons.
         */
        boolean waterLoaded =
                EarthWaterData.loadGuemesWaterMask();


        if (waterLoaded) {

            getLogger().info(
                    "Real Guemes water polygons loaded successfully!"
            );

        } else {

            getLogger().warning(
                    "Guemes water polygons did not load!"
            );

        }


        /*
         * Load real Census TIGERweb
         * road centerlines.
         */
        boolean roadsLoaded =
                EarthRoadData.loadGuemesRoadMask();


        if (roadsLoaded) {

            getLogger().info(
                    "Real Guemes road centerlines loaded successfully!"
            );

        } else {

            getLogger().warning(
                    "Guemes road centerlines did not load!"
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
