package com.earthbound;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

public class EarthBound extends JavaPlugin {

    @Override
    public void onEnable() {

        getLogger().info(
                "EarthBound is now online!"
        );

        /*
         * Register /earth command.
         */
        if (getCommand("earth") != null) {

            getCommand("earth").setExecutor(
                    new EarthCommand()
            );
        }
    }


    @Override
    public ChunkGenerator getDefaultWorldGenerator(
            String worldName,
            String id) {

        getLogger().info(
                "EarthBound generator loading..."
        );


        /*
         * Load the Guemes USGS elevation raster.
         */
        if (!EarthTerrainLoader.isGuemesTerrainLoaded()) {

            getLogger().info(
                    "Loading Guemes terrain before world generation..."
            );

            boolean terrainLoaded =
                    EarthTerrainLoader.loadGuemesTerrainTile();

            if (terrainLoaded) {

                getLogger().info(
                        "Guemes terrain loaded and ready for generation!"
                );

            } else {

                getLogger().warning(
                        "Guemes terrain could not be loaded. "
                                + "Flat fallback terrain will be used."
                );
            }
        }


        /*
         * Load the Guemes water/coastline mask.
         */
        if (!EarthWaterData.isLoaded()) {

            getLogger().info(
                    "Loading Guemes water mask before world generation..."
            );

            boolean waterLoaded =
                    EarthWaterData.loadGuemesWaterMask();

            if (waterLoaded) {

                getLogger().info(
                        "Guemes water mask loaded and ready for generation!"
                );

            } else {

                getLogger().warning(
                        "Guemes water mask could not be loaded. "
                                + "Terrain will generate without ocean masking."
                );
            }
        }


        /*
         * Load the Guemes road mask.
         *
         * EarthRoadData downloads the road image once,
         * converts it into a local mask, and then
         * EarthGenerator can read that mask without
         * making Internet requests for individual blocks.
         */
        if (!EarthRoadData.isLoaded()) {

            getLogger().info(
                    "Loading Guemes road mask before world generation..."
            );

            boolean roadsLoaded =
                    EarthRoadData.loadGuemesRoadMask();

            if (roadsLoaded) {

                getLogger().info(
                        "Guemes road mask loaded and ready for generation!"
                );

            } else {

                getLogger().warning(
                        "Guemes road mask could not be loaded. "
                                + "Terrain will generate without roads."
                );
            }
        }


        /*
         * Give Paper our EarthBound
         * custom world generator.
         */
        return new EarthGenerator();
    }
}
