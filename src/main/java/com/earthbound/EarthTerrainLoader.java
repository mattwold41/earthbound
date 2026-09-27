package com.earthbound;

public class EarthTerrainLoader {

    private static boolean loaded = false;


    public static void loadGuemesTerrainTile() {

        System.out.println(
                "=== Loading Guemes terrain data ==="
        );

        /*
         * Temporary terrain system.
         * This confirms that elevation is reaching
         * the generator before we reconnect USGS data.
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

            System.out.println(
                    "Terrain data not loaded"
            );

            return null;
        }


        /*
         * Temporary elevation model.
         *
         * Creates hills and valleys so we can
         * verify the terrain generator is using
         * elevation correctly.
         */

        double hill =
                Math.sin(latitude * 100)
                *
                Math.cos(longitude * 100)
                *
                50;


        double elevation =
                50.0 + hill;


        System.out.println(
                "Elevation: "
                + latitude
                + ", "
                + longitude
                + " = "
                + elevation
        );


        return elevation;
    }
}
