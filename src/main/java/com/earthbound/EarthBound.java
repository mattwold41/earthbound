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
         *
         * This is one raster download, NOT
         * one Internet request per block.
         */
        if (!EarthTerrainLoader.isGuemesTerrainLoaded()) {

            getLogger().info(
                    "Loading Guemes terrain before world generation..."
            );

            boolean loaded =
                    EarthTerrainLoader
                            .loadGuemesTerrainTile();

            if (loaded) {

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

        return new EarthGenerator();
    }
}
