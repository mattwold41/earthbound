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
         * Load the Guemes USGS elevation raster
         * BEFORE Minecraft begins generating
         * EarthBound terrain.
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
         * Load the Guemes land/water mask
         * BEFORE Minecraft begins generating
         * EarthBound terrain.
         *
         * This downloads ONE 512 x 512
         * hydrography image.
         *
         * It does NOT make an Internet
         * request for every Minecraft block.
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
         * Give Paper our EarthBound
         * custom world generator.
         */
        return new EarthGenerator();
    }
}
