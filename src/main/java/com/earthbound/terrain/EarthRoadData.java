package com.earthbound.roads;


/*
 * ============================================================
 * EARTHBOUND ROAD DATA
 *
 * Stores road mask information.
 *
 * Loaded from:
 * - GIS road data
 * - road masks
 *
 * Used by:
 * EarthRoadGenerator
 *
 * ============================================================
 */


public class EarthRoadData {


    private static boolean loaded = false;



    /*
     * Load road data
     */

    public static void load() {


        /*
         * Future:
         *
         * Load TIGER/road centerlines
         * Convert to road mask
         *
         */


        loaded = true;


        System.out.println(
                "[EarthBound] Road data loaded"
        );

    }



    public static boolean isLoaded() {

        return loaded;

    }



    /*
     * Check if a Minecraft location
     * contains a road.
     */

    public static boolean isRoad(
            int x,
            int z
    ) {


        /*
         * Temporary testing logic.
         *
         * Replace with real road mask.
         */


        if (!loaded) {

            return false;

        }


        return false;

    }


}
