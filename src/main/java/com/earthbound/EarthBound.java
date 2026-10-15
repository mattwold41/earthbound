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

    /*
     * ============================================================
     * GUEMES ISLAND SPAWN
     * ============================================================
     */

    private static final double GUEMES_SPAWN_X = -654.0;
    private static final double GUEMES_SPAWN_Y = 86.0;
    private static final double GUEMES_SPAWN_Z = -355.0;


    /*
     * ============================================================
     * ENABLE EARTHBOUND
     * ============================================================
     */

    @Override
    public void onEnable() {

        getLogger().info("EarthBound starting...");

        saveDefaultConfig();


        /*
         * WORLD RULES
         */

        Bukkit.getPluginManager().registerEvents(
                new EarthBoundWorldRules(this),
                this
        );


        /*
         * ECONOMY
         */

        EarthEconomy.setup(this);

        if (getCommand("balance") != null) {

            getCommand("balance").setExecutor(
                    new EarthEconomyCommand()
            );

        } else {

            getLogger().warning(
                    "Balance command missing from plugin.yml"
            );
        }


        /*
         * PLAYER-TO-PLAYER PAYMENTS
         */

        if (getCommand("pay") != null) {

            getCommand("pay").setExecutor(
                    new EarthPayCommand()
            );

            getLogger().info(
                    "Player payment command registered!"
            );

        } else {

            getLogger().warning(
                    "Pay command missing from plugin.yml"
            );
        }


        /*
         * GOVERNMENT BANKING
         */

        if (getCommand("earthbank") != null) {

            getCommand("earthbank").setExecutor(
                    new EarthBankCommand()
            );

            getLogger().info(
                    "Government banking commands registered!"
            );

        } else {

            getLogger().warning(
                    "Earthbank command missing from plugin.yml"
            );
        }


        /*
         * GENERAL STORE
         */

        EarthStore.setup();

        if (getCommand("store") != null) {

            getCommand("store").setExecutor(
                    new EarthStoreCommand()
            );

        } else {

            getLogger().warning(
                    "Store command missing from plugin.yml"
            );
        }


        /*
         * ========================================================
         * PROPERTY OWNERSHIP SYSTEM
         * ========================================================
         *
         * Loads registered properties and saved ownership.
         *
         * Government property sale revenue:
         *
         * 60% - Anacortes
         * 20% - Washington
         * 20% - USA
         *
         */

        EarthPropertyManager.setup(this);

        getLogger().info(
                "EarthBound property system initialized!"
        );


        /*
         * ========================================================
         * BLUE FOR SALE BANNER SYSTEM
         * ========================================================
         *
         * Clear previous banner registrations.
         *
         * Register property interaction listener.
         *
         * Handles:
         *
         * - Official blue For Sale banner clicks
         * - Opening the property purchase menu
         * - Buy Home button
         * - Cancel button
         * - Purchase processing
         * - Removing the banner after purchase
         *
         */

        EarthPropertyBannerRegistry.clear();

        Bukkit.getPluginManager().registerEvents(
                new EarthPropertyListener(),
                this
        );

        getLogger().info(
                "EarthBound property banner listener registered!"
        );


        /*
         * LOAD GUEMES TERRAIN
         */

        EarthTerrainDownloader.loadGuemesIsland();

        if (EarthTerrainDownloader.isLoaded()) {

            getLogger().info(
                    "Guemes terrain loaded!"
            );

        } else {

            getLogger().warning(
                    "Guemes terrain did not finish loading."
            );
        }


        /*
         * LOAD GUEMES WATER
         */

        boolean waterLoaded =
                EarthWaterData.loadGuemesWaterMask();

        getLogger().info(
                "Water loaded: " + waterLoaded
        );


        /*
         * LOAD GUEMES ROADS
         */

        boolean roadsLoaded =
                EarthRoadData.loadGuemesRoadMask();

        getLogger().info(
                "Roads loaded: " + roadsLoaded
        );


        /*
         * DISABLE NORMAL MOB SPAWNING
         */

        Bukkit.getScheduler().runTask(
                this,
                () -> {

                    for (World world : Bukkit.getWorlds()) {

                        world.setGameRule(
                                org.bukkit.GameRule.DO_MOB_SPAWNING,
                                false
                        );

                    }

                }
        );


        /*
         * SET GUEMES SPAWN
         */

        Bukkit.getScheduler().runTask(
                this,
                () -> {

                    World world =
                            Bukkit.getWorld("earthbound");

                    if (world == null) {

                        getLogger().warning(
                                "EarthBound world not found. "
                                + "Guemes spawn was not set."
                        );

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


        /*
         * STARTUP COMPLETE
         */

        getLogger().info(
                "EarthBound enabled!"
        );

    }


    /*
     * ============================================================
     * SAVE EARTHBOUND DATA ON SHUTDOWN
     * ============================================================
     */

    @Override
    public void onDisable() {

        /*
         * SAVE PROPERTY OWNERSHIP
         */

        if (EarthPropertyManager.save()) {

            getLogger().info(
                    "EarthBound properties saved!"
            );

        } else {

            getLogger().warning(
                    "EarthBound property data could not be saved."
            );
        }


        /*
         * SAVE ECONOMY
         */

        EarthEconomy.save();

        getLogger().info(
                "EarthBound economy saved!"
        );


        /*
         * SHUTDOWN COMPLETE
         */

        getLogger().info(
                "EarthBound shutdown complete."
        );

    }


    /*
     * ============================================================
     * EARTH WORLD GENERATOR
     * ============================================================
     */

    @Override
    public ChunkGenerator getDefaultWorldGenerator(
            String worldName,
            String id
    ) {

        return new EarthGenerator();

    }

}
