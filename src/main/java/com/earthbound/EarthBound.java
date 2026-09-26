package com.earthbound;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

public class EarthBound extends JavaPlugin {


    @Override
    public void onEnable() {


        getLogger().info(
                "EarthBound enabled!"
        );


        /*
         * Register tree listener
         */
        getServer()
                .getPluginManager()
                .registerEvents(
                        new EarthTreeListener(),
                        this
                );


        /*
         * Register earth command
         */
        if (getCommand("earth") != null) {

            getCommand("earth")
                    .setExecutor(
                            new EarthCommand()
                    );
        }



        /*
         * Create EarthBound world
         * using custom generator
         */
        createEarthWorld();



        getLogger().info(
                "EarthBound systems loaded!"
        );
    }



    private void createEarthWorld() {


        World world =
                Bukkit.getWorld(
                        "earthbound"
                );


        if (world != null) {

            getLogger().info(
                    "EarthBound world already exists."
            );

            return;
        }



        getLogger().info(
                "Creating EarthBound world with EarthGenerator..."
        );



        WorldCreator creator =
                new WorldCreator(
                        "earthbound"
                );


        creator.generator(
                new EarthGenerator()
        );


        creator.environment(
                World.Environment.NORMAL
        );


        Bukkit.createWorld(
                creator
        );


        getLogger().info(
                "EarthBound world created!"
        );
    }



    @Override
    public void onDisable() {


        getLogger().info(
                "EarthBound disabled!"
        );
    }



    /*
     * Paper custom generator support
     */
    @Override
    public EarthGenerator getDefaultWorldGenerator(
            String worldName,
            String id) {


        return new EarthGenerator();
    }
}
