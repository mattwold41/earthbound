package com.earthbound.roads;


/*
 * ============================================================
 * EARTHBOUND ROAD DATA
 *
 * Stores road mask information.
 *
 * Used by:
 * EarthRoadGenerator
 *
 * ============================================================
 */


public class EarthRoadData {


    public static final int LOCAL_WIDTH = 2;


    private static boolean loaded = false;



    /*
     * Original loader
     */

    public static void load() {


        loaded = true;


        System.out.println(
                "[EarthBound] Road data loaded"
        );

    }




    /*
     * Compatibility loader
     *
     * Used by EarthBound.java
     */

    public static boolean loadGuemesRoadMask() {


        load();


        return true;

    }




    public static boolean isLoaded() {


        return loaded;

    }




    /*
     * Check if location contains road
     */

    public static boolean isRoad(
            int x,
            int z
    ) {


        if (!loaded) {

            return false;

        }


        /*
         * Temporary.
         *
         * Real Guemes road mask
         * will replace this.
         */


        return false;

    }


}
