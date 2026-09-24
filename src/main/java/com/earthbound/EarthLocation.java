package com.earthbound;

public class EarthLocation {

    // Guemes Island Ferry Terminal reference point
    private static final double GUEMES_LAT = 48.5228603;
    private static final double GUEMES_LON = -122.624694;


    // Search radius around Guemes Island
    private static final double TEST_RADIUS_KM = 20.0;


    public static boolean isNearGuemes(
            double latitude,
            double longitude) {


        double latDistance =
                latitude - GUEMES_LAT;

        double lonDistance =
                longitude - GUEMES_LON;


        double distance =
                Math.sqrt(
                        (latDistance * latDistance) +
                        (lonDistance * lonDistance)
                );


        // Convert rough degree distance to kilometers
        double kilometers =
                distance * 111;


        return kilometers <= TEST_RADIUS_KM;
    }


    public static String getRegion(
            double latitude,
            double longitude) {


        if (isNearGuemes(latitude, longitude)) {

            return "Guemes Island, Washington";
        }


        return "Unknown EarthBound Region";
    }
}
