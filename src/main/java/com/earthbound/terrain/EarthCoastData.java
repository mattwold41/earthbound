package com.earthbound.terrain;


import com.earthbound.water.EarthWaterData;


/*
 * ============================================================
 * EARTHBOUND COAST DATA
 *
 * Handles coastline checks.
 *
 * Uses:
 *
 * EarthWaterData
 * EarthTerrainLoader
 *
 * ============================================================
 */


public class EarthCoastData {



    /*
     * Check if a location is water/coast
     */

    public static boolean isCoast(
            double latitude,
            double longitude
    ) {


        return EarthWaterData.isWater(
                latitude,
                longitude
        );

    }




    /*
     * Get elevation near coastline
     */

    public static double getCoastElevation(
            double latitude,
            double longitude
    ) {


        return EarthTerrainLoader.getGuemesElevation(
                latitude,
                longitude
        );

    }



}
