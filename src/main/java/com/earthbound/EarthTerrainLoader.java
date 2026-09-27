package com.earthbound;

public class EarthTerrainLoader {

    private static boolean loaded = false;


    public static void loadGuemesTerrainTile() {

        if (loaded) {
            return;
        }

        System.out.println("=== Loading Guemes terrain data ===");

        /*
         * Temporary terrain loader.
         * Later this will read USGS elevation files.
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

            return 50.0;

        }


        /*
         * Temporary hills.
         * This confirms elevation generation works.
         */

        double hill =
                Math.sin(latitude * 100)
                *
                Math.cos(longitude * 100)
                *
                40;


        return 50.0 + hill;
    }
}
