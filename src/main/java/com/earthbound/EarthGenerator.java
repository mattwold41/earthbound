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
     */

    private static final int ROAD_SMOOTH_RADIUS = 2;

    /*
     * Main road surface.
     *
     * This gives the roads the darker paved appearance
     * we want for the EarthBound road standard.
     */
    private static final Material ROAD_SURFACE =
            Material.GRAY_CONCRETE;

    /*
     * Light edge markings.
     */
    private static final Material ROAD_EDGE =
            Material.WHITE_CONCRETE;


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
                 * =================================================
                 * NATURAL EARTH TERRAIN
                 * =================================================
                 */

                int terrainHeight =
                        naturalHeight;


                /*
                 * =================================================
                 * GUEMES GENERAL STORE TERRAIN BLEND
                 * =================================================
                 *
                 * Keep the approved store at Y=65.
                 *
                 * The surrounding terrain transitions gradually
                 * back into the real USGS terrain.
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
                     * Flat foundation immediately
                     * around the building.
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
                         * Smooth transition between the
                         * store and natural terrain.
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


                        /*
                         * Limit how quickly terrain can
                         * rise or fall around the store.
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
                 * REAL-WORLD ROADS
                 * =================================================
                 *
                 * TIGER/Census centerlines determine WHERE
                 * the roads are located.
                 *
                 * EarthBound determines how those roads look.
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
                 * =================================================
                 * GENERAL STORE ROAD PROTECTION
                 * =================================================
                 *
                 * IMPORTANT:
                 *
                 * The old generator disabled the road throughout
                 * the entire 32-block store blend area.
                 *
                 * That caused the real road beside the General
                 * Store to disappear.
                 *
                 * We now protect ONLY the actual building
                 * foundation.
                 *
                 * Roads are allowed through the surrounding
                 * terrain blend.
                 */

                if (EarthBuildingGenerator
                        .isInsideGuemesStoreFoundation(
                                worldX,
                                worldZ
                        )) {

                    road = false;

                    roadType =
                            EarthRoadData.RoadType.NONE;
                }


                /*
                 * =================================================
                 * ROAD HEIGHT
                 * =================================================
                 *
                 * Roads follow smoothed real terrain rather than
                 * every tiny elevation variation.
                 */

                if (road) {

                    terrainHeight =
                            getSmoothedRoadHeight(
                                    worldX,
                                    worldZ
                            );


                    /*
                     * When a road is inside the General Store
                     * terrain transition, keep its height compatible
                     * with the store's graded terrain.
                     *
                     * This prevents the restored road from cutting
                     * a deep trench or forming a tall wall beside
                     * the store.
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
                 * ROAD EDGE MARKINGS
                 * =================================================
                 *
                 * Blocks along the outside of the road become
                 * light-colored edge markings.
                 */

                boolean roadEdge =
                        road
                                && isRoadEdge(
                                        worldX,
                                        worldZ
                                );


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
                        road,
                        roadEdge
                );
            }
        }


        /*
         * ========================================================
         * GUEMES GENERAL STORE
         * ========================================================
         *
         * Keep the approved store.
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
         * AREA A - RESIDENTIAL TEST BLOCK
         * ========================================================
         *
         * Keep the existing test homes.
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
     * LAND TERRAIN
     * ============================================================
     */

    private void generateLandColumn(
            ChunkData chunkData,
            int localX,
            int localZ,
            int surfaceY,
            boolean road,
            boolean roadEdge
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
         * Dirt layer.
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

            if (roadEdge) {

                chunkData.setBlock(
                        localX,
                        surfaceY,
                        localZ,
                        ROAD_EDGE
                );

            } else {

                chunkData.setBlock(
                        localX,
                        surfaceY,
                        localZ,
                        ROAD_SURFACE
                );
            }

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
         * Water up to sea level.
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
     *
     * Average the terrain surrounding each road block.
     *
     * This reduces tiny bumps in the USGS terrain and gives
     * vehicles/players a smoother road surface.
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
     *
     * Reproduce the store's approved terrain transition for
     * roads that pass through the blend area.
     *
     * This keeps the road connected to the surrounding terrain
     * instead of allowing road smoothing to ignore the store
     * grading completely.
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
         * Blend the regular smoothed road height with
         * the approved store terrain height.
         */

        double storeInfluence =
                1.0 - blend;

        int compatibleHeight =
                (int) Math.round(
                        roadHeight
                                * (1.0 - storeInfluence)
                                + storeTerrainHeight
                                * storeInfluence
                );

        return compatibleHeight;
    }


    /*
     * ============================================================
     * ROAD EDGE DETECTION
     * ============================================================
     *
     * A road block is considered an outside edge if at least
     * one neighboring block is not part of the road.
     *
     * This creates the light edge lines while keeping the
     * center of the road dark.
     */

    private boolean isRoadEdge(
            int worldX,
            int worldZ
    ) {

        boolean north =
                isRoadAllowedAt(
                        worldX,
                        worldZ - 1
                );

        boolean south =
                isRoadAllowedAt(
                        worldX,
                        worldZ + 1
                );

        boolean west =
                isRoadAllowedAt(
                        worldX - 1,
                        worldZ
                );

        boolean east =
                isRoadAllowedAt(
                        worldX + 1,
                        worldZ
                );

        return !north
                || !south
                || !west
                || !east;
    }


    /*
     * ============================================================
     * ROAD LOCATION CHECK
     * ============================================================
     *
     * Uses the real road data while protecting the actual
     * General Store foundation.
     */

    private boolean isRoadAllowedAt(
            int worldX,
            int worldZ
    ) {

        if (EarthBuildingGenerator
                .isInsideGuemesStoreFoundation(
                        worldX,
                        worldZ
                )) {

            return false;
        }

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

        return EarthRoadData.isRoad(
                latitude,
                longitude
        );
    }
}
