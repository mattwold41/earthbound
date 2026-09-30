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


    public static final int SEA_LEVEL = 63;


    private static boolean loaded = false;



    /*
     * Original loader
     */

    public static void load() {


        loaded = true;


        System.out.println(
                "[EarthBound] Water data loaded"
        );

    }



    /*
     * Compatibility loader
     *
     * Used by EarthBound.java
     */

    public static boolean loadGuemesWaterMask() {


        load();


        return true;

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
         * Temporary.
         *
         * Real Guemes water mask
         * will replace this.
         */


        return false;

    }


}
