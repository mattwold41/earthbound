package com.earthbound;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

public class EarthBound extends JavaPlugin {

    /*
     * GUEMES ISLAND SPAWN
     *
     * Temporary approved spawn location.
     * We can fine-tune this later when the
     * ferry/waterfront area is completed.
     */
    private static final double GUEMES_SPAWN_X = -654.0;
    private static final double GUEMES_SPAWN_Y = 86.0;
    private static final double GUEMES_SPAWN_Z = -355.0;


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


        /*
         * Set the Guemes Island world spawn.
         *
         * This runs after the world has finished
         * loading so the EarthBound world exists.
         */
        Bukkit.getScheduler().runTask(
                this,
                () -> {

                    World world =
                            Bukkit.getWorld(
                                    "earthbound"
                            );

                    if (world == null) {

                        getLogger().warning(
                                "Could not set Guemes spawn because "
                                        + "the earthbound world was not found."
                        );

                        return;
                    }


                    Location spawn =
                            new Location(
                                    world,
                                    GUEMES_SPAWN_X,
                                    GUEMES_SPAWN_Y,
                                    GUEMES_SPAWN_Z
                            );


                    /*
                     * Set Minecraft's world spawn.
                     */
                    world.setSpawnLocation(
                            spawn
                    );


                    getLogger().info(
                            "Guemes Island spawn set to "
                                    + GUEMES_SPAWN_X
                                    + ", "
                                    + GUEMES_SPAWN_Y
                                    + ", "
                                    + GUEMES_SPAWN_Z
                    );
                }
        );


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
