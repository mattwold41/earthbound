package com.earthbound;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import java.util.Random;

public class EarthGenerator extends ChunkGenerator {

    private static final int ROAD_SMOOTH_RADIUS = 2;

    // How far outside the store footprint terrain gradually
    // returns to the real USGS terrain height.
    private static final double STORE_BLEND_DISTANCE = 14.0;

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
                 * Keep real water generation unchanged.
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
                 * Real USGS terrain elevation.
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
                 * Underneath the actual building we use
                 * one stable foundation height.
                 *
                 * Outside the building we gradually blend
                 * back into the real terrain.
                 *
                 * This removes the giant rectangular cliff
                 * that the previous generator created.
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
                     * Directly beneath the store and its
                     * immediate foundation, keep it flat.
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
                         * 0.0 = store height
                         * 1.0 = completely natural terrain
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
                         * Smoothstep produces a softer,
                         * more natural transition than
                         * straight linear interpolation.
                         */
                        blend =
                                blend
                                        * blend
                                        * (3.0
                                        - 2.0 * blend);

                        height =
                                (int) Math.round(
                                        storeHeight
                                                * (1.0 - blend)
                                                + naturalHeight
                                                * blend
                                );

                        /*
                         * Don't draw a road through the
                         * landscaped store transition.
                         */
                        road = false;
                    }
                }

                /*
                 * Generate the terrain column.
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
         * Generate the rotated General Store after
         * the terrain underneath it is complete.
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
