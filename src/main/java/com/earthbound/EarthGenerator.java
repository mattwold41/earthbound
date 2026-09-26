package com.earthbound;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

public class EarthGenerator extends ChunkGenerator {

    private static final int SEA_LEVEL = 63;


    @Override
    public void generateNoise(
            WorldInfo worldInfo,
            Random random,
            int chunkX,
            int chunkZ,
            ChunkData chunkData) {


        for (int x = 0; x < 16; x++) {

            for (int z = 0; z < 16; z++) {


                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;


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


                Double elevation =
                        EarthTerrainLoader.getGuemesElevation(
                                latitude,
                                longitude
                        );


                boolean water =
                        EarthWaterData.isWater(
                                latitude,
                                longitude
                        );


                boolean road =
                        EarthRoadData.isRoad(
                                latitude,
                                longitude
                        );


                int height;


                if (elevation != null
                        && Double.isFinite(elevation)) {

                    height =
                            EarthElevation.getMinecraftHeight(
                                    elevation
                            );

                } else {

                    height = SEA_LEVEL;
                }


                height =
                        Math.max(
                                worldInfo.getMinHeight(),
                                Math.min(
                                        worldInfo.getMaxHeight() - 1,
                                        height
                                )
                        );



                /*
                 * OCEAN GENERATION
                 */
                if (water) {


                    int oceanFloor =
                            Math.min(
                                    height,
                                    SEA_LEVEL - 4
                            );


                    oceanFloor =
                            Math.max(
                                    worldInfo.getMinHeight(),
                                    oceanFloor
                            );


                    for (int y = worldInfo.getMinHeight();
                         y <= oceanFloor;
                         y++) {


                        if (y >= oceanFloor - 4) {

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


                    for (int y = oceanFloor + 1;
                         y <= SEA_LEVEL;
                         y++) {


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
                 * LAND GENERATION
                 */
                for (int y = worldInfo.getMinHeight();
                     y <= height;
                     y++) {


                    if (y == height) {


                        /*
                         * ROADS ALWAYS WIN
                         */
                        if (road) {

                            chunkData.setBlock(
                                    x,
                                    y,
                                    z,
                                    Material.GRAY_CONCRETE
                            );


                        } else {


                            /*
                             * COAST SYSTEM
                             */
                            int beachLevel =
                                    EarthCoastData.getBeachLevel(
                                            latitude,
                                            longitude
                                    );


                            if (beachLevel > 0) {

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
                        }



                    } else if (y >= height - 4) {


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



                /*
                 * PACIFIC NORTHWEST VEGETATION
                 */
                if (!road
                        && height > SEA_LEVEL + 5) {


                    int chance =
                            random.nextInt(100);


                    if (chance < 12) {


                        chunkData.setBlock(
                                x,
                                height + 1,
                                z,
                                Material.TALL_GRASS
                        );


                    } else if (chance < 18) {


                        chunkData.setBlock(
                                x,
                                height + 1,
                                z,
                                Material.FERN
                        );


                    } else if (chance == 99) {


                        chunkData.setBlock(
                                x,
                                height + 1,
                                z,
                                Material.SPRUCE_SAPLING
                        );
                    }
                }
            }
        }
    }
}
