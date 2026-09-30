package com.earthbound;


import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;


import com.earthbound.terrain.EarthTerrainDownloader;
import com.earthbound.water.EarthWaterData;
import com.earthbound.roads.EarthRoadData;



public class EarthBound extends JavaPlugin {



    private static final double GUEMES_SPAWN_X = -654.0;
    private static final double GUEMES_SPAWN_Y = 86.0;
    private static final double GUEMES_SPAWN_Z = -355.0;




    @Override
    public void onEnable() {


        getLogger().info(
                "EarthBound starting..."
        );



        saveDefaultConfig();



        /*
         * WORLD RULES
         */


        Bukkit.getPluginManager()
                .registerEvents(
                        new EarthBoundWorldRules(this),
                        this
                );



        /*
         * ECONOMY
         */


        EarthEconomy.setup(this);


        if (getCommand("balance") != null) {


            getCommand("balance")
                    .setExecutor(
                            new EarthEconomyCommand()
                    );

        }



        /*
         * STORE
         */


        EarthStore.setup();


        if (getCommand("store") != null) {


            getCommand("store")
                    .setExecutor(
                            new EarthStoreCommand()
                    );

        }



        /*
         * LOAD TERRAIN
         */


        EarthTerrainDownloader.loadGuemesIsland();


        if (EarthTerrainDownloader.isLoaded()) {


            getLogger().info(
                    "Terrain loaded!"
            );


        }



        /*
         * LOAD WATER
         */


        boolean waterLoaded =
                EarthWaterData.loadGuemesWaterMask();


        getLogger().info(
                "Water loaded: "
                        + waterLoaded
        );



        /*
         * LOAD ROADS
         */


        boolean roadsLoaded =
                EarthRoadData.loadGuemesRoadMask();


        getLogger().info(
                "Roads loaded: "
                        + roadsLoaded
        );



        /*
         * DISABLE NORMAL MOBS
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


                            }


                        }
                );




        /*
         * SET SPAWN
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

                                return;

                            }



                            world.setSpawnLocation(
                                    new Location(
                                            world,
                                            GUEMES_SPAWN_X,
                                            GUEMES_SPAWN_Y,
                                            GUEMES_SPAWN_Z
                                    )
                            );


                            getLogger().info(
                                    "Guemes spawn set!"
                            );


                        }
                );



        getLogger().info(
                "EarthBound enabled!"
        );


    }





    @Override
    public ChunkGenerator getDefaultWorldGenerator(
            String worldName,
            String id
    ) {


        return new EarthGenerator();

    }



}
