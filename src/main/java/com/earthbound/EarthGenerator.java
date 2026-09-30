package com.earthbound;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;
import org.bukkit.generator.BlockPopulator;

import java.util.Random;
import java.util.List;


public class EarthGenerator extends ChunkGenerator {


    /*
     * ============================================================
     * EARTHBOUND ROAD SETTINGS
     * ============================================================
     */

    private static final int ROAD_SMOOTH_RADIUS = 3;

    private static final double ROAD_BORDER_WIDTH = 1.0;


    private static final Material ROAD_SURFACE =
            Material.POLISHED_BLACKSTONE_BRICKS;

    private static final Material ROAD_SURFACE_SLAB =
            Material.POLISHED_BLACKSTONE_BRICK_SLAB;

    private static final Material ROAD_BORDER =
            Material.STONE_BRICKS;

    private static final Material ROAD_BORDER_SLAB =
            Material.STONE_BRICK_SLAB;


    private static final double HALF_BLOCK = 0.5;


    private static final double STORE_BLEND_DISTANCE = 32.0;

    private static final double STORE_SLOPE_RUN = 4.0;



    public EarthGenerator() {


        System.out.println(
                "=== EARTHBOUND REAL TERRAIN GENERATOR ACTIVE ==="
        );


        System.out.println(
                "[EarthBound] Road design: "
                        + "Polished Blackstone Bricks + Stone Brick borders"
        );


        System.out.println(
                "[EarthBound] Half-block road slope transitions enabled."
        );


        System.out.println(
                "[EarthBound] Continuous Stone Brick road borders enabled."
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



                    if (EarthBuildingGenerator
                            .isInsideGuemesStoreFoundation(
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
                 * EARTHBOUND ROAD CLASSIFICATION
                 * =================================================
                 */


                RoadInfo roadInfo =
                        getRoadInfo(
                                worldX,
                                worldZ,
                                latitude,
                                longitude
                        );



                /*
                 * =================================================
                 * GENERAL STORE ROAD PROTECTION
                 * =================================================
                 */


                if (EarthBuildingGenerator
                        .isInsideGuemesStoreFoundation(
                                worldX,
                                worldZ
                        )) {


                    roadInfo =
                            RoadInfo.none();

                }



                /*
                 * =================================================
                 * ROAD HEIGHT
                 * =================================================
                 */


                if (roadInfo.isRoad()) {


                    double roadHeight =
                            getSmoothedRoadHeightPrecise(
                                    worldX,
                                    worldZ
                            );



                    if (EarthBuildingGenerator
                            .isInsideGuemesStoreBlendArea(
                                    worldX,
                                    worldZ
                            )) {


                        roadHeight =
                                getStoreCompatibleRoadHeightPrecise(
                                        worldX,
                                        worldZ,
                                        roadHeight,
                                        naturalHeight
                                );

                    }



                    roadHeight =
                            quantizeToHalfBlock(
                                    roadHeight
                            );



                    generateRoadColumn(
                            chunkData,
                            localX,
                            localZ,
                            roadHeight,
                            roadInfo.surface,
                            roadInfo.border
                    );


                } else {


                    generateNaturalLandColumn(
                            chunkData,
                            localX,
                            localZ,
                            terrainHeight
                    );

                }

            }

        }
               /*
         * ========================================================
         * GUEMES GENERAL STORE
         * ========================================================
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
     * EARTHBOUND VEGETATION SYSTEM
     *
     * Adds trees and plants to newly generated chunks.
     *
     * Guemes Island regional vegetation:
     *
     * Spruce = Douglas Fir / Cedar
     * Oak = Maple / Alder
     *
     * ============================================================
     */


    @Override
    public List<BlockPopulator> getDefaultPopulators(
            World world
    ) {


        return List.of(
                new EarthVegetationPopulator()
        );


    }

}
