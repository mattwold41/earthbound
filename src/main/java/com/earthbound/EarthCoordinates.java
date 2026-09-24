package com.earthbound;

public class EarthCoordinates {

    private static final double SCALE = 2.0;

    // Starting point: Guemes Island, Washington
    private static final double START_LAT = 48.56;
    private static final double START_LON = -122.59;

    public static double getLatitude(double x, double z) {

        double metersNorth = z * SCALE;

        return START_LAT + (metersNorth / 111000.0);
    }

    public static double getLongitude(double x, double z) {

        double metersEast = x * SCALE;

        double longitudeDistance =
                111000.0 * Math.cos(Math.toRadians(START_LAT));

        return START_LON + (metersEast / longitudeDistance);
    }
}
