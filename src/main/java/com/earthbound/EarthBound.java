package com.earthbound;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

public class EarthBound extends JavaPlugin {


    /*
     * ============================================================
     * GUEMES ISLAND SPAWN
     * ============================================================
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
         * ========================================================
         * LOAD CONFIGURATION
         * ========================================================
         */

        saveDefaultConfig();


        boolean hostileMobs =
                getConfig()
                        .getBoolean(
                                "world.hostile-mobs",
                                false
                        );


        boolean netherEnabled =
                getConfig()
                        .getBoolean(
                                "world.nether",
                                false
                        );


        boolean endEnabled =
                getConfig()
                        .getBoolean(
                                "world.end",
                                false
                        );


        double hungerRate =
                getConfig()
                        .getDouble(
                                "player.hunger-rate",
                                0.5
                        );


        getLogger().info(
                "EarthBound Settings Loaded"
        );


        getLogger().info(
                "Hostile mobs: " + hostileMobs
        );


        getLogger().info(
                "Nether enabled: " + netherEnabled
        );


        getLogger().info(
                "End enabled: " + endEnabled
        );


        getLogger().info(
                "Hunger rate: " + hungerRate
        );



        /*
         * ========================================================
         * WORLD RULES
         * ========================================================
         */

        Bukkit.getPluginManager()
                .registerEvents(
                        new EarthBoundWorldRules(this),
                        this
                );



        /*
         * ========================================================
         * ECONOMY SYSTEM
         * ========================================================
         */

        EarthEconomy.setup(this);


        getCommand("balance")
                .setExecutor(
                        new EarthEconomyCommand()
                );



        getLogger().info(
                "EarthBound economy loaded!"
        );



        /*
         * ========================================================
         * DISABLE NORMAL MOB SPAWNING
         * ========================================================
         */

        Bukkit.getScheduler()
                .runTask(
                        this,
                        () -> {


                            for (World world :
                                    Bukkit.getWorlds()) {


                                world.setGameRule(
                                        org.bukkit.GameRule.DO_MOB_SPAWNING,
                                        false
                                );


                                getLogger().info(
                                        "Disabled natural mob spawning in "
                                                + world.getName()
                                );

                            }

                        }
                );



        /*
         * ========================================================
         * LOAD USGS TERRAIN
         * ========================================================
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
         * ========================================================
         * LOAD WATER
         * ========================================================
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
         * ========================================================
         * LOAD ROADS
         * ========================================================
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
         * ========================================================
         * SET GUEMES SPAWN
         * ========================================================
         */

        Bukkit.getScheduler()
                .runTask(
                        this,
                        () -> {


                            World world =
                                    Bukkit.getWorld(
                                            "earthbound"
                                    );


                            if (world == null) {


                                getLogger().warning(
                                        "Could not set Guemes spawn."
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


                            world.setSpawnLocation(
                                    spawn
                            );


                            getLogger().info(
                                    "Guemes Island spawn set."
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
