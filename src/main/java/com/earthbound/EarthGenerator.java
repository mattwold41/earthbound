package com.earthbound;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import java.util.Random;

public class EarthGenerator extends ChunkGenerator {

    private static final int ROAD_SMOOTH_RADIUS = 2;

    /*
     * Only the immediate building footprint
     * receives foundation adjustment.
     *
     * We no longer raise the entire large
     * foundation area into a giant platform.
     */
    private static final int STORE_MAX_FOUNDATION_RAISE = 2;


    public EarthGenerator() {

        System.out.println(
                "=== EARTHBOUND REAL TERRAIN GENERATOR ACTIVE ==="
        );
    }


    @Override
    public ChunkData generateChunkData(
            World world,
            Random random,
            int chunkX,
            int chunkZ,
            BiomeGrid biome
    ) {

        ChunkData chunk =
                createChunkData(world);


        for (int x = 0; x < 16; x++) {

            for (int z = 0; z < 16; z++) {

                int worldX =
                        chunkX * 16 + x;

                int worldZ =
                        chunkZ * 16 + z;


                double latitude =
                        EarthCoordinates.minecraftToLatitude(
                                worldZ
                        );

                double longitude =
                        EarthCoordinates.minecraftToLongitude(
                                worldX
                        );


                boolean storeFootprint =
                        EarthBuildingGenerator
                                .isInsideGuemesStore(
                                        worldX,
                                        worldZ
                                );


                /*
                 * Real USGS elevation.
                 */
                double elevation =
                        EarthTerrainLoader.getGuemesElevation(
                                worldX,
                                worldZ
                        );

                int naturalHeight =
                        EarthElevation.getMinecraftHeight(
                                elevation
                        );

                int height =
                        naturalHeight;


                /*
                 * Real road information.
                 */
                EarthRoadData.RoadType roadType =
                        EarthRoadData.getRoadType(
                                latitude,
                                longitude
                        );

                boolean road =
                        roadType
                                != EarthRoadData.RoadType.NONE;


                /*
                 * Water information.
                 */
                boolean water =
                        EarthWaterData.isWater(
                                latitude,
                                longitude
                        );


                /*
                 * =================================================
                 * STORE FOUNDATION
                 * =================================================
                 *
                 * The old generator forced the entire store
                 * area to one Y level. On sloping terrain that
                 * produced the enormous rectangular pedestal.
                 *
                 * Now the real terrain remains in place.
                 * We only allow a small foundation adjustment.
                 */
                if (storeFootprint) {

                    int desiredGround =
                            EarthBuildingGenerator
                                    .getGuemesStoreGroundY();

                    if (desiredGround
                            > naturalHeight
                            + STORE_MAX_FOUNDATION_RAISE) {

                        height =
                                naturalHeight
                                        + STORE_MAX_FOUNDATION_RAISE;

                    } else if (desiredGround
                            < naturalHeight
                            - STORE_MAX_FOUNDATION_RAISE) {

                        height =
                                naturalHeight
                                        - STORE_MAX_FOUNDATION_RAISE;

                    } else {

                        height =
                                desiredGround;
                    }


                    /*
                     * The building itself takes
                     * priority over water/road
                     * inside its footprint.
                     */
                    water = false;
                    road = false;
                }


                /*
                 * WATER
                 */
                if (water) {

                    int seaFloor =
                            EarthWaterData.SEA_LEVEL - 8;


                    for (int y = 0;
                         y <= seaFloor;
                         y++) {

                        if (y == seaFloor) {

                            chunk.setBlock(
                                    x,
                                    y,
                                    z,
                                    Material.SAND
                            );

                        } else if (
                                y > seaFloor - 4
                        ) {

                            chunk.setBlock(
                                    x,
                                    y,
                                    z,
                                    Material.GRAVEL
                            );

                        } else {

                            chunk.setBlock(
                                    x,
                                    y,
                                    z,
                                    Material.STONE
                            );
                        }
                    }


                    for (int y = seaFloor + 1;
                         y <= EarthWaterData.SEA_LEVEL;
                         y++) {

                        chunk.setBlock(
                                x,
                                y,
                                z,
                                Material.WATER
                        );
                    }

                    continue;
                }


                /*
                 * Smooth roads while leaving
                 * the store footprint alone.
                 */
                if (road) {

                    height =
                            getSmoothedRoadHeight(
                                    worldX,
                                    worldZ
                            );
                }


                /*
                 * Generate land.
                 */
                for (int y = 0;
                     y <= height;
                     y++) {

                    if (y == height) {

                        if (road) {

                            chunk.setBlock(
                                    x,
                                    y,
                                    z,
                                    Material.GRAY_CONCRETE
                            );

                        } else {

                            chunk.setBlock(
                                    x,
                                    y,
                                    z,
                                    Material.GRASS_BLOCK
                            );
                        }

                    } else if (
                            y > height - 4
                    ) {

                        chunk.setBlock(
                                x,
                                y,
                                z,
                                Material.DIRT
                        );

                    } else {

                        chunk.setBlock(
                                x,
                                y,
                                z,
                                Material.STONE
                        );
                    }
                }
            }
        }


        /*
         * Buildings are placed after terrain.
         */
        EarthBuildingGenerator.generateGuemesStore(
                chunk,
                chunkX,
                chunkZ
        );


        return chunk;
    }


    private int getSmoothedRoadHeight(
            int worldX,
            int worldZ
    ) {

        double totalElevation = 0.0;
        int samples = 0;


        for (int offsetX = -ROAD_SMOOTH_RADIUS;
             offsetX <= ROAD_SMOOTH_RADIUS;
             offsetX++) {

            for (int offsetZ = -ROAD_SMOOTH_RADIUS;
                 offsetZ <= ROAD_SMOOTH_RADIUS;
                 offsetZ++) {

                double sampleElevation =
                        EarthTerrainLoader.getGuemesElevation(
                                worldX + offsetX,
                                worldZ + offsetZ
                        );

                totalElevation +=
                        sampleElevation;

                samples++;
            }
        }


        double averageElevation =
                totalElevation / samples;


        return EarthElevation.getMinecraftHeight(
                averageElevation
        );
    }
}
