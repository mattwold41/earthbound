package com.earthbound;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

public class EarthBound extends JavaPlugin {

    @Override
    public void onEnable() {

        getLogger().info(
                "EarthBound is now online!"
        );

        /*
         * Register /earth command.
         */
        if (getCommand("earth") != null) {

            getCommand("earth").setExecutor(
                    new EarthCommand()
            );
        }


        /*
         * Run USGS tests asynchronously so the
         * Minecraft server is not frozen while
         * downloading Earth data.
         */
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


                            if (!connected) {

                                getLogger().warning(
                                        "USGS 3DEP terrain service test failed."
                                );

                                return;
                            }


                            getLogger().info(
                                    "USGS 3DEP terrain service is ready!"
                            );


                            /*
                             * First real terrain tile test.
                             */
                            getLogger().info(
                                    "Testing Guemes Island terrain download..."
                            );


                            boolean guemesDownloaded =
                                    EarthTerrainLoader
                                            .testGuemesTerrainTile();


                            if (guemesDownloaded) {

                                getLogger().info(
                                        "Guemes Island terrain data is available!"
                                );

                            } else {

                                getLogger().warning(
                                        "Guemes Island terrain download failed."
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
