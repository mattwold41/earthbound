package com.earthbound;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import java.util.Random;

public class EarthGenerator extends ChunkGenerator {

    private static final int ROAD_SMOOTH_RADIUS = 2;

    /*
     * Approved General Store terrain settings.
     */
    private static final double STORE_BLEND_DISTANCE = 32.0;
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

        ChunkData chunkData =
                createChunkData(world);

        int chunkMinX =
                chunkX << 4;

        int chunkMinZ =
                chunkZ << 4;


        /*
         * ========================================================
         * BASE EARTH TERRAIN
         * ========================================================
         */

        for (int localX = 0;
             localX < 16;
             localX++) {

            for (int localZ = 0;
                 localZ < 16;
                 localZ++) {

                int worldX =
                        chunkMinX + localX;

                int worldZ =
                        chunkMinZ + localZ;


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
                 * Real USGS elevation.
                 */
                double elevationMeters =
                        EarthTerrainLoader.getGuemesElevation(
                                latitude,
                                longitude
                        );


                int naturalHeight =
                        EarthElevation.getMinecraftHeight(
                                elevationMeters
                        );


                /*
                 * =================================================
                 * WATER
                 * =================================================
                 */

                boolean water =
                        EarthWaterData.isWater(
                                latitude,
                                longitude
                        );


                if (water) {

                    generateWaterColumn(
                            chunkData,
                            localX,
                            localZ
                    );

                    continue;
                }


                /*
                 * =================================================
                 * NATURAL EARTH TERRAIN
                 * =================================================
                 *
                 * Keep the real USGS-generated land.
                 *
                 * Area A does NOT raise, flatten, compress,
                 * or grade this terrain.
                 */

                int terrainHeight =
                        naturalHeight;


                /*
                 * =================================================
                 * GENERAL STORE TERRAIN BLEND
                 * =================================================
                 *
                 * Keep the existing approved General Store system.
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
                     * Approved flat foundation.
                     */
                    if (EarthBuildingGenerator
                            .isInsideGuemesStoreFoundation(
                                    worldX,
                                    worldZ
                            )) {

                        terrainHeight =
                                storeHeight;

                    } else {

                        /*
                         * Existing approved store transition.
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


                        double smoothBlend =
                                blend
                                        * blend
                                        * (3.0
                                        - 2.0
                                        * blend);


                        double blendedHeight =
                                storeHeight
                                        + (naturalHeight
                                        - storeHeight)
                                        * smoothBlend;


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


                        terrainHeight =
                                (int) Math.round(
                                        blendedHeight
                                );


                        terrainHeight =
                                Math.max(
                                        minimumHeight,
                                        Math.min(
                                                maximumHeight,
                                                terrainHeight
                                        )
                                );
                    }
                }


                /*
                 * =================================================
                 * REAL ROADS
                 * =================================================
                 */

                boolean road =
                        EarthRoadData.isRoad(
                                latitude,
                                longitude
                        );


                /*
                 * Protect the General Store area from
                 * real-road generation.
                 */
                if (EarthBuildingGenerator
                        .isInsideGuemesStoreBlendArea(
                                worldX,
                                worldZ
                        )) {

                    road = false;
                }


                /*
                 * Real roads follow their existing
                 * EarthBound road smoothing system.
                 */
                if (road) {

                    terrainHeight =
                            getSmoothedRoadHeight(
                                    worldX,
                                    worldZ
                            );
                }


                /*
                 * Build terrain.
                 */
                generateLandColumn(
                        chunkData,
                        localX,
                        localZ,
                        terrainHeight,
                        road
                );
            }
        }


        /*
         * ========================================================
         * GUEMES GENERAL STORE
         * ========================================================
         *
         * Existing approved building:
         *
         * Y = 65
         * Front = WEST
         *
         * Do not rotate.
         */

        EarthBuildingGenerator.generateGuemesStore(
                chunkData,
                chunkX,
                chunkZ
        );


        /*
         * ========================================================
         * AREA A - RESIDENTIAL TEST BLOCK
         * ========================================================
         *
         * Area A now sits directly on the existing
         * natural USGS-generated land.
         *
         * It does NOT modify the terrain underneath it.
         */

        EarthResidentialGenerator.generate(
                chunkData,
                chunkX,
                chunkZ
        );


        /*
         * ========================================================
         * GUEMES ISLAND DEVELOPMENT CONTROLLER
         * ========================================================
         *
         * This controls future island-wide development while
         * preserving the approved General Store and Area A.
         *
         * Island-wide homes are currently disabled inside
         * EarthGuemesGenerator, so connecting the controller here
         * does not add experimental houses yet.
         */

        EarthGuemesGenerator.generate(
                chunkData,
                chunkX,
                chunkZ
        );


        return chunkData;
    }


    /*
     * ============================================================
     * LAND TERRAIN
     * ============================================================
     */

    private void generateLandColumn(
            ChunkData chunkData,
            int localX,
            int localZ,
            int surfaceY,
            boolean road
    ) {

        int minHeight =
                chunkData.getMinHeight();


        /*
         * Stone.
         */
        for (int y = minHeight;
             y < surfaceY - 3;
             y++) {

            chunkData.setBlock(
                    localX,
                    y,
                    localZ,
                    Material.STONE
            );
        }


        /*
         * Dirt.
         */
        for (int y =
             Math.max(
                     minHeight,
                     surfaceY - 3
             );
             y < surfaceY;
             y++) {

            chunkData.setBlock(
                    localX,
                    y,
                    localZ,
                    Material.DIRT
            );
        }


        /*
         * Surface.
         */
        if (road) {

            chunkData.setBlock(
                    localX,
                    surfaceY,
                    localZ,
                    Material.GRAY_CONCRETE
            );

        } else {

            chunkData.setBlock(
                    localX,
                    surfaceY,
                    localZ,
                    Material.GRASS_BLOCK
            );
        }
    }


    /*
     * ============================================================
     * WATER
     * ============================================================
     */

    private void generateWaterColumn(
            ChunkData chunkData,
            int localX,
            int localZ
    ) {

        int seaLevel =
                EarthWaterData.SEA_LEVEL;


        int seabed =
                seaLevel - 8;


        int minHeight =
                chunkData.getMinHeight();


        /*
         * Stone below seabed.
         */
        for (int y = minHeight;
             y < seabed - 3;
             y++) {

            chunkData.setBlock(
                    localX,
                    y,
                    localZ,
                    Material.STONE
            );
        }


        /*
         * Sand seabed.
         */
        for (int y =
             Math.max(
                     minHeight,
                     seabed - 3
             );
             y <= seabed;
             y++) {

            chunkData.setBlock(
                    localX,
                    y,
                    localZ,
                    Material.SAND
            );
        }


        /*
         * Water up to Y=63.
         */
        for (int y =
             seabed + 1;
             y <= seaLevel;
             y++) {

            chunkData.setBlock(
                    localX,
                    y,
                    localZ,
                    Material.WATER
            );
        }
    }


    /*
     * ============================================================
     * ROAD HEIGHT SMOOTHING
     * ============================================================
     */

    private int getSmoothedRoadHeight(
            int worldX,
            int worldZ
    ) {

        double totalHeight =
                0.0;

        int samples =
                0;


        for (int offsetX =
             -ROAD_SMOOTH_RADIUS;
             offsetX <= ROAD_SMOOTH_RADIUS;
             offsetX++) {

            for (int offsetZ =
                 -ROAD_SMOOTH_RADIUS;
                 offsetZ <= ROAD_SMOOTH_RADIUS;
                 offsetZ++) {

                int sampleX =
                        worldX + offsetX;

                int sampleZ =
                        worldZ + offsetZ;


                double latitude =
                        EarthCoordinates.getLatitude(
                                sampleX,
                                sampleZ
                        );


                double longitude =
                        EarthCoordinates.getLongitude(
                                sampleX,
                                sampleZ
                        );


                double elevationMeters =
                        EarthTerrainLoader
                                .getGuemesElevation(
                                        latitude,
                                        longitude
                                );


                int height =
                        EarthElevation
                                .getMinecraftHeight(
                                        elevationMeters
                                );


                totalHeight +=
                        height;

                samples++;
            }
        }


        if (samples == 0) {

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


            double elevationMeters =
                    EarthTerrainLoader
                            .getGuemesElevation(
                                    latitude,
                                    longitude
                            );


            return EarthElevation
                    .getMinecraftHeight(
                            elevationMeters
                    );
        }


        return (int) Math.round(
                totalHeight / samples
        );
    }
}
