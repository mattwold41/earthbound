package com.earthbound;

public class EarthTerrainLoader {

    private static boolean loaded = false;


    public static void loadGuemesTerrainTile() {

        if (loaded) {
            return;
        }

        System.out.println(
                "=== Loading Guemes terrain data ==="
        );

        /*
         * Temporary terrain loader.
         * This will later be replaced with
         * real USGS elevation data.
         */

        loaded = true;
    }


    public static boolean isGuemesTerrainLoaded() {

        return loaded;
    }


    public static double getGuemesElevation(
            double latitude,
            double longitude) {


        if (!loaded) {

            return 45.0;

        }


        /*
         * Smooth temporary terrain.
         *
         * Lower values = flatter land
         * Higher values = bigger hills
         *
         * Designed to look more like
         * San Juan Islands instead of spikes.
         */


        double largeHills =
                Math.sin(latitude * 0.5)
                *
                Math.cos(longitude * 0.5)
                *
                15;


        double smallHills =
                Math.sin(latitude * 2.0)
                *
                Math.cos(longitude * 2.0)
                *
                3;


        double baseElevation =
                45.0;


        double elevation =
                baseElevation
                + largeHills
                + smallHills;


        return elevation;
    }
}
