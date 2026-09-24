package com.earthbound;

public class EarthCoordinates {

    // Guemes Island reference point
    private static final double START_LAT = 48.5500;
    private static final double START_LON = -122.5800;

    // Minecraft scale
    // 1 block = 2 meters
    private static final double METERS_PER_BLOCK = 2.0;

    public static double getLatitude(int x, int z) {

        double metersNorth = -z * METERS_PER_BLOCK;

        double latitudeChange = metersNorth / 111320.0;

        return START_LAT + latitudeChange;
    }


    public static double getLongitude(int x, int z) {

        double metersEast = x * METERS_PER_BLOCK;

        double longitudeChange =
                metersEast /
                (111320.0 * Math.cos(Math.toRadians(START_LAT)));

        return START_LON + longitudeChange;
    }
}
