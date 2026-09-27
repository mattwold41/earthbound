package com.earthbound;

public class EarthTerrainLoader {


    public static void loadGuemesTerrainTile() {

        System.out.println(
                "=== Loading Guemes Island elevation ==="
        );

    }


    public static boolean isGuemesTerrainLoaded() {

        return true;

    }


    public static double getGuemesElevation(
            double x,
            double z
    ) {

        double latitude =
                EarthCoordinates.getLatitude(
                        (int) x,
                        (int) z
                );


        double longitude =
                EarthCoordinates.getLongitude(
                        (int) x,
                        (int) z
                );


        System.out.println(
                "Earth location: lat="
                + latitude
                + " lon="
                + longitude
        );


        return EarthElevation.getElevation(
                longitude,
                latitude
        );

    }
}
