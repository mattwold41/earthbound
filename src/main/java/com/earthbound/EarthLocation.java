package com.earthbound;

public class EarthLocation {

    private static final double GUEMES_LAT = 48.56;
    private static final double GUEMES_LON = -122.59;

    // Test radius around Guemes Island
    private static final double TEST_RADIUS_KM = 20.0;

    public static boolean isNearGuemes(double latitude, double longitude) {

        double latDistance = latitude - GUEMES_LAT;
        double lonDistance = longitude - GUEMES_LON;

        double distance =
                Math.sqrt(
                    (latDistance * latDistance) +
                    (lonDistance * lonDistance)
                );

        // Rough conversion to kilometers
        double kilometers = distance * 111;

        return kilometers <= TEST_RADIUS_KM;
    }
}
