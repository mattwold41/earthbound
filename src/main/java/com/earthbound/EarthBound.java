package com.earthbound;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;


public class EarthBound extends JavaPlugin {


    @Override
    public void onEnable() {


        getLogger().info(
                "EarthBound enabled!"
        );


        getLogger().info(
                "EarthBound systems loaded!"
        );


        // Test that the generator class exists
        EarthGenerator generator =
                new EarthGenerator();


        getLogger().info(
                "EarthGenerator loaded: "
                + generator.getClass().getName()
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
