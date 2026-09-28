package com.earthbound;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import java.util.Random;

public class EarthGenerator extends ChunkGenerator {

    private static final int ROAD_SMOOTH_RADIUS = 2;

    /*
     * Wider transition around the Guemes General Store.
     */
    private static final double STORE_BLEND_DISTANCE = 32.0;

    /*
     * Approximately 4 horizontal blocks for every
     * 1 vertical block of terrain change.
     */
    private static final double STORE_SLOPE_RUN = 4.0;

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

        ChunkData chunk = createChunkData(world);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;

                double latitude =
                        EarthCoordinates.minecraftToLatitude(
                                worldZ
                        );

                double longitude =
                        EarthCoordinates.minecraftToLongitude(
                                worldX
                        );

                boolean water =
                        EarthWaterData.isWater(
                                latitude,
                                longitude
                        );

                /*
                 * REAL WATER
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

                        } else if (y > seaFloor - 4) {

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
                 * REAL USGS TERRAIN
                 */
                double elevation =
                        EarthTerrainLoader
                                .getGuemesElevation(
                                        worldX,
                                        worldZ
                                );

                int naturalHeight =
                        EarthElevation
                                .getMinecraftHeight(
                                        elevation
                                );

                int height = naturalHeight;

                /*
                 * REAL ROADS
                 */
                EarthRoadData.RoadType roadType =
                        EarthRoadData.getRoadType(
                                latitude,
                                longitude
                        );

                boolean road =
                        roadType
                                != EarthRoadData.RoadType.NONE;

                if (road) {

                    height =
                            getSmoothedRoadHeight(
                                    worldX,
                                    worldZ
                            );
                }

                /*
                 * GUEMES GENERAL STORE TERRAIN
                 *
                 * EarthBuildingGenerator keeps the store
                 * itself at Y=65.
                 *
                 * We flatten the immediate foundation and
                 * then gradually transition back toward
                 * the natural USGS terrain.
                 */
                if (EarthBuildingGenerator
                        .isInsideGuemesStoreBlendArea(
                                worldX,
                                worldZ
                        )) {

                    int storeHeight =
                            EarthBuildingGenerator
                                    .getGuemesStoreGroundY();

                    double distance =
                            EarthBuildingGenerator
                                    .getDistanceFromGuemesStore(
                                            worldX,
                                            worldZ
                                    );

                    /*
                     * FLAT STORE FOUNDATION
                     */
                    if (EarthBuildingGenerator
                            .isInsideGuemesStoreFoundation(
                                    worldX,
                                    worldZ
                            )) {

                        height = storeHeight;
                        road = false;

                    } else {

                        /*
                         * Calculate how far we are through
                         * the 32-block transition.
                         */
                        double blend =
                                distance
                                        / STORE_BLEND_DISTANCE;

                        blend =
                                Math.max(
                                        0.0,
                                        Math.min(
                                                1.0,
                                                blend
                                        )
                                );

                        /*
                         * Smoothstep interpolation.
                         */
                        blend =
                                blend
                                        * blend
                                        * (3.0
                                        - 2.0 * blend);

                        int blendedHeight =
                                (int) Math.round(
                                        storeHeight
                                                * (1.0 - blend)
                                                + naturalHeight
                                                * blend
                                );

                        /*
                         * SLOPE LIMIT
                         *
                         * Prevent the nearby USGS terrain
                         * from immediately creating large
                         * staircase cliffs around Y=65.
                         */
                        int allowedDifference =
                                Math.max(
                                        1,
                                        (int) Math.floor(
                                                distance
                                                        / STORE_SLOPE_RUN
                                        )
                                );

                        int minimumHeight =
                                storeHeight
                                        - allowedDifference;

                        int maximumHeight =
                                storeHeight
                                        + allowedDifference;

                        height =
                                Math.max(
                                        minimumHeight,
                                        Math.min(
                                                maximumHeight,
                                                blendedHeight
                                        )
                                );

                        /*
                         * Don't draw a road through the
                         * landscaped transition for now.
                         */
                        road = false;
                    }
                }

                /*
                 * BUILD TERRAIN COLUMN
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

                    } else if (y > height - 4) {

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
         * Generate the General Store after the terrain.
         */
        EarthBuildingGenerator.generateGuemesStore(
                chunk,
                chunkX,
                chunkZ
        );

        return chunk;
    }

    /*
     * ROAD TERRAIN SMOOTHING
     */
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
                        EarthTerrainLoader
                                .getGuemesElevation(
                                        worldX + offsetX,
                                        worldZ + offsetZ
                                );

                totalElevation += sampleElevation;
                samples++;
            }
        }

        double averageElevation =
                totalElevation / samples;

        return EarthElevation
                .getMinecraftHeight(
                        averageElevation
                );
    }
}
