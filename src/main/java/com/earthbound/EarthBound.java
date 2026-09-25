package com.earthbound;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

public class EarthBound extends JavaPlugin {

    @Override
    public void onEnable() {

        getLogger().info("EarthBound is now online!");

        if (getCommand("earth") != null) {
            getCommand("earth").setExecutor(
                    new EarthCommand()
            );
        }

        getServer()
                .getScheduler()
                .runTaskAsynchronously(
                        this,
                        () -> {

                            getLogger().info(
                                    "Testing USGS 3DEP terrain service..."
                            );

                            boolean connected =
                                    EarthTerrainLoader
                                            .test3DEPConnection();

                            if (connected) {

                                getLogger().info(
                                        "USGS 3DEP terrain service is ready!"
                                );

                            } else {

                                getLogger().warning(
                                        "USGS 3DEP terrain service test failed."
                                );
                            }
                        }
                );
    }


    @Override
    public ChunkGenerator getDefaultWorldGenerator(
            String worldName,
            String id) {

        getLogger().info(
                "EarthBound generator loading..."
        );

        return new EarthGenerator();
    }
}
