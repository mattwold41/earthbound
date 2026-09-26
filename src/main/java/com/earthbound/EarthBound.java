package com.earthbound;

import org.bukkit.plugin.java.JavaPlugin;

public class EarthBound extends JavaPlugin {


    @Override
    public void onEnable() {

        getLogger().info(
                "EarthBound enabled!"
        );


        getServer()
                .getPluginManager()
                .registerEvents(
                        new EarthTreeListener(),
                        this
                );


        if (getCommand("earth") != null) {

            getCommand("earth")
                    .setExecutor(
                            new EarthCommand()
                    );
        }


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



    @Override
    public EarthGenerator getDefaultWorldGenerator(
            String worldName,
            String id) {


        return new EarthGenerator();
    }
}
