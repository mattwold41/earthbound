package com.earthbound.water;


/*
 * ============================================================
 * EARTHBOUND WATER DATA
 *
 * Stores water mask information.
 *
 * Used by:
 * EarthWaterGenerator
 *
 * ============================================================
 */


public class EarthWaterData {


    private static boolean loaded = false;



    /*
     * Load water data
     */

    public static void load() {


        /*
         * Future:
         *
         * Load GIS water polygons
         * Convert to water mask
         *
         */


        loaded = true;


        System.out.println(
                "[EarthBound] Water data loaded"
        );

    }




    public static boolean isLoaded() {

        return loaded;

    }




    /*
     * Check if location is water
     */

    public static boolean isWater(
            double latitude,
            double longitude
    ) {


        if (!loaded) {

            return false;

        }



        /*
         * Temporary value.
         *
         * Real Guemes water mask
         * will replace this.
         */


        return false;

    }


}
