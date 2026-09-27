package com.earthbound;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

public class EarthGenerator extends ChunkGenerator {

    private static final int SEA_LEVEL = 63;

    private boolean announced = false;


    @Override
    public void generateNoise(
            WorldInfo worldInfo,
            Random random,
            int chunkX,
            int chunkZ,
            ChunkData chunkData) {


        if (!announced) {
            getLogger().info(
                    "EARTHBOUND REAL TERRAIN GENERATOR ACTIVE"
            );
            announced = true;
        }


        for (int x = 0; x < 16; x++) {

            for (int z = 0; z < 16; z++) {


                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;


                /*
                 * Convert Minecraft position
                 * to Earth coordinates
                 */
                double latitude =
                        EarthCoordinates.getLatitude(
                                worldX,
                                worldZ
                        );


                double longitude =
                        EarthCoordinates.getLongitude(
                                worldX,
                                worldZ
                        );



                /*
                 * Get real elevation
                 */
                Double elevation =
                        EarthTerrainLoader.getElevation(
                                latitude,
                                longitude
                        );



                int height;


                if (elevation != null) {

                    height =
                            EarthElevation
                                    .getMinecraftHeight(
                                            elevation
                                    );

                } else {

                    height = SEA_LEVEL;
                }



                /*
                 * Keep world height safe
                 */
                height =
                        Math.max(
                                worldInfo.getMinHeight(),
                                Math.min(
                                        worldInfo.getMaxHeight() - 1,
                                        height
                                )
                        );



                /*
                 * Water
                 */
                boolean water =
                        EarthWaterData.isWater(
                                latitude,
                                longitude
                        );



                if (water) {


                    int floor =
                            Math.min(
                                    height,
                                    SEA_LEVEL - 5
                            );


                    for (
                            int y = worldInfo.getMinHeight();
                            y <= floor;
                            y++
                    ) {


                        if (y >= floor - 3) {

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
                                    Material.STONE
                            );
                        }
                    }



                    for (
                            int y = floor + 1;
                            y <= SEA_LEVEL;
                            y++
                    ) {

                        chunkData.setBlock(
                                x,
                                y,
                                z,
                                Material.WATER
                        );
                    }


                    continue;
                }



                /*
                 * Land
                 */
                for (
                        int y = worldInfo.getMinHeight();
                        y <= height;
                        y++
                ) {


                    if (y == height) {


                        boolean road =
                                EarthRoadData.isRoad(
                                        latitude,
                                        longitude
                                );


                        if (road) {

                            chunkData.setBlock(
                                    x,
                                    y,
                                    z,
                                    Material.GRAY_CONCRETE
                            );

                        } else {

                            chunkData.setBlock(
                                    x,
                                    y,
                                    z,
                                    Material.GRASS_BLOCK
                            );
                        }


                    } else if (y >= height - 3) {


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
}
