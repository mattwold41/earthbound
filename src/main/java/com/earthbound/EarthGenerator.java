package com.earthbound;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import java.util.Random;

public class EarthGenerator extends ChunkGenerator {

    /*
     * ============================================================
     * EARTHBOUND ROAD SETTINGS
     * ============================================================
     *
     * Standard local-road design:
     *
     * BORDER | ROAD | ROAD | ROAD | BORDER
     *
     * Center driving surface = 3 blocks
     * Border = 1 block on each side
     * Total = approximately 5 blocks
     */

    private static final int ROAD_SMOOTH_RADIUS = 2;

    /*
     * Distance from the real-world road centerline.
     *
     * Minecraft scale in this test region is close enough
     * to use these meter distances as block-width targets.
     */

    private static final double ROAD_SURFACE_RADIUS = 1.5;

    private static final double ROAD_BORDER_RADIUS = 2.5;


    /*
     * Three-block driving surface.
     */

    private static final Material ROAD_SURFACE =
            Material.GRAY_CONCRETE;


    /*
     * Gray border on both sides.
     *
     * Smooth stone keeps the border gray while making it
     * visibly different from the driving surface.
     */

    private static final Material ROAD_BORDER =
            Material.SMOOTH_STONE;


    /*
     * ============================================================
     * GENERAL STORE TERRAIN SETTINGS
     * ============================================================
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
         * GENERATE EARTH TERRAIN
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
                 * into real Earth coordinates.
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
                 * REAL USGS ELEVATION
                 * =================================================
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
                 * Start with natural USGS terrain.
                 */

                int terrainHeight =
                        naturalHeight;


                /*
                 * =================================================
                 * GENERAL STORE TERRAIN BLEND
                 * =================================================
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

                        terrainHeight =
                                storeHeight;

                    } else {

                        /*
                         * Smooth terrain transition around store.
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
                 * NEW EARTHBOUND ROAD DESIGN
                 * =================================================
                 *
                 * Measure distance from this block to the nearest
                 * real Census/TIGER road centerline.
                 */

                double roadDistance =
                        EarthRoadData
                                .getDistanceToNearestRoadMeters(
                                        latitude,
                                        longitude
                                );


                /*
                 * Three-block-wide center driving surface.
                 */

                boolean roadSurface =
                        roadDistance
                                <= ROAD_SURFACE_RADIUS;


                /*
                 * One-block gray border outside the road surface.
                 */

                boolean roadBorder =
                        roadDistance
                                > ROAD_SURFACE_RADIUS
                                && roadDistance
                                <= ROAD_BORDER_RADIUS;


                boolean road =
                        roadSurface
                                || roadBorder;


                /*
                 * =================================================
                 * GENERAL STORE ROAD PROTECTION
                 * =================================================
                 *
                 * Roads are allowed through the large terrain
                 * blend around the store.
                 *
                 * Only the actual store foundation is protected.
                 */

                if (EarthBuildingGenerator
                        .isInsideGuemesStoreFoundation(
                                worldX,
                                worldZ
                        )) {

                    road = false;
                    roadSurface = false;
                    roadBorder = false;
                }


                /*
                 * =================================================
                 * ROAD HEIGHT
                 * =================================================
                 */

                if (road) {

                    terrainHeight =
                            getSmoothedRoadHeight(
                                    worldX,
                                    worldZ
                            );


                    /*
                     * Keep roads compatible with the General Store
                     * terrain transition.
                     */

                    if (EarthBuildingGenerator
                            .isInsideGuemesStoreBlendArea(
                                    worldX,
                                    worldZ
                            )) {

                        terrainHeight =
                                getStoreCompatibleRoadHeight(
                                        worldX,
                                        worldZ,
                                        terrainHeight,
                                        naturalHeight
                                );
                    }
                }


                /*
                 * =================================================
                 * BUILD TERRAIN COLUMN
                 * =================================================
                 */

                generateLandColumn(
                        chunkData,
                        localX,
                        localZ,
                        terrainHeight,
                        roadSurface,
                        roadBorder
                );
            }
        }


        /*
         * ========================================================
         * GUEMES GENERAL STORE
         * ========================================================
         *
         * Keep existing approved store.
         *
         * Ground Y = 65
         * Front = WEST
         */

        EarthBuildingGenerator.generateGuemesStore(
                chunkData,
                chunkX,
                chunkZ
        );


        /*
         * ========================================================
         * RESIDENTIAL TEST AREA
         * ========================================================
         */

        EarthResidentialGenerator.generate(
                chunkData,
                chunkX,
                chunkZ
        );


        /*
         * ========================================================
         * GUEMES DEVELOPMENT CONTROLLER
         * ========================================================
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
     * LAND GENERATION
     * ============================================================
     */

    private void generateLandColumn(
            ChunkData chunkData,
            int localX,
            int localZ,
            int surfaceY,
            boolean roadSurface,
            boolean roadBorder
    ) {

        int minHeight =
                chunkData.getMinHeight();


        /*
         * Stone base.
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
         * Dirt underneath surface.
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
         * ========================================================
         * SURFACE BLOCK
         * ========================================================
         */

        if (roadSurface) {

            /*
             * Three-block-wide paved road.
             */

            chunkData.setBlock(
                    localX,
                    surfaceY,
                    localZ,
                    ROAD_SURFACE
            );

        } else if (roadBorder) {

            /*
             * One-block gray border.
             */

            chunkData.setBlock(
                    localX,
                    surfaceY,
                    localZ,
                    ROAD_BORDER
            );

        } else {

            /*
             * Normal land.
             */

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
         * Water to sea level.
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


    /*
     * ============================================================
     * STORE-COMPATIBLE ROAD HEIGHT
     * ============================================================
     */

    private int getStoreCompatibleRoadHeight(
            int worldX,
            int worldZ,
            int roadHeight,
            int naturalHeight
    ) {

        int storeHeight =
                EarthBuildingGenerator
                        .getGuemesStoreGroundY();


        double distance =
                EarthBuildingGenerator
                        .getDistanceFromGuemesStore(
                                worldX,
                                worldZ
                        );


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


        double blendedTerrain =
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


        int storeTerrainHeight =
                (int) Math.round(
                        blendedTerrain
                );


        storeTerrainHeight =
                Math.max(
                        minimumHeight,
                        Math.min(
                                maximumHeight,
                                storeTerrainHeight
                        )
                );


        /*
         * Gradually blend the road into the approved
         * General Store terrain.
         */

        double storeInfluence =
                1.0 - blend;


        return (int) Math.round(

                roadHeight
                        * (1.0 - storeInfluence)

                        + storeTerrainHeight
                        * storeInfluence
        );
    }
}
