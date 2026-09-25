package com.earthbound;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

public class EarthGenerator extends ChunkGenerator {


    /*
     * EarthBound scale:
     *
     * 1 Minecraft block = 2 real-world meters
     */
    private static final double METERS_PER_BLOCK = 2.0;


    /*
     * Starting point:
     * Guemes Island, Washington
     */
    private static final double SPAWN_LAT =
            48.5265;

    private static final double SPAWN_LON =
            -122.6165;



    @Override
    public void generateNoise(
            WorldInfo worldInfo,
            Random random,
            int chunkX,
            int chunkZ,
            ChunkData chunkData) {


        for (int x = 0; x < 16; x++) {

            for (int z = 0; z < 16; z++) {


                int worldX =
                        chunkX * 16 + x;

                int worldZ =
                        chunkZ * 16 + z;


                double latitude =
                        minecraftToLatitude(
                                worldZ
                        );


                double longitude =
                        minecraftToLongitude(
                                worldX
                        );


                int height =
                        EarthTerrainLoader
                                .loadMinecraftHeight(
                                        latitude,
                                        longitude
                                );


                /*
                 * Keep terrain inside Minecraft limits
                 */
                if (height < 1) {

                    height = 1;
                }

                if (height > 319) {

                    height = 319;
                }



                /*
                 * Generate terrain column
                 */
                for (int y = 0; y <= height; y++) {


                    if (y == height) {


                        if (height <= 63) {

                            chunkData.setBlock(
                                    x,
                                    y,
                                    z,
                                    Material.SAND
                            );

                        } else {

                            chunkData.setBlock(
                                    x,
                                    y,
                                    z,
                                    Material.GRASS_BLOCK
                            );
                        }


                    } else if (y > height - 4) {


                        chunkData.setBlock(
                                x,
                                y,
                                z,
                                Material.DIRT
                        );


                    } else {


                        chunkData.setBlock(
                                x,
                                y,
                                z,
                                Material.STONE
                        );
                    }
                }
            }
        }
    }



    /*
     * Minecraft Z → latitude
     */
    private double minecraftToLatitude(
            int z) {


        return SPAWN_LAT -
                (z * METERS_PER_BLOCK
                        / 111320.0);
    }



    /*
     * Minecraft X → longitude
     */
    private double minecraftToLongitude(
            int x) {


        double metersPerLongitude =
                111320.0 *
                Math.cos(
                        Math.toRadians(
                                SPAWN_LAT
                        )
                );


        return SPAWN_LON +
                (x * METERS_PER_BLOCK
                        / metersPerLongitude);
    }
}
