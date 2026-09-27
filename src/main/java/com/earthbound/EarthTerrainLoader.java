package com.earthbound;

public class EarthTerrainLoader {

    private static boolean loaded = false;

    // Guemes Island approximate elevation settings
    private static final double SEA_LEVEL = 62.0;

    public static void loadGuemesTerrainTile() {

        System.out.println(
                "=== Loading Guemes Island elevation data ==="
        );

        /*
         * Phase 1:
         * Guemes Island terrain model.
         *
         * Later this will be replaced with
         * USGS elevation raster data.
         */

        loaded = true;
    }


    public static boolean isGuemesTerrainLoaded() {

        return loaded;
    }


    public static double getGuemesElevation(
            double x,
            double z) {


        if (!loaded) {
            return SEA_LEVEL;
        }


        /*
         * Guemes Island terrain shape.
         *
         * Uses real-world style elevation:
         * - shoreline near sea level
         * - rolling island hills
         * - higher center areas
         */


        double distance =
                Math.sqrt(
                        Math.pow(x / 300.0, 2) +
                        Math.pow(z / 300.0, 2)
                );


        double hills =
                Math.sin(x * 0.02)
                *
                Math.cos(z * 0.02)
                *
                12;


        double island =
                Math.max(
                        0,
                        80 - (distance * 25)
                );


        return SEA_LEVEL + island + hills;
    }
}
