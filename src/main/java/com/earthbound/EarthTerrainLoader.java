package com.earthbound;


public class EarthTerrainLoader {


    /*
     * Loads the Guemes Island terrain data.
     *
     * Later this will load larger terrain
     * files automatically as EarthBound expands.
     */
    public static void loadGuemesTerrainTile() {


        System.out.println(
                "=== Loading Guemes Island terrain ==="
        );


    }



    /*
     * Checks if Guemes terrain data exists.
     */
    public static boolean isGuemesTerrainLoaded() {


        return true;


    }



    /*
     * Gets elevation for the terrain generator.
     *
     * EarthGenerator calls this method.
     *
     * Flow:
     *
     * EarthGenerator
     *       |
     *       v
     * EarthTerrainLoader
     *       |
     *       v
     * GuemesElevation
     *
     */
    public static double getGuemesElevation(
            double x,
            double z
    ) {


        return GuemesElevation.getElevation(
                (int) x,
                (int) z
        );


    }


}
