package com.earthbound;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

public class EarthBound extends JavaPlugin {

    @Override
    public void onEnable() {

        getLogger().info("EarthBound is now online!");

        if (getCommand("earth") != null) {
            getCommand("earth").setExecutor(new EarthCommand());
        }
    }


    @Override
    public ChunkGenerator getDefaultWorldGenerator(
            String worldName,
            String id) {

        getLogger().info("EarthBound generator loading...");

        return new EarthGenerator();
    }
}
