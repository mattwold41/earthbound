package com.earthbound;

public class EarthTerrainLoader {

    private static boolean loaded = false;

    public static void loadGuemesTerrainTile() {

        System.out.println(
                "=== Loading Guemes terrain data ==="
        );

        /*
         * Temporary elevation test data.
         * We will connect the USGS raster back after
         * the generator compiles correctly.
         */

        loaded = true;
    }


    public static boolean isGuemesTerrainLoaded() {

        return loaded;
    }


    public static Double getGuemesElevation(
            double latitude,
            double longitude) {


        if (!loaded) {

            return null;
        }


        /*
         * Temporary terrain shape test.
         * This gives hills so we can confirm
         * elevation is working.
         */

        double hill =
                Math.sin(latitude * 100)
                *
                Math.cos(longitude * 100)
                *
                50;


        return 50.0 + hill;
    }
}
