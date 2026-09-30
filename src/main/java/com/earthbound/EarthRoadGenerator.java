package com.earthbound;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator.ChunkData;


/*
 * ============================================================
 * EARTHBOUND ROAD GENERATOR
 *
 * Handles:
 *
 * - Real road detection
 * - Road height smoothing
 * - Road blocks
 * - Road borders
 * - Road information
 *
 * ============================================================
 */


public class EarthRoadGenerator {


    private static final Material ROAD_SURFACE =
            Material.POLISHED_BLACKSTONE_BRICKS;


    private static final Material ROAD_BORDER =
            Material.STONE_BRICKS;


    private static final int SEA_LEVEL = 63;



    /*
     * ============================================================
     * ROAD LOOKUP
     * ============================================================
     */


    public static RoadInfo getRoadInfo(
            int x,
            int z,
            double latitude,
            double longitude
    ) {


        boolean isRoad =
                EarthRoadData.isRoad(
                        x,
                        z
                );


        if (!isRoad) {


            return RoadInfo.none();

        }



        return new RoadInfo(
                true,
                2
        );

    }




    /*
     * ============================================================
     * ROAD GENERATION
     * ============================================================
     */


    public static void generateRoadColumn(
            ChunkData chunkData,
            int x,
            int z,
            double height,
            Material surface,
            Material border
    ) {


        int roadHeight =
                quantizeToHalfBlock(
                        height
                );



        for (
                int y = SEA_LEVEL;
                y < roadHeight;
                y++
        ) {


            chunkData.setBlock(
                    x,
                    y,
                    z,
                    Material.STONE
            );

        }



        chunkData.setBlock(
                x,
                roadHeight,
                z,
                surface
        );


    }




    /*
     * ============================================================
     * ROAD HEIGHT SMOOTHING
     * ============================================================
     */


    public static double getSmoothedRoadHeightPrecise(
            int x,
            int z
    ) {


        double latitude =
                EarthCoordinates.getLatitude(
                        x,
                        z
                );


        double longitude =
                EarthCoordinates.getLongitude(
                        x,
                        z
                );



        double elevation =
                EarthTerrainLoader.getGuemesElevation(
                        latitude,
                        longitude
                );



        return EarthElevation.getMinecraftHeight(
                elevation
        );

    }




    /*
     * ============================================================
     * STORE COMPATIBLE ROAD HEIGHT
     * ============================================================
     */


    public static double getStoreCompatibleRoadHeightPrecise(
            int x,
            int z,
            double roadHeight,
            int terrainHeight
    ) {


        return Math.max(
                roadHeight,
                terrainHeight
        );

    }




    /*
     * ============================================================
     * HALF BLOCK HEIGHT
     * ============================================================
     */


    public static int quantizeToHalfBlock(
            double height
    ) {


        return (int)
                Math.round(
                        height * 2
                )
                / 2;

    }




    /*
     * ============================================================
     * ROAD INFORMATION
     * ============================================================
     */


    public static class RoadInfo {


        private final boolean road;

        private final int width;



        public RoadInfo(
                boolean road,
                int width
        ) {

            this.road = road;
            this.width = width;

        }



        public boolean isRoad() {

            return road;

        }



        public int getWidth() {

            return width;

        }



        public Material getSurface() {

            return ROAD_SURFACE;

        }



        public Material getBorder() {

            return ROAD_BORDER;

        }



        public static RoadInfo none() {


            return new RoadInfo(
                    false,
                    0
            );

        }


    }

}
