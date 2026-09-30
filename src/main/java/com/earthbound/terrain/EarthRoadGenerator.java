package com.earthbound.roads;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator.ChunkData;


/*
 * ============================================================
 * EARTHBOUND ROAD GENERATOR
 *
 * Handles:
 *
 * - Road detection
 * - Road height
 * - Road blocks
 * - Road information
 *
 * ============================================================
 */


public class EarthRoadGenerator {


    private static final Material ROAD_SURFACE =
            Material.POLISHED_BLACKSTONE_BRICKS;


    private static final Material ROAD_BORDER =
            Material.STONE_BRICKS;



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


        if (!EarthRoadData.isRoad(
                x,
                z
        )) {


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
            double height
    ) {


        int roadHeight =
                quantizeToHalfBlock(
                        height
                );



        chunkData.setBlock(
                x,
                roadHeight,
                z,
                ROAD_SURFACE
        );


    }




    /*
     * ============================================================
     * ROAD HEIGHT
     * ============================================================
     */


    public static double getSmoothedRoadHeightPrecise(
            int x,
            int z
    ) {


        /*
         * Later:
         *
         * use terrain elevation
         * and smooth road slopes
         *
         */


        return 63;

    }




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




    public static int quantizeToHalfBlock(
            double height
    ) {


        return (int)
                Math.round(
                        height
                );

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
