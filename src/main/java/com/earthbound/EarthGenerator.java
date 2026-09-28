package com.earthbound;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import java.util.Random;

public class EarthGenerator extends ChunkGenerator {

    private static final int ROAD_SMOOTH_RADIUS = 2;

    /*
     * Keep the currently approved General Store
     * terrain blending settings.
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


                /*
                 * Convert Minecraft coordinates
                 * to real Earth coordinates.
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
                 * Read real USGS elevation.
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
                 * GENERAL STORE TERRAIN BLEND
                 * =================================================
                 */

                int terrainHeight =
                        naturalHeight;


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
                     * Keep the General Store foundation
                     * completely flat at the approved Y=65.
                     */
                    if (EarthBuildingGenerator
                            .isInsideGuemesStoreFoundationArea(
                                    worldX,
                                    worldZ
                            )) {

                        terrainHeight =
                                storeHeight;

                    } else {

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
                         * Smoothstep transition.
                         */
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


                        /*
                         * Keep the existing approved
                         * slope protection.
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
                 * Don't allow a road to cut through
                 * the General Store blend/foundation.
                 */
                if (EarthBuildingGenerator
                        .isInsideGuemesStoreBlendArea(
                                worldX,
                                worldZ
                        )) {

                    road = false;
                }


                if (road) {

                    terrainHeight =
                            getSmoothedRoadHeight(
                                    worldX,
                                    worldZ
                            );
                }


                /*
                 * Generate the actual terrain column.
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
         * Do not change:
         *
         * Ground Y = 65
         * Current orientation approved.
         */
        EarthBuildingGenerator.generate(
                chunkData,
                chunkX,
                chunkZ
        );


        /*
         * ========================================================
         * AREA A - RESIDENTIAL TEST BLOCK
         * ========================================================
         *
         * This calls the separate:
         *
         * EarthResidentialGenerator.java
         *
         * That file currently creates:
         *
         * - test neighborhood road
         * - sidewalks
         * - eight property plots
         * - yellow property markers
         */
        EarthResidentialGenerator.generate(
                chunkData,
                chunkX,
                chunkZ
        );


        return chunkData;
    }


    /*
     * ============================================================
     * LAND
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
         * Stone below the terrain.
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
         * Dirt near the surface.
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
         * Surface block.
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
                EarthWaterData.getSeaLevel();

        int seabed =
                seaLevel - 8;

        int minHeight =
                chunkData.getMinHeight();


        /*
         * Stone underneath seabed.
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
         * Sandy seabed.
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
         * Ocean water.
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
