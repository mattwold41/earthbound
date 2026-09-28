package com.earthbound;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import java.util.Random;

public class EarthGenerator extends ChunkGenerator {

    private static final int ROAD_SMOOTH_RADIUS = 2;

    /*
     * Wider transition around the General Store.
     *
     * The old version used 14 blocks.
     * This version uses 32 blocks so the terrain has
     * much more room to transition away from Y=65.
     */
    private static final double STORE_BLEND_DISTANCE = 32.0;

    /*
     * Controls how quickly the terrain is allowed to rise
     * or fall as it leaves the store area.
     *
     * 4 blocks horizontally for each 1 block vertically
     * creates a much gentler Minecraft slope.
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
                        EarthCoordinates.minecraftToLatitude(worldZ);

                double longitude =
                        EarthCoordinates.minecraftToLongitude(worldX);

                boolean water =
                        EarthWaterData.isWater(
                                latitude,
                                longitude
                        );

                /*
                 * Keep real Guemes water unchanged.
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
                 * Real USGS elevation.
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
                 * Real road detection.
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
                 * GENERAL STORE TERRAIN
                 *
                 * The store itself stays at Y=65.
                 *
                 * Immediately around the building we keep
                 * the ground flat.
                 *
                 * Then the ground slowly transitions back
                 * toward the real USGS terrain.
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
                     * Keep the actual store foundation flat.
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
                         * Smooth transition from the store
                         * platform toward natural terrain.
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
                         * Smoothstep.
                         *
                         * This removes the abrupt beginning
                         * and ending of the transition.
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
                         * Even if the USGS terrain rises
                         * quickly nearby, do not allow the
                         * store grounds to immediately turn
                         * into a giant staircase.
                         *
                         * Every 4 horizontal blocks permits
                         * approximately 1 vertical block.
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

                        road = false;
                    }
                }

                /*
                 * Build terrain column.
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
         *
         * EarthBuildingGenerator now controls:
         *
         * - Y=65
         * - approved orientation
         * - entrance
         * - porch
         * - roof
         */
        EarthBuildingGenerator.generateGuemesStore(
                chunk,
                chunkX,
                chunkZ
        );

        return chunk;
    }

    /*
     * Existing road smoothing system.
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
