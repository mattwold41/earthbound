package com.earthbound;


import com.earthbound.water.EarthWaterData;
import com.earthbound.terrain.EarthTerrainLoader;

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
     * Check if location is coastline/water edge
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
     * Get terrain height near coast
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
