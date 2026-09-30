package com.earthbound;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator.ChunkData;


/*
 * ============================================================
 * EARTHBOUND ROAD GENERATOR
 *
 * Handles:
 * - road detection
 * - road heights
 * - road blocks
 * - road information
 *
 * ============================================================
 */

public class EarthRoadGenerator {


    private static final int SEA_LEVEL = 63;



    /*
     * Get road information for a location
     */

    public static RoadInfo getRoadInfo(
            int x,
            int z,
            double latitude,
            double longitude
    ) {


        boolean road =
                EarthRoadData.isRoad(
                        x,
                        z
                );


        if (!road) {

            return RoadInfo.none();

        }



        return new RoadInfo(
                true,
                2
        );

    }




    /*
     * Generate a road column
     */

    public static void generateRoadColumn(
            ChunkData chunkData,
            int x,
            int z,
            int height
    ) {


        int roadHeight =
                quantizeToHalfBlock(
                        height
                );


        for (
                int y = SEA_LEVEL;
                y <= roadHeight;
                y++
        ) {


            if (y == roadHeight) {


                chunkData.setBlock(
                        x,
                        y,
                        z,
                        Material.DIRT_PATH
                );


            } else {


                chunkData.setBlock(
                        x,
                        y,
                        z,
                        Material.STONE
                );

            }

        }


    }




    /*
     * Smooth road elevation
     */

    public static int getSmoothedRoadHeightPrecise(
            int x,
            int z
    ) {


        int height =
                EarthTerrainLoader
                        .getGuemesElevation(
                                x,
                                z
                        );


        return height;

    }




    /*
     * Store compatible road height
     */

    public static int getStoreCompatibleRoadHeightPrecise(
            int x,
            int z,
            double storeHeight,
            int roadHeight
    ) {


        return Math.max(
                roadHeight,
                (int) storeHeight
        );

    }




    /*
     * Round terrain heights
     */

    public static int quantizeToHalfBlock(
            double height
    ) {


        return (int)
                Math.round(
                        height
                );

    }




    /*
     * Road information container
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




        public static RoadInfo none() {


            return new RoadInfo(
                    false,
                    0
            );

        }


    }


}
