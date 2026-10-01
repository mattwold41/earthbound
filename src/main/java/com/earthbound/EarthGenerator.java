package com.earthbound;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import com.earthbound.roads.EarthRoadData;
import com.earthbound.roads.EarthRoadData.RoadType;
import com.earthbound.water.EarthWaterGenerator;


/*
 * ============================================================
 * EARTHBOUND WORLD GENERATOR
 *
 * GUEMES TERRAIN + WATER + ROADS
 *
 * Generates:
 *
 * - Real USGS elevation terrain
 * - Smoothed terrain slopes
 * - Real Guemes hydrography / coastline
 * - Natural water columns
 * - Real TIGERweb road centerlines
 *
 * Road standard:
 *
 * LOCAL:
 * 5-block driving surface
 * + 1-block Stone Brick border on each side
 *
 * HIGHWAY:
 * 8-block driving surface
 * + 1-block Stone Brick border on each side
 *
 * FREEWAY:
 * 10-block driving surface
 * + 1-block Stone Brick border on each side
 *
 * Driving surface:
 * POLISHED_BLACKSTONE_BRICKS
 *
 * Road border:
 * STONE_BRICKS
 *
 * ============================================================
 */

public class EarthGenerator extends ChunkGenerator {


    /*
     * EarthBound currently uses:
     *
     * 1 Minecraft block = 2 real-world meters.
     */

    private static final double
            METERS_PER_BLOCK = 2.0;


    /*
     * One Stone Brick border block
     * on each side of the road.
     */

    private static final double
            ROAD_BORDER_BLOCKS = 1.0;


    public EarthGenerator() {

        System.out.println(
                "=== EARTHBOUND GENERATOR ACTIVE ==="
        );

        System.out.println(
                "[EarthBound] Guemes terrain + water + roads generator loaded"
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


        System.out.println(
                "[EarthBound] GENERATING CHUNK: "
                        + chunkX
                        + ", "
                        + chunkZ
        );


        ChunkData chunkData =
                createChunkData(world);


        /*
         * ========================================================
         * GENERATE CHUNK
         * ========================================================
         */

        for (int localX = 0;
             localX < 16;
             localX++) {

            for (int localZ = 0;
                 localZ < 16;
                 localZ++) {


                /*
                 * Convert chunk-local coordinates into
                 * global Minecraft coordinates.
                 */

                int worldX =
                        (chunkX * 16)
                                + localX;

                int worldZ =
                        (chunkZ * 16)
                                + localZ;


                /*
                 * Convert Minecraft coordinates into
                 * real-world latitude and longitude.
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
                 * =================================================
                 * WATER
                 * =================================================
                 *
                 * Water remains higher priority than roads for now.
                 *
                 * This prevents ordinary TIGER road centerlines
                 * from accidentally filling ocean/water columns.
                 *
                 * Bridges will be restored separately later.
                 */

                if (
                        EarthWaterGenerator.isWater(
                                latitude,
                                longitude
                        )
                ) {

                    EarthWaterGenerator
                            .generateWaterColumn(
                                    chunkData,
                                    localX,
                                    localZ,
                                    latitude,
                                    longitude
                            );

                    continue;
                }


                /*
                 * =================================================
                 * TERRAIN
                 * =================================================
                 */

                int terrainHeight =
                        EarthTerrainGenerator
                                .getTerrainHeight(
                                        latitude,
                                        longitude
                                );


                EarthTerrainGenerator
                        .generateNaturalLandColumn(
                                chunkData,
                                localX,
                                localZ,
                                terrainHeight
                        );


                /*
                 * =================================================
                 * ROADS
                 * =================================================
                 *
                 * Find the nearest real TIGERweb road centerline.
                 */

                RoadType roadType =
                        EarthRoadData
                                .getNearestRoadType(
                                        latitude,
                                        longitude
                                );


                if (roadType
                        == RoadType.NONE) {

                    continue;
                }


                /*
                 * Distance from this real-world point
                 * to the nearest centerline belonging
                 * to this road class.
                 */

                double distanceMeters =
                        EarthRoadData
                                .getDistanceToRoadTypeMeters(
                                        roadType,
                                        latitude,
                                        longitude
                                );


                /*
                 * Convert the selected Minecraft road
                 * width into real-world meters.
                 *
                 * Example:
                 *
                 * Local road:
                 *
                 * 5 blocks wide
                 * x 2 meters/block
                 * = 10 real-world meters wide
                 *
                 * Radius = 5 meters.
                 */

                double drivingRadiusMeters =
                        (
                                roadType.getWidth()
                                        * METERS_PER_BLOCK
                        )
                                / 2.0;


                /*
                 * The border is one additional
                 * Minecraft block on each side.
                 *
                 * At the current 1:2 scale:
                 *
                 * 1 block = 2 meters.
                 */

                double borderRadiusMeters =
                        drivingRadiusMeters
                                + (
                                ROAD_BORDER_BLOCKS
                                        * METERS_PER_BLOCK
                        );


                /*
                 * Outside both the driving surface
                 * and border.
                 */

                if (distanceMeters
                        > borderRadiusMeters) {

                    continue;
                }


                /*
                 * =================================================
                 * ROAD SURFACE HEIGHT
                 * =================================================
                 *
                 * For this first restored road pass,
                 * roads follow the existing terrain.
                 *
                 * Later we can add road grading so roads
                 * cut/fill terrain more realistically.
                 */

                int roadY =
                        terrainHeight;


                /*
                 * =================================================
                 * DRIVING SURFACE
                 * =================================================
                 */

                if (distanceMeters
                        <= drivingRadiusMeters) {

                    chunkData.setBlock(
                            localX,
                            roadY,
                            localZ,
                            Material.POLISHED_BLACKSTONE_BRICKS
                    );

                    continue;
                }


                /*
                 * =================================================
                 * ROAD BORDER / SHOULDER
                 * =================================================
                 */

                chunkData.setBlock(
                        localX,
                        roadY,
                        localZ,
                        Material.STONE_BRICKS
                );
            }
        }


        System.out.println(
                "[EarthBound] FINISHED CHUNK: "
                        + chunkX
                        + ", "
                        + chunkZ
        );


        return chunkData;
    }
}
