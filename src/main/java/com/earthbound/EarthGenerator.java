package com.earthbound;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import java.util.Random;

public class EarthGenerator extends ChunkGenerator {


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


                /*
                 * Convert Minecraft coordinates
                 * into real Earth coordinates.
                 */
                double latitude =
                        EarthCoordinates.minecraftToLatitude(
                                worldZ
                        );


                double longitude =
                        EarthCoordinates.minecraftToLongitude(
                                worldX
                        );


                /*
                 * Check the real Census
                 * hydrography polygons.
                 */
                boolean water =
                        EarthWaterData.isWater(
                                latitude,
                                longitude
                        );


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
                 * LAND
                 *
                 * Get real USGS elevation.
                 */
                double elevation =
                        EarthTerrainLoader.getGuemesElevation(
                                worldX,
                                worldZ
                        );


                int height =
                        EarthElevation.getMinecraftHeight(
                                elevation
                        );


                /*
                 * Determine whether this block
                 * lies on a real road.
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
                 * Generate terrain.
                 */
                for (int y = 0;
                     y <= height;
                     y++) {


                    if (y == height) {


                        if (road) {


                            /*
                             * Temporary road surface.
                             *
                             * Later we can give different
                             * road classes different
                             * materials and smoothing.
                             */
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


        return chunk;

    }

}
