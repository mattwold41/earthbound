package com.earthbound;

import org.bukkit.plugin.java.JavaPlugin;

public class EarthBound extends JavaPlugin {


    @Override
    public void onEnable() {


        getLogger().info(
                "EarthBound enabled!"
        );


        /*
         * Register tree generation listener
         */
        getServer()
                .getPluginManager()
                .registerEvents(
                        new EarthTreeListener(),
                        this
                );


        /*
         * Register commands
         */
        getCommand("earth")
                .setExecutor(
                        new EarthCommand()
                );


        getLogger().info(
                "EarthBound systems loaded!"
        );
    }



    @Override
    public void onDisable() {


        getLogger().info(
                "EarthBound disabled!"
        );
    }



    /*
     * World generator connection
     */
    @Override
    public EarthGenerator getDefaultWorldGenerator(
            String worldName,
            String id) {


        return new EarthGenerator();
    }
}
