package com.earthbound.terrain;


/*
 * ============================================================
 * EARTHBOUND TERRAIN LOADER
 *
 * Provides terrain height lookup.
 *
 * Uses:
 * EarthTerrainData
 *
 * ============================================================
 */


public class EarthTerrainLoader {


    private static EarthTerrainData terrainData;



    /*
     * Load terrain data
     */

    public static void load(
            EarthTerrainData data
    ) {

        terrainData = data;

    }



    /*
     * Check if terrain is loaded
     */

    public static boolean isLoaded() {


        return terrainData != null;


    }



    /*
     * Get elevation by terrain coordinates
     */

    public static double getElevation(
            int x,
            int z
    ) {


        if (!isLoaded()) {

            return 0;

        }


        return terrainData.getElevation(
                x,
                z
        );

    }



    /*
     * Get Guemes elevation
     *
     * Used by EarthGenerator
     *
     */

    public static double getGuemesElevation(
            double latitude,
            double longitude
    ) {


        if (!isLoaded()) {

            return 0;

        }


        /*
         * Temporary coordinate conversion.
         *
         * We will connect the real
         * Guemes raster mapping here.
         */


        int x =
                (int) Math.round(
                        latitude
                );


        int z =
                (int) Math.round(
                        longitude
                );


        return getElevation(
                x,
                z
        );

    }


}
