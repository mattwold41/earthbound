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

                /*
                 * Convert Minecraft coordinates into
                 * real-world latitude and longitude.
                 *
                 * Guemes Ferry Terminal:
                 * Minecraft X = -624
                 * Minecraft Z = -544
                 *
                 * Latitude  = 48.5228603
                 * Longitude = -122.624694
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
                 * Read the USGS Guemes elevation raster
                 * already stored in memory.
                 */
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
                 * WATER
                 *
                 * Water-mask locations become ocean.
                 * The ocean floor is placed below sea level,
                 * then water fills up to Y=63.
                 */
                if (water) {

                    int oceanFloor =
                            Math.min(height, SEA_LEVEL - 4);

                    oceanFloor =
                            Math.max(
                                    worldInfo.getMinHeight(),
                                    oceanFloor
                            );

                    for (int y = worldInfo.getMinHeight();
                         y <= oceanFloor;
                         y++) {

                        if (y == oceanFloor) {

                            chunkData.setBlock(
                                    x,
                                    y,
                                    z,
                                    Material.SAND
                            );

                        } else if (y >= oceanFloor - 3) {

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

                    /*
                     * Never put roads over ocean during
                     * Phase 1. Bridges/ferries come later.
                     */
                    continue;
                }

                /*
                 * LAND
                 */
                for (int y = worldInfo.getMinHeight();
                     y <= height;
                     y++) {

                    if (y == height) {

                        /*
                         * ROAD
                         *
                         * Road-mask locations get a visible
                         * gray road surface.
                         */
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

                        if (road) {

                            /*
                             * Give roads a solid foundation.
                             */
                            chunkData.setBlock(
                                    x,
                                    y,
                                    z,
                                    Material.STONE
                            );

                        } else {

                            chunkData.setBlock(
                                    x,
                                    y,
                                    z,
                                    Material.DIRT
                            );
                        }

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
